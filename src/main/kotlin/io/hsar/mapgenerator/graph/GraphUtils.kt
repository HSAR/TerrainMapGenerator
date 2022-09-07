package io.hsar.mapgenerator.graph

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kynosarges.tektosyne.geometry.GeoUtils
import org.kynosarges.tektosyne.geometry.RectD
import org.kynosarges.tektosyne.geometry.Voronoi
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

    private val logger: Logger = LogManager.getLogger(GraphUtils::class.java)

}