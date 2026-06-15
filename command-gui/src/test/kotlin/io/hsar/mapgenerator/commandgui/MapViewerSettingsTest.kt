package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerSettingsTest {

    @Test
    fun `default settings enable all layers`() {
        val settings = MapViewerSettings()

        assertThat(settings.enabledLayers, equalTo(MapLayer.values().toSet()))
    }

    @Test
    fun `custom settings can enable a subset of layers`() {
        val settings = MapViewerSettings(
            enabledLayers = setOf(MapLayer.GRID, MapLayer.CONTOUR),
        )

        assertThat(settings.enabledLayers, equalTo(setOf(MapLayer.GRID, MapLayer.CONTOUR)))
    }

    @Test
    fun `zoom scales within the configured bounds`() {
        val settings = MapViewerSettings(zoom = 1.0)

        assertThat(settings.withZoom(10.0).zoom, equalTo(MapViewerSettings.MAX_ZOOM))
        assertThat(settings.withZoom(0.01).zoom, equalTo(MapViewerSettings.MIN_ZOOM))
    }

    @Test
    fun `window title includes dimensions and zoom`() {
        val settings = MapViewerSettings(
            width = 800,
            height = 600,
            zoom = 1.5,
        )

        assertThat(settings.windowTitle(), equalTo("Terrain Map Viewer - 800x600px - 150%"))
    }
}
