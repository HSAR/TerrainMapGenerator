package io.hsar.mapgenerator.terrain

import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.image.ImageWriter
import io.hsar.mapgenerator.noise.NoiseGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.imgscalr.Scalr
import java.nio.file.Path
import kotlin.math.round

class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    fun generateImage(path: Path) {
        logger.info("Creating map of ${height}x${width}px at $metresPerPixel metres per pixel.")
        val sampleHeight = round(metresPerPixel * height * SAMPLE_SIZE).toInt()
        val sampleWidth = round(metresPerPixel * width * SAMPLE_SIZE).toInt()

        NoiseGenerator().generate2DArray(sampleWidth, sampleHeight)
            .toBufferedImage()
            .let { originalImage -> Scalr.resize(originalImage, height);}
            .let { resizedImage -> ImageWriter.writeGreyScaleImage(resizedImage, path) }

        logger.info("Saved image to $path")
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        private val SAMPLE_SIZE = 0.01
    }
}