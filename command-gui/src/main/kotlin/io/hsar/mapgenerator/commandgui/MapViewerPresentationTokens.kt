package io.hsar.mapgenerator.commandgui

/** Renderer-independent measurements shared by the KorGE and Compose notice hosts. */
object MapViewerTransientNoticeTokens {
    const val VIEWPORT_INSET = 16.0
    const val HORIZONTAL_PADDING = 12.0
    const val VERTICAL_PADDING = 8.0
    const val STACK_GAP = 8.0
    const val FONT_SIZE = 24.0
    const val BACKGROUND_ALPHA = 180
    const val ENTRY_OFFSET = 12.0
    const val FADE_LIFT = 12.0
}

/** Renderer-independent non-modal map-dialog chrome inherited from KorGE. */
object MapViewerDialogChromeTokens {
    const val BORDER_WIDTH = 2.0
    const val TITLE_BAR_HEIGHT = 38.0
    const val CONTROL_SIZE = TITLE_BAR_HEIGHT + BORDER_WIDTH * 2.0
    const val TITLE_TEXT_LEFT = 14.0
    const val TITLE_TEXT_RIGHT_GAP = 14.0
    const val CONTENT_PADDING = 18.0
    const val PANEL_ALPHA = 220
    const val TITLE_BAR_ALPHA = 235
    const val ACTIVE_CONTROL_FILL_ALPHA = 72
    const val ICON_STROKE_WIDTH = 2.0
    const val RESIZE_HANDLE_SIZE = 24.0
    const val RESIZE_HANDLE_OUTSET = 4.0
    const val RESIZE_BRACKET_LENGTH = 18.0
    const val RESIZE_BRACKET_THICKNESS = 5.0
}

/** The legacy launchpad's logical-pixel geometry. */
object MapViewerLaunchpadTokens {
    const val HEADER_TOP = 20.0
    const val HEADER_WIDTH = 540.0
    const val HEADER_HEIGHT = 78.0
    const val HEADER_FONT_SIZE = 34.0
    const val PANEL_WIDTH = 420.0
    const val PANEL_HEIGHT = 320.0
    const val PANEL_TITLE_TOP = 10.0
    const val PANEL_TITLE_HEIGHT = 56.0
    const val WELCOME_TOP = 62.0
    const val WELCOME_HEIGHT = 28.0
    const val BODY_FONT_SIZE = 24.0
    const val BUTTON_LEFT = 60.0
    const val BUTTON_WIDTH = 300.0
    const val BUTTON_HEIGHT = 44.0
    const val FIRST_BUTTON_TOP = 126.0
    const val BUTTON_STEP = 62.0
    const val PANEL_ALPHA = 220
    const val BACKGROUND_WASH_ALPHA = 72
}

/** The legacy ongoing-operations browser's logical-pixel geometry. */
object MapViewerOngoingOperationsTokens {
    const val PANEL_WIDTH = 760.0
    const val PANEL_HEIGHT_FRACTION = 0.78
    const val PANEL_MIN_HEIGHT = 520.0
    const val BACK_LEFT = 20.0
    const val BACK_TOP = 16.0
    const val BACK_WIDTH = 120.0
    const val BACK_HEIGHT = 42.0
    const val LIST_LEFT = 24.0
    const val LIST_TOP = 110.0
    const val LIST_BOTTOM_PADDING = 112.0
    const val LIST_CONTENT_INSET = 12.0
    const val SCROLLBAR_GAP = 12.0
    const val SCROLLBAR_WIDTH = 18.0
    const val SCROLLBAR_INSET = 2.0
    const val SCROLLBAR_MIN_THUMB_HEIGHT = 28.0
    const val ROW_STEP = 108.0
    const val ROW_HEIGHT = 96.0
    const val PREVIEW_INSET = 4.0
    const val PREVIEW_ASPECT_RATIO = 16.0 / 9.0
    const val TEXT_GAP = 18.0
    const val NAME_TOP = 12.0
    const val TIMESTAMP_TOP = 44.0
    const val TEXT_HEIGHT = 28.0
    const val BROWSE_WIDTH = 280.0
    const val BROWSE_BOTTOM = 24.0
}
