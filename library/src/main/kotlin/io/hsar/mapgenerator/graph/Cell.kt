package io.hsar.mapgenerator.graph

data class Cell(
    val site: Point,
    val shape: List<Point>,
    val height: Double,
    val adjacentCells: Map<Int, Point>
)
