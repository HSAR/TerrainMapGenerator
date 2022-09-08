package io.hsar.mapgenerator.graph

import kotlin.math.sqrt

object Path {

    enum class CompassRose { N, NE, E, SE, S, SW, W, NW }

    /**
     * For any arbitrary two points, generates a path that emerges diagonally from point1 towards point2, passes it and comes back around in an octagon.
     */
    fun createPath(point1: Point, point2: Point): List<Line> {
        val direction = when {
            (point1.x == point2.x) && (point1.y < point2.y) -> CompassRose.N
            (point1.x < point2.x) && (point1.y < point2.y) -> CompassRose.NE
            (point1.x < point2.x) && (point1.y == point2.y) -> CompassRose.E
            (point1.x < point2.x) && (point1.y > point2.y) -> CompassRose.SE
            (point1.x == point2.x) && (point1.y > point2.y) -> CompassRose.S
            (point1.x > point2.x) && (point1.y > point2.y) -> CompassRose.SW
            (point1.x > point2.x) && (point1.y == point2.y) -> CompassRose.W
            (point1.x > point2.x) && (point1.y < point2.y) -> CompassRose.NW
            else -> throw IllegalStateException("Failed to navigate between $point1 and $point2")
        }

        val c = when (direction) {
            CompassRose.N, CompassRose.NE, CompassRose.E, CompassRose.SW -> getPosInterceptY(point1)
            CompassRose.SE, CompassRose.S, CompassRose.W, CompassRose.NW -> getPosInterceptY(point2)
        }

        val d = when (direction) {
            CompassRose.N, CompassRose.NE, CompassRose.E, CompassRose.SW -> getNegInterceptY(point1)
            CompassRose.SE, CompassRose.S, CompassRose.W, CompassRose.NW -> getNegInterceptY(point2)
        }

        val turningOffX = (d - c) / 2.0
        val turningOffY = (c + d) / 2.0
        val turningOffPoint = Point(turningOffX, turningOffY)

        val diagonalLine1 = Line(point1, turningOffPoint)

        val octagonSideLength = getOctagonSideLength(turningOffPoint.x, point2.x)
        val octagonPoint1 = Point(turningOffX + (octagonSideLength / 2), turningOffY)
        val turningOffSegment1 = Line(turningOffPoint, octagonPoint1)

        val octagonPoint2 = Point(point2.x, point2.y - (octagonSideLength / 2))
        val turningOffSegment2 = Line(octagonPoint1, octagonPoint2)
        val turningOffSegment3 = Line(octagonPoint2, point2)

        return listOf(diagonalLine1, turningOffSegment1, turningOffSegment2, turningOffSegment3)
    }

    /**
     * For the NE/SW diagonal line, get the Y-intercept that would cause the line to pass through the given point.
     */
    fun getPosInterceptY(point: Point): Double = point.y - point.x

    /**
     * For the NW/SE diagonal line, get the Y-intercept that would cause the line to pass through the given point.
     */
    fun getNegInterceptY(point: Point): Double = point.y + point.x

    fun getOctagonSideLength(x1: Double, x2: Double): Double {
        val dx = x1 - x2
        return (2 * SQRT_2 * dx) / (2 + SQRT_2)
    }

    /**
     * For any arbitrary two points, generates a path that joins to the grid, then comes back around in an octagon.
     */
    fun createGridPath(point1: Point, point2: Point): List<Line> {
        return TODO("Actually implement this")
    }

    val SQRT_2 = sqrt(2.0)
}