package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D

internal data class MapViewerLedgerPanelLayout(
    val toggleBounds: ScreenBounds,
    val panelBounds: ScreenBounds?,
) {
    fun blocks(point: Vector2D): Boolean =
        toggleBounds.contains(point) || panelBounds?.contains(point) == true
}

internal object MapViewerLedgerPanelLayoutBuilder {
    const val OUTER_PADDING = MapViewerSidebarLayoutBuilder.OUTER_PADDING
    const val TOGGLE_WIDTH = MapViewerSidebarLayoutBuilder.TOGGLE_WIDTH
    const val TOGGLE_HEIGHT = MapViewerSidebarLayoutBuilder.TOGGLE_HEIGHT
    const val PANEL_WIDTH = 384.0
    const val PANEL_TOP_GAP = MapViewerSidebarLayoutBuilder.PANEL_TOP_GAP
    const val PANEL_MIN_HEIGHT = MapViewerSidebarLayoutBuilder.PANEL_MIN_HEIGHT

    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        collapsed: Boolean,
    ): MapViewerLedgerPanelLayout {
        val toggleBounds = ScreenBounds(
            left = OUTER_PADDING,
            top = OUTER_PADDING,
            width = TOGGLE_WIDTH,
            height = TOGGLE_HEIGHT,
        )
        val panelTop = toggleBounds.bottom + PANEL_TOP_GAP
        val panelHeight = (viewportHeight - panelTop - OUTER_PADDING).coerceAtLeast(PANEL_MIN_HEIGHT)
        val panelBounds = if (collapsed) {
            null
        } else {
            ScreenBounds(
                left = OUTER_PADDING,
                top = panelTop,
                width = PANEL_WIDTH,
                height = panelHeight,
            )
        }

        return MapViewerLedgerPanelLayout(
            toggleBounds = toggleBounds,
            panelBounds = panelBounds,
        )
    }
}
