package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSelection
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSelectionKind
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import korlibs.event.MouseButton
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.math.geom.Vector2D

internal class MapDrawingManager(
    private val root: View,
    private val geometry: ViewerGeometry,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val viewportProvider: () -> CameraViewport,
    private val currentTool: () -> MapViewerTool,
    private val drawingStore: MapDrawingStore,
    private val contextMenuManager: ContextMenuManager,
    private val isHigherPriorityDragCandidateUnderPointer: (Vector2D) -> Boolean = { false },
    private val isHigherPriorityDragInProgress: () -> Boolean = { false },
    private val canInteractWithDrawings: () -> Boolean = { true },
    private val onDrawingsChanged: (MapDrawingState) -> Unit = {},
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private var dragState: DragState? = null

    fun install() {
        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            if (currentTool() != MapViewerTool.SELECT || !canInteractWithDrawings()) {
                dragState = null
            }
            if (updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))) {
                onDrawingsChanged(drawingStore.state)
            }
        }
    }

    val isDragging: Boolean
        get() = dragState != null

    fun blocksScreenPoint(point: Vector2D): Boolean {
        if (!canInteractWithDrawings()) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = point,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return false
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        return drawingStore.state.drawings.any { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }
    }

    fun selectionAt(point: Vector2D): MapPlacedObjectSelection? {
        if (!canInteractWithDrawings()) {
            return null
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = point,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return null
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val drawing = drawingStore.findAt(mapCoordinate, tolerance, coordinateSpace) ?: return null
        return MapPlacedObjectSelection(
            kind = MapPlacedObjectSelectionKind.DRAWING,
            id = drawing.id,
            distanceToPointer = 0.0,
        )
    }

    fun beginDragAt(point: Vector2D): Boolean {
        if (dragState != null || !canInteractWithDrawings() || currentTool() != MapViewerTool.SELECT || isHigherPriorityDragCandidateUnderPointer(point) || isHigherPriorityDragInProgress()) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = point,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return false
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val target = drawingStore.findAt(mapCoordinate, tolerance, coordinateSpace) ?: return false
        dragState = DragState(
            drawing = target,
            startMapCoordinate = mapCoordinate,
            lastDelta = MapCoordinateDelta(0.0, 0.0),
            originalSnapshot = target.snapshot(),
        )
        return true
    }

    fun openContextMenuAt(point: Vector2D): Boolean {
        if (!canInteractWithDrawings() || currentTool() != MapViewerTool.SELECT) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = point,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return false
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val drawing = drawingStore.findAt(mapCoordinate, tolerance, coordinateSpace) ?: return false
        contextMenuManager.open(
            target = drawing,
            screenPoint = point,
            actions = listOf(
                io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction(
                    label = "Delete",
                    onSelected = { onObjectDeleted(drawing.snapshot()) },
                ),
            ),
            onActionSelected = { onDrawingsChanged(drawingStore.state) },
        )
        return true
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean): Boolean {
        val state = dragState
        if (state == null) {
            return false
        }

        if (!leftHeld) {
            val updatedSnapshot = state.drawing.snapshot()
            if (updatedSnapshot != state.originalSnapshot) {
                onObjectMoved(state.originalSnapshot, updatedSnapshot)
            }
            dragState = null
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = pointer,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        )
        val delta = MapCoordinateDelta(
            x = mapCoordinate.x - state.startMapCoordinate.x,
            y = mapCoordinate.y - state.startMapCoordinate.y,
        )
        val incrementalDelta = MapCoordinateDelta(
            x = delta.x - state.lastDelta.x,
            y = delta.y - state.lastDelta.y,
        )
        state.drawing.moveBy(incrementalDelta)
        state.lastDelta = delta
        return true
    }

    private companion object {
        private const val HIT_TOLERANCE = 8.0
    }

    private data class DragState(
        val drawing: PlacedDrawing,
        val startMapCoordinate: MapCoordinate,
        var lastDelta: MapCoordinateDelta,
        val originalSnapshot: MapPlacedObjectSnapshot,
    )

    fun cancelTransientState(targetId: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId? = null) {
        if (targetId == null || dragState?.drawing?.id == targetId) {
            dragState = null
        }
    }
}
