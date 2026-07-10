package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Shape
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.PathIterator
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.roundToInt

internal object MapViewerIconLoader {
    fun load(file: File): Bitmap32? =
        when (file.extension.lowercase()) {
            "svg" -> loadSvg(file)
            else -> loadRaster(file)
        }

    fun load(bytes: ByteArray, fileName: String): Bitmap32? =
        when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "svg" -> loadSvg(bytes)
            else -> loadRaster(bytes)
        }

    private fun loadRaster(file: File): Bitmap32? {
        val image = ImageIO.read(file) ?: return null
        return image.toBitmap32().trimTransparentBounds().premultipliedIfRequired()
    }

    private fun loadRaster(bytes: ByteArray): Bitmap32? {
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
        return image.toBitmap32().trimTransparentBounds().premultipliedIfRequired()
    }

    private fun loadSvg(file: File): Bitmap32? = runCatching {
        renderSvg(file.readText())
    }.getOrNull()

    private fun loadSvg(bytes: ByteArray): Bitmap32? = runCatching {
        renderSvg(String(bytes, StandardCharsets.UTF_8))
    }.getOrNull()

    private fun renderSvg(source: String): Bitmap32 {
        val transcoder = BufferedImageTranscoder().apply {
            addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, false)
            addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, false)
            source.explicitSvgDimension("width")?.let { addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, it.toFloat()) }
            source.explicitSvgDimension("height")?.let { addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, it.toFloat()) }
        }
        transcoder.transcode(TranscoderInput(StringReader(source.outlineSimpleTextElements())), null)
        val bitmap = transcoder.image?.toBitmap32() ?: error("Batik did not produce an SVG image")
        return bitmap.premultipliedIfRequired()
    }

    private fun String.outlineSimpleTextElements(): String = runCatching {
        val document = parseSvgDocument()
        val textNodes = document.getElementsByTagNameNS("*", "text").asElements()

        if (textNodes.isEmpty()) {
            return this
        }

        textNodes.forEach { textNode ->
            val pathData = textNode.outlineAsPathData() ?: return@forEach
            val pathNode = if (textNode.namespaceURI != null) {
                document.createElementNS(textNode.namespaceURI, "path")
            } else {
                document.createElement("path")
            }.apply {
                textNode.attributes.asSequence().forEach { attribute ->
                    if (attribute.namespaceURI != null) {
                        setAttributeNS(attribute.namespaceURI, attribute.nodeName, attribute.nodeValue)
                    } else {
                        setAttribute(attribute.nodeName, attribute.nodeValue)
                    }
                }
                setAttribute("d", pathData)
            }

            textNode.parentNode.replaceChild(pathNode, textNode)
        }

        document.serialize()
    }.getOrElse {
        this
    }

    private fun String.parseSvgDocument(): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }

        return factory.newDocumentBuilder().parse(InputSource(StringReader(this)))
    }

    private fun NodeList.asElements(): List<Element> =
        buildList {
            for (index in 0 until length) {
                (item(index) as? Element)?.let(::add)
            }
        }

    private fun Element.outlineAsPathData(): String? {
        if (childNodes.asElements().isNotEmpty()) {
            return null
        }

        val text = textContent.trim()
        if (text.isEmpty()) {
            return null
        }

        val font = resolveFont()
        val fontRenderContext = FontRenderContext(AffineTransform(), true, true)
        val glyphVector = font.createGlyphVector(fontRenderContext, text)
        val bounds = font.getStringBounds(text, fontRenderContext)
        val lineMetrics = font.getLineMetrics(text, fontRenderContext)

        val x = attributeAsDouble("x") ?: 0.0
        val y = attributeAsDouble("y") ?: 0.0
        val anchorShift = when (attributeValue("text-anchor")) {
            "middle" -> -bounds.width / 2.0
            "end" -> -bounds.width
            else -> 0.0
        }
        val baselineShift = when (attributeValue("dominant-baseline")) {
            "middle", "central" -> (lineMetrics.ascent.toDouble() - lineMetrics.descent.toDouble()) / 2.0
            "text-before-edge", "hanging" -> lineMetrics.ascent.toDouble()
            "text-after-edge" -> -lineMetrics.descent.toDouble()
            else -> 0.0
        }

        return glyphVector.outline
            .transformed(AffineTransform.getTranslateInstance(x + anchorShift, y + baselineShift))
            .toSvgPathData()
    }

    private fun Element.resolveFont(): Font {
        val families = attributeValue("font-family")
            ?.split(',')
            ?.map { it.trim().trim('"', '\'') }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val family = families.firstOrNull { requestedFamily ->
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .any { availableFamily -> availableFamily.equals(requestedFamily, ignoreCase = true) }
        } ?: families.firstOrNull() ?: Font.SANS_SERIF

        val style = when (attributeValue("font-style")?.lowercase()) {
            "italic", "oblique" -> Font.ITALIC
            else -> Font.PLAIN
        } or when (attributeValue("font-weight")?.lowercase()) {
            "bold", "bolder" -> Font.BOLD
            else -> if (attributeValue("font-weight")?.toIntOrNull()?.let { it >= 600 } == true) Font.BOLD else Font.PLAIN
        }

        val size = attributeValue("font-size")
            ?.substringBefore("px")
            ?.toFloatOrNull()
            ?.coerceAtLeast(1f)
            ?: 12f

        return Font(family, style, size.roundToInt().coerceAtLeast(1))
    }

    private fun Element.attributeValue(name: String): String? =
        getAttribute(name).takeIf { it.isNotBlank() }

    private fun Element.attributeAsDouble(name: String): Double? =
        attributeValue(name)
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.substringBefore("px")
            ?.toDoubleOrNull()

    private fun NamedNodeMap.asSequence(): Sequence<org.w3c.dom.Node> =
        (0 until length).asSequence().mapNotNull { item(it) }

    private fun Shape.transformed(transform: AffineTransform): Shape =
        transform.createTransformedShape(this)

    private fun Shape.toSvgPathData(): String {
        val iterator = getPathIterator(null)
        val coordinates = DoubleArray(6)
        val builder = StringBuilder()

        while (!iterator.isDone) {
            when (iterator.currentSegment(coordinates)) {
                PathIterator.SEG_MOVETO -> builder.append("M ").append(coordinates[0]).append(',').append(coordinates[1])
                PathIterator.SEG_LINETO -> builder.append(" L ").append(coordinates[0]).append(',').append(coordinates[1])
                PathIterator.SEG_QUADTO -> builder.append(" Q ")
                    .append(coordinates[0]).append(',').append(coordinates[1]).append(' ')
                    .append(coordinates[2]).append(',').append(coordinates[3])

                PathIterator.SEG_CUBICTO -> builder.append(" C ")
                    .append(coordinates[0]).append(',').append(coordinates[1]).append(' ')
                    .append(coordinates[2]).append(',').append(coordinates[3]).append(' ')
                    .append(coordinates[4]).append(',').append(coordinates[5])

                PathIterator.SEG_CLOSE -> builder.append(" Z")
            }
            iterator.next()
        }

        return builder.toString().trim()
    }

    private fun Document.serialize(): String {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty(OutputKeys.INDENT, "no")
        }
        val buffer = StringWriter()
        transformer.transform(DOMSource(this), StreamResult(buffer))
        return buffer.toString()
    }

    private fun String.explicitSvgDimension(attribute: String): Int? =
        Regex("\\b$attribute\\s*=\\s*\"([0-9]+(?:\\.[0-9]+)?)(?:px)?\"")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?.roundToInt()
            ?.coerceAtLeast(1)

    private fun Bitmap32.trimTransparentBounds(padding: Int = 0): Bitmap32 {
        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (getRgba(x, y).a == 0) {
                    continue
                }

                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }

        return if (right < left || bottom < top) {
            this
        } else {
            copySliceWithBounds(
                (left - padding).coerceAtLeast(0),
                (top - padding).coerceAtLeast(0),
                (right + padding + 1).coerceAtMost(width),
                (bottom + padding + 1).coerceAtMost(height),
            )
        }
    }

    private fun BufferedImage.toBitmap32(): Bitmap32 =
        Bitmap32(width, height) { x, y ->
            val argb = getRGB(x, y)
            RGBA(
                (argb ushr 16) and 0xFF,
                (argb ushr 8) and 0xFF,
                argb and 0xFF,
                (argb ushr 24) and 0xFF,
            )
        }

    private class BufferedImageTranscoder : ImageTranscoder() {
        var image: BufferedImage? = null
            private set

        override fun createImage(width: Int, height: Int): BufferedImage =
            BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        override fun writeImage(image: BufferedImage, output: TranscoderOutput?) {
            this.image = image
        }
    }
}
