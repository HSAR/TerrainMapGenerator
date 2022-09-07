package io.hsar.mapgenerator.graph

import de.alsclo.voronoi.graph.Edge as VoronoiEdge

data class Edge(
    val site1: Point,
    val site2: Point,
    val vertices: List<Point>
)

fun VoronoiEdge.toEdge(): Edge {
    return Edge(
        this.site1.toPoint(),
        this.site2.toPoint(),
        listOf(this.a?.location?.toPoint(), this.b?.location?.toPoint()).filterNotNull()
    )
}
