package io.hsar.mapgenerator.graph

data class Cell(
    val site: Point,
    val shape: List<Point>,
    val height: Double,
    val adjacentCells: Map<Int, Point>
) {
    fun scale(factorX: Double, factorY: Double = factorX) = Cell(
        site = site.scale(factorX, factorY),
        shape = shape.scale(factorX, factorY),
        height = height,
        adjacentCells = adjacentCells.mapValues { (_, point) -> point.scale(factorX, factorY) },
    )
}
