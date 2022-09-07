package io.hsar.mapgenerator.map

import io.hsar.mapgenerator.graph.Point

data class Cell(
    val site: Point,
    val shape: List<Point>,
    val height: Double
)
