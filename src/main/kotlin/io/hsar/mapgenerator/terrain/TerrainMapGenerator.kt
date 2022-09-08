package io.hsar.mapgenerator.terrain

import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.image.ContourRenderer
import io.hsar.mapgenerator.image.ImageUtils.compose
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.map.Cell
import io.hsar.mapgenerator.map.TerrainGenerator
import io.hsar.mapgenerator.randomness.NoiseGenerator
import io.hsar.mapgenerator.randomness.PointGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import java.awt.image.BufferedImage


class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    fun generateImage(): BufferedImage {
        logger.info("Creating map ${width}px wide by ${height}px high at $metresPerPixel metres per pixel.")

        val graph = (1..NUM_POINTS).map { PointGenerator.randomDoublePoint(height.toDouble(), width.toDouble()) }
            .let { points ->
                Voronoi.findAll(points.toTypedArray(), RectD(0.0, 0.0, width.toDouble(), height.toDouble()))
            }
//            .relax(height, width)
//            .relax(height, width)

        val regions = graph.voronoiRegions().map { it.map { it.toPoint() } }
        val mapCells = graph.generatorSites
            .mapIndexed { index, pointD ->
                Cell(
                    site = pointD.toPoint(),
                    shape = regions[index],
                    height = NoiseGenerator.DEFAULT.generatePoint(pointD.x, pointD.y)
                )
            }

//        val graphImage: BufferedImage = ImageBuilder(width, height)
//            .also { imageBuilder ->
//                mapCells.forEach { cell ->
//                    CellImageRenderer.drawCell(imageBuilder, cell)
//                }
//            }.build()

//        val sampleWidth = round(metresPerPixel * width * SAMPLE_SIZE).toInt()
//        val sampleHeight = round(metresPerPixel * height * SAMPLE_SIZE).toInt()
//        val heightData = NoiseGenerator().generate2DArray(width = sampleWidth, height = sampleHeight)
//            .toBufferedImage()
//            .let { originalImage -> Scalr.resize(originalImage, width); }
//            .let { resizedImage -> resizedImage.getIntData() }
        val heightData = TerrainGenerator.generateTerrain(width = width, height = height)

        val heightImage = heightData.toBufferedImage()

        val contourImage = ContourRenderer.createImage(heightData = heightData, contourHeight = 0.025)

        return listOf(
            heightImage,
//            graphImage,
            contourImage
        ).compose()
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        val NUM_POINTS = 50
        val SAMPLE_SIZE = 0.02
    }
}