package io.hsar.mapgenerator.terrain

import de.alsclo.voronoi.Voronoi
import io.hsar.mapgenerator.graph.toEdge
import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.image.GraphImageGenerator
import io.hsar.mapgenerator.image.ImageUtils.plus
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.image.ImageWriter
import io.hsar.mapgenerator.randomness.NoiseGenerator
import io.hsar.mapgenerator.randomness.PointGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.imgscalr.Scalr
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.math.round


class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    fun generateImage(path: Path) {
        logger.info("Creating map of ${height}x${width}px at $metresPerPixel metres per pixel.")

        val graph = (1..NUM_POINTS).map { PointGenerator.randomDoublePoint(height.toDouble(), width.toDouble()) }
            .let { points -> Voronoi(points).graph }
        val sites = graph.sitePoints.map { it.toPoint() }
        val edges = graph.edgeStream().map { it.toEdge() }.collect(Collectors.toList())
        val graphImage = GraphImageGenerator.generate(sites, edges, height, width)

        val sampleHeight = round(metresPerPixel * height * SAMPLE_SIZE).toInt()
        val sampleWidth = round(metresPerPixel * width * SAMPLE_SIZE).toInt()
        val noiseImage = NoiseGenerator().generate2DArray(sampleWidth, sampleHeight)
            .toBufferedImage()
            .let { originalImage -> Scalr.resize(originalImage, height); }

        val finalImage = noiseImage + graphImage

        ImageWriter.writeGreyScaleImage(finalImage, path)
        logger.info("Saved image to $path")
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        private val NUM_POINTS = 20
        private val SAMPLE_SIZE = 0.01
    }
}