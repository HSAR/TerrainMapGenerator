package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerSettingsTest {

    @Test
    fun `default settings enable all layers`() {
        val settings = MapViewerSettings()

        assertThat(settings.availableLayers, equalTo(MapLayer.values().toList()))
        assertThat(settings.enabledLayerIds, equalTo(MapLayer.values().map { it.id }.toSet()))
    }

    @Test
    fun `default settings use twenty metres per pixel`() {
        val settings = MapViewerSettings()

        assertThat(settings.metresPerPixel, equalTo(20.0))
    }

    @Test
    fun `custom settings can enable a subset of layers`() {
        val settings = MapViewerSettings(
            enabledLayerIds = setOf(MapLayer.GRID.id, MapLayer.CONTOUR.id),
        )

        assertThat(settings.enabledLayerIds, equalTo(setOf(MapLayer.GRID.id, MapLayer.CONTOUR.id)))
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

        assertThat(settings.windowTitle(), equalTo("Operational Command Tool - 800x600px - 150%"))
    }

    @Test
    fun `window title can be formatted for an explicit zoom`() {
        val settings = MapViewerSettings(
            width = 800,
            height = 600,
            zoom = 1.0,
        )

        assertThat(settings.windowTitle(2.0), equalTo("Operational Command Tool - 800x600px - 200%"))
    }

    @Test
    fun `window title includes operation name when present`() {
        val settings = MapViewerSettings(
            operationName = "Operation Copperhead",
            width = 800,
            height = 600,
        )

        assertThat(settings.windowTitle(), equalTo("Operation Copperhead - Operational Command Tool - 800x600px - 100%"))
    }
}
