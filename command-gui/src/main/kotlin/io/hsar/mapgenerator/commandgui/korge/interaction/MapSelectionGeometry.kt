package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
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

internal fun MapPlacedObjectSnapshot.supportsResizeHandles(): Boolean =
    this is DrawingSnapshot || this is GraphicSnapshot

internal fun MapPlacedObjectSnapshot.selectionBounds(coordinateSpace: MapCoordinateSpace): MapSelectionBounds =
    when (this) {
        is UnitSnapshot -> MapSelectionBounds(
            left = mapCoordinate.x - normalizedWidth * 0.5,
            top = mapCoordinate.y - normalizedHeight * 0.5,
            right = mapCoordinate.x + normalizedWidth * 0.5,
            bottom = mapCoordinate.y + normalizedHeight * 0.5,
        )

        is GraphicSnapshot -> MapSelectionBounds(
            left = mapCoordinate.x - normalizedWidth * 0.5,
            top = mapCoordinate.y - normalizedHeight * 0.5,
            right = mapCoordinate.x + normalizedWidth * 0.5,
            bottom = mapCoordinate.y + normalizedHeight * 0.5,
        )

        is DrawingSnapshot -> drawing.selectionBounds(coordinateSpace)
    }

internal fun pivotHandleForSelection(
    snapshots: List<MapPlacedObjectSnapshot>,
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    groupFrame: ScreenSelectionFrame? = null,
): ScreenPivotHandle? {
    if (snapshots.isEmpty()) {
        return null
    }
    groupFrame?.let {
        return ScreenPivotHandle(
            center = it.topRightOffsetCorner(PIVOT_HANDLE_OFFSET_PX),
            radius = PIVOT_HANDLE_RADIUS_PX,
        )
    }
    val frame = snapshots.groupSelectionFrame(coordinateSpace, viewport) ?: return null
    return ScreenPivotHandle(
        center = frame.topRightOffsetCorner(PIVOT_HANDLE_OFFSET_PX),
        radius = PIVOT_HANDLE_RADIUS_PX,
    )
}

internal fun List<MapPlacedObjectSnapshot>.combinedSelectionBounds(
    coordinateSpace: MapCoordinateSpace,
): MapSelectionBounds? =
    takeIf { it.isNotEmpty() }
        ?.map { it.selectionBounds(coordinateSpace) }
        ?.let { bounds ->
            MapSelectionBounds(
                left = bounds.minOf { it.left },
                top = bounds.minOf { it.top },
                right = bounds.maxOf { it.right },
                bottom = bounds.maxOf { it.bottom },
            )
        }

internal fun List<MapPlacedObjectSnapshot>.groupSelectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
): ScreenSelectionFrame? =
    combinedSelectionBounds(coordinateSpace)?.let { bounds ->
        ScreenSelectionFrame.fromBounds(bounds, coordinateSpace, viewport)
    }

internal fun MapPlacedObjectSnapshot.selectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    screenPadding: Double = 0.0,
): ScreenSelectionFrame =
    when (this) {
        is UnitSnapshot -> selectionBounds(coordinateSpace)
            .let { ScreenSelectionFrame.fromBounds(it, coordinateSpace, viewport) }

        is GraphicSnapshot -> selectionBounds(coordinateSpace)
            .let { ScreenSelectionFrame.fromBounds(it, coordinateSpace, viewport) }
            .expanded(screenPadding)

        is DrawingSnapshot -> drawing.selectionFrame(coordinateSpace, viewport, screenPadding)
    }

internal fun MapPlacedObjectSnapshot.overlapsSelectionBox(
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean =
    when (this) {
        is UnitSnapshot -> selectionBounds(coordinateSpace).overlaps(box)
        is GraphicSnapshot -> selectionBounds(coordinateSpace).overlaps(box)
        is DrawingSnapshot -> drawing.overlapsSelectionBox(box, coordinateSpace)
    }

internal fun MapDrawing.selectionBounds(coordinateSpace: MapCoordinateSpace): MapSelectionBounds =
    when (this) {
        is FreeformDrawing -> points.bounds()
        is LineDrawing -> listOf(start, end).bounds()
        is RectangleDrawing -> MapDrawingGeometry.rectangleCorners(start, end, rotationDegrees, coordinateSpace.aspectRatio).bounds()
        is OvalDrawing -> ovalPoints(coordinateSpace).bounds()
    }

internal fun MapDrawing.selectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    screenPadding: Double = 0.0,
): ScreenSelectionFrame =
    when (this) {
        is FreeformDrawing ->
            selectionBounds(coordinateSpace).let { ScreenSelectionFrame.fromBounds(it, coordinateSpace, viewport) }

        is LineDrawing -> ScreenSelectionFrame.fromSegment(
            start = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(start.x, start.y)), viewport),
            end = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(end.x, end.y)), viewport),
            padding = screenPadding.coerceAtLeast(SELECTION_FRAME_SCREEN_PADDING),
        )

        is RectangleDrawing -> {
            val points = MapDrawingGeometry.rectangleCorners(start, end, rotationDegrees, coordinateSpace.aspectRatio)
            ScreenSelectionFrame(
                topLeft = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(points[0].x, points[0].y)), viewport),
                topRight = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(points[1].x, points[1].y)), viewport),
                bottomRight = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(points[2].x, points[2].y)), viewport),
                bottomLeft = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(points[3].x, points[3].y)), viewport),
            )
        }

        is OvalDrawing -> {
            val unrotated = ScreenSelectionFrame.fromBounds(
                MapSelectionBounds(
                    left = center.x - xRadius,
                    top = center.y - yRadius,
                    right = center.x + xRadius,
                    bottom = center.y + yRadius,
                ),
                coordinateSpace,
                viewport,
            )
            unrotated.rotatedAround(
                pivot = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(center.x, center.y)), viewport),
                deltaRadians = Math.toRadians(rotationDegrees),
            )
        }
    }

internal fun MapDrawing.overlapsSelectionBox(
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean =
    when (this) {
        is RectangleDrawing -> rectangleOutlineOverlapsBox(this, box, coordinateSpace)
        is OvalDrawing -> ovalOutlineOverlapsBox(this, box, coordinateSpace)
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

private fun rectangleOutlineOverlapsBox(
    drawing: RectangleDrawing,
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean {
    val points = MapDrawingGeometry.rectangleCorners(
        drawing.start,
        drawing.end,
        drawing.rotationDegrees,
        coordinateSpace.aspectRatio,
    )
    return points.closedSegments()
        .any { (start, end) -> segmentOverlapsBox(start, end, box) }
}

private fun ovalOutlineOverlapsBox(
    drawing: OvalDrawing,
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
): Boolean =
    drawing.ovalPoints(coordinateSpace)
        .closedSegments()
        .any { (start, end) -> segmentOverlapsBox(start, end, box) }

private fun OvalDrawing.ovalPoints(coordinateSpace: MapCoordinateSpace): List<MapDrawingPoint> =
    MapDrawingGeometry.ovalPoints(
        center = center,
        xRadius = xRadius,
        yRadius = yRadius,
        rotationDegrees = rotationDegrees,
        segmentCount = OVAL_SELECTION_SEGMENTS,
        aspectRatio = coordinateSpace.aspectRatio,
    )

private fun List<MapDrawingPoint>.closedSegments(): List<Pair<MapDrawingPoint, MapDrawingPoint>> =
    takeIf { it.isNotEmpty() }
        ?.let { (it + it.first()).zipWithNext() }
        ?: emptyList()

private fun segmentOverlapsBox(start: MapDrawingPoint, end: MapDrawingPoint, box: MapSelectionBounds): Boolean =
    listOf(start, end).bounds().overlaps(box)

private const val PIVOT_HANDLE_OFFSET_PX = 16.0
private const val PIVOT_HANDLE_RADIUS_PX = 7.0
private const val OVAL_SELECTION_SEGMENTS = 48
