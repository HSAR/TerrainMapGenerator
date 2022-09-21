package io.hsar.mapgenerator.graph

import java.awt.Shape
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import kotlin.math.max
import kotlin.math.min

object GeomUtils {

    fun Rectangle2D.toRectangle() = Rectangle(Point(this.minX, this.minY), Point(this.maxX, this.maxY))

    fun Rectangle.toAWTRectangle() = Rectangle2D.Double(this.point1.x, this.point1.y, this.width, this.height)

    fun Rectangle.toArea() = Area(this.toAWTRectangle())

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

    fun Collection<Point>.toShape(): Shape {
        val path = Path2D.Double(Path2D.WIND_NON_ZERO, this.size)

        val firstPoint = this.first()
        path.moveTo(firstPoint.x, firstPoint.y)
        this.drop(1).forEach { eachPoint ->
            path.lineTo(eachPoint.x, eachPoint.y)
        }
        path.closePath()

        return path
    }
}