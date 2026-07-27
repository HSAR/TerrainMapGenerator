package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import korlibs.event.MouseButton
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.math.geom.Vector2D
import kotlin.math.hypot

internal class MapPlacedGraphicManager(
    private val root: Container,
    private val graphicLayer: Container,
    geometry: ViewerGeometry,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val viewportProvider: () -> CameraViewport,
    private val contextMenuManager: ContextMenuManager,
    private val onEditNameRequested: (GraphicSnapshot) -> Unit = {},
    private val noteReferenceMenuActionsProvider: (MapPlacedObjectSnapshot) -> List<MapPlacedObjectMenuAction> = { emptyList() },
    private val canInteract: () -> Boolean = { true },
    private val isHigherPriorityDragInProgress: () -> Boolean = { false },
    private val onMovePreviewChanged: (MapObjectMovePreview?) -> Unit = {},
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private val graphics = mutableListOf<PlacedGraphic>()
    private var dragState: DragState? = null

    val isDragging: Boolean
        get() = dragState != null

    fun install() = root.addUpdater {
        val input = root.stage?.views?.input ?: return@addUpdater
        updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
    }

    fun refresh() = graphics.forEach(::positionView)

    fun selectionAt(screenPoint: Vector2D): MapPlacedObjectSelection? {
        if (!canInteract()) return null
        val map = coordinateSpace.screenToMap(screenPoint, viewportProvider())
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewportProvider())
        return graphics.asSequence()
            .filter { it.containsMapCoordinate(map, tolerance, coordinateSpace) }
            .minByOrNull { graphic ->
                val center = coordinateSpace.worldToScreen(coordinateSpace.toWorld(graphic.snapshot.mapCoordinate), viewportProvider())
                hypot(center.x - screenPoint.x, center.y - screenPoint.y)
            }
            ?.let { MapPlacedObjectSelection(MapPlacedObjectSelectionKind.GRAPHIC, it.id, 0.0) }
    }

    fun hasDragCandidate(screenPoint: Vector2D): Boolean = selectionAt(screenPoint) != null

    fun blocksScreenPoint(screenPoint: Vector2D): Boolean = selectionAt(screenPoint) != null

    fun beginDragAt(screenPoint: Vector2D): Boolean {
        if (!canInteract() || dragState != null || isHigherPriorityDragInProgress()) return false
        val selection = selectionAt(screenPoint) ?: return false
        val target = graphics.firstOrNull { it.id == selection.id } ?: return false
        dragState = DragState(target, coordinateSpace.screenToMap(screenPoint, viewportProvider()), target.snapshot)
        return true
    }

    fun openContextMenuAt(screenPoint: Vector2D): Boolean {
        if (!canInteract()) return false
        val selection = selectionAt(screenPoint) ?: return false
        val target = graphics.firstOrNull { it.id == selection.id } ?: return false
        contextMenuManager.open(
            target = target,
            screenPoint = screenPoint,
            actions = listOf(
                MapPlacedObjectMenuAction("Edit Name", { onEditNameRequested(target.snapshot) }),
                MapPlacedObjectMenuAction("Delete", { onObjectDeleted(target.snapshot) }),
            ) +
                    noteReferenceMenuActionsProvider(target.snapshot),
        )
        return true
    }

    fun upsert(snapshot: GraphicSnapshot, notify: Boolean = true) {
        graphics.firstOrNull { it.id == snapshot.id }?.let { placed ->
            placed.snapshot = snapshot
            placed.rebuild()
        } ?: graphics.add(PlacedGraphic(snapshot, graphicLayer.container()))
        graphics.first { it.id == snapshot.id }.let(::positionView)
        if (notify) refresh()
    }

    fun preview(snapshot: GraphicSnapshot) = upsert(snapshot, notify = false)

    fun restoreAll(snapshots: List<GraphicSnapshot>) {
        graphics.forEach { it.container.removeFromParent() }
        graphics.clear()
        snapshots.forEach { upsert(it, notify = false) }
        refresh()
    }

    fun remove(id: MapPlacedObjectId): GraphicSnapshot? = graphics.indexOfFirst { it.id == id }
        .takeIf { it >= 0 }
        ?.let { index -> graphics.removeAt(index).also { it.container.removeFromParent() }.snapshot }

    fun snapshot(id: MapPlacedObjectId): GraphicSnapshot? = graphics.firstOrNull { it.id == id }?.snapshot

    fun snapshots(): List<GraphicSnapshot> = graphics.map { it.snapshot }

    fun replaceTemplate(snapshot: GraphicTemplateSnapshot) = graphics
        .filter { it.snapshot.graphicTemplateId == snapshot.id }
        .forEach { placed ->
            placed.snapshot = placed.snapshot.copy(template = snapshot.template)
            placed.rebuild()
            positionView(placed)
        }

    fun cancelTransientState() {
        dragState = null
        onMovePreviewChanged(null)
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean) {
        val state = dragState ?: return
        if (!leftHeld) {
            val updated = state.graphic.snapshot
            if (updated != state.original) onObjectMoved(state.original, updated)
            dragState = null
            onMovePreviewChanged(null)
            return
        }
        val current = coordinateSpace.resolveScreenPointClamped(pointer, viewportProvider(), coordinateSpace.backgroundBounds)
        val delta = MapCoordinateDelta(current.x - state.start.x, current.y - state.start.y)
        state.graphic.snapshot = state.original.copy(
            mapCoordinate = MapCoordinate(state.original.mapCoordinate.x + delta.x, state.original.mapCoordinate.y + delta.y),
        )
        positionView(state.graphic)
        onMovePreviewChanged(MapObjectMovePreview(state.original.mapCoordinate, state.graphic.snapshot.mapCoordinate))
    }

    private fun positionView(placed: PlacedGraphic) {
        val world = coordinateSpace.toWorld(placed.snapshot.mapCoordinate)
        val worldWidth = coordinateSpace.toWorldWidth(placed.snapshot.normalizedWidth)
        val worldHeight = coordinateSpace.toWorldHeight(placed.snapshot.normalizedHeight)
        placed.container.x = 0.0
        placed.container.y = 0.0
        placed.view.scaleX = worldWidth / placed.snapshot.template.bitmap.width
        placed.view.scaleY = worldHeight / placed.snapshot.template.bitmap.height
        placed.view.x = world.x - worldWidth * 0.5
        placed.view.y = world.y - worldHeight * 0.5
        placed.labelView?.let { label ->
            val zoom = viewportProvider().zoom.coerceAtLeast(0.0001)
            val size = LABEL_TEXT_SIZE / zoom
            val bounds = MapViewerTextViews.measure(label.text, size = size)
            label.textSize = size
            label.setTextBounds(korlibs.math.geom.Rectangle(0.0, 0.0, bounds.width, bounds.height))
            label.x = world.x - bounds.width * 0.5
            label.y = world.y - worldHeight * 0.5 - bounds.height - LABEL_GAP / zoom
        }
    }

    private class PlacedGraphic(
        var snapshot: GraphicSnapshot,
        val container: Container,
    ) : MapPlacedObject {
        var view: Image = container.image(snapshot.template.bitmap)
        var labelView: Text? = null

        init {
            rebuild()
        }
        override val id: MapPlacedObjectId get() = snapshot.id
        override fun snapshot(): MapPlacedObjectSnapshot = snapshot
        override fun containsMapCoordinate(mapCoordinate: MapCoordinate, tolerance: Double, coordinateSpace: MapCoordinateSpace?): Boolean =
            snapshot.selectionBounds(coordinateSpace ?: error("Coordinate space required")).expanded(tolerance).contains(mapCoordinate)

        override fun moveBy(mapDelta: MapCoordinateDelta) {
            snapshot = snapshot.copy(mapCoordinate = MapCoordinate(snapshot.mapCoordinate.x + mapDelta.x, snapshot.mapCoordinate.y + mapDelta.y))
        }

        override fun contextMenuActions(): List<MapPlacedObjectMenuAction> = emptyList()
        fun rebuild() {
            container.removeChildren()
            view = container.image(snapshot.template.bitmap)
            labelView = snapshot.displayName()?.let { displayName ->
                MapViewerTextViews.box(
                    parent = container,
                    text = displayName,
                    width = 1.0,
                    height = 1.0,
                    size = LABEL_TEXT_SIZE,
                    alignment = TextAlignment.MIDDLE_CENTER,
                )
            }
        }
    }

    private data class DragState(val graphic: PlacedGraphic, val start: MapCoordinate, val original: GraphicSnapshot)

    private companion object {
        const val HIT_TOLERANCE = 8.0
        const val LABEL_TEXT_SIZE = 24.0
        const val LABEL_GAP = 4.0
    }
}

private fun MapSelectionBounds.contains(point: MapCoordinate): Boolean =
    point.x in left..right && point.y in top..bottom
