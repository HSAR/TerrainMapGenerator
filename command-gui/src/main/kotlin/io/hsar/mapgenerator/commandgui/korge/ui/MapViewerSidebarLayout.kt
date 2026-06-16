package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D

internal data class ScreenBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    val right: Double
        get() = left + width

    val bottom: Double
        get() = top + height

    fun contains(point: Vector2D): Boolean =
        point.x in left..right && point.y in top..bottom
}

internal data class MapViewerSidebarLayout(
    val toggleBounds: ScreenBounds,
    val panelBounds: ScreenBounds?,
) {
    fun blocks(point: Vector2D): Boolean =
        toggleBounds.contains(point) || panelBounds?.contains(point) == true
}

internal object MapViewerSidebarLayoutBuilder {
    const val OUTER_PADDING = 16.0
    const val TOGGLE_WIDTH = 120.0
    const val TOGGLE_HEIGHT = 40.0
    const val PANEL_WIDTH = 320.0
    const val PANEL_TOP_GAP = 12.0
    const val PANEL_MIN_HEIGHT = 120.0

    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        collapsed: Boolean,
    ): MapViewerSidebarLayout {
        val toggleBounds = ScreenBounds(
            left = viewportWidth - OUTER_PADDING - TOGGLE_WIDTH,
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
                left = viewportWidth - OUTER_PADDING - PANEL_WIDTH,
                top = panelTop,
                width = PANEL_WIDTH,
                height = panelHeight,
            )
        }

        return MapViewerSidebarLayout(
            toggleBounds = toggleBounds,
            panelBounds = panelBounds,
        )
    }
}
