package io.hsar.mapgenerator.graph

import de.alsclo.voronoi.graph.Point as VoronoiPoint

data class Point(val x: Double, val y: Double)

fun VoronoiPoint.toPoint() = Point(x = this.x, y = this.y)