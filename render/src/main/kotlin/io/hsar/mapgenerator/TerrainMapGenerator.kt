package io.hsar.mapgenerator

import com.raylabz.opensimplex.Range
import io.hsar.mapgenerator.graph.GraphUtils
import io.hsar.mapgenerator.graph.GraphUtils.relax
import io.hsar.mapgenerator.graph.GraphUtils.toAdjacentMap
import io.hsar.mapgenerator.graph.Cell
import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.toLine
import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.graph.toPointD
import io.hsar.mapgenerator.graph.scale
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.image.ImageUtils.toBufferedImage
import io.hsar.mapgenerator.image.PathRenderer
import io.hsar.mapgenerator.image.TerrainGenerator
import io.hsar.mapgenerator.random.PointGenerator
import java.awt.image.BufferedImage
import java.util.Random
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import kotlin.math.roundToInt
import java.util.concurrent.ConcurrentHashMap


class TerrainMapGenerator(
    val metresPerPixel: Double,
    val metresPerContour: Double,
    val height: Int,
    val width: Int,
    private val graphMapWidthMetres: Double = width * metresPerPixel,
    private val graphMapHeightMetres: Double = height * metresPerPixel,
    seed: Long = System.currentTimeMillis(),
) {

    init {
        logger.info("Creating map images ${width}px wide by ${height}px high at $metresPerPixel metres per pixel.")
    }

    private val seedSource = Random(seed)
    private val graphSeed = seedSource.nextLong()
    private val facilitySeed = seedSource.nextLong()
    private val gridSeed = seedSource.nextLong()

    val heightData = TerrainGenerator.generateTerrain(
        width = width,
        height = height,
        metresPerPixel = metresPerPixel,
    )
    private val graphData by lazy { graphCache.getOrPut(cacheKey) { buildGraphData() } }

    fun generateGraphImage(): BufferedImage {
        val facilityRandom = Random(facilitySeed)
        val graphScale = 1 / metresPerPixel
        val graph = graphData.scale(graphScale)

        val graphImage: BufferedImage = io.hsar.mapgenerator.image.ImageBuilder(width = width, height = height)
            .fillTransparent()
            .also { imageBuilder ->
                val cellImageRenderer = io.hsar.mapgenerator.image.CellImageRenderer(imageBuilder)
                val facilityRenderer = io.hsar.mapgenerator.image.FacilityRenderer(metresPerPixel, imageBuilder, facilityRandom)

                PathRenderer(metresPerPixel, imageBuilder).drawPaths(graph.siteJoins)

                graph.mapCells.forEach { cell ->
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
        io.hsar.mapgenerator.grid.GridLayerPainter.draw(imageBuilder, generateGridLayer())
        return imageBuilder.build()
    }

    fun generateGridLayer(): GridLayer = GridLayerBuilder.build(
        width = width,
        height = height,
        gridScale = io.hsar.mapgenerator.image.GridRenderer.ONE_KM_GRID_SQUARES,
        metresPerPixel = metresPerPixel,
        random = Random(gridSeed),
        mapId = "25F XK",
    )

    companion object {
        private val logger: Logger = LogManager.getLogger(TerrainMapGenerator::class.java)

        private const val POINTS_PER_MAP_HEIGHT_METRE = 90.0 // Roughly 25 points at 1920x1080 with 2m/px.
        val SAMPLE_SIZE = 0.02
        private val graphCache = ConcurrentHashMap<GraphCacheKey, GraphData>()
    }

    private val cacheKey = GraphCacheKey(
        seed = seed,
        widthMetres = graphMapWidthMetres,
        heightMetres = graphMapHeightMetres,
    )

    private fun buildGraphData(): GraphData {
        val numPoints = (graphMapHeightMetres / POINTS_PER_MAP_HEIGHT_METRE).roundToInt().coerceAtLeast(3)

        val clipRect = RectD(0.0, 0.0, graphMapWidthMetres, graphMapHeightMetres)
        val rangeX = Range(0.0, graphMapWidthMetres)
        val rangeY = Range(0.0, graphMapHeightMetres)
        val graphRandom = Random(graphSeed)
        val points = (0..numPoints).map { PointGenerator.randomDoublePoint(rangeX, rangeY, graphRandom) }
        val graph = points
            .map {
                it.toPoint(rangeX, rangeY).toPointD()
            }
            .let { adjustedPoints ->
                Voronoi.findAll(adjustedPoints.toTypedArray(), clipRect)
            }
            .relax(graphMapHeightMetres, graphMapWidthMetres)

        val regions = graph.voronoiRegions().map { it.map { it.toPoint() } }
        val sites = graph.generatorSites.map { it.toPoint() }
        val vertices = graph.voronoiVertices.map { it.toPoint() }
        val siteJoins = graph.voronoiEdges.map { it.toLine(sites, vertices) }
        val mapCells = GraphUtils.generateCells(
            sites, regions, graph.voronoiEdges.toAdjacentMap()
        )
        return GraphData(
            sites = sites,
            regions = regions,
            siteJoins = siteJoins,
            mapCells = mapCells,
        )
    }

    private data class GraphCacheKey(
        val seed: Long,
        val widthMetres: Double,
        val heightMetres: Double,
    )

    private data class GraphData(
        val sites: List<io.hsar.mapgenerator.graph.Point>,
        val regions: List<List<io.hsar.mapgenerator.graph.Point>>,
        val siteJoins: List<Line>,
        val mapCells: List<Cell>,
    ) {
        fun scale(factor: Double) = GraphData(
            sites = sites.scale(factor),
            regions = regions.map { it.scale(factor) },
            siteJoins = siteJoins.map { it.scale(factor) },
            mapCells = mapCells.map { it.scale(factor) },
        )
    }

    internal fun graphPointCount(): Int = (graphMapHeightMetres / POINTS_PER_MAP_HEIGHT_METRE).roundToInt().coerceAtLeast(3) + 1
}
