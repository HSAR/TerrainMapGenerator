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
    fun `default settings use twenty units per pixel`() {
        val settings = MapViewerSettings()

        assertThat(settings.unitsPerPixel, equalTo(20.0))
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
    fun `window title uses the launchpad title when no operation name is present`() {
        val settings = MapViewerSettings(
            width = 800,
            height = 600,
            zoom = 1.5,
        )

        assertThat(settings.windowTitle(), equalTo("Operational Command Tool - Launchpad"))
    }

    @Test
    fun `window title ignores explicit zoom values`() {
        val settings = MapViewerSettings(
            width = 800,
            height = 600,
            zoom = 1.0,
        )

        assertThat(settings.windowTitle(), equalTo("Operational Command Tool - Launchpad"))
    }

    @Test
    fun `window title includes operation name when present`() {
        val settings = MapViewerSettings(
            operationName = "Operation Copperhead",
            width = 800,
            height = 600,
        )

        assertThat(settings.windowTitle(), equalTo("Operational Command Tool - Operation Copperhead"))
    }
}
