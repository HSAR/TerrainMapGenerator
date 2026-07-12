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
    private val isSelectableObject: (MapPlacedObjectSnapshot) -> Boolean = { true },
    private val onStateChanged: (MapSelectionState) -> Unit,
    private val onMovePreviewChanged: (MapObjectMovePreview?) -> Unit = {},
    private val onLedgerEntry: (LedgerEntry) -> Unit,
) {
    private val selectedIds = linkedSetOf<MapPlacedObjectId>()
    private var boxState: BoxSelectionState? = null
    private var moveState: GroupMoveState? = null
    private var pivotState: PivotState? = null
    private var resizeState: ResizeState? = null

    val isDragging: Boolean
        get() = boxState != null || moveState != null || pivotState != null || resizeState != null

    val isMovingSelection: Boolean
        get() = moveState != null

    fun selectedIds(): Set<MapPlacedObjectId> =
        selectedIds.toSet()

    fun hasPivotHandleAt(screenPoint: Vector2D): Boolean =
        boxState == null &&
                selectedIds
                    .mapNotNull(objectController::snapshot)
                    .pivotHandle(groupFrame = currentGroupFrame())
                    ?.contains(screenPoint) == true

    fun hasResizeHandleAt(screenPoint: Vector2D): Boolean =
        boxState == null &&
                selectedIds
                    .mapNotNull(objectController::snapshot)
                    .resizeHandleAt(screenPoint) != null

    fun refresh() {
        publish()
    }

    fun clear() {
        selectedIds.clear()
        boxState = null
        moveState = null
        resizeState = null
        publish()
    }

    fun cancelTransientState() {
        moveState?.let { state ->
            state.originalSnapshots.forEach(objectController::updateObject)
        }
        onMovePreviewChanged(null)
        boxState = null
        moveState = null
        pivotState?.originalSnapshots?.forEach(objectController::updateObject)
        pivotState = null
        resizeState?.originalSnapshots?.filterIsInstance<DrawingSnapshot>()?.forEach(objectController::updateObject)
        resizeState = null
        publish()
    }

    fun selectOnly(id: MapPlacedObjectId) {
        selectedIds.clear()
        selectedIds += id
        publish()
    }

    fun restoreSelection(ids: Iterable<MapPlacedObjectId>) {
        selectedIds.clear()
        selectedIds += ids.filter { objectController.snapshot(it)?.let(isSelectableObject) == true }
        publish()
    }

    fun beginLeftDrag(screenPoint: Vector2D, altHeld: Boolean = false, shiftHeld: Boolean = false): Boolean {
        if (beginResizeIfHandleHit(screenPoint, altHeld)) {
            return true
        }
        if (beginPivotIfHandleHit(screenPoint)) {
            return true
        }
        val selection = objectAt(screenPoint)
        return if (selection != null) {
            if (shiftHeld) {
                toggleSelected(selection.id)
                publish()
                true
            } else {
                if (selection.id !in selectedIds) {
                    selectedIds.clear()
                    selectedIds += selection.id
                }
                beginGroupMove(screenPoint)
                true
            }
        } else {
            boxState = BoxSelectionState(start = screenPoint, current = screenPoint, shiftHeld = shiftHeld)
            moveState = null
            publish()
            true
        }
    }

    fun update(pointer: Vector2D, leftHeld: Boolean, shiftHeld: Boolean = false) {
        resizeState?.let { updateResize(it, pointer, leftHeld) }
        pivotState?.let { updatePivot(it, pointer, leftHeld, shiftHeld) }
        moveState?.let { updateGroupMove(it, pointer, leftHeld) }
        boxState?.let { updateBoxSelection(it, pointer, leftHeld) }
    }

    private fun beginResizeIfHandleHit(screenPoint: Vector2D, altHeld: Boolean): Boolean {
        val selectedSnapshots = selectedIds.mapNotNull(objectController::snapshot)
        val handle = selectedSnapshots.resizeHandleAt(screenPoint) ?: return false
        val bounds = selectedSnapshots.combinedSelectionBounds(coordinateSpace) ?: return false
        val resizeableSnapshots = selectedSnapshots.filterIsInstance<DrawingSnapshot>()
        if (resizeableSnapshots.isEmpty()) {
            return false
        }
        val singleDrawing = selectedSnapshots.singleOrNull() as? DrawingSnapshot
        val originalFrame = singleDrawing?.selectionFrame(
            coordinateSpace = coordinateSpace,
            viewport = viewportProvider(),
            screenPadding = SELECTION_FRAME_SCREEN_PADDING,
        )
        val originalFixedCorner = originalFrame
            ?.corner(handle.corner.opposite())
            ?.let { coordinateSpace.screenToMap(it, viewportProvider()) }
        resizeState = ResizeState(
            grabbedCorner = handle.corner,
            originalBounds = bounds,
            anchor = singleDrawing?.let {
                MapSelectionResize.anchorFor(
                    drawing = it.drawing,
                    grabbedCorner = handle.corner,
                    altHeld = altHeld,
                    aspectRatio = coordinateSpace.aspectRatio,
                )
            } ?: MapSelectionResize.anchorFor(
                bounds = bounds,
                grabbedCorner = handle.corner,
                selectionSize = selectedSnapshots.size,
                altHeld = altHeld,
            ),
            originalFixedCorner = originalFixedCorner,
            originalSnapshots = selectedSnapshots,
        )
        boxState = null
        moveState = null
        pivotState = null
        applyResize(resizeState ?: return false, screenPoint)
        publish()
        return true
    }

    private fun beginPivotIfHandleHit(screenPoint: Vector2D): Boolean {
        val selectedSnapshots = selectedIds.mapNotNull(objectController::snapshot)
        val handle = selectedSnapshots.pivotHandle(groupFrame = currentGroupFrame(selectedSnapshots)) ?: return false
        if (!handle.contains(screenPoint)) {
            return false
        }
        val pivotCenter = MapSelectionPivot.pivotCenter(selectedSnapshots, coordinateSpace)
        val pivotCenterWorld = coordinateSpace.toWorld(pivotCenter)
        pivotState = PivotState(
            pivotCenterWorld = pivotCenterWorld,
            startAngle = MapSelectionPivot.bearingAngle(
                pivotCenterWorld,
                coordinateSpace.screenToWorld(screenPoint, viewportProvider()),
            ),
            originalSnapshots = selectedSnapshots,
            originalGroupFrame = selectedSnapshots.groupSelectionFrame(coordinateSpace, viewportProvider()),
            deltaRadians = 0.0,
        )
        boxState = null
        moveState = null
        applyPivot(pivotState ?: return false, 0.0, shiftHeld = false)
        publish()
        return true
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
        val snapshots = selectedIds.mapNotNull(objectController::snapshot).filter(isSelectableObject)
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

    private fun updatePivot(state: PivotState, pointer: Vector2D, leftHeld: Boolean, shiftHeld: Boolean) {
        if (!leftHeld) {
            commitPivot(state)
            pivotState = null
            publish()
            return
        }

        val currentAngle = MapSelectionPivot.bearingAngle(
            state.pivotCenterWorld,
            coordinateSpace.screenToWorld(pointer, viewportProvider()),
        )
        state.deltaRadians = currentAngle - state.startAngle
        applyPivot(state, state.deltaRadians, shiftHeld)
        publish()
    }

    private fun updateResize(state: ResizeState, pointer: Vector2D, leftHeld: Boolean) {
        if (!leftHeld) {
            commitResize(state)
            resizeState = null
            publish()
            return
        }

        applyResize(state, pointer)
        publish()
    }

    private fun applyPivot(state: PivotState, deltaRadians: Double, shiftHeld: Boolean) {
        MapSelectionPivot.rotateSnapshots(
            originalSnapshots = state.originalSnapshots,
            pivotCenterWorld = state.pivotCenterWorld,
            deltaRadians = deltaRadians,
            coordinateSpace = coordinateSpace,
            shiftHeld = shiftHeld,
        )
            .forEach(objectController::updateObject)
    }

    private fun applyResize(state: ResizeState, pointer: Vector2D) {
        MapSelectionResize.resizeSnapshots(
            originalSnapshots = state.originalSnapshots,
            bounds = state.originalBounds,
            grabbedCorner = state.grabbedCorner,
            anchor = state.anchor,
            draggedCorner = coordinateSpace.screenToMap(pointer, viewportProvider()),
            originalFixedCorner = state.originalFixedCorner,
            aspectRatio = coordinateSpace.aspectRatio,
        )
            .forEach(objectController::updateObject)
    }

    private fun commitPivot(state: PivotState) {
        val actions = state.originalSnapshots.mapNotNull { before ->
            val after = objectController.snapshot(before.id) ?: return@mapNotNull null
            MapPlacedObjectPatch.diff(before, after)
                .takeIf { it.isNotEmpty() }
                ?.let { EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before)) }
        }
        recordBatch(actions)
    }

    private fun commitResize(state: ResizeState) {
        val actions = state.originalSnapshots.mapNotNull { before ->
            val after = objectController.snapshot(before.id) ?: return@mapNotNull null
            MapPlacedObjectPatch.diff(before, after)
                .takeIf { it.isNotEmpty() }
                ?.let { EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before)) }
        }
        recordBatch(actions)
    }

    private fun updateMovePreview(state: GroupMoveState) {
        if (state.originalSnapshots.none { it is UnitSnapshot }) {
            onMovePreviewChanged(null)
            return
        }

        val before = state.originalSnapshots.selectionCentroid(coordinateSpace) ?: run {
            onMovePreviewChanged(null)
            return
        }
        val after = state.originalSnapshots
            .mapNotNull { objectController.snapshot(it.id) }
            .selectionCentroid(coordinateSpace)
            ?: run {
                onMovePreviewChanged(null)
                return
            }

        onMovePreviewChanged(
            MapObjectMovePreview(
                origin = before,
                destination = after,
                color = state.originalSnapshots.measureTextColor(),
            ),
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
            if (distance >= BOX_DRAG_THRESHOLD) {
                val overlapping = objectsOverlapping(ScreenSelectionBox(state.start, state.current)).map { it.id }
                if (state.shiftHeld) {
                    overlapping.forEach(::toggleSelected)
                } else {
                    selectedIds.clear()
                    selectedIds += overlapping
                }
            } else if (!state.shiftHeld) {
                selectedIds.clear()
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
            .filter(isSelectableObject)
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

    private fun toggleSelected(id: MapPlacedObjectId) {
        if (!selectedIds.add(id)) {
            selectedIds.remove(id)
        }
    }

    private fun publish() {
        val selectedSnapshots = selectedIds.mapNotNull(objectController::snapshot)
        val groupFrame = currentGroupFrame(selectedSnapshots)
        onStateChanged(
            MapSelectionState(
                selected = selectedSnapshots,
                box = boxState
                    ?.takeIf { hypot(it.current.x - it.start.x, it.current.y - it.start.y) >= BOX_DRAG_THRESHOLD }
                    ?.let { ScreenSelectionBox(it.start, it.current) },
                groupFrame = groupFrame,
                pivotHandle = selectedSnapshots
                    .takeIf {
                        shouldPublishSelectionPivotHandle(
                            boxSelectionActive = boxState != null,
                            moveActive = moveState != null || resizeState != null,
                        )
                    }
                    ?.pivotHandle(groupFrame = groupFrame),
            ),
        )
    }

    private fun currentGroupFrame(
        snapshots: List<MapPlacedObjectSnapshot> = selectedIds.mapNotNull(objectController::snapshot),
    ): ScreenSelectionFrame? =
        when {
            snapshots.size <= 1 -> null
            pivotState != null -> pivotState?.originalGroupFrame
                ?.rotatedAround(
                    pivot = coordinateSpace.worldToScreen(pivotState!!.pivotCenterWorld, viewportProvider()),
                    deltaRadians = pivotState!!.deltaRadians,
                )

            else -> snapshots.groupSelectionFrame(coordinateSpace, viewportProvider())
        }

    private fun List<MapPlacedObjectSnapshot>.pivotHandle(groupFrame: ScreenSelectionFrame? = null): ScreenPivotHandle? =
        pivotHandleForSelection(this, coordinateSpace, viewportProvider(), groupFrame)

    private fun List<MapPlacedObjectSnapshot>.resizeHandleAt(screenPoint: Vector2D): ScreenCornerBracket? =
        takeIf { it.any { snapshot -> snapshot is DrawingSnapshot } }
            ?.resizeFrame()
            ?.cornerBrackets(SELECTION_BRACKET_LENGTH_PX, SELECTION_BRACKET_THICKNESS_PX)
            ?.firstOrNull { it.contains(screenPoint) }

    private fun List<MapPlacedObjectSnapshot>.resizeFrame(): ScreenSelectionFrame? =
        when {
            isEmpty() -> null
            size == 1 -> single().selectionFrame(coordinateSpace, viewportProvider(), SELECTION_FRAME_SCREEN_PADDING)
            else -> groupSelectionFrame(coordinateSpace, viewportProvider())
        }

    private data class BoxSelectionState(
        val start: Vector2D,
        var current: Vector2D,
        val shiftHeld: Boolean,
    )

    private data class GroupMoveState(
        val startCoordinate: MapCoordinate,
        var lastDelta: MapCoordinateDelta,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
    )

    private data class PivotState(
        val pivotCenterWorld: Vector2D,
        val startAngle: Double,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
        val originalGroupFrame: ScreenSelectionFrame?,
        var deltaRadians: Double,
    )

    private data class ResizeState(
        val grabbedCorner: ScreenSelectionCorner,
        val originalBounds: MapSelectionBounds,
        val anchor: MapCoordinate,
        val originalFixedCorner: MapCoordinate?,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
    )

    private companion object {
        private const val BOX_DRAG_THRESHOLD = 4.0
    }
}

internal fun shouldPublishSelectionPivotHandle(
    boxSelectionActive: Boolean,
    moveActive: Boolean,
): Boolean =
    !boxSelectionActive && !moveActive

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

internal fun List<MapPlacedObjectSnapshot>.selectionCentroid(coordinateSpace: MapCoordinateSpace): MapCoordinate? =
    takeIf { it.isNotEmpty() }
        ?.map { it.selectionBounds(coordinateSpace) }
        ?.let { bounds ->
            MapCoordinate(
                x = bounds.map { (it.left + it.right) * 0.5 }.average(),
                y = bounds.map { (it.top + it.bottom) * 0.5 }.average(),
            )
        }
