package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.solidRect
import java.awt.Color

internal object MapViewerSwatchViews {
    fun render(parent: Container, color: Color?, size: Double = DEFAULT_SIZE) {
        parent.removeChildren()
        MapViewerUiBoxes.box(parent, size, size)
        parent.solidRect(size - FILL_INSET * 2.0, size - FILL_INSET * 2.0, MapViewerUiBoxes.toRgba(color ?: Palette.Colours.MAIN)) {
            x = FILL_INSET
            y = FILL_INSET
        }
    }

    private const val DEFAULT_SIZE = 12.0
    private const val FILL_INSET = 2.0
}
