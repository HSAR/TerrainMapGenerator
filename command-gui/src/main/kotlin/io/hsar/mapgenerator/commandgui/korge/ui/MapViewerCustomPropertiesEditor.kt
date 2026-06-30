package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.awt.Color
import kotlin.math.max

@OptIn(KorgeExperimental::class)
internal class MapViewerCustomPropertiesEditor(
    initialProperties: Map<String, String>,
    private val reservedKeys: Set<String> = emptySet(),
    private val onRowsChanged: () -> Unit = {},
) {
    private val rows = initialProperties
        .filterKeys { it !in reservedKeys }
        .map { (key, value) -> CustomPropertyRow(key, value) }
        .toMutableList()
    private val inputs = mutableListOf<MapViewerInputField>()
    private var renderedParent: Container? = null
    private var renderedWidth: Double = 0.0
    private var tooltip: Container? = null
    private var pendingFocusInputIndex: Int? = null

    val isTextInputActive: Boolean
        get() = inputs.any { it.isFocused }

    fun properties(): Map<String, String> {
        syncRowsFromInputs()
        return rows.fold(linkedMapOf<String, String>()) { properties, row ->
            row.key.trim()
                .takeIf { it.isNotBlank() && it !in reservedKeys }
                ?.let { key -> properties[key] = row.value }
            properties
        }
    }

    fun render(parent: Container, width: Double) {
        renderedParent = parent
        renderedWidth = width
        parent.removeChildren()
        inputs.clear()

        MapViewerTextViews.label(parent, "Additional Properties", color = Palette.Colours.WHITE)

        val layout = layout(width)
        rows.forEachIndexed { index, row ->
            renderRow(parent, row, index, layout)
        }

        val addButton = parent.container()
        addButton.xy(0.0, addButtonTop())
        renderButton(addButton, ADD_BUTTON_WIDTH, "Add Property") {
            syncRowsFromInputs()
            rows += CustomPropertyRow("", "")
            onRowsChanged()
            rerender()
        }
    }

    fun height(): Double =
        addButtonTop() + BUTTON_HEIGHT

    fun minimumWidth(): Double =
        (MIN_INPUT_WIDTH * 2.0) + REMOVE_BUTTON_WIDTH + (MapViewerDialogSpacing.FIELD_GAP * 2.0)

    private fun renderRow(
        parent: Container,
        row: CustomPropertyRow,
        index: Int,
        layout: CustomPropertyEditorLayout,
    ) {
        val top = MapViewerDialogSpacing.LABEL_TO_FIELD_GAP + (index * ROW_HEIGHT)

        val isConflicting = row.isConflicting()
        val keyInput = MapViewerTextInputs.input(
            parent = parent,
            text = row.key,
            width = layout.keyWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            x = 0.0,
            y = top,
            frameStyle = if (isConflicting) ::errorFrameStyle else MapViewerUiBoxes::defaultStyle,
        )
        keyInput.controller.onTextUpdated.add {
            val wasConflicting = row.isConflicting()
            row.key = keyInput.text
            if (row.isConflicting() != wasConflicting) {
                pendingFocusInputIndex = index * INPUTS_PER_ROW
                rerender()
            }
        }
        inputs += keyInput
        focusIfPending(keyInput, index * INPUTS_PER_ROW)

        if (isConflicting) {
            renderConflictWarning(parent, layout.keyWidth, top, row.key.trim())
        }

        val valueInput = MapViewerTextInputs.input(
            parent = parent,
            text = row.value,
            width = layout.valueWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            x = layout.valueLeft,
            y = top,
        )
        valueInput.controller.onTextUpdated.add {
            row.value = valueInput.text
        }
        inputs += valueInput
        focusIfPending(valueInput, index * INPUTS_PER_ROW + 1)

        val removeButton = parent.container()
        removeButton.xy(layout.removeLeft, top)
        renderButton(removeButton, REMOVE_BUTTON_WIDTH, "X") {
            syncRowsFromInputs()
            rows.removeAt(index)
            onRowsChanged()
            rerender()
        }
    }

    private fun renderButton(parent: Container, width: Double, label: String, onClick: () -> Unit) {
        MapViewerUiBoxes.button(parent, width, BUTTON_HEIGHT)
        MapViewerTextViews.box(parent, label, width, BUTTON_HEIGHT, color = Palette.Colours.WHITE)
        parent.mouse {
            onClick { onClick() }
        }
    }

    private fun focusIfPending(input: MapViewerInputField, inputIndex: Int) {
        if (pendingFocusInputIndex == inputIndex) {
            input.focus()
            pendingFocusInputIndex = null
        }
    }

    private fun renderConflictWarning(parent: Container, keyWidth: Double, top: Double, key: String) {
        val marker = parent.container()
        marker.xy(keyWidth - WARNING_MARKER_WIDTH, top)
        MapViewerTextViews.box(
            parent = marker,
            text = "!",
            width = WARNING_MARKER_WIDTH,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            role = MapViewerFontRole.STRONG,
            color = Palette.Colours.RED,
        )
        marker.mouse {
            onOver {
                showTooltip(
                    parent = parent,
                    anchor = Vector2D(keyWidth, top),
                    text = "Property \"$key\" is reserved by the app and cannot be used here.",
                )
            }
            onOut {
                hideTooltip()
            }
        }
    }

    private fun showTooltip(parent: Container, anchor: Vector2D, text: String) {
        hideTooltip()
        val width = max(TOOLTIP_MIN_WIDTH, MapViewerTextViews.measure(text).width + TOOLTIP_PADDING_X * 2.0)
        val height = TOOLTIP_HEIGHT
        tooltip = parent.container().also { layer ->
            layer.xy((anchor.x - width).coerceAtLeast(0.0), anchor.y - height - TOOLTIP_GAP)
            layer.zIndex = TOOLTIP_Z_INDEX
            layer.solidRect(width, height, RGBA(0, 0, 0, 220))
            MapViewerUiBoxes.frame(
                parent = layer,
                width = width,
                height = height,
                fillColor = Palette.Colours.DARK,
                fillAlpha = 245,
                borderColor = Palette.Colours.RED,
                borderAlpha = 220,
            )
            MapViewerTextViews.leftBox(
                parent = layer,
                text = text,
                width = width - TOOLTIP_PADDING_X * 2.0,
                height = height,
                color = Palette.Colours.WHITE,
            ) {
                x = TOOLTIP_PADDING_X
            }
        }
    }

    private fun hideTooltip() {
        tooltip?.removeFromParent()
        tooltip = null
    }

    private fun syncRowsFromInputs() {
        rows.forEachIndexed { index, row ->
            val inputIndex = index * INPUTS_PER_ROW
            inputs.getOrNull(inputIndex)?.let { row.key = it.text }
            inputs.getOrNull(inputIndex + 1)?.let { row.value = it.text }
        }
    }

    private fun rerender() {
        hideTooltip()
        renderedParent?.let { parent -> render(parent, renderedWidth) }
    }

    private fun CustomPropertyRow.isConflicting(): Boolean =
        key.trim() in reservedKeys

    private fun errorFrameStyle(active: Boolean): UiFrameStyle =
        UiFrameStyle(
            fillColor = if (active) ERROR_FILL else Palette.Colours.MAIN,
            fillAlpha = if (active) 190 else MapViewerUiStateStyle.inactiveFillAlpha,
            borderColor = Palette.Colours.RED,
            borderAlpha = 235,
        )

    private fun layout(width: Double): CustomPropertyEditorLayout {
        val removeLeft = width - REMOVE_BUTTON_WIDTH
        val usableWidth = (removeLeft - MapViewerDialogSpacing.FIELD_GAP * 2.0).coerceAtLeast(MIN_INPUT_WIDTH * 2.0)
        val keyWidth = max(MIN_INPUT_WIDTH, usableWidth * KEY_WIDTH_RATIO)
        val valueWidth = max(MIN_INPUT_WIDTH, usableWidth - keyWidth)
        return CustomPropertyEditorLayout(
            keyWidth = keyWidth,
            valueLeft = keyWidth + MapViewerDialogSpacing.FIELD_GAP,
            valueWidth = valueWidth,
            removeLeft = valueWidth + keyWidth + (MapViewerDialogSpacing.FIELD_GAP * 2.0),
        )
    }

    private fun addButtonTop(): Double =
        MapViewerDialogSpacing.LABEL_TO_FIELD_GAP + (rows.size * ROW_HEIGHT)

    private data class CustomPropertyRow(
        var key: String,
        var value: String,
    )

    private data class CustomPropertyEditorLayout(
        val keyWidth: Double,
        val valueLeft: Double,
        val valueWidth: Double,
        val removeLeft: Double,
    )

    private companion object {
        const val ROW_HEIGHT = 44.0
        const val BUTTON_HEIGHT = 32.0
        const val ADD_BUTTON_WIDTH = 130.0
        const val REMOVE_BUTTON_WIDTH = 36.0
        const val MIN_INPUT_WIDTH = 120.0
        const val KEY_WIDTH_RATIO = 0.36
        const val INPUTS_PER_ROW = 2
        const val WARNING_MARKER_WIDTH = 24.0
        const val TOOLTIP_GAP = 8.0
        const val TOOLTIP_HEIGHT = 34.0
        const val TOOLTIP_MIN_WIDTH = 260.0
        const val TOOLTIP_PADDING_X = 10.0
        const val TOOLTIP_Z_INDEX = 1000.0
        val ERROR_FILL = Color(82, 16, 20)
    }
}
