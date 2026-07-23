package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuDeferredActionRunner
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapContextMenuTarget
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
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
    private val collapsedAffiliationGroups = mutableSetOf<UnitAffiliation?>()
    private var collapsed = false
    private var dragState: DragState? = null
    private var lastDragPointer: Vector2D? = null
    private var dialogWindow: MapViewerDialogWindow? = null
    private var restoredPanelBounds: MapViewerDialogBounds? = null
    private var restoreCollapseStateOnNextOpen = false
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
        dialogWindow?.persistedDialog()

    fun restoreDialog(dialog: MapViewerPersistedDialog) {
        restoredPanelBounds = dialog.bounds
        val collapsedKeys = MapViewerUniversalList.restoreCollapsedSections(
            draft = dialog.draft,
            sectionFromKey = { key -> key.takeIf(::isAffiliationKey) },
        )
        collapsedAffiliationGroups.clear()
        collapsedAffiliationGroups += collapsedKeys.mapNotNull(::affiliationFromKey)
        if (UNAFFILIATED_AFFILIATION_KEY in collapsedKeys) collapsedAffiliationGroups += null
        restoreCollapseStateOnNextOpen = true
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
        if (!restoreCollapseStateOnNextOpen) {
            collapseGroupsToFit(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        }
        dialogWindow = MapViewerDialogWindow(
            root = overlay,
            title = "Unit Types",
            initialPosition = ::initialDialogPosition,
            persistence = MapViewerDialogPersistence(
                type = "unit-types",
                restored = restoredPanelBounds?.let { bounds -> MapViewerPersistedDialog("unit-types", bounds = bounds) },
                draft = {
                    MapViewerUniversalList.collapseStateDraft(
                        collapsedSections = collapsedAffiliationGroups.map(::affiliationKey).toSet(),
                        sectionKey = { it },
                    )
                },
            ),
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
        restoreCollapseStateOnNextOpen = false
    }

    private fun initialDialogPosition(defaultBounds: MapViewerDialogBounds): Vector2D =
        MapViewerDialogLayoutBuilder.alignRight(defaultBounds, layoutViewportWidth())

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
        renderGroupedUnitSources(unitList, itemWidth)
    }

    private fun renderGroupedUnitSources(parent: Container, itemWidth: Double) {
        MapViewerUniversalList.renderUnitAffiliationGroups(
            parent = parent,
            width = itemWidth,
            items = unitTemplates,
            affiliationOf = { it.template.affiliation },
            collapsedGroups = collapsedAffiliationGroups,
            rowHeight = MapViewerUniversalList.DEFAULT_ROW_HEIGHT,
            rowGap = UNIT_ITEM_GAP,
            headerHeight = GROUP_HEADER_HEIGHT,
            headerGap = GROUP_GAP,
            renderRow = { row, top -> addUnitSource(parent, row, top, itemWidth) },
            onGroupsChanged = { dialogWindow?.refresh() },
        )
    }

    private fun addUnitSource(parent: Container, row: UnitTemplateRow, top: Double, itemWidth: Double) {
        MapViewerUniversalList.renderItem(
            parent = parent,
            item = MapViewerUniversalListItem(
                title = row.template.name,
                bitmap = row.template.bitmap,
                swatch = UnitAffiliationUi.swatchColor(row.template.affiliation),
            ),
            width = itemWidth,
            top = top,
            onDown = { button, point ->
                if (button == MouseButton.LEFT) beginDrag(row.template) else if (button == MouseButton.RIGHT) openContextMenu(row, point)
            },
        )
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
        MapViewerUniversalList.unitAffiliationGroupsHeight(
            items = unitTemplates,
            affiliationOf = { it.template.affiliation },
            collapsedGroups = collapsedAffiliationGroups,
            rowHeight = MapViewerUniversalList.DEFAULT_ROW_HEIGHT,
            rowGap = UNIT_ITEM_GAP,
            headerHeight = GROUP_HEADER_HEIGHT,
            headerGap = GROUP_GAP,
        )

    private fun preferredDialogHeight(): Double =
        contentHeight(MapViewerSidebarLayoutBuilder.PANEL_WIDTH)

    private fun collapseGroupsToFit(viewportWidth: Int, viewportHeight: Int) {
        val minimumWidth = MapViewerDialogLayoutBuilder.minimumShellWidth(CONTENT_MINIMUM_WIDTH)
        val shellWidth = MapViewerDialogLayoutBuilder.clampSize(
            size = MapViewerDialogSize(MapViewerSidebarLayoutBuilder.PANEL_WIDTH, MapViewerDialogLayoutBuilder.MIN_HEIGHT),
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            minimumWidth = minimumWidth,
        ).width
        val collapsibleGroups = MapViewerUniversalList.unitAffiliationGroups
            .filter { group -> unitTemplates.count { it.template.affiliation == group.affiliation } > 1 }
            .map(MapViewerUnitAffiliationGroup::affiliation)

        MapViewerUniversalList.collapseSectionsToFit(
            sections = collapsibleGroups,
            collapsedSections = collapsedAffiliationGroups,
            availableHeight = MapViewerDialogLayoutBuilder.maximumInitialContentHeight(shellWidth, viewportHeight),
            contentHeight = ::unitListContentHeight,
            collapseOrder = listOf(
                null,
                UnitAffiliation.Neutral,
                UnitAffiliation.Friend,
                UnitAffiliation.Unknown,
                UnitAffiliation.Hostile,
            ),
        )
    }

    private fun unitListContentHeight(): Double =
        HEADER_HEIGHT + HEADER_GAP + unitListHeight()

    private fun affiliationKey(affiliation: UnitAffiliation?): String =
        affiliation?.name ?: UNAFFILIATED_AFFILIATION_KEY

    private fun affiliationFromKey(key: String): UnitAffiliation? =
        UnitAffiliation.entries.firstOrNull { it.name == key }

    private fun isAffiliationKey(key: String): Boolean =
        key == UNAFFILIATED_AFFILIATION_KEY || affiliationFromKey(key) != null

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
        const val UNIT_ITEM_GAP = 10.0
        const val GROUP_HEADER_HEIGHT = 36.0
        const val GROUP_GAP = 8.0
        const val PREVIEW_WIDTH = 120.0
        const val PREVIEW_HEIGHT = 40.0
        const val PREVIEW_OFFSET_X = 14.0
        const val PREVIEW_OFFSET_Y = 14.0
        const val UNAFFILIATED_AFFILIATION_KEY = "UNAFFILIATED"
    }
}
