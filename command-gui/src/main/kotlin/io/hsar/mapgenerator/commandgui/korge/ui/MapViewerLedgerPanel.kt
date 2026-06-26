package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CurrentTurn
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnMarker
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiMaterialLayer
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.size
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Vector2D
import java.awt.Color

internal class MapViewerLedgerPanel(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val canUndo: () -> Boolean,
    private val canRedo: () -> Boolean,
    private val turnMarkers: () -> List<TurnMarker>,
    private val currentTurn: () -> CurrentTurn,
    private val isReadOnly: () -> Boolean,
    private val onUndo: () -> Unit,
    private val onRedo: () -> Unit,
    private val onAddTurn: (String) -> Unit,
    private val onRevertToTurn: (TurnMarker) -> Unit,
    private val onSelectCurrent: () -> Unit,
) {
    private val overlay = root.container()
    private val panel = overlay.uiMaterialLayer()
    private val content = overlay.container()
    private val toggleButton = overlay.container()
    private var collapsed = true
    private var markerInput: MapViewerStyledTextInput? = null
    private var inputActive = false
    private var layout = MapViewerLedgerPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

    val isTextInputActive: Boolean
        get() = inputActive

    init {
        overlay.zIndex = Z_INDEX
        panel.radius = RectCorners(0.0)
        panel.bgColor = toRgba(Palette.Colours.DARK, 220)
        panel.borderSize = 2.0
        panel.borderColor = RGBA(255, 255, 255, 255)
        panel.shadowColor = RGBA(0, 0, 0, 0)
        panel.shadowRadius = 0.0

        toggleButton.mouse {
            onClick {
                inputActive = false
                collapsed = !collapsed
                relayout(layoutViewportWidth(), layoutViewportHeight())
            }
        }
        overlay.onStageResized { width, height ->
            relayout(width, height)
        }

        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        layout.blocks(point).also { blocked ->
            if (!blocked) {
                inputActive = false
            }
        }

    fun refresh() {
        relayout(layoutViewportWidth(), layoutViewportHeight())
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerLedgerPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

        toggleButton.removeChildren()
        toggleButton.xy(layout.toggleBounds.left, layout.toggleBounds.top)
        buildButton(
            parent = toggleButton,
            width = layout.toggleBounds.width,
            height = layout.toggleBounds.height,
            label = if (collapsed) "Show Turns" else "Hide Turns",
        )

        val panelBounds = layout.panelBounds
        panel.visible = panelBounds != null
        content.visible = panelBounds != null
        markerInput = null

        if (panelBounds != null) {
            panel.xy(panelBounds.left, panelBounds.top)
            panel.size(panelBounds.width, panelBounds.height)

            content.removeChildren()
            content.xy(panelBounds.left + CONTENT_PADDING, panelBounds.top + CONTENT_PADDING)
            renderContent()
        }
    }

    private fun renderContent() {
        val itemWidth = MapViewerLedgerPanelLayoutBuilder.PANEL_WIDTH - (CONTENT_PADDING * 2)
        val current = currentTurn()
        val headerLabel = if (isReadOnly()) "Turns (Read-Only)" else "Turns"
        MapViewerTextViews.leftBox(
            parent = content,
            text = headerLabel,
            width = itemWidth,
            height = HEADER_HEIGHT,
            role = MapViewerFontRole.STRONG,
            color = Palette.Colours.WHITE,
        )

        val undoButton = content.container()
        undoButton.xy(0.0, HEADER_HEIGHT + HEADER_GAP)
        buildButton(undoButton, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, "Undo", enabled = canUndo()) {
            inputActive = false
            onUndo()
        }

        val redoButton = content.container()
        redoButton.xy(CONTROL_BUTTON_WIDTH + BUTTON_GAP, HEADER_HEIGHT + HEADER_GAP)
        buildButton(redoButton, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, "Redo", enabled = canRedo()) {
            inputActive = false
            onRedo()
        }

        val markerRowTop = HEADER_HEIGHT + HEADER_GAP + CONTROL_BUTTON_HEIGHT + SECTION_GAP
        val addButtonWidth = 120.0
        val markerInputWidth = itemWidth - addButtonWidth - BUTTON_GAP
        markerInput = MapViewerStyledTextInput(
            parent = content,
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

        val addButton = content.container()
        addButton.xy(0.0, markerRowTop)
        buildButton(addButton, addButtonWidth, CONTROL_BUTTON_HEIGHT, "Add Turn", enabled = !isReadOnly()) {
            inputActive = false
            val fallback = defaultTurnName()
            onAddTurn(markerInput?.text?.takeIf(String::isNotBlank) ?: fallback)
        }

        val markersTop = markerRowTop + CONTROL_BUTTON_HEIGHT + SECTION_GAP
        renderMarkers(markersTop, current)
        if (current.label == "Current") {
            renderCurrentTurn(markersTop + renderMarkersHeight() + MARKER_ROW_GAP)
        }
    }

    private fun renderMarkers(top: Double, current: CurrentTurn) {
        turnMarkers().forEachIndexed { index, marker ->
            val rowTop = top + index * (MARKER_ROW_HEIGHT + MARKER_ROW_GAP)
            val row = content.container()
            row.xy(0.0, rowTop)
            buildMarkerRow(
                parent = row,
                width = MapViewerLedgerPanelLayoutBuilder.PANEL_WIDTH - (CONTENT_PADDING * 2),
                height = MARKER_ROW_HEIGHT,
                marker = marker,
                isCurrentState = current.label != "Current" && marker.appliedIndex == current.appliedIndex,
            ) {
                inputActive = false
                onRevertToTurn(marker)
            }
        }
    }

    private fun renderCurrentTurn(top: Double) {
        val current = currentTurn()
        val row = content.container()
        row.xy(0.0, top)
        buildCurrentTurnRow(
            parent = row,
            width = MapViewerLedgerPanelLayoutBuilder.PANEL_WIDTH - (CONTENT_PADDING * 2),
            height = MARKER_ROW_HEIGHT,
            current = current,
        ) {
            inputActive = false
            onSelectCurrent()
        }
    }

    private fun buildMarkerRow(
        parent: Container,
        width: Double,
        height: Double,
        marker: TurnMarker,
        isCurrentState: Boolean,
        onClick: () -> Unit,
    ) {
        val rowAlpha = when {
            marker.isFuture -> 170
            isCurrentState -> 255
            else -> 255
        }
        parent.solidRect(width, height, RGBA(255, 255, 255, rowAlpha))
        parent.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK, if (marker.isFuture) 200 else 255)) {
            x = 2.0
            y = 2.0
        }

        val nameWidth = width - TURN_INDEX_WIDTH - TURN_TEXT_INSET - TURN_TEXT_GAP
        MapViewerTextViews.leftBox(
            parent = parent,
            text = marker.name,
            width = nameWidth,
            height = height,
            color = Palette.Colours.WHITE.withAlpha(if (marker.isFuture) 170 else 255),
        ) {
            x = TURN_TEXT_INSET
        }
        MapViewerTextViews.box(
            parent = parent,
            text = if (isCurrentState) "Current" else "%04X".format(marker.appliedIndex),
            width = TURN_INDEX_WIDTH,
            height = height,
            role = MapViewerFontRole.FLAVOUR,
            size = 18.0,
            color = Palette.Colours.LIGHT.withAlpha(if (marker.isFuture) 140 else 255),
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
        current: CurrentTurn,
        onClick: () -> Unit,
    ) {
        val rowAlpha = if (current.selected) 255 else 170
        parent.solidRect(width, height, RGBA(255, 255, 255, rowAlpha))
        parent.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK, if (current.selected) 255 else 200)) {
            x = 2.0
            y = 2.0
        }

        val nameWidth = width - TURN_INDEX_WIDTH - TURN_TEXT_INSET - TURN_TEXT_GAP
        MapViewerTextViews.leftBox(
            parent = parent,
            text = current.label,
            width = nameWidth,
            height = height,
            color = Palette.Colours.WHITE.withAlpha(rowAlpha),
        ) {
            x = TURN_TEXT_INSET
        }
        MapViewerTextViews.box(
            parent = parent,
            text = "Current",
            width = TURN_INDEX_WIDTH,
            height = height,
            role = MapViewerFontRole.FLAVOUR,
            size = 18.0,
            color = Palette.Colours.LIGHT.withAlpha(if (current.selected) 255 else 140),
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

    private fun buildButton(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
    ) {
        parent.solidRect(width, height, RGBA(255, 255, 255, 255))
        parent.solidRect(width - 4.0, height - 4.0, toRgba(if (enabled) Palette.Colours.DARK else Palette.Colours.LIGHT, if (enabled) 255 else 120)) {
            x = 2.0
            y = 2.0
        }
        MapViewerTextViews.box(parent, label, width, height, color = Palette.Colours.WHITE)
        parent.mouse {
            this.onClick {
                if (enabled) {
                    onClick()
                }
            }
        }
    }

    private fun defaultTurnName(): String =
        "Turn ${turnMarkers().count { !it.isFuture } + 1}".trim()

    private fun renderMarkersHeight(): Double =
        if (turnMarkers().isEmpty()) {
            0.0
        } else {
            (turnMarkers().size * MARKER_ROW_HEIGHT) + ((turnMarkers().size - 1) * MARKER_ROW_GAP)
        }

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun layoutViewportHeight(): Int =
        overlay.stage?.views?.actualVirtualHeight ?: 0

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private fun Color.withAlpha(alpha: Int): Color =
        Color(red, green, blue, alpha)

    private companion object {
        const val Z_INDEX = 10_000.0
        const val CONTENT_PADDING = 20.0
        const val HEADER_HEIGHT = 40.0
        const val HEADER_GAP = 12.0
        const val SECTION_GAP = 16.0
        const val BUTTON_GAP = 12.0
        const val CONTROL_BUTTON_WIDTH = 134.0
        const val CONTROL_BUTTON_HEIGHT = 40.0
        const val MARKER_ROW_HEIGHT = 40.0
        const val MARKER_ROW_GAP = 10.0
        const val TURN_TEXT_INSET = 12.0
        const val TURN_TEXT_GAP = 10.0
        const val TURN_INDEX_WIDTH = 64.0
        const val TURN_INDEX_INSET = 10.0
    }
}
