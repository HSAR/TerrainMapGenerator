package io.hsar.mapgenerator.commandgui.korge.drawing

import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

internal object MapDrawingOutlineBuilder {
    fun build(drawing: MapDrawing, zoom: Double): List<MapDrawingSegment> =
        when (drawing) {
            is FreeformDrawing -> drawing.points.zipWithNext()
                .map { (start, end) -> MapDrawingSegment(start, end, drawing.color) }

            is RectangleDrawing -> rectangleSegments(drawing.start, drawing.end, drawing.color)
            is CircleDrawing -> circleSegments(drawing.center, drawing.edge, drawing.color, zoom)
        }

    private fun rectangleSegments(start: MapDrawingPoint, end: MapDrawingPoint, color: Color): List<MapDrawingSegment> {
        val topRight = MapDrawingPoint(end.x, start.y)
        val bottomLeft = MapDrawingPoint(start.x, end.y)

        return listOf(
            MapDrawingSegment(start, topRight, color),
            MapDrawingSegment(topRight, end, color),
            MapDrawingSegment(end, bottomLeft, color),
            MapDrawingSegment(bottomLeft, start, color),
        )
    }

    private fun circleSegments(
        center: MapDrawingPoint,
        edge: MapDrawingPoint,
        color: Color,
        zoom: Double,
    ): List<MapDrawingSegment> {
        val radius = hypot(edge.x - center.x, edge.y - center.y)
        if (radius <= 0.0) {
            return emptyList()
        }

        val segmentCount = max(MIN_CIRCLE_SEGMENTS, (radius * zoom / CIRCLE_PIXELS_PER_SEGMENT).toInt())
        val points = (0..segmentCount).map { index ->
            val angle = 2.0 * PI * index / segmentCount
            MapDrawingPoint(
                x = center.x + cos(angle) * radius,
                y = center.y + sin(angle) * radius,
            )
        }

        return points.zipWithNext()
            .map { (start, end) -> MapDrawingSegment(start, end, color) }
    }

    private const val MIN_CIRCLE_SEGMENTS = 24
    private const val CIRCLE_PIXELS_PER_SEGMENT = 12.0
}

internal data class MapDrawingSegment(
    val start: MapDrawingPoint,
    val end: MapDrawingPoint,
    val color: Color,
)
