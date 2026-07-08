package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import korlibs.korge.view.Container
import korlibs.korge.view.xy

internal data class LaunchpadTemplateLayout(
    val headerBounds: ScreenBounds,
    val screenHeaderBounds: ScreenBounds,
)

internal object MapViewerLaunchpadTemplate {
    fun renderHeader(parent: Container, headerBounds: ScreenBounds) {
        val header = MapViewerLaunchUi.panel(parent, headerBounds)
        MapViewerLaunchUi.title(
            parent = header,
            text = "Operational Command Tool",
            width = headerBounds.width,
            height = headerBounds.height,
            bold = true,
            size = 34.0,
            color = java.awt.Color.WHITE,
        )
    }

    fun renderScreenShell(
        parent: Container,
        bounds: ScreenBounds,
        title: String,
        onBack: (() -> Unit)? = null,
    ): Container {
        val header = MapViewerLaunchUi.panel(parent, bounds)
        MapViewerLaunchUi.title(
            parent = header,
            text = title,
            width = bounds.width,
            height = 56.0,
            size = 34.0,
            color = java.awt.Color.WHITE,
        ).xy(0.0, 10.0)
        onBack?.let {
            MapViewerLaunchUi.backButton(header) {
                it()
            }.xy(20.0, 16.0)
        }
        return header
    }
}
