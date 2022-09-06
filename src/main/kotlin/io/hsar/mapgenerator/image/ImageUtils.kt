package io.hsar.mapgenerator.image

import java.awt.Color
import java.awt.image.BufferedImage

object ImageUtils {

    /**
     * Takes an array of doubles between 0.0 and 1.0 and generates a greyscale image from them, then writes it to disk.
     */
    fun List<List<Double>>.toBufferedImage() = BufferedImage(this.size, this[0].size, BufferedImage.TYPE_INT_ARGB)
        .also { image ->
            for (y in 0 until this[0].size) {
                for (x in this.indices) {
                    // Enforce data validity by truncating images at 0 and 1.
                    val color = this[x][y]
                        .let { java.lang.Double.max(it, 0.0) }
                        .let { java.lang.Double.min(it, 1.0).toFloat() }
                        .let { Color(it, it, it) }
                    image.setRGB(x, y, color.rgb)
                }
            }
        }

    /**
     * Combines two images together by using the brightest color at each pixel.
     */
    operator fun BufferedImage.plus(other: BufferedImage) = BufferedImage(this.width, this.height, this.type)
        .also { image ->
            for (y in 0 until this.height) {
                for (x in 0 until this.width) {
                    val hsb1 = Color(this.getRGB(x, y)).toHSB()
                    val hsb2 = Color(other.getRGB(x, y)).toHSB()

                    // use the brightest colour as the overriding one
                    val brighterColor = listOf(hsb1, hsb2).sortedWith(HSBComparator()).first()
                        .let { (r, g, b) -> Color(r, g, b) }
                    image.setRGB(x, y, brighterColor.rgb)
                }
            }
        }

    private fun Color.toHSB() = Color.RGBtoHSB(this.red, this.green, this.blue, null)

    private class HSBComparator : Comparator<FloatArray> {
        override fun compare(o1: FloatArray, o2: FloatArray): Int {
            val o1brightness = o1[2]
            val o2brightness = o2[2]

            return o1brightness.compareTo(o2brightness)
        }
    }
}