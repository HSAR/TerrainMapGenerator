package io.hsar.mapgenerator.commandgui

data class MapViewerChromeBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    val right get() = left + width
    val bottom get() = top + height

    fun intersects(other: MapViewerChromeBounds) =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class MapViewerChromeLayout(
    val game: MapViewerChromeBounds,
    val notes: MapViewerChromeBounds,
    val graphics: MapViewerChromeBounds,
    val units: MapViewerChromeBounds,
    val toolRail: MapViewerChromeBounds,
)

/** Pure screen-space layout shared by rendering and geometry tests. */
object MapViewerChromeLayoutBuilder {
    const val OUTER_PADDING = 16.0
    const val GAP = 12.0
    const val BUTTON_WIDTH = 120.0
    const val BUTTON_HEIGHT = 40.0
    const val DEFAULT_TOOL_RAIL_WIDTH = 384.0
    const val DEFAULT_TOOL_RAIL_HEIGHT = 40.0

    fun build(
        viewportWidth: Double,
        toolRailWidth: Double = DEFAULT_TOOL_RAIL_WIDTH,
        toolRailHeight: Double = DEFAULT_TOOL_RAIL_HEIGHT,
    ): MapViewerChromeLayout {
        val game = MapViewerChromeBounds(OUTER_PADDING, OUTER_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
        val notes = MapViewerChromeBounds(game.right + GAP, OUTER_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
        val units = MapViewerChromeBounds(viewportWidth - OUTER_PADDING - BUTTON_WIDTH, OUTER_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
        val graphics = MapViewerChromeBounds(units.left - GAP - BUTTON_WIDTH, OUTER_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
        val centredRail = MapViewerChromeBounds((viewportWidth - toolRailWidth) / 2.0, OUTER_PADDING, toolRailWidth, toolRailHeight)
        val buttonGroups = MapViewerChromeBounds(game.left, game.top, notes.right - game.left, BUTTON_HEIGHT) to
            MapViewerChromeBounds(graphics.left, graphics.top, units.right - graphics.left, BUTTON_HEIGHT)
        val rail = centredRail.copy(
            top = if (centredRail.intersects(buttonGroups.first) || centredRail.intersects(buttonGroups.second))
                OUTER_PADDING + BUTTON_HEIGHT + GAP
            else OUTER_PADDING,
        )
        return MapViewerChromeLayout(game, notes, graphics, units, rail)
    }
}
