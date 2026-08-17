package io.hsar.mapgenerator.commandgui

object MapViewerMenuTokens {
    const val ROW_HEIGHT = 44.0
    const val PADDING = 4.0
    const val OPTION_VERTICAL_INSET = 2.0
    const val LABEL_LEFT_PADDING = 12.0
    const val MIN_WIDTH = 180.0
    const val BORDER_WIDTH = 2.0
    const val SUBMENU_ARROW_WIDTH = 36.0
    const val DISABLED_ICON_WIDTH = 24.0
    const val DISABLED_ICON_SIZE = 20.0
    const val TOOLTIP_GAP = 8.0
    const val TOOLTIP_HEIGHT = 34.0
    const val TOOLTIP_MIN_WIDTH = 260.0
    const val TOOLTIP_HORIZONTAL_PADDING = 10.0
    const val SUBMENU_CORRIDOR = 8.0
    const val CLOSE_DELAY_MILLIS = 180L
}

object MapViewerMenuLayoutBuilder {
    fun root(
        requestedLeft: Double,
        requestedTop: Double,
        width: Double,
        height: Double,
        viewportWidth: Double,
        viewportHeight: Double,
    ) = MapViewerChromeBounds(
        requestedLeft.coerceIn(0.0, (viewportWidth - width).coerceAtLeast(0.0)),
        requestedTop.coerceIn(0.0, (viewportHeight - height).coerceAtLeast(0.0)),
        width.coerceAtMost(viewportWidth),
        height.coerceAtMost(viewportHeight),
    )

    fun child(parent: MapViewerChromeBounds, rowIndex: Int, width: Double, height: Double, viewportWidth: Double, viewportHeight: Double): MapViewerChromeBounds {
        val preferredRight = parent.right + MapViewerMenuTokens.SUBMENU_CORRIDOR
        val left = if (preferredRight + width <= viewportWidth) preferredRight
        else (parent.left - MapViewerMenuTokens.SUBMENU_CORRIDOR - width).coerceAtLeast(0.0)
        return root(left, parent.top + MapViewerMenuTokens.PADDING + rowIndex * MapViewerMenuTokens.ROW_HEIGHT, width, height, viewportWidth, viewportHeight)
    }
}
