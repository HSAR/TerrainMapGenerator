package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal object MapViewerInlineInputs {
    fun block(labels: List<String>): Block =
        Block((labels.maxOfOrNull(::labelWidth) ?: 0.0) + LABEL_TO_INPUT_GAP)

    fun text(
        parent: Container,
        label: String,
        text: String,
        x: Double,
        y: Double,
        width: Double,
        inputMinimumWidth: Double,
        height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
        frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
    ): MapViewerInputField =
        control(
            parent = parent,
            label = label,
            x = x,
            y = y,
            width = width,
            inputMinimumWidth = inputMinimumWidth,
            height = height,
        ) { input, inputWidth ->
            MapViewerTextInputs.input(
                parent = input,
                text = text,
                width = inputWidth,
                height = height,
                frameStyle = frameStyle,
            )
        }

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
        control(
            parent = parent,
            label = label,
            x = x,
            y = y,
            width = width,
            inputMinimumWidth = inputMinimumWidth,
            height = height,
        ) { input, inputWidth ->
            dropdown.renderTrigger(
                parent = input,
                value = value,
                width = inputWidth,
                height = height,
            )
        }
    }

    fun <T> control(
        parent: Container,
        label: String,
        x: Double,
        y: Double,
        width: Double,
        inputMinimumWidth: Double,
        height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
        renderInput: (Container, Double) -> T,
    ): T {
        val inputLeft = labelWidth(label) + LABEL_TO_INPUT_GAP
        return controlAtInputLeft(
            parent = parent,
            label = label,
            x = x,
            y = y,
            width = width,
            inputMinimumWidth = inputMinimumWidth,
            height = height,
            inputLeft = inputLeft,
            renderInput = renderInput,
        )
    }

    fun minimumWidth(label: String, inputMinimumWidth: Double): Double =
        labelWidth(label) + LABEL_TO_INPUT_GAP + inputMinimumWidth

    internal fun <T> controlAtInputLeft(
        parent: Container,
        label: String,
        x: Double,
        y: Double,
        width: Double,
        inputMinimumWidth: Double,
        height: Double,
        inputLeft: Double,
        renderInput: (Container, Double) -> T,
    ): T {
        val labelWidth = (inputLeft - LABEL_TO_INPUT_GAP).coerceAtLeast(0.0)
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
        return parent.container().let { input ->
            input.xy(x + inputLeft, y)
            renderInput(input, inputWidth)
        }
    }

    private fun labelWidth(label: String): Double =
        MapViewerTextViews.measure(label).width + LABEL_RIGHT_PADDING

    private const val LABEL_RIGHT_PADDING = 8.0
    private const val LABEL_TO_INPUT_GAP = 12.0

    internal class Block internal constructor(private val inputLeft: Double) {
        fun <T> controlWithoutLabel(
            parent: Container,
            x: Double,
            y: Double,
            width: Double,
            inputMinimumWidth: Double,
            height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
            renderInput: (Container, Double) -> T,
        ): T {
            val inputWidth = width.coerceAtLeast(inputMinimumWidth)
            return parent.container().let { input ->
                input.xy(x, y)
                renderInput(input, inputWidth)
            }
        }

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
            control(
                parent = parent,
                label = label,
                x = x,
                y = y,
                width = width,
                inputMinimumWidth = inputMinimumWidth,
                height = height,
            ) { input, inputWidth ->
                dropdown.renderTrigger(
                    parent = input,
                    value = value,
                    width = inputWidth,
                    height = height,
                )
            }
        }

        fun text(
            parent: Container,
            label: String,
            text: String,
            x: Double,
            y: Double,
            width: Double,
            inputMinimumWidth: Double,
            height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
        ): MapViewerInputField =
            control(
                parent = parent,
                label = label,
                x = x,
                y = y,
                width = width,
                inputMinimumWidth = inputMinimumWidth,
                height = height,
            ) { input, inputWidth ->
                MapViewerTextInputs.input(
                    parent = input,
                    text = text,
                    width = inputWidth,
                    height = height,
                    frameStyle = frameStyle,
                )
            }

        fun <T> control(
            parent: Container,
            label: String,
            x: Double,
            y: Double,
            width: Double,
            inputMinimumWidth: Double,
            height: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
            renderInput: (Container, Double) -> T,
        ): T =
            controlAtInputLeft(
                parent = parent,
                label = label,
                x = x,
                y = y,
                width = width,
                inputMinimumWidth = inputMinimumWidth,
                height = height,
                inputLeft = inputLeft,
                renderInput = renderInput,
            )

        fun minimumWidth(inputMinimumWidth: Double): Double =
            inputLeft + inputMinimumWidth
    }
}
