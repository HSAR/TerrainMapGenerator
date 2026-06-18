package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import kotlin.math.hypot

internal object MapDrawingHitTest {
    fun contains(
        drawing: MapDrawing,
        point: MapDrawingPoint,
        tolerance: Double,
        coordinateSpace: MapCoordinateSpace? = null,
    ): Boolean =
        when (drawing) {
            is FreeformDrawing -> drawing.points.zipWithNext()
                .any { (start, end) -> distanceToSegment(point, start, end) <= tolerance * FREEFORM_TOLERANCE_MULTIPLIER }

            is LineDrawing -> distanceToSegment(point, drawing.start, drawing.end) <= tolerance * FREEFORM_TOLERANCE_MULTIPLIER
            is RectangleDrawing -> rectangleContains(drawing, point, tolerance)
            is CircleDrawing -> circleContains(drawing, point, tolerance, coordinateSpace)
        }

    private fun rectangleContains(drawing: RectangleDrawing, point: MapDrawingPoint, tolerance: Double): Boolean {
        val topLeft = MapDrawingPoint(minOf(drawing.start.x, drawing.end.x), minOf(drawing.start.y, drawing.end.y))
        val bottomRight = MapDrawingPoint(maxOf(drawing.start.x, drawing.end.x), maxOf(drawing.start.y, drawing.end.y))
        val topRight = MapDrawingPoint(bottomRight.x, topLeft.y)
        val bottomLeft = MapDrawingPoint(topLeft.x, bottomRight.y)
        return distanceToSegment(point, topLeft, topRight) <= tolerance ||
                distanceToSegment(point, topRight, bottomRight) <= tolerance ||
                distanceToSegment(point, bottomRight, bottomLeft) <= tolerance ||
                distanceToSegment(point, bottomLeft, topLeft) <= tolerance
    }

    private fun circleContains(
        drawing: CircleDrawing,
        point: MapDrawingPoint,
        tolerance: Double,
        coordinateSpace: MapCoordinateSpace?,
    ): Boolean {
        val radius = drawing.radius
        if (radius <= 0.0) {
            return false
        }

        val aspectRatio = coordinateSpace?.aspectRatio ?: 1.0
        val dx = (point.x - drawing.center.x) * aspectRatio
        val dy = point.y - drawing.center.y
        val distance = hypot(dx, dy)
        return kotlin.math.abs(distance - radius) <= tolerance
    }

    private fun distanceToSegment(point: MapDrawingPoint, start: MapDrawingPoint, end: MapDrawingPoint): Double {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0.0 && dy == 0.0) {
            return hypot(point.x - start.x, point.y - start.y)
        }

        val t = (((point.x - start.x) * dx) + ((point.y - start.y) * dy)) / ((dx * dx) + (dy * dy))
        val clampedT = t.coerceIn(0.0, 1.0)
        val nearestX = start.x + clampedT * dx
        val nearestY = start.y + clampedT * dy
        return hypot(point.x - nearestX, point.y - nearestY)
    }

    private const val FREEFORM_TOLERANCE_MULTIPLIER = 4.0
}
