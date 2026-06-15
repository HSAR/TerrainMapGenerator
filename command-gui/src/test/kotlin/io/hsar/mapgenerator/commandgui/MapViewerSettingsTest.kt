package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerSettingsTest {

    @Test
    fun `default settings build a useful window title`() {
        val settings = MapViewerSettings()

        assertThat(settings.windowTitle(), equalTo("Terrain Map Viewer - Graph - 1920x1080px"))
    }

    @Test
    fun `custom settings include the selected layer and size`() {
        val settings = MapViewerSettings(
            layer = MapLayer.GRID,
            width = 800,
            height = 600,
        )

        assertThat(settings.windowTitle(), equalTo("Terrain Map Viewer - Grid - 800x600px"))
    }
}
