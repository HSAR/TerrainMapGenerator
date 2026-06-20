package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.xy

internal data class LaunchpadTemplateLayout(
    val headerBounds: ScreenBounds,
    val screenHeaderBounds: ScreenBounds,
)

internal object MapViewerLaunchpadTemplate {
    fun renderHeader(parent: Container, headerBounds: ScreenBounds) {
        val header = MapViewerLaunchUi.panel(parent, headerBounds)
        MapViewerTextViews.box(
            parent = header,
            text = "Operational Command Tool",
            width = headerBounds.width,
            height = headerBounds.height,
            role = MapViewerFontRole.STRONG,
            size = 34.0,
            color = java.awt.Color.WHITE,
            alignment = TextAlignment.MIDDLE_CENTER,
        )
    }

    fun renderScreenShell(
        parent: Container,
        bounds: ScreenBounds,
        title: String,
        onBack: (() -> Unit)? = null,
    ): Container {
        val header = MapViewerLaunchUi.panel(parent, bounds)
        MapViewerTextViews.box(
            parent = header,
            text = title,
            width = bounds.width,
            height = 56.0,
            role = MapViewerFontRole.STRONG,
            size = 34.0,
            color = java.awt.Color.WHITE,
            alignment = TextAlignment.TOP_CENTER,
        ).xy(0.0, 10.0)
        onBack?.let {
            MapViewerLaunchUi.backButton(header) {
                it()
            }.xy(20.0, 16.0)
        }
        return header
    }
}
