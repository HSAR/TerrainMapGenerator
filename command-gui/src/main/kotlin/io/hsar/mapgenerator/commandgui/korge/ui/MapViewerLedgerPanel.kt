package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CurrentTurn
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnMarker
import io.hsar.mapgenerator.commandgui.korge.interaction.nextTurnNumber
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiMaterialLayer
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.size
import korlibs.korge.view.xy
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Vector2D
import java.awt.Color
import java.util.*

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
    private val turnThumbnail: (UUID) -> Bitmap32? = { null },
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
        panel.borderColor = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, MapViewerUiStateStyle.ACTIVE_ALPHA)
        panel.shadowColor = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, 0)
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

        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = layout.toggleBounds,
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

        val redoButton = content.container()
        redoButton.xy(CONTROL_BUTTON_WIDTH + BUTTON_GAP, HEADER_HEIGHT + HEADER_GAP)
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
        renderMarkers(markersTop, current)
        if (current.showInjectedCurrentRow) {
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
                isCurrentState = marker.appliedIndex == current.appliedIndex,
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
        val active = isCurrentState
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
        current: CurrentTurn,
        onClick: () -> Unit,
    ) {
        val active = current.selected
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
            text = current.label,
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
        "Turn ${turnMarkers().nextTurnNumber()}".trim()

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

    private companion object {
        const val Z_INDEX = 10_000.0
        const val CONTENT_PADDING = 20.0
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
