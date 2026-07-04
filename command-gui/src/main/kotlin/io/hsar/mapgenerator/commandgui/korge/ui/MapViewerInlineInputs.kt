package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal object MapViewerInlineInputs {
    fun <T> dropdown(
        parent: Container,
        label: String,
        dropdown: MapViewerDropdownMenu<T>,
        value: T?,
        x: Double,
        y: Double,
        width: Double,
        inputMinimumWidth: Double,
        height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
    ) {
        val labelWidth = labelWidth(label)
        val inputLeft = labelWidth + LABEL_TO_INPUT_GAP
        val inputWidth = (width - inputLeft).coerceAtLeast(inputMinimumWidth.coerceAtMost(width))
        MapViewerTextViews.leftBox(
            parent = parent,
            text = label,
            width = labelWidth,
            height = height,
            color = Palette.Colours.WHITE,
        ) {
            this.x = x
            this.y = y
        }
        parent.container().also { input ->
            input.xy(x + inputLeft, y)
            dropdown.renderTrigger(
                parent = input,
                value = value,
                width = inputWidth,
                height = height,
            )
        }
    }

    fun minimumWidth(label: String, inputMinimumWidth: Double): Double =
        labelWidth(label) + LABEL_TO_INPUT_GAP + inputMinimumWidth

    private fun labelWidth(label: String): Double =
        MapViewerTextViews.measure(label).width + LABEL_RIGHT_PADDING

    private const val LABEL_RIGHT_PADDING = 8.0
    private const val LABEL_TO_INPUT_GAP = 12.0
}
