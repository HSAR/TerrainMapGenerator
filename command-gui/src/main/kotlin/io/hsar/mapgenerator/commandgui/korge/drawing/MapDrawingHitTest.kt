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
            is RectangleDrawing -> outlineContains(drawing, point, tolerance, coordinateSpace)
            is OvalDrawing -> outlineContains(drawing, point, tolerance, coordinateSpace)
        }

    private fun outlineContains(
        drawing: MapDrawing,
        point: MapDrawingPoint,
        tolerance: Double,
        coordinateSpace: MapCoordinateSpace?,
    ): Boolean =
        when {
            drawing is RectangleDrawing -> MapDrawingGeometry.rectangleCorners(
                drawing.start,
                drawing.end,
                drawing.rotationDegrees,
                coordinateSpace?.aspectRatio ?: 1.0,
            ).closedSegments(drawing.color)

            coordinateSpace != null -> MapDrawingOutlineBuilder.build(drawing, zoom = 1.0, coordinateSpace = coordinateSpace)
            else -> emptyList()
        }
            .any { distanceToSegment(point, it.start, it.end) <= tolerance }

    private fun List<MapDrawingPoint>.closedSegments(color: java.awt.Color): List<MapDrawingSegment> =
        (this + first()).zipWithNext()
            .map { (start, end) -> MapDrawingSegment(start, end, color) }

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
