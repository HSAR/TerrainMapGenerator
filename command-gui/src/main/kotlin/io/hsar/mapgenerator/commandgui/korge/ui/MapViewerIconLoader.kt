package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.vector.format.SVG
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
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
        val source = file.readText()
        val svg = SVG(source)
        val width = source.explicitSvgDimension("width") ?: renderDimension(svg.width, svg.dwidth)
        val height = source.explicitSvgDimension("height") ?: renderDimension(svg.height, svg.dheight)
        val bitmap = Bitmap32(width, height) { _: Int, _: Int -> RGBA(0, 0, 0, 0) }
        val ctx = bitmap.getContext2d(true)
        try {
            ctx.draw(svg)
        } finally {
            ctx.close()
        }
        bitmap.trimTransparentBounds(padding = svgTrimPadding(bitmap.width, bitmap.height)).premultipliedIfRequired()
    }.getOrNull()

    private fun loadSvg(bytes: ByteArray): Bitmap32? = runCatching {
        val source = String(bytes, StandardCharsets.UTF_8)
        val svg = SVG(source)
        val width = source.explicitSvgDimension("width") ?: renderDimension(svg.width, svg.dwidth)
        val height = source.explicitSvgDimension("height") ?: renderDimension(svg.height, svg.dheight)
        val bitmap = Bitmap32(width, height) { _: Int, _: Int -> RGBA(0, 0, 0, 0) }
        val ctx = bitmap.getContext2d(true)
        try {
            ctx.draw(svg)
        } finally {
            ctx.close()
        }
        bitmap.trimTransparentBounds(padding = svgTrimPadding(bitmap.width, bitmap.height)).premultipliedIfRequired()
    }.getOrNull()

    private fun renderDimension(intValue: Int, doubleValue: Double): Int {
        val preferred = intValue.takeIf { it > 0 } ?: doubleValue.roundToInt()
        return preferred.coerceAtLeast(1)
    }

    private fun String.explicitSvgDimension(attribute: String): Int? =
        Regex("\\b$attribute\\s*=\\s*\"([0-9]+(?:\\.[0-9]+)?)(?:px)?\"")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?.roundToInt()
            ?.coerceAtLeast(1)

    private fun svgTrimPadding(width: Int, height: Int): Int =
        (maxOf(width, height) * SVG_TRIM_PADDING_RATIO).roundToInt().coerceIn(SVG_TRIM_MIN_PADDING_PX, SVG_TRIM_MAX_PADDING_PX)

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

    private const val SVG_TRIM_PADDING_RATIO = 0.04
    private const val SVG_TRIM_MIN_PADDING_PX = 2
    private const val SVG_TRIM_MAX_PADDING_PX = 12
}
