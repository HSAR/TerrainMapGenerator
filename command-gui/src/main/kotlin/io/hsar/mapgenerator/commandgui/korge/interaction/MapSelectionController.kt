package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import io.hsar.mapgenerator.commandgui.korge.adapter.toLegacyObject
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperation
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationContent
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationObject
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationTransformGesture
import io.hsar.mapgenerator.commandgui.operation.OperationTransformModifiers
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.selectionBounds
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
    private val constrainUnitMove: (UnitSnapshot, Boolean) -> UnitSnapshot = { snapshot, _ -> snapshot },
    private val onGraphicTemplateResized: (GraphicTemplateId, Double, Double) -> LedgerEntry? = { _, _, _ -> null },
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
        resizeState?.originalSnapshots?.filter { it is DrawingSnapshot || it is GraphicSnapshot }?.forEach(objectController::updateObject)
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
        resizeState?.let { updateResize(it, pointer, leftHeld, shiftHeld) }
        pivotState?.let { updatePivot(it, pointer, leftHeld, shiftHeld) }
        moveState?.let { updateGroupMove(it, pointer, leftHeld, shiftHeld) }
        boxState?.let { updateBoxSelection(it, pointer, leftHeld) }
    }

    private fun beginResizeIfHandleHit(screenPoint: Vector2D, altHeld: Boolean): Boolean {
        val selectedSnapshots = selectedIds.mapNotNull(objectController::snapshot)
        val handle = selectedSnapshots.resizeHandleAt(screenPoint) ?: return false
        val bounds = selectedSnapshots.combinedSelectionBounds(coordinateSpace) ?: return false
        val resizeableSnapshots = selectedSnapshots.filter(MapPlacedObjectSnapshot::supportsResizeHandles)
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
        val resizedTemplateIds = selectedSnapshots
            .filterIsInstance<GraphicSnapshot>()
            .map(GraphicSnapshot::graphicTemplateId)
            .toSet()
        val originalSnapshots = (
                selectedSnapshots + objectController.snapshots()
                    .filterIsInstance<GraphicSnapshot>()
                    .filter { it.graphicTemplateId in resizedTemplateIds }
                ).distinctBy(MapPlacedObjectSnapshot::id)
        val originalContent = objectController.snapshots().toOperationContent()
        resizeState = ResizeState(
            gesture = OperationTransformGesture.Resize(
                original = originalContent,
                selectedIds = selectedSnapshots.mapTo(linkedSetOf()) { it.id.toOperation() },
                grabbedCorner = MapSelectionCorner.valueOf(handle.corner.name),
                aspectRatio = coordinateSpace.aspectRatio,
                originalFixedCorner = originalFixedCorner,
            ),
            originalSnapshots = originalSnapshots,
            altHeld = altHeld,
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
        val originalContent = objectController.snapshots().toOperationContent()
        val selectedOperationIds = selectedSnapshots.mapTo(linkedSetOf()) { it.id.toOperation() }
        val pivotCenter = originalContent.selectionBounds(selectedOperationIds, coordinateSpace.aspectRatio)?.centre ?: return false
        pivotState = PivotState(
            gesture = OperationTransformGesture.Rotate(
                original = originalContent,
                selectedIds = selectedOperationIds,
                pivot = pivotCenter,
                start = coordinateSpace.screenToMap(screenPoint, viewportProvider()),
                aspectRatio = coordinateSpace.aspectRatio,
            ),
            originalSnapshots = selectedSnapshots,
            originalGroupFrame = selectedSnapshots.groupSelectionFrame(coordinateSpace, viewportProvider()),
            deltaRadians = 0.0,
        )
        boxState = null
        moveState = null
        (pivotState ?: return false).let { applyPivot(it, it.gesture.start, shiftHeld = false) }
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
        val originalContent = objectController.snapshots().toOperationContent()
        moveState = GroupMoveState(
            gesture = OperationTransformGesture.Move(
                original = originalContent,
                selectedIds = snapshots.mapTo(linkedSetOf()) { it.id.toOperation() },
                start = startCoordinate,
                backgroundBounds = coordinateSpace.backgroundBounds,
                aspectRatio = coordinateSpace.aspectRatio,
                constrainUnit = { unit, bypass ->
                    (constrainUnitMove(unit.toLegacyObject(originalContent) as UnitSnapshot, bypass).toOperationObject() as OperationUnit)
                },
            ),
            originalSnapshots = snapshots,
        )
        boxState = null
        publish()
        return true
    }

    private fun updateGroupMove(state: GroupMoveState, pointer: Vector2D, leftHeld: Boolean, shiftHeld: Boolean) {
        if (!leftHeld) {
            commitGroupMove(state)
            moveState = null
            onMovePreviewChanged(null)
            publish()
            return
        }

        applyPreview(
            state.gesture.preview(
                coordinateSpace.screenToMap(pointer, viewportProvider()),
                OperationTransformModifiers(shiftHeld = shiftHeld),
            )
        )
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

        val current = coordinateSpace.screenToMap(pointer, viewportProvider())
        state.deltaRadians = io.hsar.mapgenerator.commandgui.operation.rotationDelta(
            state.gesture.pivot, state.gesture.start, current, coordinateSpace.aspectRatio,
        )
        applyPivot(state, current, shiftHeld)
        publish()
    }

    private fun updateResize(state: ResizeState, pointer: Vector2D, leftHeld: Boolean, shiftHeld: Boolean) {
        if (!leftHeld) {
            commitResize(state)
            resizeState = null
            publish()
            return
        }

        applyResize(state, pointer, shiftHeld)
        publish()
    }

    private fun applyPivot(state: PivotState, pointer: MapCoordinate, shiftHeld: Boolean) =
        applyPreview(state.gesture.preview(pointer, OperationTransformModifiers(shiftHeld = shiftHeld)))

    private fun applyResize(state: ResizeState, pointer: Vector2D, shiftHeld: Boolean = false) {
        applyPreview(
            state.gesture.preview(
                coordinateSpace.screenToMap(pointer, viewportProvider()),
                OperationTransformModifiers(altHeld = state.altHeld, shiftHeld = shiftHeld),
            )
        )
    }

    private fun applyPreview(content: OperationContent) = content.objects.forEach { operationObject ->
        objectController.previewObject(operationObject.toLegacyObject(content))
    }

    private fun commitPivot(state: PivotState) {
        val actions = state.originalSnapshots.mapNotNull { before ->
            val after = objectController.snapshot(before.id) ?: return@mapNotNull null
            MapPlacedObjectPatch.diff(before, after)
                .takeIf { it.isNotEmpty() }
                ?.let { EditObjectAction.fromSnapshots(before, after) }
        }
        recordBatch(actions)
    }

    private fun commitResize(state: ResizeState) {
        val objectActions = state.originalSnapshots.mapNotNull { before ->
            val after = objectController.snapshot(before.id) ?: return@mapNotNull null
            MapPlacedObjectPatch.diff(before, after)
                .takeIf { it.isNotEmpty() }
                ?.let { EditObjectAction.fromSnapshots(before, after) }
        }
        val templateActions = state.originalSnapshots
            .filterIsInstance<GraphicSnapshot>()
            .distinctBy(GraphicSnapshot::graphicTemplateId)
            .mapNotNull { before ->
                (objectController.snapshot(before.id) as? GraphicSnapshot)
                    ?.let { after ->
                        onGraphicTemplateResized(
                            after.graphicTemplateId,
                            after.normalizedWidth,
                            after.normalizedHeight,
                        )
                    }
            }
        recordBatch(objectActions + templateActions)
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
                ?.let { EditObjectAction.fromSnapshots(before, after) }
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
                    pivot = coordinateSpace.worldToScreen(coordinateSpace.toWorld(pivotState!!.gesture.pivot), viewportProvider()),
                    deltaRadians = pivotState!!.deltaRadians,
                )

            else -> snapshots.groupSelectionFrame(coordinateSpace, viewportProvider())
        }

    private fun List<MapPlacedObjectSnapshot>.pivotHandle(groupFrame: ScreenSelectionFrame? = null): ScreenPivotHandle? =
        pivotHandleForSelection(this, coordinateSpace, viewportProvider(), groupFrame)

    private fun List<MapPlacedObjectSnapshot>.resizeHandleAt(screenPoint: Vector2D): ScreenCornerBracket? =
        takeIf { it.any(MapPlacedObjectSnapshot::supportsResizeHandles) }
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
        val gesture: OperationTransformGesture.Move,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
    )

    private data class PivotState(
        val gesture: OperationTransformGesture.Rotate,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
        val originalGroupFrame: ScreenSelectionFrame?,
        var deltaRadians: Double,
    )

    private data class ResizeState(
        val gesture: OperationTransformGesture.Resize,
        val originalSnapshots: List<MapPlacedObjectSnapshot>,
        val altHeld: Boolean,
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

internal fun List<MapPlacedObjectSnapshot>.selectionCentroid(coordinateSpace: MapCoordinateSpace): MapCoordinate? =
    takeIf { it.isNotEmpty() }
        ?.map { it.selectionBounds(coordinateSpace) }
        ?.let { bounds ->
            MapCoordinate(
                x = bounds.map { (it.left + it.right) * 0.5 }.average(),
                y = bounds.map { (it.top + it.bottom) * 0.5 }.average(),
            )
        }
