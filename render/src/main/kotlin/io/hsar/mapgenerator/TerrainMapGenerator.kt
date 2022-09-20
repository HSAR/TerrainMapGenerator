package io.hsar.mapgenerator

import com.raylabz.opensimplex.Range
import io.hsar.mapgenerator.graph.GraphUtils
import io.hsar.mapgenerator.graph.GraphUtils.relax
import io.hsar.mapgenerator.graph.GraphUtils.toAdjacentMap
import io.hsar.mapgenerator.graph.toLine
import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.graph.toPointD
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.image.PathRenderer
import io.hsar.mapgenerator.image.TerrainGenerator
import io.hsar.mapgenerator.random.PointGenerator
import java.awt.image.BufferedImage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import kotlin.math.roundToInt
import kotlin.math.sqrt


class TerrainMapGenerator(val metresPerPixel: Double, val metresPerContour: Double, val height: Int, val width: Int) {

    init {
        logger.info("Creating map images ${width}px wide by ${height}px high at $metresPerPixel metres per pixel.")
    }

    val heightData = TerrainGenerator.generateTerrain(width = width, height = height)

    fun generateGraphImage(): BufferedImage {
        val numPoints = (sqrt(height.toDouble() * width) / POINT_RATIO).roundToInt()

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
            .relax(height, width)
//            .relax(height, width)

        val regions = graph.voronoiRegions().map { it.map { it.toPoint() } }
        val sites = graph.generatorSites.map { it.toPoint() }
        val vertices = graph.voronoiVertices.map { it.toPoint() }
        val siteJoins = graph.voronoiEdges.map { it.toLine(sites, vertices) }

        val mapCells = GraphUtils.generateCells(
            sites, regions, graph.voronoiEdges.toAdjacentMap()
        )

        val graphImage: BufferedImage = io.hsar.mapgenerator.image.ImageBuilder(width = width, height = height)
            .fillTransparent()
            .also { imageBuilder ->
                val cellImageRenderer = io.hsar.mapgenerator.image.CellImageRenderer(imageBuilder)
                val facilityRenderer = io.hsar.mapgenerator.image.FacilityRenderer(metresPerPixel, imageBuilder)

                PathRenderer(metresPerPixel, imageBuilder).drawPaths(siteJoins)

                mapCells.forEach { cell ->
                    cellImageRenderer.drawCell(cell)
                    facilityRenderer.drawFacility(cell)
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
        return io.hsar.mapgenerator.image.ContourRenderer.createImage(heightData = heightData, contourHeight = 0.025)
    }

    fun generateGridImage(): BufferedImage {
        val imageBuilder = io.hsar.mapgenerator.image.ImageBuilder(width = width, height = height)
            .also { it.fillTransparent() }
        io.hsar.mapgenerator.image.GridRenderer(imageBuilder).draw(
            GZDand100kmGSID = "25F XK",
            gridScale = io.hsar.mapgenerator.image.GridRenderer.HUNDRED_M_GRID_SQUARES,
            metresPerPixel = metresPerPixel
        )
        return imageBuilder.build()
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        val POINT_RATIO = 60 // Roughly 25 points at 1920x1080
        val SAMPLE_SIZE = 0.02
    }
}