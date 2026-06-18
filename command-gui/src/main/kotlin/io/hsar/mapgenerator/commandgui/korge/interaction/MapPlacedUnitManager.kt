package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitEditDialog
import io.hsar.mapgenerator.grid.GridLayer
import korlibs.event.MouseButton
import korlibs.korge.input.mouse
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.math.geom.Vector2D

@OptIn(KorgeInternal::class)
internal class MapPlacedUnitManager(
    private val root: Container,
    private val unitLayer: Container,
    private val geometry: ViewerGeometry,
    private val mapMetresPerPixel: Double,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val gridLayer: GridLayer? = null,
    private val viewportProvider: () -> CameraViewport,
    private val contextMenuManager: MapPlacedObjectContextMenuManager,
    private val canInteractWithUnits: () -> Boolean = { true },
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
    private val onUnitEdited: (UnitSnapshot, UnitSnapshot) -> Unit = { _, _ -> },
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private val units = mutableListOf<PlacedUnit>()
    private val dragPreviewLayer = unitLayer.container()
    private var dragState: DragState? = null
    private var editDialog: MapViewerUnitEditDialog? = null
    private var editDialogUnitId: MapPlacedObjectId? = null

    fun install() {
        dragPreviewLayer.zIndex = DRAG_PREVIEW_Z_INDEX
        dragPreviewLayer.mouseEnabled = false

        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
        }
    }

    val isDragging: Boolean
        get() = dragState != null

    val isContextMenuOpen: Boolean
        get() = contextMenuManager.isOpen

    val isEditDialogOpen: Boolean
        get() = editDialog != null

    fun blocksScreenPoint(point: Vector2D): Boolean {
        if (!canInteractWithUnits()) {
            return false
        }
        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(point, viewport)
        return units.any { unit -> unit.containsMapCoordinate(mapCoordinate) }
    }

    fun resolvePlacement(screenPoint: Vector2D): MapCoordinate? =
        coordinateSpace.resolveScreenPoint(
            screenPoint = screenPoint,
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        )

    fun buildSnapshot(
        id: MapPlacedObjectId,
        template: MapViewerUnitTemplate,
        instanceName: String,
        mapCoordinate: MapCoordinate,
    ): UnitSnapshot {
        val unitSize = worldUnitSize(template.bitmap)
        return UnitSnapshot(
            id = id,
            template = template,
            instanceName = instanceName,
            mapCoordinate = mapCoordinate,
            normalizedWidth = unitSize.worldWidth / mapWidth,
            normalizedHeight = unitSize.worldHeight / mapHeight,
        )
    }

    fun upsert(snapshot: UnitSnapshot) {
        val existing = units.firstOrNull { it.id == snapshot.id }
        val view = unitLayer.image(snapshot.template.bitmap)
        if (existing != null) {
            existing.template = snapshot.template
            existing.instanceName = snapshot.instanceName
            existing.mapCoordinate = snapshot.mapCoordinate
            existing.normalizedWidth = snapshot.normalizedWidth
            existing.normalizedHeight = snapshot.normalizedHeight
            val unitSize = worldUnitSize(snapshot.template.bitmap)
            existing.worldWidth = unitSize.worldWidth
            existing.worldHeight = unitSize.worldHeight
            existing.view.bitmap = view.bitmap
            existing.view.scale = unitSize.worldScale
            positionView(existing)
            return
        }

        val unitSize = worldUnitSize(snapshot.template.bitmap)
        val placedUnit = PlacedUnit(
            id = snapshot.id,
            view = view,
            template = snapshot.template,
            instanceName = snapshot.instanceName,
            mapCoordinate = snapshot.mapCoordinate,
            normalizedWidth = snapshot.normalizedWidth,
            normalizedHeight = snapshot.normalizedHeight,
            worldWidth = unitSize.worldWidth,
            worldHeight = unitSize.worldHeight,
        )
        view.scale = unitSize.worldScale
        positionView(placedUnit)
        units += placedUnit
        bindViewEvents(placedUnit)
    }

    fun remove(id: MapPlacedObjectId): UnitSnapshot? =
        units.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }
            ?.let { index ->
                units.removeAt(index).also { removed ->
                    removed.view.removeFromParent()
                    if (dragState?.unit?.id == removed.id) {
                        dragState?.preview?.removeFromParent()
                        dragState = null
                    }
                    if (editDialogUnitId == removed.id) {
                        editDialog?.close()
                    }
                }.snapshot()
            }

    fun snapshot(id: MapPlacedObjectId): UnitSnapshot? =
        units.firstOrNull { it.id == id }?.snapshot()

    fun cancelTransientState(targetId: MapPlacedObjectId? = null) {
        if (targetId == null || dragState?.unit?.id == targetId) {
            dragState?.preview?.removeFromParent()
            dragState?.unit?.view?.visible = true
            dragState = null
        }
        if (targetId == null || editDialogUnitId == targetId) {
            editDialog?.close()
        }
    }

    private fun beginDrag(unit: PlacedUnit, screenPoint: Vector2D) {
        if (dragState != null || !canInteractWithUnits()) {
            return
        }

        val viewport = viewportProvider()
        val currentScreenPoint = coordinateSpace.worldToScreen(coordinateSpace.toWorld(unit.mapCoordinate), viewport)
        val grabOffset = Vector2D(
            x = screenPoint.x - currentScreenPoint.x,
            y = screenPoint.y - currentScreenPoint.y,
        )
        val mapCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = Vector2D(
                x = screenPoint.x - grabOffset.x,
                y = screenPoint.y - grabOffset.y,
            ),
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        )
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)

        unit.view.visible = false
        dragPreviewLayer.removeChildren()
        val preview = dragPreviewLayer.image(unit.template.bitmap)
        preview.scale = unit.view.scaleX
        preview.x = worldPoint.x - unit.worldWidth * 0.5
        preview.y = worldPoint.y - unit.worldHeight * 0.5
        preview.zIndex = DRAG_PREVIEW_Z_INDEX

        dragState = DragState(
            unit = unit,
            preview = preview,
            offset = grabOffset,
            originalSnapshot = unit.snapshot(),
        )
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean) {
        val state = dragState ?: return
        if (!canInteractWithUnits()) {
            dragState = null
            state.preview.removeFromParent()
            state.unit.view.visible = true
            return
        }
        if (!leftHeld) {
            val updatedSnapshot = state.unit.snapshot()
            if (updatedSnapshot != state.originalSnapshot) {
                onObjectMoved(state.originalSnapshot, updatedSnapshot)
            }
            dragState = null
            state.preview.removeFromParent()
            state.unit.view.visible = true
            return
        }

        val mapCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = Vector2D(
                x = pointer.x - state.offset.x,
                y = pointer.y - state.offset.y,
            ),
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        )
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)

        state.unit.mapCoordinate = mapCoordinate
        positionView(state.unit)
        state.preview.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.preview.y = worldPoint.y - state.unit.worldHeight * 0.5
    }

    private fun openContextMenu(unit: PlacedUnit, screenPoint: Vector2D) {
        if (!canInteractWithUnits()) {
            return
        }
        contextMenuManager.open(unit, screenPoint)
    }

    private fun openEditDialog(unit: PlacedUnit) {
        editDialog?.close()
        editDialogUnitId = unit.id
        val position = initialReferenceFields(unit)
        editDialog = MapViewerUnitEditDialog(
            root = root,
            unitType = unit.template,
            initialName = unit.instanceName,
            initialPositionX = position.easting,
            initialPositionY = position.northing,
            onSave = save@{ newName, newPositionX, newPositionY ->
                val before = unit.snapshot()
                val updatedCoordinate = referenceToMapCoordinate(newPositionX, newPositionY) ?: return@save
                val nameChanged = newName != unit.instanceName
                val positionChanged = updatedCoordinate != unit.mapCoordinate
                if (nameChanged || positionChanged) {
                    unit.instanceName = newName
                    unit.mapCoordinate = updatedCoordinate
                    positionView(unit)
                    onUnitEdited(before, unit.snapshot())
                }
            },
            onDismiss = {
                editDialog = null
                editDialogUnitId = null
            },
        ).also {
            it.show(root.stage?.views?.actualVirtualWidth ?: 0, root.stage?.views?.actualVirtualHeight ?: 0)
        }
    }

    private fun initialReferenceFields(unit: PlacedUnit): GridReferenceFields {
        val mapPixels = coordinateSpace.toMapPixels(unit.mapCoordinate)
        return gridLayer?.let { layer ->
            GridReferenceFormatter.formatFields(layer, mapPixels.x, mapPixels.y)
        } ?: GridReferenceFields(
            easting = fallbackReference(mapCoordinate = unit.mapCoordinate.x),
            northing = fallbackReference(mapCoordinate = unit.mapCoordinate.y),
        )
    }

    private fun referenceToMapCoordinate(easting: String, northing: String): MapCoordinate? {
        val layer = gridLayer
        return if (layer != null) {
            GridReferenceFormatter.parseFields(layer, easting, northing)?.let { mapPixels ->
                coordinateSpace.fromMapPixels(Vector2D(mapPixels.x, mapPixels.y))
            }
        } else {
            val parsedX = easting.trim().toIntOrNull() ?: return null
            val parsedY = northing.trim().toIntOrNull() ?: return null
            MapCoordinate(
                x = parsedX.coerceIn(-9_999, 9_999) / 9_999.0,
                y = parsedY.coerceIn(-9_999, 9_999) / 9_999.0,
            )
        }
    }

    private fun fallbackReference(mapCoordinate: Double): String {
        val scaled = (mapCoordinate.coerceIn(-1.0, 1.0) * 9_999).toInt()
        return if (scaled < 0) {
            "-${scaled.unaryMinus().toString().padStart(4, '0')}"
        } else {
            scaled.toString().padStart(4, '0')
        }
    }

    private inner class PlacedUnit(
        override val id: MapPlacedObjectId,
        val view: Image,
        var template: MapViewerUnitTemplate,
        var instanceName: String,
        var mapCoordinate: MapCoordinate,
        var normalizedWidth: Double,
        var normalizedHeight: Double,
        var worldWidth: Double,
        var worldHeight: Double,
    ) : MapPlacedObject {
        override fun snapshot(): UnitSnapshot =
            UnitSnapshot(
                id = id,
                template = template,
                instanceName = instanceName,
                mapCoordinate = mapCoordinate,
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
            )

        override fun containsMapCoordinate(
            mapCoordinate: MapCoordinate,
            tolerance: Double,
            coordinateSpace: MapCoordinateSpace?,
        ): Boolean {
            val halfWidth = normalizedWidth * 0.5
            val halfHeight = normalizedHeight * 0.5
            return mapCoordinate.x in (this.mapCoordinate.x - halfWidth - tolerance)..(this.mapCoordinate.x + halfWidth + tolerance) &&
                    mapCoordinate.y in (this.mapCoordinate.y - halfHeight - tolerance)..(this.mapCoordinate.y + halfHeight + tolerance)
        }

        override fun contextMenuActions(): List<MapPlacedObjectMenuAction> =
            listOf(
                MapPlacedObjectMenuAction(
                    label = "Edit Unit",
                    onSelected = { openEditDialog(this) },
                ),
                MapPlacedObjectMenuAction(
                    label = "Delete Unit",
                    onSelected = { onObjectDeleted(snapshot()) },
                ),
            )

        override fun moveBy(mapDelta: MapCoordinateDelta) {
            mapCoordinate = MapCoordinate(
                x = mapCoordinate.x + mapDelta.x,
                y = mapCoordinate.y + mapDelta.y,
            )
            positionView(this)
        }
    }

    private data class DragState(
        val unit: PlacedUnit,
        val preview: Image,
        val offset: Vector2D,
        val originalSnapshot: UnitSnapshot,
    )

    private fun bindViewEvents(unit: PlacedUnit) {
        unit.view.mouse {
            onDown {
                if (!canInteractWithUnits()) {
                    return@onDown
                }
                when (button) {
                    MouseButton.LEFT -> beginDrag(unit, currentPosGlobal)
                    MouseButton.RIGHT -> openContextMenu(unit, currentPosGlobal)
                    else -> Unit
                }
            }
        }
    }

    private fun positionView(unit: PlacedUnit) {
        val worldPoint = coordinateSpace.toWorld(unit.mapCoordinate)
        unit.view.x = worldPoint.x - unit.worldWidth * 0.5
        unit.view.y = worldPoint.y - unit.worldHeight * 0.5
    }

    private fun worldUnitSize(bitmap: korlibs.image.bitmap.Bitmap32): WorldUnitSize {
        val worldWidth = UNIT_ICON_WIDTH_METRES / mapMetresPerPixel
        val worldScale = worldWidth / bitmap.width
        return WorldUnitSize(
            worldWidth = worldWidth,
            worldHeight = bitmap.height * worldScale,
            worldScale = worldScale,
        )
    }

    private data class WorldUnitSize(
        val worldWidth: Double,
        val worldHeight: Double,
        val worldScale: Double,
    )

    private companion object {
        const val UNIT_ICON_WIDTH_METRES = 250.0
        const val DRAG_PREVIEW_Z_INDEX = 15_000.0
    }
}
