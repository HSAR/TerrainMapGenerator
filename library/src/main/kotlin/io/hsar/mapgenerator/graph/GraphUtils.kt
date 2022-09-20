package io.hsar.mapgenerator.graph

import io.hsar.mapgenerator.random.NoiseGenerator
import io.hsar.util.mergeReduce
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.GeoUtils
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
import org.kynosarges.tektosyne.geometry.VoronoiEdge
import org.kynosarges.tektosyne.geometry.VoronoiResults

object GraphUtils {
    /**
     * Conduct Lloyd relaxation by identifying the centre of each Voronoi region and using these points as sites for another Voronoi diagram.
     */
    fun VoronoiResults.relax(height: Int, width: Int): VoronoiResults = this.voronoiRegions()
        .map {
            GeoUtils.polygonCentroid(*it)
        }
        .let { newSites ->
            logger.info("Relaxing Voronoi graph with ${newSites.size} sites.")
            Voronoi.findAll(newSites.toTypedArray(), RectD(0.0, 0.0, width.toDouble(), height.toDouble()))
        }

    fun generateCells(
        cellCentres: List<Point>,
        cellRegions: List<List<Point>>,
        cellJoins: Map<Int, List<Int>>
    ): List<Cell> =
        cellCentres.mapIndexed { index, point ->
            Cell(
                site = point,
                shape = cellRegions[index],
                height = NoiseGenerator.DEFAULT.generatePoint(point.x, point.y),
                adjacentCells = cellJoins[index]!!.associateWith { cellCentres[it] }
            )
        }

    fun Array<VoronoiEdge>.toAdjacentMap(): Map<Int, List<Int>> {
        val map1 = this.groupBy { it.site1 }.mapValues { (_, value) -> value.map { it.site2 } }
        val map2 = this.groupBy { it.site2 }.mapValues { (_, value) -> value.map { it.site1 } }
        return map1.mergeReduce(map2) { _, value1, value2 ->
            listOfNotNull(
                value1,
                value2
            ).reduce { acc, curr -> acc + curr }
        }
    }

    private val logger: Logger = LogManager.getLogger(GraphUtils::class.java)

}