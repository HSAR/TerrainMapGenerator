package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import org.hamcrest.MatcherAssert.assertThat
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
    fun `distance rounds to the nearest metre and formats in metres`() {
        val measurement = measurement(
            originMapX = 0.0,
            originMapY = 0.0,
            destinationMapX = 3.0,
            destinationMapY = 4.0,
            metresPerPixel = 20.0,
        )

        assertThat(measurement.distanceMetres, equalTo(100))
        assertThat(measurement.distanceText, equalTo("100m"))
        assertThat(measurement.bearingText, equalTo("143\u00b0"))
    }

    private fun measurement(
        originMapX: Double,
        originMapY: Double,
        destinationMapX: Double,
        destinationMapY: Double,
        metresPerPixel: Double = 10.0,
    ) = GridMeasurementCalculator.calculate(
        layer = GridLayer(
            mapId = "25F XK",
            gridScale = 1000,
            metresPerPixel = metresPerPixel,
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
}
