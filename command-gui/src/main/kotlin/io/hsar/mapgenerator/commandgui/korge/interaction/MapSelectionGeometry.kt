package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionFrame
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationDrawing
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationObject
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationSelectionBounds
import io.hsar.mapgenerator.commandgui.operation.OperationSelectionProjection
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.objectsOverlappingSelectionBox
import io.hsar.mapgenerator.commandgui.operation.selectionBounds
import io.hsar.mapgenerator.commandgui.operation.selectionFrame
import io.hsar.mapgenerator.commandgui.operation.supportsResizeHandles
import korlibs.math.geom.Vector2D
import java.util.*

internal typealias MapSelectionBounds = OperationSelectionBounds

internal fun MapPlacedObjectSnapshot.supportsResizeHandles() = toOperationObject().supportsResizeHandles()

internal fun MapPlacedObjectSnapshot.selectionBounds(coordinateSpace: MapCoordinateSpace) =
    toOperationObject().selectionBounds(coordinateSpace.aspectRatio)

internal fun List<MapPlacedObjectSnapshot>.combinedSelectionBounds(coordinateSpace: MapCoordinateSpace) =
    map(MapPlacedObjectSnapshot::toOperationObject).let { objects ->
        OperationContent(objects = objects).selectionBounds(objects.mapTo(linkedSetOf()) { it.id }, coordinateSpace.aspectRatio)
    }

internal fun List<MapPlacedObjectSnapshot>.groupSelectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
) = frame(coordinateSpace, viewport, forceGroup = true)

internal fun MapPlacedObjectSnapshot.selectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    screenPadding: Double = 0.0,
) = listOf(this).frame(coordinateSpace, viewport, screenPadding = screenPadding)
    ?: error("A selected object must have a selection frame")

internal fun MapPlacedObjectSnapshot.overlapsSelectionBox(
    box: MapSelectionBounds,
    coordinateSpace: MapCoordinateSpace,
) = OperationContent(objects = listOf(toOperationObject()))
    .objectsOverlappingSelectionBox(box, coordinateSpace.aspectRatio)
    .isNotEmpty()

internal fun MapDrawing.selectionBounds(coordinateSpace: MapCoordinateSpace) =
    drawingObject(this).selectionBounds(coordinateSpace.aspectRatio)

internal fun MapDrawing.selectionFrame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    screenPadding: Double = 0.0,
) = DrawingSnapshot(MapPlacedObjectId(UUID(0L, 0L)), this)
    .selectionFrame(coordinateSpace, viewport, screenPadding)

internal fun MapDrawing.overlapsSelectionBox(box: MapSelectionBounds, coordinateSpace: MapCoordinateSpace) =
    OperationContent(objects = listOf(drawingObject(this)))
        .objectsOverlappingSelectionBox(box, coordinateSpace.aspectRatio)
        .isNotEmpty()

internal fun pivotHandleForSelection(
    snapshots: List<MapPlacedObjectSnapshot>,
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    groupFrame: ScreenSelectionFrame? = null,
): ScreenPivotHandle? = snapshots.takeIf { it.isNotEmpty() }?.let {
    val frame = groupFrame ?: snapshots.groupSelectionFrame(coordinateSpace, viewport) ?: return null
    ScreenPivotHandle(frame.toShared().rotationHandle(PIVOT_HANDLE_OFFSET_PX).toVector(), PIVOT_HANDLE_RADIUS_PX)
}

private fun List<MapPlacedObjectSnapshot>.frame(
    coordinateSpace: MapCoordinateSpace,
    viewport: CameraViewport,
    forceGroup: Boolean = false,
    screenPadding: Double = 0.0,
): ScreenSelectionFrame? {
    val objects = map(MapPlacedObjectSnapshot::toOperationObject)
    val projection = LegacySelectionProjection(coordinateSpace, viewport)
    val frame = if (forceGroup && objects.size == 1) {
        objects.single().selectionBounds(coordinateSpace.aspectRatio).let { bounds ->
            MapSelectionFrame.fromBounds(
                io.hsar.mapgenerator.commandgui.MapCoordinateBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                projection::toScreen,
            )
        }
    } else OperationContent(objects = objects).selectionFrame(objects.mapTo(linkedSetOf()) { it.id }, projection, screenPadding)
    return frame?.toLegacy()
}

private class LegacySelectionProjection(
    private val coordinateSpace: MapCoordinateSpace,
    private val viewport: CameraViewport,
) : OperationSelectionProjection {
    override val aspectRatio = coordinateSpace.aspectRatio
    override fun toScreen(point: MapCoordinate) = coordinateSpace.worldToScreen(coordinateSpace.toWorld(point), viewport).toViewer()
    override fun toMap(point: ViewerPoint) = coordinateSpace.screenToMap(point.toVector(), viewport)
    override fun unitScreenBounds(unit: OperationUnit) = unit.selectionBounds(aspectRatio).let { bounds ->
        val first = toScreen(MapCoordinate(bounds.left, bounds.top))
        val second = toScreen(MapCoordinate(bounds.right, bounds.bottom))
        MapSelectionScreenBounds(minOf(first.x, second.x), minOf(first.y, second.y), maxOf(first.x, second.x), maxOf(first.y, second.y))
    }
}

private fun drawingObject(drawing: MapDrawing) = OperationDrawingObject(OperationObjectId(UUID(0L, 0L)), drawing.toOperationDrawing())
private fun MapSelectionFrame.toLegacy() = ScreenSelectionFrame(topLeft.toVector(), topRight.toVector(), bottomRight.toVector(), bottomLeft.toVector())
private fun ScreenSelectionFrame.toShared() = MapSelectionFrame(topLeft.toViewer(), topRight.toViewer(), bottomRight.toViewer(), bottomLeft.toViewer())
private fun Vector2D.toViewer() = ViewerPoint(x, y)
private fun ViewerPoint.toVector() = Vector2D(x, y)

private const val PIVOT_HANDLE_OFFSET_PX = 16.0
private const val PIVOT_HANDLE_RADIUS_PX = 7.0
