package io.hsar.mapgenerator.terrain

import com.raylabz.opensimplex.Range
import io.hsar.mapgenerator.graph.GraphUtils
import io.hsar.mapgenerator.graph.GraphUtils.toAdjacentMap
import io.hsar.mapgenerator.graph.toLine
import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.graph.toPointD
import io.hsar.mapgenerator.image.CellImageRenderer
import io.hsar.mapgenerator.image.ContourRenderer
import io.hsar.mapgenerator.image.ImageBuilder
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.map.TerrainGenerator
import io.hsar.mapgenerator.randomness.PointGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import java.awt.Color
import java.awt.image.BufferedImage


class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    init {
        logger.info("Creating map images ${width}px wide by ${height}px high at $metresPerPixel metres per pixel.")
    }

    val heightData = TerrainGenerator.generateTerrain(width = width, height = height)

    fun generateGraphImage(): BufferedImage {
        val numPoints = height * width / POINT_RATIO

        val clipRect = RectD(0.0, 0.0, width.toDouble(), height.toDouble())
        val rangeX = Range(0.0, width.toDouble())
        val rangeY = Range(0.0, height.toDouble())
        val points = (0..numPoints).map { PointGenerator.randomDoublePoint(rangeX, rangeY) }
//        val points = listOf(
//            RangedValue(rangeX, width * 0.5) to RangedValue(rangeY, height * 0.5),
//            RangedValue(rangeX, width * 0.7) to RangedValue(rangeY, height * 0.3),
//            RangedValue(rangeX, width * 0.7) to RangedValue(rangeY, height * 0.9),
//            RangedValue(rangeX, width * 0.3) to RangedValue(rangeY, height * 0.7),
//            RangedValue(rangeX, width * 0.3) to RangedValue(rangeY, height * 0.3),
//        )
        val graph = points
            .map {
                it.toPoint(rangeX, rangeY).toPointD()
            }
            .let { adjustedPoints ->
                Voronoi.findAll(adjustedPoints.toTypedArray(), clipRect)
            }
//            .relax(height, width)
//            .relax(height, width)

        val regions = graph.voronoiRegions().map { it.map { it.toPoint() } }
        val sites = graph.generatorSites.map { it.toPoint() }
        val vertices = graph.voronoiVertices.map { it.toPoint() }
        val siteJoins = graph.voronoiEdges.map { it.toLine(sites, vertices) }

        val mapCells = GraphUtils.generateCells(
            sites, regions, graph.voronoiEdges.toAdjacentMap()
        )

        val graphImage: BufferedImage = ImageBuilder(width = width, height = height)
            .fillTransparent()
            .also { imageBuilder ->

                imageBuilder.drawDiagonalPaths(siteJoins, Color.GREEN)

                mapCells.forEach { cell ->
                    CellImageRenderer.drawCell(imageBuilder, cell)
                }
            }.build()

        return graphImage
    }

    fun generateHeightImage(): BufferedImage {
//        val sampleWidth = (metresPerPixel * width * SAMPLE_SIZE).roundToInt()
//        val sampleHeight = (metresPerPixel * height * SAMPLE_SIZE).roundToInt()
//        val heightData = NoiseGenerator().generate2DArray(width = sampleWidth, height = sampleHeight)
//            .toBufferedImage()
//            .let { originalImage -> Scalr.resize(originalImage, width); }
//            .let { resizedImage -> resizedImage.getIntData() }
        return heightData.toBufferedImage()
    }

    fun generateContourImage(): BufferedImage {
        return ContourRenderer.createImage(heightData = heightData, contourHeight = 0.025)
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        val POINT_RATIO = 8000 // Roughly 250 points at 1920x1080
        val SAMPLE_SIZE = 0.02
    }
}