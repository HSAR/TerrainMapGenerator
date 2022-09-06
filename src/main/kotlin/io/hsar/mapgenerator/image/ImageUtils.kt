package io.hsar.mapgenerator.image

import java.awt.Color
import java.awt.image.BufferedImage

object ImageUtils {

    /**
     * Takes an array of doubles between 0.0 and 1.0 and generates a greyscale image from them, then writes it to disk.
     */
    fun List<List<Double>>.toBufferedImage() = BufferedImage(this.size, this[0].size, BufferedImage.TYPE_BYTE_GRAY)
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
}