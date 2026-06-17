package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextBitmapRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitEditDialog
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
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
    private val canInteractWithUnits: () -> Boolean = { true },
) {
    private val units = mutableListOf<PlacedUnit>()
    private val dragPreviewLayer = unitLayer.container()
    private val contextMenuLayer = root.container()
    private var dragState: DragState? = null
    private var contextMenuState: ContextMenuState? = null

    fun install() {
        dragPreviewLayer.zIndex = DRAG_PREVIEW_Z_INDEX
        dragPreviewLayer.mouseEnabled = false

        contextMenuLayer.zIndex = CONTEXT_MENU_Z_INDEX
        contextMenuLayer.mouseEnabled = true

        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
        }
    }

    fun place(template: MapViewerUnitTemplate, screenPoint: Vector2D) {
        if (dragState != null || !canInteractWithUnits()) {
            return
        }

        val worldPoint = MapViewerUnitPlacementResolver.resolve(
            screenPoint = screenPoint,
            viewport = viewportProvider(),
            geometry = geometry,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
        ) ?: return

        addUnit(template, worldPoint)
    }

    val isDragging: Boolean
        get() = dragState != null

    val isContextMenuOpen: Boolean
        get() = contextMenuState != null

    fun blocksScreenPoint(point: Vector2D): Boolean {
        if (!canInteractWithUnits()) {
            return false
        }
        val viewport = viewportProvider()
        return units.any { unit -> MapPlacedUnitScreenBounds.contains(point, unit.snapshot(), viewport) }
    }

    private fun addUnit(template: MapViewerUnitTemplate, worldPoint: Vector2D) {
        val unitSize = worldUnitSize(template.bitmap)
        val view = unitLayer.image(template.bitmap) {
            this.scale = unitSize.worldScale
            x = worldPoint.x - unitSize.worldWidth * 0.5
            y = worldPoint.y - unitSize.worldHeight * 0.5
        }

        val placedUnit = PlacedUnit(
            view = view,
            template = template,
            instanceName = template.name,
            mapPoint = worldPoint,
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
        val currentScreenPoint = Vector2D(
            x = viewport.screenX(unit.mapPoint.x),
            y = viewport.screenY(unit.mapPoint.y),
        )
        val grabOffset = Vector2D(
            x = screenPoint.x - currentScreenPoint.x,
            y = screenPoint.y - currentScreenPoint.y,
        )
        val worldPoint = MapViewerUnitPlacementResolver.resolveClamped(
            screenPoint = Vector2D(
                x = screenPoint.x - grabOffset.x,
                y = screenPoint.y - grabOffset.y,
            ),
            viewport = viewport,
            geometry = geometry,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
        )

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

        val worldPoint = MapViewerUnitPlacementResolver.resolveClamped(
            screenPoint = Vector2D(
                x = pointer.x - state.offset.x,
                y = pointer.y - state.offset.y,
            ),
            viewport = viewportProvider(),
            geometry = geometry,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
        )

        state.unit.mapPoint = worldPoint
        state.unit.view.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.unit.view.y = worldPoint.y - state.unit.worldHeight * 0.5
        state.preview.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.preview.y = worldPoint.y - state.unit.worldHeight * 0.5
    }

    private fun openContextMenu(unit: PlacedUnit, screenPoint: Vector2D) {
        if (!canInteractWithUnits()) {
            return
        }
        closeContextMenu()
        val stage = root.stage ?: return
        val stageWidth = stage.views.actualVirtualWidth.toDouble()
        val stageHeight = stage.views.actualVirtualHeight.toDouble()
        val menuWidth = 180.0
        val menuHeight = 44.0
        val left = screenPoint.x.coerceIn(0.0, (stageWidth - menuWidth).coerceAtLeast(0.0))
        val top = screenPoint.y.coerceIn(0.0, (stageHeight - menuHeight).coerceAtLeast(0.0))

        val backdrop = contextMenuLayer.container()
        backdrop.solidRect(stageWidth, stageHeight, RGBA(0, 0, 0, 0))
        backdrop.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    closeContextMenu()
                }
            }
        }

        val menu = contextMenuLayer.container()
        menu.xy(left, top)
        menu.solidRect(menuWidth, menuHeight, RGBA(255, 255, 255, 255))
        menu.solidRect(menuWidth - 4.0, menuHeight - 4.0, RGBA(48, 58, 58, 255)) {
            x = 2.0
            y = 2.0
        }

        val option = menu.container()
        option.solidRect(menuWidth - 8.0, menuHeight - 8.0, RGBA(255, 255, 255, 16)) {
            x = 4.0
            y = 4.0
        }
        option.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    closeContextMenu()
                    openEditDialog(unit)
                }
            }
        }
        option.image(MapViewerTextBitmapRenderer.render("Edit Unit", Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = 12.0
            y = (menuHeight - height) * 0.5
        }

        contextMenuState = ContextMenuState(menu = menu, backdrop = backdrop)
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

    private fun closeContextMenu() {
        contextMenuLayer.removeChildren()
        contextMenuState = null
    }

    private data class PlacedUnit(
        val view: Image,
        val template: MapViewerUnitTemplate,
        var instanceName: String,
        var mapPoint: Vector2D,
        val worldWidth: Double,
        val worldHeight: Double,
    ) {
        fun snapshot(): MapPlacedUnitSnapshot =
            MapPlacedUnitSnapshot(
                mapPoint = mapPoint,
                worldWidth = worldWidth,
                worldHeight = worldHeight,
            )
    }

    private data class DragState(
        val unit: PlacedUnit,
        val preview: Image,
        val offset: Vector2D,
    )

    private data class ContextMenuState(
        val menu: Container,
        val backdrop: Container,
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
        const val CONTEXT_MENU_Z_INDEX = 35_000.0
    }
}
