package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.vector.format.SVG
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

internal object MapViewerIconLoader {
    fun load(file: File): Bitmap32? =
        when (file.extension.lowercase()) {
            "svg" -> loadSvg(file)
            else -> loadRaster(file)
        }

    private fun loadRaster(file: File): Bitmap32? {
        val image = ImageIO.read(file) ?: return null
        return image.toBMP32().premultipliedIfRequired().trimTransparentBounds()
    }

    private fun loadSvg(file: File): Bitmap32? = runCatching {
        val svg = SVG(file.readText())
        val width = renderDimension(svg.width, svg.dwidth)
        val height = renderDimension(svg.height, svg.dheight)
        val bitmap = Bitmap32(width, height) { _: Int, _: Int -> RGBA(0, 0, 0, 0) }
        val ctx = bitmap.getContext2d(true)
        try {
            ctx.draw(svg)
        } finally {
            ctx.close()
        }
        bitmap.premultipliedIfRequired().trimTransparentBounds()
    }.getOrNull()

    private fun renderDimension(intValue: Int, doubleValue: Double): Int {
        val preferred = intValue.takeIf { it > 0 } ?: doubleValue.roundToInt()
        return preferred.coerceAtLeast(1)
    }

    private fun Bitmap32.trimTransparentBounds(): Bitmap32 {
        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((getRgba(x, y).value ushr 24) == 0) {
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
            copySliceWithBounds(left, top, right + 1, bottom + 1)
        }
    }
}
