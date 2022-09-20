package io.hsar.mapgenerator.graph

import java.awt.geom.Rectangle2D
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

    fun resizeTopLeft(factorX: Double, factorY: Double): Rectangle =
        Rectangle(point1, point1.translate(width * factorX, height * factorY))

    /**
     * Resize the rectangle, preserving its centre and reducing its width and height by the given scales.
     */
    fun resizeCentred(factorX: Double, factorY: Double): Rectangle {
        val point1deltaX = (point1.x - centre.x) * factorX
        val point1deltaY = (point1.y - centre.y) * factorY

        val point2deltaX = (point2.x - centre.x) * factorX
        val point2deltaY = (point2.y - centre.y) * factorY

        return Rectangle(
            Point(centre.x + point1deltaX, centre.y + point1deltaY),
            Point(centre.x + point2deltaX, centre.y + point2deltaY)
        )
    }

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

fun Rectangle2D.toRectangle() = Rectangle(Point(this.minX, this.minY), Point(this.maxX, this.maxY))