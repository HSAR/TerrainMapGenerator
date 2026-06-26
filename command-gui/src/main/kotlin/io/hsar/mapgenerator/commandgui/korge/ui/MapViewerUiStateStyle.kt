package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import java.awt.Color

internal object MapViewerUiStateStyle {
    const val ACTIVE_ALPHA: Int = 255

    val inactiveFillAlpha: Int = INACTIVE_ALPHA

    fun backing(active: Boolean): RGBA =
        toRgba(if (active) Palette.Colours.DARK else Palette.Colours.MAIN, if (active) ACTIVE_ALPHA else INACTIVE_ALPHA)

    fun backing(activeColor: Color, active: Boolean): RGBA =
        toRgba(activeColor, if (active) ACTIVE_ALPHA else INACTIVE_ALPHA)

    fun text(active: Boolean): Color =
        if (active) Palette.Colours.WHITE else Palette.Colours.WHITE.withAlpha(INACTIVE_ALPHA)

    fun secondaryText(active: Boolean): Color =
        if (active) Palette.Colours.LIGHT else Palette.Colours.WHITE.withAlpha(INACTIVE_ALPHA)

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private fun Color.withAlpha(alpha: Int): Color =
        Color(red, green, blue, alpha)

    private const val INACTIVE_ALPHA = 204
}
