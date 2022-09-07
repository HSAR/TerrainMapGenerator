package io.hsar.mapgenerator.terrain

import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.image.GraphImageBuilder
import io.hsar.mapgenerator.image.ImageUtils.plus
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.randomness.NoiseGenerator
import io.hsar.mapgenerator.randomness.PointGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.imgscalr.Scalr
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.round


class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    fun generateImage(): BufferedImage {
        logger.info("Creating map of ${height}x${width}px at $metresPerPixel metres per pixel.")

        val graph = (1..NUM_POINTS).map { PointGenerator.randomDoublePoint(height.toDouble(), width.toDouble()) }
            .let { points ->
                Voronoi.findAll(points.toTypedArray(), RectD(0.0, 0.0, width.toDouble(), height.toDouble()))
            }
//            .relax()
        val graphImage = GraphImageBuilder(height, width)
//            .drawLines(graph.delaunayEdges().map { it.toLine() }, Color.BLACK)
            .drawPoints(graph.generatorSites.map { it.toPoint() }, Color.RED)
//            .drawPoints(graph.voronoiVertices.map { it.toPoint() }, Color.BLUE)
            .drawShapes(graph.voronoiRegions().map { it.map { it.toPoint() } })
            .build()

        val sampleHeight = round(metresPerPixel * height * SAMPLE_SIZE).toInt()
        val sampleWidth = round(metresPerPixel * width * SAMPLE_SIZE).toInt()
        val noiseImage = NoiseGenerator().generate2DArray(sampleWidth, sampleHeight)
            .toBufferedImage()
            .let { originalImage -> Scalr.resize(originalImage, height); }

        return noiseImage + graphImage
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        private val NUM_POINTS = 20
        private val SAMPLE_SIZE = 0.01
    }
}