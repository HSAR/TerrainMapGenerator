package io.hsar.mapgenerator.image

import java.awt.Color
import java.awt.image.BufferedImage
import java.lang.Double.max
import java.lang.Double.min
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

object ImageWriter {

    /**
     * Write a greyscale image.
     */
    fun writeGreyScaleImage(bufferedImage: BufferedImage, path: Path) {
        // Write image
        path.deleteIfExists()
        path.createDirectories()
        ImageIO.write(bufferedImage, "png", path.toFile())
    }
}