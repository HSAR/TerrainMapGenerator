package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class GridMeasurementCalculatorTest {

    @Test
    fun `eastward drag gives 090 degrees`() {
        val measurement = measurement(
            originMapX = 100.0,
            originMapY = 100.0,
            destinationMapX = 140.0,
            destinationMapY = 100.0,
        )

        assertThat(measurement.bearingDegrees, equalTo(90))
    }

    @Test
    fun `northward drag gives 000 degrees`() {
        val measurement = measurement(
            originMapX = 100.0,
            originMapY = 100.0,
            destinationMapX = 100.0,
            destinationMapY = 60.0,
        )

        assertThat(measurement.bearingDegrees, equalTo(0))
    }

    @Test
    fun `southward drag gives 180 degrees`() {
        val measurement = measurement(
            originMapX = 100.0,
            originMapY = 100.0,
            destinationMapX = 100.0,
            destinationMapY = 130.0,
        )

        assertThat(measurement.bearingDegrees, equalTo(180))
    }

    @Test
    fun `westward drag gives 270 degrees`() {
        val measurement = measurement(
            originMapX = 100.0,
            originMapY = 100.0,
            destinationMapX = 60.0,
            destinationMapY = 100.0,
        )

        assertThat(measurement.bearingDegrees, equalTo(270))
    }

    @Test
    fun `distance keeps precise units and formats whole metres compactly`() {
        val measurement = measurement(
            originMapX = 0.0,
            originMapY = 0.0,
            destinationMapX = 3.0,
            destinationMapY = 4.0,
            unitsPerPixel = 20.0,
        )

        assertThat(measurement.distanceUnits, equalTo(100.0))
        assertThat(measurement.distanceText, equalTo("100m"))
        assertThat(measurement.bearingText, equalTo("143\u00b0"))
    }

    @Test
    fun `distance label uses four significant digits and one decimal place`() {
        assertThat(measurement(distanceUnits = 12345.67).distanceText, equalTo("12350m"))
        assertThat(measurement(distanceUnits = 12.34567).distanceText, equalTo("12.4m"))
        assertThat(measurement(distanceUnits = 0.1234567).distanceText, equalTo("0.1m"))
    }

    @Test
    fun `epsilon distance keeps sub unit precision`() {
        val measurement = GridMeasurementCalculator.calculate(
            layer = GridLayer(
                mapId = "EPSILON",
                referenceSystem = ReferenceSystem.EPSILON,
                gridScale = ReferenceSystem.EPSILON.gridScale,
                unitsPerPixel = 0.05,
                detailLevel = 2,
                pixelsPerLine = 400.0,
                verticalLineNumberStart = EpsilonCoord.fromUnits(0),
                horizontalLineNumberStart = EpsilonCoord.fromUnits(0),
                verticalLineCount = 10,
                horizontalLineCount = 10,
            ),
            originMapX = 0.0,
            originMapY = 0.0,
            destinationMapX = 3.0,
            destinationMapY = 4.0,
        )

        assertThat(measurement.distanceUnits, closeTo(0.25, 0.0001))
        assertThat(measurement.distanceText, equalTo("0.3U"))
    }

    private fun measurement(
        originMapX: Double,
        originMapY: Double,
        destinationMapX: Double,
        destinationMapY: Double,
        unitsPerPixel: Double = 10.0,
    ) = GridMeasurementCalculator.calculate(
        layer = GridLayer(
            mapId = "25F XK",
            gridScale = 1000,
            unitsPerPixel = unitsPerPixel,
            detailLevel = 2,
            pixelsPerLine = 50.0,
            verticalLineNumberStart = io.hsar.mapgenerator.grid.MgrsCoord.fromMetres(0),
            horizontalLineNumberStart = io.hsar.mapgenerator.grid.MgrsCoord.fromMetres(0),
            verticalLineCount = 10,
            horizontalLineCount = 10,
        ),
        originMapX = originMapX,
        originMapY = originMapY,
        destinationMapX = destinationMapX,
        destinationMapY = destinationMapY,
    )

    private fun measurement(distanceUnits: Double) =
        io.hsar.mapgenerator.commandgui.korge.overlay.GridMeasurement(
            originMapX = 0.0,
            originMapY = 0.0,
            destinationMapX = 0.0,
            destinationMapY = 0.0,
            distanceUnits = distanceUnits,
            unitLabel = "m",
            bearingDegrees = 0,
        )
}
