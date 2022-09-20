package io.hsar.mapgenerator.image

import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

object ImageUtils {

    fun BufferedImage.getIntData(): Array<DoubleArray> {
        val pixels = (this.raster.dataBuffer as DataBufferInt).data
        val width: Int = this.width
        val height: Int = this.height
        val hasAlphaChannel = this.alphaRaster != null

        val result = Array(height) { DoubleArray(width) }
        if (hasAlphaChannel) {
            val pixelLength = 4
            var pixel = 0
            var row = 0
            var col = 0
            while (pixel + 3 < pixels.size) {
                var argb = 0
                argb += pixels[pixel] and 0xff shl 24 // alpha
                argb += pixels[pixel + 1] and 0xff // blue
                argb += pixels[pixel + 2] and 0xff shl 8 // green
                argb += pixels[pixel + 3] and 0xff shl 16 // red
                result[row][col] = argb / 255.0
                col++
                if (col == width) {
                    col = 0
                    row++
                }
                pixel += pixelLength
            }
        } else {
            val pixelLength = 3
            var pixel = 0
            var row = 0
            var col = 0
            while (pixel + 2 < pixels.size) {
                var argb = 0
                argb += -16777216 // 255 alpha
                argb += pixels[pixel] and 0xff // blue
                argb += pixels[pixel + 1] and 0xff shl 8 // green
                argb += pixels[pixel + 2] and 0xff shl 16 // red
                result[row][col] = argb / 255.0
                col++
                if (col == width) {
                    col = 0
                    row++
                }
                pixel += pixelLength
            }
        }

        return result
    }

    /**
     * Takes an array of doubles between 0.0 and 1.0 and generates a greyscale image from them, then writes it to disk.
     */
    fun List<List<Double>>.toBufferedImage() = BufferedImage(this.size, this[0].size, BufferedImage.TYPE_INT_ARGB)
        .also { image ->
            for (x in 0 until this.size) {
                for (y in 0 until this[0].size) {
                    // Enforce data validity by truncating images at 0 and 1.
                    val color = this[x][y]
                        .let { java.lang.Double.max(it, 0.0) }
                        .let { java.lang.Double.min(it, 1.0).toFloat() }
                        .let { Color(it, it, it) }
                    image.setRGB(x, y, color.rgb)
                }
            }
        }

    fun Array<DoubleArray>.toBufferedImage() = this.map { it.toList() }.toBufferedImage()

    fun Array<DoubleArray>.invert(): Array<DoubleArray> {
        val rows = this.size
        val columns = this[0].size
        return Array(this[0].size) { DoubleArray(this.size) }
            .also {
                for (i in 0 until rows) {
                    for (j in 0 until columns) {
                        it[j][i] = this[i][j]
                    }
                }
            }
    }

    /**
     * Combines two images together by using the brightest color at each pixel.
     */
    operator fun BufferedImage.plus(other: BufferedImage) =
        BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB)
            .also { image ->
                val graphics = image.graphics
                graphics.drawImage(this, 0, 0, null)
                graphics.drawImage(other, 0, 0, null)
            }

    fun List<BufferedImage>.compose() = this.reduce { acc, curr -> acc + curr }

    private fun Color.toHSB() = Color.RGBtoHSB(this.red, this.green, this.blue, null)

    private class HSBComparator : Comparator<FloatArray> {
        override fun compare(o1: FloatArray, o2: FloatArray): Int {
            val o1brightness = o1[2]
            val o2brightness = o2[2]

            return o1brightness.compareTo(o2brightness)
        }
    }
}