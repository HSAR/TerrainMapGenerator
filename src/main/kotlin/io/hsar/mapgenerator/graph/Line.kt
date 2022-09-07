package io.hsar.mapgenerator.graph

import org.kynosarges.tektosyne.geometry.LineD
import org.kynosarges.tektosyne.geometry.VoronoiEdge

data class Line(
    val site1: Point,
    val site2: Point,
    val vertices: List<Point> = emptyList()
)

fun VoronoiEdge.toLine(sites: List<Point>, vertices: List<Point>): Line = Line(
    sites[site1],
    sites[site2],
//    listOfNotNull(vertex1.let { vertices[it] }, vertex1.let { vertices[it] })
)

fun LineD.toLine() = Line(start.toPoint(), end.toPoint())