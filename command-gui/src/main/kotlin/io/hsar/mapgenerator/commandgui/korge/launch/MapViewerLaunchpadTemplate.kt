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
        val title = MapViewerLaunchUi.label(header, "Operational Command Tool", strong = true, scale = HEADER_TITLE_SCALE)
        title.xy(
            (headerBounds.width - (title.width * HEADER_TITLE_SCALE)) * 0.5,
            (headerBounds.height - (title.height * HEADER_TITLE_SCALE)) * 0.5,
        )
    }

    fun renderScreenHeader(
        parent: Container,
        bounds: ScreenBounds,
        title: String,
        onBack: () -> Unit,
    ) {
        val header = MapViewerLaunchUi.panel(parent, bounds)
        val screenTitle = MapViewerLaunchUi.label(header, title, strong = true, scale = SCREEN_TITLE_SCALE)
        screenTitle.xy(
            (bounds.width - (screenTitle.width * SCREEN_TITLE_SCALE)) * 0.5,
            (bounds.height - (screenTitle.height * SCREEN_TITLE_SCALE)) * 0.5,
        )
        MapViewerLaunchUi.button(header, 120.0, 42.0, "Back") {
            onBack()
        }.xy(20.0, (bounds.height - 42.0) * 0.5)
    }

    private const val HEADER_TITLE_SCALE = 1.9
    private const val SCREEN_TITLE_SCALE = 1.55
}
