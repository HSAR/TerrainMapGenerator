package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import java.awt.Color

internal object MapViewerTextInputValidation {
    fun isPresent(text: String): Boolean =
        text.trim().isNotBlank()

    fun isReservedKey(text: String, reservedKeys: Set<String>): Boolean =
        text.trim() in reservedKeys

    fun frameStyle(valid: Boolean): (Boolean) -> UiFrameStyle =
        if (valid) MapViewerUiBoxes::defaultStyle else ::errorFrameStyle

    fun errorFrameStyle(active: Boolean): UiFrameStyle =
        UiFrameStyle(
            fillColor = if (active) ERROR_FILL else Palette.Colours.MAIN,
            fillAlpha = if (active) 190 else MapViewerUiStateStyle.inactiveFillAlpha,
            borderColor = Palette.Colours.RED,
            borderAlpha = 235,
        )

    private val ERROR_FILL = Color(82, 16, 20)
}
