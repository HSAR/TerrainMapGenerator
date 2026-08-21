package io.hsar.mapgenerator.commandgui

import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
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
import javax.imageio.stream.ImageInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.roundToInt

object MapViewerImageLoader {
    fun load(file: File, trimTransparentBounds: Boolean = true): BufferedImage? =
        runCatching { loadChecked(file, trimTransparentBounds) }.getOrNull()

    fun load(bytes: ByteArray, fileName: String, trimTransparentBounds: Boolean = true): BufferedImage? =
        runCatching { loadChecked(bytes, fileName, trimTransparentBounds) }.getOrNull()

    fun loadChecked(
        file: File,
        trimTransparentBounds: Boolean = true,
        limits: MapViewerLoadLimits = MapViewerLoadLimits(),
        cancellation: MapViewerCancellationSignal = MapViewerCancellationSignal.NONE,
    ): BufferedImage {
        cancellation.ensureActive()
        validateEncodedSize(file.length(), file.name, limits)
        val isSvg = file.extension.equals("svg", ignoreCase = true)
        val image = when {
            isSvg -> loadSvgChecked(file.readBytes(), file.name, limits, cancellation)
            else -> ImageIO.createImageInputStream(file)?.use { loadRasterChecked(it, file.name, limits, cancellation) }
                ?: unsupported(file.name)
        }
        cancellation.ensureActive()
        return if (isSvg) image else image.trimIfRequested(trimTransparentBounds)
    }

    fun loadChecked(
        bytes: ByteArray,
        fileName: String,
        trimTransparentBounds: Boolean = true,
        limits: MapViewerLoadLimits = MapViewerLoadLimits(),
        cancellation: MapViewerCancellationSignal = MapViewerCancellationSignal.NONE,
    ): BufferedImage {
        cancellation.ensureActive()
        validateEncodedSize(bytes.size.toLong(), fileName, limits)
        val isSvg = fileName.substringAfterLast('.', missingDelimiterValue = "").equals("svg", ignoreCase = true)
        val image = when {
            isSvg -> loadSvgChecked(bytes, fileName, limits, cancellation)
            else -> ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
                ?.use { loadRasterChecked(it, fileName, limits, cancellation) }
                ?: unsupported(fileName)
        }
        cancellation.ensureActive()
        return if (isSvg) image else image.trimIfRequested(trimTransparentBounds)
    }

    private fun loadRasterChecked(
        input: ImageInputStream,
        fileName: String,
        limits: MapViewerLoadLimits,
        cancellation: MapViewerCancellationSignal,
    ): BufferedImage {
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: unsupported(fileName)
        return try {
            reader.input = input
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            validateDimensions(width, height, fileName, limits)
            cancellation.ensureActive()
            reader.read(0)?.toArgb() ?: unsupported(fileName)
        } finally {
            reader.dispose()
        }
    }

    private fun loadSvgChecked(
        bytes: ByteArray,
        fileName: String,
        limits: MapViewerLoadLimits,
        cancellation: MapViewerCancellationSignal,
    ): BufferedImage {
        val source = String(bytes, StandardCharsets.UTF_8)
        source.svgDimensions()?.let { (width, height) -> validateDimensions(width, height, fileName, limits) }
        cancellation.ensureActive()
        return runCatching { renderSvg(source) }
            .getOrElse { failure ->
                throw MapViewerLoadException(MapViewerLoadFailure.Unsupported("$fileName is not a supported SVG image: ${failure.message ?: "decode failed"}"))
            }
            .also { image -> validateDimensions(image.width, image.height, fileName, limits) }
    }

    private fun validateEncodedSize(size: Long, fileName: String, limits: MapViewerLoadLimits) {
        if (size > limits.maximumEncodedImageBytes) {
            throw MapViewerLoadException(
                MapViewerLoadFailure.LimitExceeded(fileName, size, limits.maximumEncodedImageBytes),
            )
        }
    }

    private fun validateDimensions(width: Int, height: Int, fileName: String, limits: MapViewerLoadLimits) {
        val dimension = maxOf(width, height).toLong()
        if (dimension > limits.maximumImageDimension) {
            throw MapViewerLoadException(
                MapViewerLoadFailure.LimitExceeded("$fileName image dimension", dimension, limits.maximumImageDimension.toLong(), "pixels"),
            )
        }
        val pixels = width.toLong() * height.toLong()
        if (pixels > limits.maximumImagePixels) {
            throw MapViewerLoadException(
                MapViewerLoadFailure.LimitExceeded("$fileName decoded image", pixels, limits.maximumImagePixels, "pixels"),
            )
        }
    }

    private fun unsupported(fileName: String): Nothing =
        throw MapViewerLoadException(MapViewerLoadFailure.Unsupported("$fileName is not a supported image."))

    private fun String.svgDimensions(): Pair<Int, Int>? {
        val width = explicitSvgDimension("width")
        val height = explicitSvgDimension("height")
        if (width != null && height != null) return width to height
        val viewBox = Regex("\\bviewBox\\s*=\\s*\"([^\"]+)\"")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.split(Regex("[\\s,]+"))
            ?.mapNotNull(String::toDoubleOrNull)
        return viewBox?.takeIf { it.size == 4 }
            ?.let { it[2].roundToInt().coerceAtLeast(1) to it[3].roundToInt().coerceAtLeast(1) }
    }

    private fun renderSvg(source: String): BufferedImage {
        val transcoder = BufferedImageTranscoder().apply {
            addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, false)
            addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, false)
            source.explicitSvgDimension("width")?.let { addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, it.toFloat()) }
            source.explicitSvgDimension("height")?.let { addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, it.toFloat()) }
        }
        transcoder.transcode(TranscoderInput(StringReader(source.outlineSimpleTextElements())), null)
        return transcoder.image?.toArgb() ?: error("Batik did not produce an SVG image")
    }

    private fun BufferedImage.trimIfRequested(trim: Boolean) = if (trim) trimTransparentBounds() else this

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

    private fun NamedNodeMap.asSequence(): Sequence<Node> =
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

    private fun BufferedImage.trimTransparentBounds(padding: Int = 0): BufferedImage {
        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((getRGB(x, y) ushr 24) == 0) {
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
            getSubimage(
                (left - padding).coerceAtLeast(0),
                (top - padding).coerceAtLeast(0),
                ((right + padding + 1).coerceAtMost(width) - (left - padding).coerceAtLeast(0)).coerceAtLeast(1),
                ((bottom + padding + 1).coerceAtMost(height) - (top - padding).coerceAtLeast(0)).coerceAtLeast(1),
            ).toArgb()
        }
    }

    private fun BufferedImage.toArgb(): BufferedImage =
        if (type == BufferedImage.TYPE_INT_ARGB) this else BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().use { graphics -> graphics.drawImage(this, 0, 0, null) }
        }

    private fun java.awt.Graphics2D.use(block: (java.awt.Graphics2D) -> Unit) = try {
        block(this)
    } finally {
        dispose()
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
