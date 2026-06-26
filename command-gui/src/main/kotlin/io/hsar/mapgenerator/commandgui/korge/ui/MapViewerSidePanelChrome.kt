package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.xy

internal object MapViewerSidePanelChrome {
    fun renderToggle(
        parent: Container,
        bounds: ScreenBounds,
        label: String,
    ) {
        parent.removeChildren()
        parent.xy(bounds.left, bounds.top)
        val button = MapViewerUiBoxes.button(parent, bounds.width, bounds.height)
        MapViewerTextViews.box(button, label, bounds.width, bounds.height, color = Palette.Colours.WHITE)
    }

    fun renderPanelFrame(
        parent: Container,
        bounds: ScreenBounds,
    ) {
        parent.xy(bounds.left, bounds.top)
        parent.removeChildren()
        MapViewerUiBoxes.box(parent, bounds.width, bounds.height)
    }
}
