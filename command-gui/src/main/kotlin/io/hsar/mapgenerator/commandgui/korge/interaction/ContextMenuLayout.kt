package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerMenuTokens

internal object ContextMenuLayout {
    const val ACTION_HEIGHT = MapViewerMenuTokens.ROW_HEIGHT
    const val PADDING = MapViewerMenuTokens.PADDING
    const val OPTION_VERTICAL_INSET = MapViewerMenuTokens.OPTION_VERTICAL_INSET
    const val MIN_MENU_WIDTH = MapViewerMenuTokens.MIN_WIDTH
    const val LABEL_LEFT_PADDING = MapViewerMenuTokens.LABEL_LEFT_PADDING
    const val SUBMENU_ARROW_WIDTH = MapViewerMenuTokens.SUBMENU_ARROW_WIDTH
    const val DISABLED_ICON_WIDTH = MapViewerMenuTokens.DISABLED_ICON_WIDTH
    const val BORDER_WIDTH = MapViewerMenuTokens.BORDER_WIDTH
    const val SUBMENU_HOVER_PADDING = MapViewerMenuTokens.SUBMENU_CORRIDOR
    const val SUBMENU_CLOSE_DELAY_MS = MapViewerMenuTokens.CLOSE_DELAY_MILLIS

    fun menuHeight(actionCount: Int): Double =
        (actionCount * ACTION_HEIGHT) + PADDING * 2

    fun optionTop(index: Int): Double =
        PADDING + (index * ACTION_HEIGHT)
}
