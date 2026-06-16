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
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val viewportProvider: () -> CameraViewport,
) {
    private val units = mutableListOf<PlacedUnit>()
    private val dragPreviewLayer = root.container()
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
            updateUnitScales()
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
        }
    }

    fun place(template: MapViewerUnitTemplate, screenPoint: Vector2D) {
        if (dragState != null) {
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
        val viewport = viewportProvider()
        return units.any { unit -> MapPlacedUnitScreenBounds.contains(point, unit.snapshot(), viewport) }
    }

    private fun addUnit(template: MapViewerUnitTemplate, worldPoint: Vector2D) {
        val view = unitLayer.image(template.bitmap) {
            x = worldPoint.x - width * 0.5
            y = worldPoint.y - height * 0.5
        }

        val placedUnit = PlacedUnit(
            view = view,
            template = template,
            instanceName = template.name,
            mapPoint = worldPoint,
            screenWidth = template.bitmap.width.toDouble(),
            screenHeight = template.bitmap.height.toDouble(),
        )
        units += placedUnit
        updateUnitScale(placedUnit, currentUnitScale())

        view.mouse {
            onDown {
                when (button) {
                    MouseButton.LEFT -> beginDrag(placedUnit, currentPosGlobal)
                    MouseButton.RIGHT -> openContextMenu(placedUnit, currentPosGlobal)
                    else -> Unit
                }
            }
        }
    }

    private fun beginDrag(unit: PlacedUnit, screenPoint: Vector2D) {
        if (dragState != null) {
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
        preview.xy(
            screenPoint.x - grabOffset.x - unit.screenWidth * 0.5,
            screenPoint.y - grabOffset.y - unit.screenHeight * 0.5,
        )
        preview.zIndex = DRAG_PREVIEW_Z_INDEX

        dragState = DragState(
            unit = unit,
            preview = preview,
            offset = grabOffset,
        )
        unit.view.x = worldPoint.x - unit.screenWidth * 0.5
        unit.view.y = worldPoint.y - unit.screenHeight * 0.5
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean) {
        val state = dragState ?: return
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
        state.unit.view.x = worldPoint.x - state.unit.screenWidth * 0.5
        state.unit.view.y = worldPoint.y - state.unit.screenHeight * 0.5
        state.preview.xy(
            pointer.x - state.offset.x - state.unit.screenWidth * 0.5,
            pointer.y - state.offset.y - state.unit.screenHeight * 0.5,
        )
    }

    private fun updateUnitScales() {
        val scale = currentUnitScale()
        units.forEach { unit ->
            updateUnitScale(unit, scale)
        }
        dragState?.unit?.let { updateUnitScale(it, scale) }
    }

    private fun currentUnitScale(): Double {
        val zoom = viewportProvider().zoom
        return if (zoom == 0.0) 1.0 else 1.0 / zoom
    }

    private fun updateUnitScale(unit: PlacedUnit, scale: Double) {
        if (unit.view.scaleX != scale || unit.view.scaleY != scale) {
            unit.view.scale = scale
        }
    }

    private fun openContextMenu(unit: PlacedUnit, screenPoint: Vector2D) {
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
        val screenWidth: Double,
        val screenHeight: Double,
    ) {
        fun snapshot(): MapPlacedUnitSnapshot =
            MapPlacedUnitSnapshot(
                mapPoint = mapPoint,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
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

    private companion object {
        const val DRAG_PREVIEW_Z_INDEX = 15_000.0
        const val CONTEXT_MENU_Z_INDEX = 35_000.0
    }
}
