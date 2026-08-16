package io.hsar.mapgenerator.commandgui

object MapViewerMenuTokens {
    const val ROW_HEIGHT = 44.0
    const val PADDING = 4.0
    const val MIN_WIDTH = 180.0
    const val BORDER_WIDTH = 2.0
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
