package io.hsar.mapgenerator.graph

import org.kynosarges.tektosyne.geometry.Voronoi
import org.kynosarges.tektosyne.geometry.VoronoiResults

object GraphUtils {
    fun VoronoiResults.relax() = Voronoi.findAll(this.voronoiVertices)
}