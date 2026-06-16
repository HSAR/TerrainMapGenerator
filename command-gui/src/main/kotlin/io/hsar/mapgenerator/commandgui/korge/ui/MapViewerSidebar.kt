package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiMaterialLayer
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.size
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Vector2D
import java.awt.Color

internal class MapViewerSidebar(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val onUnitDropped: (MapViewerUnitTemplate, Vector2D) -> Unit,
) {
    private val overlay = root.container()
    private val panel = overlay.uiMaterialLayer()
    private val content = overlay.container()
    private val header = content.container()
    private val unitList = content.container()
    private val toggleButton = overlay.container()
    private val dragLayer = overlay.container()
    private val labelRenderer = MapViewerTextBitmapRenderer
    private val unitTemplates = MapViewerUnitTemplateFactory.defaults().toMutableList()
    private var collapsed = false
    private var dragState: DragState? = null
    private var createUnitDialog: MapViewerUnitCreateDialog? = null
    private var layout = MapViewerSidebarLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

    internal val isUnitDragging: Boolean
        get() = dragState != null

    internal val isUnitDialogOpen: Boolean
        get() = createUnitDialog != null

    init {
        overlay.zIndex = 10_000.0
        dragLayer.zIndex = 20_000.0
        dragLayer.mouseEnabled = false

        panel.radius = RectCorners(0.0)
        panel.bgColor = toRgba(Palette.Colours.DARK, 220)
        panel.borderSize = 2.0
        panel.borderColor = RGBA(255, 255, 255, 255)
        panel.shadowColor = RGBA(0, 0, 0, 0)
        panel.shadowRadius = 0.0

        toggleButton.mouse {
            onClick {
                collapsed = !collapsed
                relayout(layoutViewportWidth(), layoutViewportHeight())
            }
        }

        overlay.addUpdater {
            updateDrag(overlay.stage?.views?.input)
        }

        overlay.onStageResized { width, height ->
            relayout(width, height)
        }

        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        isUnitDialogOpen || layout.blocks(point)

    fun addUnitTemplate(template: MapViewerUnitTemplate) {
        unitTemplates += template
        relayout(layoutViewportWidth(), layoutViewportHeight())
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerSidebarLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

        toggleButton.removeChildren()
        toggleButton.xy(layout.toggleBounds.left, layout.toggleBounds.top)
        buildToggleButton(
            parent = toggleButton,
            width = layout.toggleBounds.width,
            height = layout.toggleBounds.height,
            label = if (collapsed) "Show Units" else "Hide Units",
        )

        val panelBounds = layout.panelBounds
        panel.visible = panelBounds != null
        content.visible = panelBounds != null

        if (panelBounds != null) {
            panel.xy(panelBounds.left, panelBounds.top)
            panel.size(panelBounds.width, panelBounds.height)

            content.removeChildren()
            content.addChild(header)
            content.addChild(unitList)
            content.xy(panelBounds.left + CONTENT_PADDING, panelBounds.top + CONTENT_PADDING)

            header.removeChildren()
            header.xy(0.0, 0.0)
            header.image(labelRenderer.render("Unit Types", Palette.Fonts.STRONG.first, Palette.Colours.WHITE)) {
                y = (HEADER_HEIGHT - height) * 0.5
            }

            val buttonWidth = 120.0
            val addButton = header.container()
            addButton.xy(MapViewerSidebarLayoutBuilder.PANEL_WIDTH - (CONTENT_PADDING * 2) - buttonWidth, 0.0)
            buildToggleButton(addButton, buttonWidth, HEADER_HEIGHT, "New Type") {
                openCreateUnitDialog()
            }

            unitList.removeChildren()
            unitList.xy(0.0, HEADER_HEIGHT + HEADER_GAP)
            unitTemplates.forEachIndexed { index, template ->
                addUnitSource(unitList, template, index)
            }
        }

    }

    private fun addUnitSource(parent: Container, template: MapViewerUnitTemplate, index: Int) {
        val itemWidth = MapViewerSidebarLayoutBuilder.PANEL_WIDTH - (CONTENT_PADDING * 2)
        val itemTop = index * (UNIT_ITEM_HEIGHT + UNIT_ITEM_GAP)
        val iconSlotWidth = 40.0
        val iconInset = 2.0
        val item = parent.container()

        item.xy(0.0, itemTop.toDouble())
        item.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    beginDrag(template)
                }
            }
        }

        item.solidRect(itemWidth, UNIT_ITEM_HEIGHT, RGBA(255, 255, 255, 255))
        item.solidRect(itemWidth - 4.0, UNIT_ITEM_HEIGHT - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }

        val iconSize = UNIT_ITEM_HEIGHT - 10.0
        val iconFrame = item.container()
        iconFrame.xy(iconInset, (UNIT_ITEM_HEIGHT - iconSize) * 0.5)
        iconFrame.solidRect(iconSlotWidth, iconSize, RGBA(0, 0, 0, 0))
        val fittedIcon = MapViewerIconFit.inside(template.bitmap, iconSlotWidth, iconSize)
        iconFrame.image(template.bitmap) {
            scale = fittedIcon.scale
            x = fittedIcon.x
            y = fittedIcon.y
        }
        item.image(labelRenderer.render(template.name, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = iconInset + iconSlotWidth + 4.0
            y = (UNIT_ITEM_HEIGHT - height) * 0.5
        }
    }

    private fun buildToggleButton(parent: Container, width: Double, height: Double, label: String, onClick: () -> Unit = {}) {
        parent.solidRect(width, height, RGBA(255, 255, 255, 255))
        parent.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }
        parent.image(labelRenderer.render(label, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = (width - this.width) * 0.5
            y = (height - this.height) * 0.5
        }
        parent.mouse {
            this.onClick {
                onClick()
            }
        }
    }

    private fun beginDrag(template: MapViewerUnitTemplate) {
        if (dragState?.template == template) {
            return
        }

        dragLayer.removeChildren()
        val preview = dragLayer.container()
        preview.alpha = 0.92
        preview.solidRect(PREVIEW_WIDTH, PREVIEW_HEIGHT, RGBA(255, 255, 255, 255))
        preview.solidRect(PREVIEW_WIDTH - 4.0, PREVIEW_HEIGHT - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }
        val fittedIcon = MapViewerIconFit.inside(template.bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
        preview.image(template.bitmap) {
            scale = fittedIcon.scale
            x = 6.0 + fittedIcon.x
            y = 6.0 + fittedIcon.y
        }

        dragState = DragState(template = template, preview = preview)
    }

    private fun updateDrag(input: korlibs.korge.input.Input?) {
        val state = dragState ?: return
        val mouse = input?.mousePos ?: return

        state.preview.xy(mouse.x + PREVIEW_OFFSET_X, mouse.y + PREVIEW_OFFSET_Y)

        if (input.mouseButtonPressed(MouseButton.LEFT)) {
            return
        }

        if (mouse.x >= 0.0 && mouse.y >= 0.0) {
            onUnitDropped(state.template, mouse)
        }

        state.preview.removeFromParent()
        dragState = null
    }

    private fun openCreateUnitDialog() {
        if (createUnitDialog != null) {
            return
        }

        val stage = overlay.stage ?: return
        createUnitDialog = MapViewerUnitCreateDialog(
            root = overlay,
            onCreated = ::addUnitTemplate,
            onDismiss = {
                createUnitDialog = null
            },
        ).also {
            it.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        }
    }

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun layoutViewportHeight(): Int =
        overlay.stage?.views?.actualVirtualHeight ?: 0

    private data class DragState(
        val template: MapViewerUnitTemplate,
        val preview: Container,
    )

    private companion object {
        const val CONTENT_PADDING = 20.0
        const val HEADER_HEIGHT = 40.0
        const val HEADER_GAP = 12.0
        const val UNIT_ITEM_HEIGHT = 40.0
        const val UNIT_ITEM_GAP = 10.0
        const val PREVIEW_WIDTH = 120.0
        const val PREVIEW_HEIGHT = 40.0
        const val PREVIEW_OFFSET_X = 14.0
        const val PREVIEW_OFFSET_Y = 14.0
    }
}

private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
    RGBA(color.red, color.green, color.blue, alpha)
