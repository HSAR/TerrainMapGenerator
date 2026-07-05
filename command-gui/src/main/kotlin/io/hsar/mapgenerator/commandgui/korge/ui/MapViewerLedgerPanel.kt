package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.TurnMarker
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnPanelRow
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnPanelState
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitIconDisplaySize
import io.hsar.mapgenerator.commandgui.korge.interaction.nextTurnNumber
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.util.*

@OptIn(KorgeExperimental::class)
internal class MapViewerLedgerPanel(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val canUndo: () -> Boolean,
    private val canRedo: () -> Boolean,
    private val turnPanelState: () -> TurnPanelState,
    private val isReadOnly: () -> Boolean,
    private val onUndo: () -> Unit,
    private val onRedo: () -> Unit,
    private val onAddTurn: (String) -> Unit,
    private val onRevertToTurn: (TurnMarker) -> Unit,
    private val onSelectCurrent: () -> Unit,
    initialDestroyedUnitsDisplay: DestroyedUnitsDisplay = DestroyedUnitsDisplay.Full,
    private val onDestroyedUnitsDisplayChanged: (DestroyedUnitsDisplay) -> Unit = {},
    initialUnitIconSizePx: Double = UnitIconDisplaySize.DEFAULT_PX,
    private val onUnitIconSizeChanged: (Double) -> Unit = {},
    private val turnThumbnail: (UUID) -> Bitmap32? = { null },
) {
    private val overlay = root.container()
    private val toggleButton = overlay.container()
    private val destroyedUnitsDropdown = MapViewerDropdownMenu(
        root = overlay,
        options = DestroyedUnitsDisplay.entries.map { MapViewerDropdownOption(it, it.label) },
        onSelected = { value ->
            destroyedUnitsDisplay = value ?: DestroyedUnitsDisplay.Full
            onDestroyedUnitsDisplayChanged(destroyedUnitsDisplay)
        },
        labelForValue = { value -> (value ?: DestroyedUnitsDisplay.Full).label },
        menuWidth = DESTROYED_UNITS_DROPDOWN_MINIMUM_WIDTH,
    )
    private var collapsed = true
    private var activeTab = LedgerPanelTab.Turns
    private var destroyedUnitsDisplay = initialDestroyedUnitsDisplay
    private var unitIconSizePx = UnitIconDisplaySize.coerce(initialUnitIconSizePx)
    private var unitIconSizeText = UnitIconDisplaySize.format(unitIconSizePx)
    private var markerInput: MapViewerStyledTextInput? = null
    private var unitIconSizeInput: MapViewerInputField? = null
    private var inputActive = false
    private var dialogWindow: MapViewerDialogWindow? = null
    private var layout = MapViewerLedgerPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

    val isTextInputActive: Boolean
        get() = inputActive

    init {
        overlay.zIndex = Z_INDEX

        toggleButton.mouse {
            onClick {
                inputActive = false
                if (collapsed) {
                    openDialog()
                } else {
                    closeDialog()
                }
                relayout(layoutViewportWidth(), layoutViewportHeight())
            }
        }
        overlay.onStageResized { width, height ->
            relayout(width, height)
        }

        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        (layout.toggleBounds.contains(point) || dialogWindow?.blocksScreenPoint(point) == true).also { blocked ->
            if (!blocked) {
                inputActive = false
            }
        }

    fun refresh() {
        dialogWindow?.refresh()
    }

    fun updateUnitIconSize(sizePx: Double) {
        unitIconSizePx = UnitIconDisplaySize.coerce(sizePx)
        unitIconSizeText = UnitIconDisplaySize.format(unitIconSizePx)
        refresh()
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerLedgerPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed = true)

        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = layout.toggleBounds,
            label = "Game",
        )
    }

    private fun openDialog() {
        val stage = overlay.stage ?: return
        collapsed = false
        markerInput = null
        dialogWindow = MapViewerDialogWindow(
            root = overlay,
            title = "Game",
            titleTabs = ::titleTabs,
            initialPosition = ::initialDialogPosition,
            preferredWidth = MapViewerLedgerPanelLayoutBuilder.PANEL_WIDTH,
            preferredHeight = PREFERRED_DIALOG_HEIGHT,
            contentMinimumWidth = ::contentMinimumWidth,
            contentHeight = ::contentHeight,
            onClose = ::closeDialog,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
        )
        dialogWindow?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
    }

    private fun initialDialogPosition(defaultBounds: MapViewerDialogBounds): Vector2D =
        MapViewerLedgerPanelLayoutBuilder.build(
            viewportWidth = layoutViewportWidth(),
            viewportHeight = layoutViewportHeight(),
            collapsed = false,
        ).panelBounds
            ?.let { Vector2D(it.left, it.top) }
            ?: Vector2D(defaultBounds.left, defaultBounds.top)

    private fun closeDialog() {
        collapsed = true
        markerInput = null
        dialogWindow?.close()
        dialogWindow = null
    }

    private fun titleTabs(): List<MapViewerDialogTitleTab> =
        LedgerPanelTab.entries.map { tab ->
            MapViewerDialogTitleTab(
                label = tab.label,
                active = activeTab == tab,
                onSelected = {
                    inputActive = false
                    activeTab = tab
                    markerInput = null
                    dialogWindow?.refresh()
                },
            )
        }

    private fun renderContent(parent: Container, itemWidth: Double) {
        markerInput = null
        unitIconSizeInput = null
        when (activeTab) {
            LedgerPanelTab.Turns -> renderTurnsContent(parent, itemWidth, 0.0)
            LedgerPanelTab.Settings -> renderSettingsContent(parent, itemWidth, 0.0)
        }
    }

    private fun renderTurnsContent(parent: Container, itemWidth: Double, top: Double) {
        val turnPanel = turnPanelState()
        val headerLabel = if (isReadOnly()) "Turns (Read-Only)" else "Turns"
        MapViewerTextViews.leftBox(
            parent = parent,
            text = headerLabel,
            width = itemWidth,
            height = HEADER_HEIGHT,
            role = MapViewerFontRole.STRONG,
            color = Palette.Colours.WHITE,
        ) {
            y = top
        }

        val undoButton = parent.container()
        undoButton.xy(0.0, top + HEADER_HEIGHT + HEADER_GAP)
        val undo = MapViewerUiBoxes.button(undoButton, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, active = canUndo())
        MapViewerTextViews.box(undo, "Undo", CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, color = MapViewerUiStateStyle.text(canUndo()))
        undo.mouse {
            this.onClick {
                if (canUndo()) {
                    inputActive = false
                    onUndo()
                }
            }
        }

        val redoButton = parent.container()
        redoButton.xy(CONTROL_BUTTON_WIDTH + BUTTON_GAP, top + HEADER_HEIGHT + HEADER_GAP)
        val redo = MapViewerUiBoxes.button(redoButton, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, active = canRedo())
        MapViewerTextViews.box(redo, "Redo", CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, color = MapViewerUiStateStyle.text(canRedo()))
        redo.mouse {
            this.onClick {
                if (canRedo()) {
                    inputActive = false
                    onRedo()
                }
            }
        }

        val markerRowTop = top + HEADER_HEIGHT + HEADER_GAP + CONTROL_BUTTON_HEIGHT + SECTION_GAP
        val addButtonWidth = 120.0
        val markerInputWidth = itemWidth - addButtonWidth - BUTTON_GAP
        markerInput = MapViewerStyledTextInput(
            parent = parent,
            width = markerInputWidth,
            height = CONTROL_BUTTON_HEIGHT,
            x = addButtonWidth + BUTTON_GAP,
            y = markerRowTop,
            initialText = defaultTurnName(),
        ).also { input ->
            input.onClick {
                inputActive = true
            }
            if (!isReadOnly()) {
                input.focus()
            }
        }

        val addButton = parent.container()
        addButton.xy(0.0, markerRowTop)
        val addTurn = MapViewerUiBoxes.button(addButton, addButtonWidth, CONTROL_BUTTON_HEIGHT, active = !isReadOnly())
        MapViewerTextViews.box(addTurn, "Add Turn", addButtonWidth, CONTROL_BUTTON_HEIGHT, color = MapViewerUiStateStyle.text(!isReadOnly()))
        addTurn.mouse {
            this.onClick {
                if (!isReadOnly()) {
                    inputActive = false
                    val fallback = defaultTurnName()
                    onAddTurn(markerInput?.text?.takeIf(String::isNotBlank) ?: fallback)
                }
            }
        }

        val markersTop = markerRowTop + CONTROL_BUTTON_HEIGHT + SECTION_GAP
        renderRows(parent, markersTop, turnPanel, itemWidth)
    }

    private fun renderSettingsContent(parent: Container, itemWidth: Double, top: Double) {
        val inlineBlock = MapViewerInlineInputs.block(SETTINGS_INLINE_LABELS)
        inlineBlock.control(
            parent = parent,
            label = "Destroyed Units",
            x = 0.0,
            y = top,
            width = itemWidth,
            inputMinimumWidth = DESTROYED_UNITS_DROPDOWN_MINIMUM_WIDTH,
            height = CONTROL_BUTTON_HEIGHT,
        ) { input, inputWidth ->
            destroyedUnitsDropdown.renderTrigger(
                parent = input,
                value = destroyedUnitsDisplay,
                width = inputWidth,
                height = CONTROL_BUTTON_HEIGHT,
            )
        }
        unitIconSizeInput = inlineBlock.text(
            parent = parent,
            label = "Unit Icon Size",
            text = unitIconSizeText,
            x = 0.0,
            y = top + CONTROL_BUTTON_HEIGHT + SETTINGS_ROW_GAP,
            width = itemWidth,
            inputMinimumWidth = UNIT_ICON_SIZE_INPUT_MINIMUM_WIDTH,
            height = CONTROL_BUTTON_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(UnitIconDisplaySize.parse(unitIconSizeText) != null),
        ).also { input ->
            input.onClick {
                inputActive = true
            }
            input.controller.onTextUpdated.add {
                unitIconSizeText = input.text
                UnitIconDisplaySize.parse(unitIconSizeText)?.let { size ->
                    if (size != unitIconSizePx) {
                        unitIconSizePx = size
                        onUnitIconSizeChanged(size)
                    }
                }
            }
        }
    }

    private fun renderRows(parent: Container, top: Double, turnPanel: TurnPanelState, itemWidth: Double) {
        turnPanel.rows.forEachIndexed { index, rowData ->
            val rowTop = top + index * (MARKER_ROW_HEIGHT + MARKER_ROW_GAP)
            val row = parent.container()
            row.xy(0.0, rowTop)
            when (rowData) {
                is TurnPanelRow.Marker -> buildMarkerRow(
                    parent = row,
                    width = itemWidth,
                    height = MARKER_ROW_HEIGHT,
                    marker = rowData.marker,
                    active = rowData.active,
                ) {
                    inputActive = false
                    onRevertToTurn(rowData.marker)
                }

                is TurnPanelRow.Current -> buildCurrentTurnRow(
                    parent = row,
                    width = itemWidth,
                    height = MARKER_ROW_HEIGHT,
                    current = rowData,
                ) {
                    inputActive = false
                    onSelectCurrent()
                }
            }
        }
    }

    private fun buildMarkerRow(
        parent: Container,
        width: Double,
        height: Double,
        marker: TurnMarker,
        active: Boolean,
        onClick: () -> Unit,
    ) {
        MapViewerUiBoxes.box(
            parent,
            width,
            height,
            active = active,
        )

        val thumbnailLeft = TURN_THUMBNAIL_INSET
        val thumbnailTop = TURN_THUMBNAIL_INSET
        val thumbnailHeight = height - TURN_THUMBNAIL_INSET * 2.0
        val thumbnailWidth = thumbnailHeight * TURN_THUMBNAIL_ASPECT_RATIO
        MapViewerThumbnailViews.render(
            parent = parent,
            thumbnail = marker.thumbnailAssetId?.let(turnThumbnail),
            left = thumbnailLeft,
            top = thumbnailTop,
            width = thumbnailWidth,
            height = thumbnailHeight,
        )

        val textLeft = thumbnailLeft + thumbnailWidth + TURN_TEXT_GAP
        val nameWidth = width - TURN_INDEX_WIDTH - textLeft - TURN_TEXT_GAP
        MapViewerTextViews.leftBox(
            parent = parent,
            text = marker.name,
            width = nameWidth,
            height = height,
            color = MapViewerUiStateStyle.text(active),
        ) {
            x = textLeft
        }
        MapViewerTextViews.box(
            parent = parent,
            text = "%04X".format(marker.appliedIndex),
            width = TURN_INDEX_WIDTH,
            height = height,
            role = MapViewerFontRole.FLAVOUR,
            size = 18.0,
            color = MapViewerUiStateStyle.secondaryText(active),
            alignment = TextAlignment.MIDDLE_RIGHT,
        ) {
            x = width - TURN_INDEX_WIDTH - TURN_INDEX_INSET
        }

        parent.mouse {
            onClick {
                onClick()
            }
        }
    }

    private fun buildCurrentTurnRow(
        parent: Container,
        width: Double,
        height: Double,
        current: TurnPanelRow.Current,
        onClick: () -> Unit,
    ) {
        val active = current.active
        MapViewerUiBoxes.box(
            parent,
            width,
            height,
            active = active,
        )

        val thumbnailWidth = (height - TURN_THUMBNAIL_INSET * 2.0) * TURN_THUMBNAIL_ASPECT_RATIO
        val textLeft = TURN_THUMBNAIL_INSET + thumbnailWidth + TURN_TEXT_GAP
        val nameWidth = width - TURN_INDEX_WIDTH - textLeft - TURN_TEXT_GAP
        MapViewerTextViews.leftBox(
            parent = parent,
            text = "Current",
            width = nameWidth,
            height = height,
            color = MapViewerUiStateStyle.text(active),
        ) {
            x = textLeft
        }
        MapViewerTextViews.box(
            parent = parent,
            text = "Current",
            width = TURN_INDEX_WIDTH,
            height = height,
            role = MapViewerFontRole.FLAVOUR,
            size = 18.0,
            color = MapViewerUiStateStyle.secondaryText(active),
            alignment = TextAlignment.MIDDLE_RIGHT,
        ) {
            x = width - TURN_INDEX_WIDTH - TURN_INDEX_INSET
        }

        parent.mouse {
            onClick {
                onClick()
            }
        }
    }

    private fun defaultTurnName(): String =
        "Turn ${turnPanelState().rows.filterIsInstance<TurnPanelRow.Marker>().map { it.marker }.nextTurnNumber()}"

    private fun renderMarkersHeight(): Double =
        if (turnPanelState().rows.isEmpty()) {
            0.0
        } else {
            (turnPanelState().rows.size * MARKER_ROW_HEIGHT) + ((turnPanelState().rows.size - 1) * MARKER_ROW_GAP)
        }

    private fun contentHeight(width: Double): Double =
        when (activeTab) {
            LedgerPanelTab.Turns -> HEADER_HEIGHT +
                    HEADER_GAP +
                    CONTROL_BUTTON_HEIGHT +
                    SECTION_GAP +
                    CONTROL_BUTTON_HEIGHT +
                    SECTION_GAP +
                    renderMarkersHeight()

            LedgerPanelTab.Settings -> (CONTROL_BUTTON_HEIGHT * 2.0) + SETTINGS_ROW_GAP
        }

    private fun contentMinimumWidth(): Double =
        maxOf(
            CONTENT_MINIMUM_WIDTH,
            if (activeTab == LedgerPanelTab.Settings) {
                MapViewerInlineInputs.block(SETTINGS_INLINE_LABELS)
                    .minimumWidth(maxOf(DESTROYED_UNITS_DROPDOWN_MINIMUM_WIDTH, UNIT_ICON_SIZE_INPUT_MINIMUM_WIDTH))
            } else {
                0.0
            },
        )

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun layoutViewportHeight(): Int =
        overlay.stage?.views?.actualVirtualHeight ?: 0

    companion object {
        fun destroyedUnitsDropdownMinimumWidth(): Double =
            DESTROYED_UNITS_DROPDOWN_MINIMUM_WIDTH

        val SETTINGS_INLINE_LABELS = listOf("Destroyed Units", "Unit Icon Size")
        const val Z_INDEX = 10_000.0
        private const val CONTENT_MINIMUM_WIDTH = 320.0
        private const val UNIT_ICON_SIZE_INPUT_MINIMUM_WIDTH = 92.0
        private const val SETTINGS_ROW_GAP = 12.0
        val DESTROYED_UNITS_DROPDOWN_MINIMUM_WIDTH = maxOf(
            160.0,
            MapViewerDropdownLayoutBuilder.minimumMenuWidth(
                labels = DestroyedUnitsDisplay.entries.map { it.label },
                hasIcons = false,
            ),
        )
        const val PREFERRED_DIALOG_HEIGHT = 520.0
        const val HEADER_HEIGHT = 40.0
        const val HEADER_GAP = 12.0
        const val SECTION_GAP = 16.0
        const val BUTTON_GAP = 12.0
        const val CONTROL_BUTTON_WIDTH = 134.0
        const val CONTROL_BUTTON_HEIGHT = 40.0
        const val MARKER_ROW_HEIGHT = 56.0
        const val MARKER_ROW_GAP = 10.0
        const val TURN_THUMBNAIL_INSET = 4.0
        const val TURN_THUMBNAIL_ASPECT_RATIO = 16.0 / 9.0
        const val TURN_TEXT_GAP = 10.0
        const val TURN_INDEX_WIDTH = 64.0
        const val TURN_INDEX_INSET = 10.0
    }
}

internal enum class LedgerPanelTab(val label: String) {
    Turns("Turns"),
    Settings("Settings"),
}

internal enum class DestroyedUnitsDisplay(val label: String) {
    Full("Full"),
    Partial("Partial"),
    Hide("Hide");

    companion object {
        fun fromSettingsValue(value: String?): DestroyedUnitsDisplay =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: Full
    }
}
