package io.hsar.mapgenerator.commandgui.korge.ui

internal object MapViewerDialogSpacing {
    const val LABEL_TO_FIELD_GAP = 28.0
    const val SECTION_GAP = 28.0
    const val FIELD_GAP = 18.0
    const val INPUT_HEIGHT = 36.0
    const val PREVIEW_HEIGHT = 50.0
    const val ACTION_BUTTON_HEIGHT = 40.0
    const val MIN_FIELD_WIDTH = 180.0
    const val FORM_BOTTOM_PADDING = 24.0
    const val PREVIEW_BOTTOM_GAP = 8.0
    const val ROW_HEIGHT = 68.0

    fun inputTop(labelTop: Double): Double =
        labelTop + LABEL_TO_FIELD_GAP

    fun sectionTop(previousBottom: Double): Double =
        previousBottom + SECTION_GAP

    fun fieldWidth(contentWidth: Double, preferredWidth: Double, minimumWidth: Double = MIN_FIELD_WIDTH): Double =
        preferredWidth.coerceAtMost(contentWidth).coerceAtLeast(minimumWidth.coerceAtMost(contentWidth))
}
