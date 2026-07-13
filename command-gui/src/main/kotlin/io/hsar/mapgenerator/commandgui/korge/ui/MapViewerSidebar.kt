package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuDeferredActionRunner
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapContextMenuTarget
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D

internal class MapViewerSidebar(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val onUnitDropped: (MapViewerUnitTemplate, Vector2D) -> Unit,
    private val contextMenuManager: ContextMenuManager? = null,
    private val deferredActionRunner: ContextMenuDeferredActionRunner? = null,
    private val dialogManager: MapViewerDialogManager,
    private val onUnitTemplateCreated: (UnitTemplateSnapshot) -> Unit = {},
    private val onUnitTemplateDeleted: (UnitTemplateSnapshot) -> Unit = {},
    private val onUnitTemplateEdited: (UnitTemplateSnapshot, UnitTemplateSnapshot) -> Unit = { _, _ -> },
    private val onTransientNotice: (String) -> Unit = {},
    private val canStartUnitDrag: () -> Boolean = { true },
    initialUnitTemplates: List<MapViewerUnitTemplate> = MapViewerUnitTemplateFactory.defaults(),
) {
    private val overlay = root.container()
    private val toggleButton = overlay.container()
    private val dragLayer = overlay.container()
    private val unitTemplates = initialUnitTemplates.mapIndexed { index, template ->
        UnitTemplateRow(template.id, template)
    }.toMutableList()
    private var collapsed = false
    private var dragState: DragState? = null
    private var lastDragPointer: Vector2D? = null
    private var dialogWindow: MapViewerDialogWindow? = null
    private var restoredPanelBounds: MapViewerDialogBounds? = null
    private var layout = MapViewerSidebarLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

    internal val isUnitDragging: Boolean
        get() = dragState != null

    internal val isUnitDialogOpen: Boolean
        get() = dialogManager.hasOpenDialogs()

    internal val isUnitDialogBlocking: Boolean
        get() = dialogManager.hasOpenDialogs()

    internal val isUnitDialogTextInputActive: Boolean
        get() = dialogManager.isTextInputActive()

    internal val unitTemplateCount: Int
        get() = unitTemplates.size

    init {
        overlay.zIndex = 10_000.0
        dragLayer.zIndex = 20_000.0
        dragLayer.mouseEnabled = false

        toggleButton.mouse {
            onClick {
                if (collapsed) {
                    openDialog()
                } else {
                    closeDialog()
                }
                relayout(layoutViewportWidth(), layoutViewportHeight())
            }
        }

        overlay.addUpdater {
            if (!collapsed && dialogWindow == null && overlay.stage != null) {
                openDialog()
            }
            updateDrag(overlay.stage?.views?.input)
        }

        overlay.onStageResized { width, height ->
            relayout(width, height)
        }

        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        layout.toggleBounds.contains(point) || dialogWindow?.blocksScreenPoint(point) == true || dialogManager.blocksScreenPoint(point)

    fun addUnitTemplate(template: MapViewerUnitTemplate, index: Int = unitTemplates.size, notify: Boolean = true): UnitTemplateSnapshot {
        val insertionIndex = index.coerceIn(0, unitTemplates.size)
        return addUnitTemplate(
            snapshot = UnitTemplateSnapshot(
                id = template.id,
                template = template,
                index = insertionIndex,
            ),
            notify = notify,
        )
    }

    fun addUnitTemplate(snapshot: UnitTemplateSnapshot, notify: Boolean = true): UnitTemplateSnapshot {
        val insertionIndex = snapshot.index.coerceIn(0, unitTemplates.size)
        val row = UnitTemplateRow(snapshot.id, snapshot.template)
        unitTemplates.add(insertionIndex, row)
        relayout(layoutViewportWidth(), layoutViewportHeight())
        dialogWindow?.refresh()
        return row.snapshot(insertionIndex).also {
            if (notify) onUnitTemplateCreated(it)
        }
    }

    fun restoreUnitTemplates(snapshots: List<UnitTemplateSnapshot>) {
        unitTemplates.clear()
        unitTemplates += snapshots.sortedBy { it.index }.map { UnitTemplateRow(it.id, it.template) }
        relayout(layoutViewportWidth(), layoutViewportHeight())
        dialogWindow?.refresh()
    }

    fun removeUnitTemplate(id: UnitTemplateId, notify: Boolean = true): UnitTemplateSnapshot? {
        dialogManager.closeTarget(id)
        val index = unitTemplates.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }

        val removed = unitTemplates.removeAt(index)
        relayout(layoutViewportWidth(), layoutViewportHeight())
        dialogWindow?.refresh()
        return removed.snapshot(index).also { if (notify) onUnitTemplateDeleted(it) }
    }

    fun snapshotUnitTemplate(id: UnitTemplateId): UnitTemplateSnapshot? {
        val index = unitTemplates.indexOfFirst { it.id == id }
        return unitTemplates.getOrNull(index)?.snapshot(index)
    }

    fun persistedDialog(): MapViewerPersistedDialog? =
        dialogWindow?.currentBounds?.let { bounds -> MapViewerPersistedDialog("unit-types", bounds = bounds) }

    fun restoreDialog(dialog: MapViewerPersistedDialog) {
        restoredPanelBounds = dialog.bounds
        closeDialog()
        openDialog()
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerSidebarLayoutBuilder.build(viewportWidth, viewportHeight, collapsed = true)

        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = layout.toggleBounds,
            label = "UNITS",
        )
    }

    private fun openDialog() {
        val stage = overlay.stage ?: return
        collapsed = false
        if (dialogWindow != null) {
            return
        }
        dialogWindow = MapViewerDialogWindow(
            root = overlay,
            title = "Unit Types",
            initialPosition = ::initialDialogPosition,
            restoredBounds = restoredPanelBounds,
            preferredWidth = MapViewerSidebarLayoutBuilder.PANEL_WIDTH,
            preferredHeight = preferredDialogHeight(),
            contentMinimumWidth = { CONTENT_MINIMUM_WIDTH },
            contentHeight = ::contentHeight,
            onClose = ::closeDialog,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
        )
        dialogWindow?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        restoredPanelBounds = null
    }

    private fun initialDialogPosition(defaultBounds: MapViewerDialogBounds): Vector2D =
        MapViewerSidebarLayoutBuilder.build(
            viewportWidth = layoutViewportWidth(),
            viewportHeight = layoutViewportHeight(),
            collapsed = false,
        ).panelBounds
            ?.let { Vector2D(it.left, it.top) }
            ?: Vector2D(defaultBounds.left, defaultBounds.top)

    private fun closeDialog() {
        collapsed = true
        dialogWindow?.close()
        dialogWindow = null
        relayout(layoutViewportWidth(), layoutViewportHeight())
    }

    private fun renderContent(parent: Container, itemWidth: Double) {
        val buttonWidth = 120.0
        val addButton = parent.container()
        addButton.xy(itemWidth - buttonWidth, 0.0)
        buildToggleButton(addButton, buttonWidth, HEADER_HEIGHT, "New Type") {
            openCreateUnitDialog()
        }

        val unitList = parent.container()
        unitList.xy(0.0, HEADER_HEIGHT + HEADER_GAP)
        unitTemplates.forEachIndexed { index, row ->
            addUnitSource(unitList, row, index, itemWidth)
        }
    }

    private fun addUnitSource(parent: Container, row: UnitTemplateRow, index: Int, itemWidth: Double) {
        val itemTop = index * (UNIT_ITEM_HEIGHT + UNIT_ITEM_GAP)
        val iconSlotWidth = 40.0
        val iconInset = 2.0
        val item = parent.container()

        item.xy(0.0, itemTop.toDouble())
        val hitbox = item.solidRect(itemWidth, UNIT_ITEM_HEIGHT, RGBA(0, 0, 0, 0))
        hitbox.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    beginDrag(row.template)
                } else if (button == MouseButton.RIGHT) {
                    openContextMenu(row, currentPosGlobal)
                }
            }
        }

        MapViewerUiBoxes.box(item, itemWidth, UNIT_ITEM_HEIGHT)

        val iconSize = UNIT_ITEM_HEIGHT - 10.0
        val iconFrame = item.container()
        iconFrame.xy(iconInset, (UNIT_ITEM_HEIGHT - iconSize) * 0.5)
        iconFrame.solidRect(iconSlotWidth, iconSize, RGBA(0, 0, 0, 0))
        val fittedIcon = MapViewerIconFit.inside(row.template.bitmap, iconSlotWidth, iconSize)
        MapViewerImageViews.icon(iconFrame, row.template.bitmap) {
            scale = fittedIcon.scale
            x = fittedIcon.x
            y = fittedIcon.y
        }
        MapViewerTextViews.leftBox(
            parent = item,
            text = row.template.name,
            width = itemWidth - (iconInset + iconSlotWidth + 4.0 + iconSlotWidth),
            height = UNIT_ITEM_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = iconInset + iconSlotWidth + 4.0
        }

        val affiliationFrame = item.container()
        affiliationFrame.xy(itemWidth - iconSlotWidth + (iconSlotWidth - iconSize) * 0.5, (UNIT_ITEM_HEIGHT - iconSize) * 0.5)
        affiliationFrame.solidRect(iconSlotWidth, iconSize, RGBA(0, 0, 0, 0))
        MapViewerSwatchViews.render(affiliationFrame, UnitAffiliationUi.swatchColor(row.template.affiliation), iconSize)
    }

    private fun buildToggleButton(parent: Container, width: Double, height: Double, label: String, onClick: () -> Unit = {}) {
        MapViewerUiBoxes.button(parent, width, height)
        MapViewerTextViews.box(parent, label, width, height, color = Palette.Colours.WHITE)
        parent.mouse {
            this.onClick {
                onClick()
            }
        }
    }

    private fun beginDrag(template: MapViewerUnitTemplate) {
        if (dragState?.template == template || !canStartUnitDrag()) {
            return
        }

        dragLayer.removeChildren()
        val preview = dragLayer.container()
        preview.alpha = 0.92
        MapViewerUiBoxes.imageBox(preview, template.bitmap, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        dragState = DragState(template = template, preview = preview)
        lastDragPointer = null
    }

    private fun updateDrag(input: korlibs.korge.input.Input?) {
        val state = dragState ?: return
        val mouse = input?.mousePos?.also { lastDragPointer = it } ?: lastDragPointer ?: return

        state.preview.xy(mouse.x + PREVIEW_OFFSET_X, mouse.y + PREVIEW_OFFSET_Y)

        if (input?.mouseButtonPressed(MouseButton.LEFT) == true) {
            return
        }

        if (blocksScreenPoint(mouse)) {
            state.preview.removeFromParent()
            dragState = null
            lastDragPointer = null
            return
        }

        onUnitDropped(state.template, mouse)

        state.preview.removeFromParent()
        dragState = null
        lastDragPointer = null
    }

    private fun openCreateUnitDialog() {
        val stage = overlay.stage ?: return
        MapViewerUnitCreateDialog(
            root = overlay,
            dialogManager = dialogManager,
            dialogTitle = "Create Unit Type",
            confirmLabel = "Create",
            onCreated = { template ->
                addUnitTemplate(template)
            },
            onDismiss = {
            },
            onTransientNotice = onTransientNotice,
        ).also {
            it.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        }
    }

    private fun openEditUnitDialog(row: UnitTemplateRow) {
        val stage = overlay.stage ?: return
        MapViewerUnitCreateDialog(
            root = overlay,
            dialogManager = dialogManager,
            dialogTitle = "Edit Unit Type",
            confirmLabel = "Save",
            dialogKey = row.id,
            initialTemplate = row.template,
            onCreated = { template ->
                replaceUnitTemplate(row.id, template)
            },
            onDismiss = {
            },
            onTransientNotice = onTransientNotice,
        ).also {
            it.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        }
    }

    fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot, notify: Boolean = true): UnitTemplateSnapshot? =
        replaceUnitTemplate(snapshot.id, snapshot.template, notify)

    fun replaceUnitTemplate(id: UnitTemplateId, template: MapViewerUnitTemplate, notify: Boolean = true): UnitTemplateSnapshot? {
        val index = unitTemplates.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }

        val before = snapshotUnitTemplate(id)
        unitTemplates[index] = unitTemplates[index].copy(template = template)
        relayout(layoutViewportWidth(), layoutViewportHeight())
        dialogWindow?.refresh()
        val after = snapshotUnitTemplate(id)
        if (notify && before != null && after != null && before != after) {
            onUnitTemplateEdited(before, after)
        }
        return after
    }

    private fun openContextMenu(row: UnitTemplateRow, screenPoint: Vector2D) {
        val manager = contextMenuManager ?: return
        val index = unitTemplates.indexOfFirst { it.id == row.id }
        if (index < 0) {
            return
        }

        manager.open(
            target = object : MapContextMenuTarget {
                override fun contextMenuActions(): List<MapPlacedObjectMenuAction> =
                    listOf(
                        MapPlacedObjectMenuAction(
                            label = "Edit Unit Type",
                            onSelected = { launchModal { openEditUnitDialog(row) } },
                        ),
                        MapPlacedObjectMenuAction(
                            label = "Delete Unit Type",
                            onSelected = { launchModal { removeUnitTemplate(row.id) } },
                        ),
                    )
            },
            screenPoint = screenPoint,
        )
    }

    private fun launchModal(action: () -> Unit) {
        deferredActionRunner?.launch(action) ?: action()
    }

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun layoutViewportHeight(): Int =
        overlay.stage?.views?.actualVirtualHeight ?: 0

    private fun contentHeight(width: Double): Double =
        HEADER_HEIGHT + HEADER_GAP + unitListHeight()

    private fun unitListHeight(): Double =
        if (unitTemplates.isEmpty()) {
            0.0
        } else {
            unitTemplates.size * UNIT_ITEM_HEIGHT + (unitTemplates.size - 1) * UNIT_ITEM_GAP
        }

    private fun preferredDialogHeight(): Double =
        MapViewerSidebarLayoutBuilder.PANEL_MIN_HEIGHT.coerceAtLeast(contentHeight(MapViewerSidebarLayoutBuilder.PANEL_WIDTH))

    private data class DragState(
        val template: MapViewerUnitTemplate,
        val preview: Container,
    )

    private data class UnitTemplateRow(
        val id: UnitTemplateId,
        val template: MapViewerUnitTemplate,
    ) {
        fun snapshot(index: Int): UnitTemplateSnapshot =
            UnitTemplateSnapshot(id = id, template = template, index = index)
    }

    private companion object {
        const val CONTENT_MINIMUM_WIDTH = 280.0
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
