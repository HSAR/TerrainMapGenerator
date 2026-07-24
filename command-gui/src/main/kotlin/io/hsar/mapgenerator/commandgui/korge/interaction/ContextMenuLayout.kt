package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerCollapsiblePanel

internal object ContextMenuLayout {
    const val ACTION_HEIGHT = 44.0
    const val PADDING = 4.0
    const val OPTION_VERTICAL_INSET = 2.0
    const val MIN_MENU_WIDTH = 180.0
    const val LABEL_LEFT_PADDING = 12.0
    const val SUBMENU_ARROW_WIDTH = MapViewerCollapsiblePanel.CHEVRON_WIDTH
    const val DISABLED_ICON_WIDTH = 24.0
    const val BORDER_WIDTH = 2.0
    const val SUBMENU_HOVER_PADDING = 8.0
    const val SUBMENU_CLOSE_DELAY_MS = 180L

    fun menuHeight(actionCount: Int): Double =
        (actionCount * ACTION_HEIGHT) + PADDING * 2

    fun optionTop(index: Int): Double =
        PADDING + (index * ACTION_HEIGHT)
}
