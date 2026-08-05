package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerMeasurementTest {
    @Test
    fun `cardinal drags use grid north clockwise`() {
        assertThat(measure(100.0, 100.0, 140.0, 100.0).bearingDegrees, equalTo(90))
        assertThat(measure(100.0, 100.0, 100.0, 60.0).bearingDegrees, equalTo(0))
        assertThat(measure(100.0, 100.0, 100.0, 130.0).bearingDegrees, equalTo(180))
        assertThat(measure(100.0, 100.0, 60.0, 100.0).bearingDegrees, equalTo(270))
    }

    @Test
    fun `distance retains precision and formats compactly`() {
        val value = measure(0.0, 0.0, 3.0, 4.0, 20.0)
        assertThat(value.distanceUnits, equalTo(100.0))
        assertThat(value.distanceText, equalTo("100m"))
        assertThat(value.bearingText, equalTo("143°"))
        assertThat(MapViewerMeasurement(MapCoordinate(0.0, 0.0), MapCoordinate(0.0, 0.0), 12.34567, "m", 0).distanceText, equalTo("12.4m"))
    }

    @Test
    fun `epsilon distance keeps sub unit precision`() {
        val layer = layer(0.05, ReferenceSystem.EPSILON)
        val value = measureMapDrag(layer, MapCoordinate(0.0, 0.0), MapCoordinate(0.003, 0.004), 1000, 1000)
        assertThat(value.distanceUnits, closeTo(0.25, 0.0001))
        assertThat(value.distanceText, equalTo("0.3U"))
    }

    private fun measure(x1: Double, y1: Double, x2: Double, y2: Double, units: Double = 10.0) =
        measureMapDrag(layer(units), MapCoordinate(x1 / 1000.0, y1 / 1000.0), MapCoordinate(x2 / 1000.0, y2 / 1000.0), 1000, 1000)

    private fun layer(units: Double, system: ReferenceSystem = ReferenceSystem.MGRS) = GridLayer(
        mapId = "GRID", referenceSystem = system, gridScale = system.gridScale, unitsPerPixel = units,
        detailLevel = 2, pixelsPerLine = 50.0,
        verticalLineNumberStart = system.coordFromUnits(0.0), horizontalLineNumberStart = system.coordFromUnits(0.0),
        verticalLineCount = 10, horizontalLineCount = 10,
    )
}
