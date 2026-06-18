package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitEditDialog
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
    private val viewportProvider: () -> CameraViewport,
    private val contextMenuManager: MapPlacedObjectContextMenuManager,
    private val canInteractWithUnits: () -> Boolean = { true },
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private val units = mutableListOf<PlacedUnit>()
    private val dragPreviewLayer = unitLayer.container()
    private var dragState: DragState? = null

    fun install() {
        dragPreviewLayer.zIndex = DRAG_PREVIEW_Z_INDEX
        dragPreviewLayer.mouseEnabled = false

        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
        }
    }

    fun place(template: MapViewerUnitTemplate, screenPoint: Vector2D) {
        if (dragState != null || !canInteractWithUnits()) {
            return
        }

        val mapCoordinate = coordinateSpace.resolveScreenPoint(
            screenPoint = screenPoint,
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        ) ?: return

        addUnit(template, mapCoordinate)
    }

    val isDragging: Boolean
        get() = dragState != null

    val isContextMenuOpen: Boolean
        get() = contextMenuManager.isOpen

    fun blocksScreenPoint(point: Vector2D): Boolean {
        if (!canInteractWithUnits()) {
            return false
        }
        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(point, viewport)
        return units.any { unit -> unit.containsMapCoordinate(mapCoordinate) }
    }

    private fun addUnit(template: MapViewerUnitTemplate, mapCoordinate: MapCoordinate) {
        val unitSize = worldUnitSize(template.bitmap)
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)
        val view = unitLayer.image(template.bitmap) {
            this.scale = unitSize.worldScale
            x = worldPoint.x - unitSize.worldWidth * 0.5
            y = worldPoint.y - unitSize.worldHeight * 0.5
        }

        val placedUnit = PlacedUnit(
            view = view,
            template = template,
            instanceName = template.name,
            mapCoordinate = mapCoordinate,
            normalizedWidth = unitSize.worldWidth / mapWidth,
            normalizedHeight = unitSize.worldHeight / mapHeight,
            worldWidth = unitSize.worldWidth,
            worldHeight = unitSize.worldHeight,
        )
        units += placedUnit

        view.mouse {
            onDown {
                if (!canInteractWithUnits()) {
                    return@onDown
                }
                when (button) {
                    MouseButton.LEFT -> beginDrag(placedUnit, currentPosGlobal)
                    MouseButton.RIGHT -> openContextMenu(placedUnit, currentPosGlobal)
                    else -> Unit
                }
            }
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
        state.unit.view.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.unit.view.y = worldPoint.y - state.unit.worldHeight * 0.5
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
        MapViewerUnitEditDialog(
            root = root,
            unitType = unit.template,
            initialName = unit.instanceName,
            onSave = { newName ->
                unit.instanceName = newName
            },
            onDismiss = {
                // No-op.
            },
        ).show(root.stage?.views?.actualVirtualWidth ?: 0, root.stage?.views?.actualVirtualHeight ?: 0)
    }

    private fun deleteUnit(unit: PlacedUnit) {
        units.remove(unit)
        unit.view.removeFromParent()
        if (dragState?.unit == unit) {
            dragState?.preview?.removeFromParent()
            dragState = null
        }
    }

    private inner class PlacedUnit(
        val view: Image,
        val template: MapViewerUnitTemplate,
        var instanceName: String,
        var mapCoordinate: MapCoordinate,
        val normalizedWidth: Double,
        val normalizedHeight: Double,
        val worldWidth: Double,
        val worldHeight: Double,
    ) : MapPlacedObject {
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
                    onSelected = { deleteUnit(this) },
                ),
            )

        override fun moveBy(mapDelta: MapCoordinateDelta) {
            mapCoordinate = MapCoordinate(
                x = mapCoordinate.x + mapDelta.x,
                y = mapCoordinate.y + mapDelta.y,
            )
            val worldPoint = coordinateSpace.toWorld(mapCoordinate)
            view.x = worldPoint.x - worldWidth * 0.5
            view.y = worldPoint.y - worldHeight * 0.5
        }

        fun snapshot(): MapPlacedUnitSnapshot =
            MapPlacedUnitSnapshot(
                mapCoordinate = mapCoordinate,
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
            )
    }

    private data class DragState(
        val unit: PlacedUnit,
        val preview: Image,
        val offset: Vector2D,
    )

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
