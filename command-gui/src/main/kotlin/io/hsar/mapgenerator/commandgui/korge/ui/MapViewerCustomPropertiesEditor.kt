package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import kotlin.math.max

@OptIn(KorgeExperimental::class, KorgeInternal::class)
internal class MapViewerCustomPropertiesEditor(
    initialProperties: Map<String, String>,
    inheritedProperties: Map<String, String> = emptyMap(),
    private val reservedKeys: Set<String> = emptySet(),
    private val onRowsChanged: () -> Unit = {},
    private val onValidationChanged: () -> Unit = {},
    private val tooltipRoot: Container? = null,
) {
    private val rows = (inheritedProperties + initialProperties)
        .filterKeys { it !in reservedKeys }
        .map { (key, value) -> CustomPropertyRow(key, value, key in inheritedProperties) }
        .toMutableList()
    private val inputs = mutableListOf<MapViewerInputField>()
    private var renderedParent: Container? = null
    private var renderedWidth: Double = 0.0
    private var tooltip: Container? = null
    private var pendingFocusInputIndex: Int? = null
    private var trailingAction: ((Container, Double, Double) -> Unit)? = null

    val isTextInputActive: Boolean
        get() = inputs.any { it.isFocused }

    val isValid: Boolean
        get() {
            syncRowsFromInputs()
            return rows.none { it.isConflicting() }
        }

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
        renderInternal(parent, width)
    }

    fun renderBody(parent: Container, width: Double, trailingAction: ((Container, Double, Double) -> Unit)? = null) {
        this.trailingAction = trailingAction
        renderInternal(parent, width)
    }

    fun propertyCount(): Int = rows.size

    fun bodyHeight(): Double =
        MapViewerDialogSpacing.LABEL_TO_FIELD_GAP + (rows.size * ROW_HEIGHT) + BUTTON_HEIGHT

    private fun renderInternal(parent: Container, width: Double) {
        renderedParent = parent
        renderedWidth = width
        parent.removeChildren()
        inputs.clear()

        val layout = layout(width)
        rows.forEachIndexed { index, row ->
            renderRow(parent, row, index, layout)
        }

        val addButton = parent.container()
        addButton.xy(0.0, addButtonTop())
        renderButton(addButton, ADD_BUTTON_WIDTH, "Add Property") {
            syncRowsFromInputs()
            rows += CustomPropertyRow("", "", isInherited = false)
            onRowsChanged()
            rerender()
        }
        trailingAction?.invoke(parent, ADD_BUTTON_WIDTH + MapViewerDialogSpacing.FIELD_GAP, addButtonTop())
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
        val firstInputIndex = inputIndexForRow(index)

        val isConflicting = row.isConflicting()
        if (row.isInherited) {
            MapViewerUiBoxes.box(parent, layout.keyWidth, MapViewerDialogSpacing.INPUT_HEIGHT, active = false).xy(0.0, top)
                .also { keyField ->
                    MapViewerTextViews.leftBox(
                        parent = keyField,
                        text = row.key,
                        width = layout.keyWidth - 12.0,
                        height = MapViewerDialogSpacing.INPUT_HEIGHT,
                        color = Palette.Colours.WHITE,
                    ) { x = 6.0 }
                }
        } else {
            val keyInput = MapViewerTextInputs.input(
                parent = parent,
                text = row.key,
                width = layout.keyWidth,
                height = MapViewerDialogSpacing.INPUT_HEIGHT,
                x = 0.0,
                y = top,
                frameStyle = MapViewerTextInputValidation.frameStyle(!isConflicting),
            )
            keyInput.controller.onTextUpdated.add {
                val wasConflicting = row.isConflicting()
                row.key = keyInput.text
                if (row.isConflicting() != wasConflicting) {
                    pendingFocusInputIndex = firstInputIndex
                    onValidationChanged()
                    rerender()
                }
            }
            inputs += keyInput
            focusIfPending(keyInput, firstInputIndex)
        }

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
        focusIfPending(valueInput, firstInputIndex + if (row.isInherited) 0 else 1)

        if (!row.isInherited) {
            val removeButton = parent.container()
            removeButton.xy(layout.removeLeft, top)
            renderButton(removeButton, REMOVE_BUTTON_WIDTH, "X") {
                syncRowsFromInputs()
                val wasValid = rows.none { it.isConflicting() }
                rows.removeAt(index)
                if (rows.none { it.isConflicting() } != wasValid) {
                    onValidationChanged()
                }
                onRowsChanged()
                rerender()
            }
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
        val icon = marker.container()
        icon.xy(
            (WARNING_MARKER_WIDTH - WARNING_ICON_SIZE) * 0.5,
            (MapViewerDialogSpacing.INPUT_HEIGHT - WARNING_ICON_SIZE) * 0.5,
        )
        MapViewerTextInputValidation.validationFailedIndicator(icon, WARNING_ICON_SIZE)
        marker.mouse {
            onOver {
                showTooltip(
                    anchor = marker.globalPos + Vector2D(WARNING_MARKER_WIDTH, 0.0),
                    text = "Property \"$key\" is reserved by the app and cannot be used here.",
                )
            }
            onOut {
                hideTooltip()
            }
        }
    }

    private fun showTooltip(anchor: Vector2D, text: String) {
        hideTooltip()
        val parent = tooltipRoot ?: renderedParent ?: return
        tooltip = MapViewerTextInputValidation.validationExplanationTooltip(parent, anchor, text)
    }

    fun hideTooltip() {
        tooltip?.removeFromParent()
        tooltip = null
    }

    private fun syncRowsFromInputs() {
        var inputIndex = 0
        rows.forEach { row ->
            if (!row.isInherited) {
                inputs.getOrNull(inputIndex++)?.let { row.key = it.text }
            }
            inputs.getOrNull(inputIndex++)?.let { row.value = it.text }
        }
    }

    private fun rerender() {
        hideTooltip()
        renderedParent?.let { parent -> render(parent, renderedWidth) }
    }

    private fun CustomPropertyRow.isConflicting(): Boolean =
        MapViewerTextInputValidation.isReservedKey(key, reservedKeys)

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
        val isInherited: Boolean,
    )

    private fun inputIndexForRow(rowIndex: Int): Int =
        rows.take(rowIndex).sumOf { if (it.isInherited) 1 else INPUTS_PER_ROW }

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
        const val WARNING_ICON_SIZE = 20.0
    }
}
