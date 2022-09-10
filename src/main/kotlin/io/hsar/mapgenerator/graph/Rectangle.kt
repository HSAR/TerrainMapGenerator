package io.hsar.mapgenerator.graph

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Rectangle(val point1: Point, val point2: Point) {
    val topLeft
        get() = Point(min(point1.x, point2.x), min(point1.y, point2.y))
    val topRight
        get() = Point(max(point1.x, point2.x), min(point1.y, point2.y))
    val bottomLeft
        get() = Point(min(point1.x, point2.x), max(point1.y, point2.y))
    val bottomRight
        get() = Point(max(point1.x, point2.x), max(point1.y, point2.y))

    val centre
        get() = Point((point1.x + point2.x) / 2, (point1.y + point2.y) / 2)

    val width
        get() = abs(point1.x - point2.x)
    val height
        get() = abs(point1.y - point2.y)

    val shape
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun translate(deltaX: Double, deltaY: Double) = Rectangle(
        point1.translate(deltaX, deltaY),
        point2.translate(deltaX, deltaY),
    )

    companion object {
        fun Collection<Point>.getBoundingBox(
            clipBox: Rectangle = Rectangle(Point(0.0, 0.0), Point(Double.MAX_VALUE, Double.MAX_VALUE))
        ): Rectangle {
            if (isEmpty()) return Rectangle(Point.ORIGIN, Point.ORIGIN)

            var north = clipBox.bottomRight.y
            var west = clipBox.bottomRight.x
            var south = clipBox.topLeft.y
            var east = clipBox.topLeft.x

            forEach { loc ->
                north = min(north, loc.y)
                west = min(west, loc.x)
                south = max(south, loc.y)
                east = max(east, loc.x)
            }

            // apply clipping box
            north = max(clipBox.topLeft.y, north)
            west = max(clipBox.topLeft.x, west)
            south = min(clipBox.bottomRight.y, south)
            east = min(clipBox.bottomRight.x, east)

            return Rectangle(Point(west, north), Point(east, south))
        }
    }
}