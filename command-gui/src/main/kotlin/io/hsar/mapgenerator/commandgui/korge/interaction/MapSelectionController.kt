package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingTranslator
import korlibs.math.geom.Vector2D
import kotlin.math.hypot

internal class MapSelectionController(
    private val coordinateSpace: MapCoordinateSpace,
    private val viewportProvider: () -> CameraViewport,
    private val objectController: MapObjectController,
    private val contextMenuManager: ContextMenuManager,
    private val objectAt: (Vector2D) -> MapPlacedObjectSelection?,
    private val onStateChanged: (MapSelectionState) -> Unit,
    private val onMovePreviewChanged: (MapObjectMovePreview?) -> Unit = {},
    private val onLedgerEntry: (LedgerEntry) -> Unit,
) {
    private val selectedIds = linkedSetOf<MapPlacedObjectId>()
    private var boxState: BoxSelectionState? = null
    private var moveState: GroupMoveState? = null

    val isDragging: Boolean
        get() = boxState != null || moveState != null

    fun selectedIds(): Set<MapPlacedObjectId> =
        selectedIds.toSet()

    fun refresh() {
        publish()
    }

    fun clear() {
        selectedIds.clear()
        boxState = null
        moveState = null
        publish()
    }

    fun cancelTransientState() {
        moveState?.let { state ->
            state.originalSnapshots.forEach(objectController::updateObject)
        }
        onMovePreviewChanged(null)
        boxState = null
        moveState = null
        publish()
    }

    fun selectOnly(id: MapPlacedObjectId) {
        selectedIds.clear()
        selectedIds += id
        publish()
    }

    fun beginLeftDrag(screenPoint: Vector2D): Boolean {
        val selection = objectAt(screenPoint)
        return if (selection != null) {
            if (selection.id !in selectedIds) {
                selectedIds.clear()
                selectedIds += selection.id
            }
            beginGroupMove(screenPoint)
            true
        } else {
            boxState = BoxSelectionState(start = screenPoint, current = screenPoint)
            moveState = null
            publish()
            true
        }
    }

    fun update(pointer: Vector2D, leftHeld: Boolean) {
        moveState?.let { updateGroupMove(it, pointer, leftHeld) }
        boxState?.let { updateBoxSelection(it, pointer, leftHeld) }
    }

    fun openContextMenu(screenPoint: Vector2D): Boolean {
        val selection = objectAt(screenPoint) ?: return false
        if (selection.id !in selectedIds) {
            selectedIds.clear()
            selectedIds += selection.id
            publish()
            return false
        }
        if (selectedIds.size <= 1) {
            return false
        }

        contextMenuManager.open(
            target = object : MapContextMenuTarget {
                override fun contextMenuActions() = listOf(
                    MapPlacedObjectMenuAction(
                        label = "Delete Selected",
                        onSelected = ::deleteSelected,
                    ),
                )
            },
            screenPoint = screenPoint,
        )
        return true
    }

    private fun beginGroupMove(screenPoint: Vector2D): Boolean {
        val snapshots = selectedIds.mapNotNull(objectController::snapshot)
        if (snapshots.isEmpty()) {
            clear()
            return false
        }
        val startCoordinate = coordinateSpace.screenToMap(screenPoint, viewportProvider())
        moveState = GroupMoveState(
            startCoordinate = startCoordinate,
            lastDelta = MapCoordinateDelta(0.0, 0.0),
            originalSnapshots = snapshots,
        )
        boxState = null
        publish()
        return true
    }

    private fun updateGroupMove(state: GroupMoveState, pointer: Vector2D, leftHeld: Boolean) {
        if (!leftHeld) {
            commitGroupMove(state)
            moveState = null
            onMovePreviewChanged(null)
            publish()
            return
        }

        val currentCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = pointer,
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        )
        val delta = MapCoordinateDelta(
            x = currentCoordinate.x - state.startCoordinate.x,
            y = currentCoordinate.y - state.startCoordinate.y,
        )
        val incrementalDelta = MapCoordinateDelta(
            x = delta.x - state.lastDelta.x,
            y = delta.y - state.lastDelta.y,
        )
        state.originalSnapshots
            .mapNotNull { objectController.snapshot(it.id) }
            .map { it.moveBy(incrementalDelta) }
            .forEach(objectController::updateObject)
        state.lastDelta = delta
        updateMovePreview(state)
        publish()
    }

    private fun updateMovePreview(state: GroupMoveState) {
        val before = state.originalSnapshots.singleOrNull() as? UnitSnapshot
        val after = before?.let { objectController.snapshot(it.id) as? UnitSnapshot }
        onMovePreviewChanged(
            if (before != null && after != null) {
                MapObjectMovePreview(before.mapCoordinate, after.mapCoordinate)
            } else {
                null
            },
        )
    }

    private fun commitGroupMove(state: GroupMoveState) {
        val actions = state.originalSnapshots.mapNotNull { before ->
            val after = objectController.snapshot(before.id) ?: return@mapNotNull null
            MapPlacedObjectPatch.diff(before, after)
                .takeIf { it.isNotEmpty() }
                ?.let { EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before)) }
        }
        recordBatch(actions)
    }

    private fun updateBoxSelection(state: BoxSelectionState, pointer: Vector2D, leftHeld: Boolean) {
        state.current = pointer
        if (!leftHeld) {
            val distance = hypot(state.current.x - state.start.x, state.current.y - state.start.y)
            selectedIds.clear()
            if (distance >= BOX_DRAG_THRESHOLD) {
                selectedIds += objectsOverlapping(ScreenSelectionBox(state.start, state.current)).map { it.id }
            }
            boxState = null
        }
        publish()
    }

    private fun objectsOverlapping(box: ScreenSelectionBox): List<MapPlacedObjectSnapshot> {
        val viewport = viewportProvider()
        val topLeft = coordinateSpace.screenToMap(Vector2D(box.left, box.top), viewport)
        val bottomRight = coordinateSpace.screenToMap(Vector2D(box.right, box.bottom), viewport)
        val selectionBounds = MapSelectionBounds.fromPoints(topLeft, bottomRight)
        return objectController.snapshots()
            .filter { it.overlapsSelectionBox(selectionBounds, coordinateSpace) }
    }

    private fun deleteSelected() {
        val deleted = selectedIds.mapNotNull(objectController::deleteObject)
        selectedIds.clear()
        recordBatch(deleted.map(::DeleteObjectAction))
        publish()
    }

    private fun recordBatch(entries: List<LedgerEntry>) {
        when (entries.size) {
            0 -> Unit
            1 -> onLedgerEntry(entries.single())
            else -> onLedgerEntry(BatchLedgerEntry(entries))
        }
    }

    private fun publish() {
        onStateChanged(
            MapSelectionState(
                selected = selectedIds.mapNotNull(objectController::snapshot),
                box = boxState
                    ?.takeIf { hypot(it.current.x - it.start.x, it.current.y - it.start.y) >= BOX_DRAG_THRESHOLD }
                    ?.let { ScreenSelectionBox(it.start, it.current) },
            ),
        )
    }

    private data class BoxSelectionState(
        val start: Vector2D,
        var current: Vector2D,
    )

    private data class GroupMoveState(
        val startCoordinate: MapCoordinate,
        var lastDelta: MapCoordinateDelta,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
    )

    private companion object {
        private const val BOX_DRAG_THRESHOLD = 4.0
    }
}

private fun MapPlacedObjectSnapshot.moveBy(delta: MapCoordinateDelta): MapPlacedObjectSnapshot =
    when (this) {
        is UnitSnapshot -> copy(
            mapCoordinate = MapCoordinate(
                x = mapCoordinate.x + delta.x,
                y = mapCoordinate.y + delta.y,
            ),
        )

        is DrawingSnapshot -> copy(drawing = MapDrawingTranslator.translate(drawing, delta))
    }
