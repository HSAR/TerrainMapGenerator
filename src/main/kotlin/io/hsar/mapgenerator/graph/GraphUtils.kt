package io.hsar.mapgenerator.graph

import org.kynosarges.tektosyne.geometry.GeoUtils
import org.kynosarges.tektosyne.geometry.Voronoi
import org.kynosarges.tektosyne.geometry.VoronoiResults

object GraphUtils {
    /**
     * Conduct Lloyd relaxation by identifying the centre of each Voronoi region and using these points as sites for another Voronoi diagram.
     */
    fun VoronoiResults.relax() = this.voronoiRegions()
        .map {
            GeoUtils.polygonCentroid(*it)
        }
        .let { newSites ->
            Voronoi.findAll(newSites.toTypedArray(), this.clippingBounds)
        }
}