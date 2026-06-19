package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
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
    private val canInteractWithDrawings: () -> Boolean = { true },
    private val onDrawingsChanged: (MapDrawingState) -> Unit = {},
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private var dragState: DragState? = null
    private var previousLeftHeld = false
    private var previousRightHeld = false

    fun install() {
        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            val tool = currentTool()
            if (tool != MapViewerTool.SELECT) {
                dragState = null
                previousLeftHeld = false
            }
            if (!canInteractWithDrawings()) {
                dragState = null
                previousLeftHeld = false
            }
            if (tool == MapViewerTool.SELECT && canInteractWithDrawings() && updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))) {
                onDrawingsChanged(drawingStore.state)
            }
            if (tool == MapViewerTool.SELECT && canInteractWithDrawings() && updateContextMenu(input.mousePos, input.mouseButtonPressed(MouseButton.RIGHT))) {
                onDrawingsChanged(drawingStore.state)
            }
            if (!input.mouseButtonPressed(MouseButton.LEFT)) {
                previousLeftHeld = false
            }
            previousRightHeld = input.mouseButtonPressed(MouseButton.RIGHT)
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

    private fun updateContextMenu(pointer: Vector2D, rightHeld: Boolean): Boolean {
        if (!rightHeld || previousRightHeld) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = pointer,
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return false
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val drawing = drawingStore.findAt(mapCoordinate, tolerance, coordinateSpace) ?: return false
        contextMenuManager.open(
            target = drawing,
            screenPoint = pointer,
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
            if (!leftHeld || previousLeftHeld || contextMenuManager.isOpen) {
                previousLeftHeld = leftHeld
                return false
            }

            val viewport = viewportProvider()
            val mapCoordinate = coordinateSpace.resolveScreenPoint(
                screenPoint = pointer,
                viewport = viewport,
                bounds = coordinateSpace.backgroundBounds,
            ) ?: run {
                previousLeftHeld = leftHeld
                return false
            }
            val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
            val target = drawingStore.findAt(mapCoordinate, tolerance, coordinateSpace) ?: run {
                previousLeftHeld = leftHeld
                return false
            }
            dragState = DragState(
                drawing = target,
                startMapCoordinate = mapCoordinate,
                lastDelta = MapCoordinateDelta(0.0, 0.0),
                originalSnapshot = target.snapshot(),
            )
            previousLeftHeld = leftHeld
            return false
        }

        if (!leftHeld) {
            val updatedSnapshot = state.drawing.snapshot()
            if (updatedSnapshot != state.originalSnapshot) {
                onObjectMoved(state.originalSnapshot, updatedSnapshot)
            }
            dragState = null
            previousLeftHeld = false
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
        previousLeftHeld = leftHeld
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
