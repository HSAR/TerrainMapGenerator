package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
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
        MapViewerTextViews.box(
            parent = header,
            text = "Operational Command Tool",
            width = headerBounds.width,
            height = headerBounds.height,
            role = MapViewerFontRole.STRONG,
            size = 20.0 * HEADER_TITLE_SCALE,
        )
    }

    fun renderScreenHeader(
        parent: Container,
        bounds: ScreenBounds,
        title: String,
        onBack: () -> Unit,
    ) {
        val header = MapViewerLaunchUi.panel(parent, bounds)
        MapViewerTextViews.box(
            parent = header,
            text = title,
            width = bounds.width,
            height = bounds.height,
            role = MapViewerFontRole.STRONG,
            size = 20.0 * SCREEN_TITLE_SCALE,
        )
        MapViewerLaunchUi.button(header, 120.0, 42.0, "Back") {
            onBack()
        }.xy(20.0, (bounds.height - 42.0) * 0.5)
    }

    private const val HEADER_TITLE_SCALE = 1.9
    private const val SCREEN_TITLE_SCALE = 1.55
}
