package io.hsar.mapgenerator.graph

import io.hsar.mapgenerator.graph.Path.CompassRose.E
import io.hsar.mapgenerator.graph.Path.CompassRose.N
import io.hsar.mapgenerator.graph.Path.CompassRose.NE
import io.hsar.mapgenerator.graph.Path.CompassRose.NW
import io.hsar.mapgenerator.graph.Path.CompassRose.S
import io.hsar.mapgenerator.graph.Path.CompassRose.SE
import io.hsar.mapgenerator.graph.Path.CompassRose.SW
import io.hsar.mapgenerator.graph.Path.CompassRose.W
import kotlin.math.abs
import kotlin.math.sqrt

object Path {

    enum class CompassRose { N, NE, E, SE, S, SW, W, NW }

    /**
     * For any two points, generates a path that emerges diagonally from point1 towards point2, passes it and comes back around with 45-degree turns.
     */
    fun createTurningPath(point1: Point, point2: Point): List<Line> {
        val mainDirection = determineDirection(point1, point2)
        val c = when (mainDirection) {
            N, NE, E, SW -> getPosInterceptY(point2)
            SE, S, W, NW -> getPosInterceptY(point1)
        }

        val d = when (mainDirection) {
            N, NE, E, SW -> getNegInterceptY(point1)
            SE, S, W, NW -> getNegInterceptY(point2)
        }

        val turningOffX = (d - c) / 2.0
        val turningOffY = (c + d) / 2.0
        val turnStartPoint = Point(turningOffX, turningOffY)

        val diagonalLineToTurnStart = Line(point1, turnStartPoint)

        val afterTurningDirection = determineDirection(turnStartPoint, point2)
        val segmentWidthX = getTurningDelta(turnStartPoint.x, point2.x)
        val segmentWidthY = getTurningDelta(point2.y, turnStartPoint.y)
        val turnLines = when (mainDirection) {
            N, NE, E -> when (afterTurningDirection) {
                SE -> turnHorizontalFirst(turnStartPoint, point2)
                NW -> turnVerticalFirst(turnStartPoint, point2)
                else -> throw IllegalStateException("Turning point was incorrectly calculated, $turnStartPoint was not 45 degrees to $point2")
            }
            SE, S -> when (afterTurningDirection) {
                NE -> turnHorizontalFirst(turnStartPoint, point2)
                SW -> turnVerticalFirst(turnStartPoint, point2)
                else -> throw IllegalStateException("Turning point was incorrectly calculated, $turnStartPoint was not 45 degrees to $point2")
            }
            SW, W -> when (afterTurningDirection) {
                SE -> turnVerticalFirst(turnStartPoint, point2)
                NW -> turnHorizontalFirst(turnStartPoint, point2)
                else -> throw IllegalStateException("Turning point was incorrectly calculated, $turnStartPoint was not 45 degrees to $point2")
            }
            NW -> when (afterTurningDirection) {
                SE -> turnHorizontalFirst(turnStartPoint, point2)
                NW -> turnVerticalFirst(turnStartPoint, point2)
                else -> throw IllegalStateException("Turning point was incorrectly calculated, $turnStartPoint was not 45 degrees to $point2")
            }
        }

        return listOf(diagonalLineToTurnStart) + turnLines
    }

    /**
     * For any two points, generates a path that emerges diagonally from point1 towards point2, then makes one 45-degree turn.
     */
    fun createAnglePath(point1: Point, point2: Point): List<Line> {
        val mainDirection = determineDirection(point1, point2)
        val yIntercept = when (mainDirection) {
            N, S, E, W -> return listOf(Line(point1, point2))
            NE, SW -> getNegInterceptY(point1)
            SE, NW -> getPosInterceptY(point1)
        }

        val possibleTurnPointX = Point(point2.x, -point2.x + yIntercept)
        val possibleTurnPointY = Point(-point2.y + yIntercept, point2.y)
        val turnPoint = listOf(possibleTurnPointX, possibleTurnPointY).minByOrNull { manhattanDistance(point1, it) }!!
        return listOf(Line(point1, turnPoint), Line(turnPoint, point2))
    }

    /**
     * For the NE/SW diagonal line, get the Y-intercept that would cause the line to pass through the given point.
     */
    private fun getPosInterceptY(point: Point): Double = point.y - point.x

    /**
     * For the NW/SE diagonal line, get the Y-intercept that would cause the line to pass through the given point.
     */
    private fun getNegInterceptY(point: Point): Double = point.y + point.x

    /**
     * For two given points, generate the length that will join the two with a path of 3 segments, turning 45 degrees each time.
     */
    private fun getTurningDelta(pos1: Double, pos2: Double): Double {
        val dx = pos2 - pos1
        return (SQRT_2 * dx) / (1 + SQRT_2)
    }

    /**
     * Returns the direction of point2 from point1.
     */
    private fun determineDirection(point1: Point, point2: Point): Path.CompassRose = when {
        (point1.x == point2.x) && (point1.y > point2.y) -> N
        (point1.x < point2.x) && (point1.y > point2.y) -> NE
        (point1.x < point2.x) && (point1.y == point2.y) -> E
        (point1.x < point2.x) && (point1.y < point2.y) -> SE
        (point1.x == point2.x) && (point1.y < point2.y) -> S
        (point1.x > point2.x) && (point1.y < point2.y) -> SW
        (point1.x > point2.x) && (point1.y == point2.y) -> W
        (point1.x > point2.x) && (point1.y > point2.y) -> NW
        else -> throw IllegalStateException("Failed to navigate between $point1 and $point2")
    }

    private fun turnHorizontalFirst(
        turnStartPoint: Point,
        turnEndPoint: Point
    ): List<Line> {
        val deltaX = getTurningDelta(turnStartPoint.x, turnEndPoint.x)
        val turnPoint1 = Point(turnStartPoint.x + deltaX, turnStartPoint.y)
        val turnLine1 = Line(turnStartPoint, turnPoint1)

        val deltaY = getTurningDelta(turnStartPoint.y, turnEndPoint.y)
        val turnPoint2 = Point(turnEndPoint.x, turnEndPoint.y - deltaY)
        val turnLine2 = Line(turnPoint1, turnPoint2)

        val turnLine3 = Line(turnPoint2, turnEndPoint)
        return listOf(turnLine1, turnLine2, turnLine3)
    }

    private fun turnVerticalFirst(
        turnStartPoint: Point,
        turnEndPoint: Point
    ): List<Line> {
        val deltaY = getTurningDelta(turnStartPoint.y, turnEndPoint.y)
        val turnPoint1 = Point(turnStartPoint.x, turnStartPoint.y + deltaY)
        val turnLine1 = Line(turnStartPoint, turnPoint1)

        val deltaX = getTurningDelta(turnStartPoint.x, turnEndPoint.x)
        val turnPoint2 = Point(turnEndPoint.x - deltaX, turnEndPoint.y)
        val turnLine2 = Line(turnPoint1, turnPoint2)

        val turnLine3 = Line(turnPoint2, turnEndPoint)
        return listOf(turnLine1, turnLine2, turnLine3)
    }

    private fun manhattanDistance(point1: Point, point2: Point): Double {
        val dx = abs(point1.x - point2.x)
        val dy = abs(point1.y - point2.y)
        return dx + dy
    }

    /**
     * For any arbitrary two points, generates a path that joins to the grid, then comes back around in an octagon.
     */
    fun createGridPath(point1: Point, point2: Point): List<Line> {
        return TODO("Actually implement this")
    }

    private val SQRT_2 = sqrt(2.0)
}