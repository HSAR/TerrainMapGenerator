package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import java.awt.Color

internal object MapDrawingOutlineBuilder {
    fun build(drawing: MapDrawing, zoom: Double, coordinateSpace: MapCoordinateSpace): List<MapDrawingSegment> =
        when (drawing) {
            is FreeformDrawing -> drawing.points.zipWithNext()
                .map { (start, end) -> MapDrawingSegment(start, end, drawing.color) }

            is LineDrawing -> listOf(MapDrawingSegment(drawing.start, drawing.end, drawing.color))
            is RectangleDrawing -> rectangleSegments(drawing, coordinateSpace)
            is OvalDrawing -> ovalSegments(drawing, zoom, coordinateSpace)
        }

    private fun rectangleSegments(drawing: RectangleDrawing, coordinateSpace: MapCoordinateSpace): List<MapDrawingSegment> =
        MapDrawingGeometry.rectangleCorners(
            drawing.start,
            drawing.end,
            drawing.rotationDegrees,
            coordinateSpace.aspectRatio,
        )
            .closedSegments(drawing.color)

    private fun ovalSegments(
        drawing: OvalDrawing,
        zoom: Double,
        coordinateSpace: MapCoordinateSpace,
    ): List<MapDrawingSegment> =
        MapDrawingGeometry.ovalPoints(
            center = drawing.center,
            xRadius = drawing.xRadius,
            yRadius = drawing.yRadius,
            rotationDegrees = drawing.rotationDegrees,
            segmentCount = MapDrawingGeometry.ovalSegmentCount(
                widthPixels = drawing.xRadius * 2.0 * coordinateSpace.toMapPixels(MapCoordinate(1.0, 1.0)).x,
                heightPixels = drawing.yRadius * 2.0 * coordinateSpace.toMapPixels(MapCoordinate(1.0, 1.0)).y,
                zoom = zoom,
            ),
            aspectRatio = coordinateSpace.aspectRatio,
        ).zipWithNext()
            .map { (start, end) -> MapDrawingSegment(start, end, drawing.color) }

    private fun List<MapDrawingPoint>.closedSegments(color: Color): List<MapDrawingSegment> =
        (this + first()).zipWithNext()
            .map { (start, end) -> MapDrawingSegment(start, end, color) }
}

internal data class MapDrawingSegment(
    val start: MapDrawingPoint,
    val end: MapDrawingPoint,
    val color: Color,
)
