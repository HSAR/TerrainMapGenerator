package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal data class MapViewerDialogAction(
    val label: String,
    val onClick: () -> Unit,
)

internal object MapViewerDialogActions {
    const val BUTTON_WIDTH = 110.0
    const val BUTTON_HEIGHT = MapViewerDialogLayoutBuilder.FOOTER_BUTTON_HEIGHT
    const val BUTTON_GAP = 18.0

    fun render(parent: Container, bounds: ScreenBounds, actions: List<MapViewerDialogAction>) {
        if (actions.isEmpty()) {
            return
        }

        val centers = centerRatios(actions.size)
        val centerY = bounds.top + bounds.height * 0.5
        actions.zip(centers).forEach { (action, centerRatio) ->
            val buttonLeft = bounds.left + (bounds.width * centerRatio) - BUTTON_WIDTH * 0.5
            val buttonTop = centerY - BUTTON_HEIGHT * 0.5
            val button = parent.container()
            button.xy(buttonLeft, buttonTop)
            MapViewerUiBoxes.button(button, BUTTON_WIDTH, BUTTON_HEIGHT)
            MapViewerTextViews.box(button, action.label, BUTTON_WIDTH, BUTTON_HEIGHT, color = MapViewerUiStateStyle.text(true))
            button.mouse {
                onClick {
                    action.onClick()
                }
            }
        }
    }

    fun centerRatios(buttonCount: Int): List<Double> =
        if (buttonCount <= 0) {
            emptyList()
        } else {
            (0 until buttonCount).map { index -> (index + 1).toDouble() / (buttonCount + 1).toDouble() }
        }
}
