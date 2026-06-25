package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import kotlin.math.max
import kotlin.math.min

internal data class MapSelectionBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    fun overlaps(other: MapSelectionBounds): Boolean =
        left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top

    fun expanded(amount: Double) = MapSelectionBounds(
        left = left - amount,
        top = top - amount,
        right = right + amount,
        bottom = bottom + amount,
    )

    companion object {
        fun fromPoints(first: MapCoordinate, second: MapCoordinate) = MapSelectionBounds(
            left = min(first.x, second.x),
            top = min(first.y, second.y),
            right = max(first.x, second.x),
            bottom = max(first.y, second.y),
        )
    }
}

internal fun MapPlacedObjectSnapshot.selectionBounds(coordinateSpace: MapCoordinateSpace): MapSelectionBounds =
    when (this) {
        is UnitSnapshot -> MapSelectionBounds(
            left = mapCoordinate.x - normalizedWidth * 0.5,
            top = mapCoordinate.y - normalizedHeight * 0.5,
            right = mapCoordinate.x + normalizedWidth * 0.5,
            bottom = mapCoordinate.y + normalizedHeight * 0.5,
        )

        is DrawingSnapshot -> drawing.selectionBounds(coordinateSpace)
    }

internal fun MapPlacedObjectSnapshot.overlapsSelectionBox(
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean =
    when (this) {
        is UnitSnapshot -> selectionBounds(coordinateSpace).overlaps(box)
        is DrawingSnapshot -> drawing.overlapsSelectionBox(box, coordinateSpace)
    }

internal fun MapDrawing.selectionBounds(coordinateSpace: MapCoordinateSpace): MapSelectionBounds =
    when (this) {
        is FreeformDrawing -> points.bounds()
        is LineDrawing -> listOf(start, end).bounds()
        is RectangleDrawing -> listOf(start, end).bounds()
        is CircleDrawing -> {
            val xRadius = radius / coordinateSpace.aspectRatio
            MapSelectionBounds(
                left = center.x - xRadius,
                top = center.y - radius,
                right = center.x + xRadius,
                bottom = center.y + radius,
            )
        }
    }

internal fun MapDrawing.overlapsSelectionBox(
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean =
    when (this) {
        is RectangleDrawing -> rectangleOutlineOverlapsBox(this, box)
        else -> selectionBounds(coordinateSpace).overlaps(box)
    }

private fun List<MapDrawingPoint>.bounds(): MapSelectionBounds =
    if (isEmpty()) {
        MapSelectionBounds(0.0, 0.0, 0.0, 0.0)
    } else {
        MapSelectionBounds(
            left = minOf { it.x },
            top = minOf { it.y },
            right = maxOf { it.x },
            bottom = maxOf { it.y },
        )
    }

private fun rectangleOutlineOverlapsBox(drawing: RectangleDrawing, box: MapSelectionBounds): Boolean {
    val left = min(drawing.start.x, drawing.end.x)
    val right = max(drawing.start.x, drawing.end.x)
    val top = min(drawing.start.y, drawing.end.y)
    val bottom = max(drawing.start.y, drawing.end.y)
    return horizontalSegmentOverlapsBox(y = top, left = left, right = right, box = box) ||
            horizontalSegmentOverlapsBox(y = bottom, left = left, right = right, box = box) ||
            verticalSegmentOverlapsBox(x = left, top = top, bottom = bottom, box = box) ||
            verticalSegmentOverlapsBox(x = right, top = top, bottom = bottom, box = box)
}

private fun horizontalSegmentOverlapsBox(y: Double, left: Double, right: Double, box: MapSelectionBounds): Boolean =
    y in box.top..box.bottom && right >= box.left && left <= box.right

private fun verticalSegmentOverlapsBox(x: Double, top: Double, bottom: Double, box: MapSelectionBounds): Boolean =
    x in box.left..box.right && bottom >= box.top && top <= box.bottom
