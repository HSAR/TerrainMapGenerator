package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds

internal data class MapViewerEscapeMenuLayout(
    val panelBounds: ScreenBounds,
    val titleX: Double,
    val titleY: Double,
    val subtitleX: Double,
    val subtitleY: Double,
    val returnButtonBounds: ScreenBounds,
    val closeButtonBounds: ScreenBounds,
)

internal object MapViewerEscapeMenuLayoutBuilder {
    fun build(viewportWidth: Int, viewportHeight: Int): MapViewerEscapeMenuLayout {
        val panelWidth = 340.0
        val panelHeight = 184.0
        val panelBounds = ScreenBounds(
            left = (viewportWidth - panelWidth) * 0.5,
            top = (viewportHeight - panelHeight) * 0.5,
            width = panelWidth,
            height = panelHeight,
        )
        val buttonHeight = 38.0
        val buttonGap = 10.0
        val buttonWidth = maxOf(
            buttonLabels().maxOf { labelWidth(it) } + 36.0,
            120.0,
        ).coerceAtMost(panelWidth - 48.0)
        val buttonLeft = (panelWidth - buttonWidth) * 0.5
        val returnButtonTop = 98.0
        val closeButtonTop = returnButtonTop + buttonHeight + buttonGap
        return MapViewerEscapeMenuLayout(
            panelBounds = panelBounds,
            titleX = panelWidth * 0.5,
            titleY = 24.0,
            subtitleX = panelWidth * 0.5,
            subtitleY = 58.0,
            returnButtonBounds = ScreenBounds(
                left = buttonLeft,
                top = returnButtonTop,
                width = buttonWidth,
                height = buttonHeight,
            ),
            closeButtonBounds = ScreenBounds(
                left = buttonLeft,
                top = closeButtonTop,
                width = buttonWidth,
                height = buttonHeight,
            ),
        )
    }

    private fun buttonLabels(): List<String> =
        listOf("Return To Launchpad", "Close")

    private fun labelWidth(text: String): Double =
        MapViewerTextViews.bounds(text).width
}
