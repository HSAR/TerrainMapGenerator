package io.hsar.mapgenerator.commandgui.korge.ui

internal data class MapViewerNotesPanelLayout(
    val toggleBounds: ScreenBounds,
    val panelBounds: ScreenBounds?,
)

internal object MapViewerNotesPanelLayoutBuilder {
    const val TOGGLE_GAP = 12.0
    const val PANEL_WIDTH = 384.0
    const val PANEL_MIN_HEIGHT = MapViewerLedgerPanelLayoutBuilder.PANEL_MIN_HEIGHT

    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        collapsed: Boolean,
    ): MapViewerNotesPanelLayout {
        val gameToggle = MapViewerLedgerPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed = true).toggleBounds
        val toggleBounds = ScreenBounds(
            left = gameToggle.right + TOGGLE_GAP,
            top = gameToggle.top,
            width = gameToggle.width,
            height = gameToggle.height,
        )
        val panelTop = toggleBounds.bottom + MapViewerLedgerPanelLayoutBuilder.PANEL_TOP_GAP
        val panelHeight = (viewportHeight - panelTop - MapViewerLedgerPanelLayoutBuilder.OUTER_PADDING).coerceAtLeast(PANEL_MIN_HEIGHT)
        val panelBounds = if (collapsed) {
            null
        } else {
            ScreenBounds(
                left = toggleBounds.left,
                top = panelTop,
                width = PANEL_WIDTH,
                height = panelHeight,
            )
        }

        return MapViewerNotesPanelLayout(
            toggleBounds = toggleBounds,
            panelBounds = panelBounds,
        )
    }
}
