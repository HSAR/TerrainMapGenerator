package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal data class MapViewerDialogAction(
    val label: String,
    val enabled: () -> Boolean = { true },
    val onClick: () -> Unit,
)

internal object MapViewerDialogActions {
    const val BUTTON_WIDTH = 110.0
    const val BUTTON_HEIGHT = MapViewerDialogLayoutBuilder.FOOTER_BUTTON_HEIGHT
    const val BUTTON_GAP = 18.0

    fun render(
        parent: Container,
        bounds: ScreenBounds,
        actions: List<MapViewerDialogAction>,
        layout: MapViewerDialogFooterLayout = footerLayout(bounds.width, actions.size),
    ) {
        if (actions.isEmpty()) {
            return
        }

        when (layout) {
            MapViewerDialogFooterLayout.ROW -> {
                val centers = centerRatios(actions.size)
                val centerY = bounds.top + bounds.height * 0.5
                actions.zip(centers).forEach { (action, centerRatio) ->
                    val buttonLeft = bounds.left + (bounds.width * centerRatio) - BUTTON_WIDTH * 0.5
                    val buttonTop = centerY - BUTTON_HEIGHT * 0.5
                    val button = parent.container()
                    button.xy(buttonLeft, buttonTop)
                    renderButton(button, action)
                }
            }

            MapViewerDialogFooterLayout.COLUMN -> {
                val stackHeight = stackedHeight(actions.size)
                val startTop = bounds.top + ((bounds.height - stackHeight) * 0.5)
                val buttonLeft = bounds.left + (bounds.width - BUTTON_WIDTH) * 0.5
                actions.forEachIndexed { index, action ->
                    val button = parent.container()
                    button.xy(buttonLeft, startTop + index * (BUTTON_HEIGHT + BUTTON_GAP))
                    renderButton(button, action)
                }
            }
        }
    }

    fun footerLayout(boundsWidth: Double, buttonCount: Int): MapViewerDialogFooterLayout =
        if (buttonCount <= 1 || boundsWidth >= rowMinimumWidth(buttonCount)) {
            MapViewerDialogFooterLayout.ROW
        } else {
            MapViewerDialogFooterLayout.COLUMN
        }

    fun footerHeight(buttonCount: Int, layout: MapViewerDialogFooterLayout): Double =
        if (buttonCount <= 0) {
            0.0
        } else {
            MapViewerDialogLayoutBuilder.FOOTER_TOP_GAP + MapViewerDialogLayoutBuilder.FOOTER_BOTTOM_PADDING + when (layout) {
                MapViewerDialogFooterLayout.ROW -> BUTTON_HEIGHT
                MapViewerDialogFooterLayout.COLUMN -> stackedHeight(buttonCount)
            }
        }

    fun minimumWidth(buttonCount: Int): Double =
        if (buttonCount <= 0) {
            0.0
        } else {
            BUTTON_WIDTH
        }

    fun centerRatios(buttonCount: Int): List<Double> =
        if (buttonCount <= 0) {
            emptyList()
        } else {
            (0 until buttonCount).map { index -> (index + 1).toDouble() / (buttonCount + 1).toDouble() }
        }

    private fun rowMinimumWidth(buttonCount: Int): Double =
        if (buttonCount <= 0) {
            0.0
        } else {
            (buttonCount + 1).toDouble() * BUTTON_WIDTH
        }

    private fun stackedHeight(buttonCount: Int): Double =
        if (buttonCount <= 0) {
            0.0
        } else {
            (buttonCount * BUTTON_HEIGHT) + ((buttonCount - 1) * BUTTON_GAP)
        }

    private fun renderButton(parent: Container, action: MapViewerDialogAction) {
        MapViewerActionButtons.button(
            parent = parent,
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            label = action.label,
            enabled = action.enabled(),
            onClick = action.onClick,
        )
    }
}
