package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class GridLinePlacementBuilderTest {

    @Test
    fun `integer grid starts place first lines at map origin`() {
        val placement = GridLinePlacementBuilder.build(
            viewport = Rectangle(Point(0.0, 0.0), Point(300.0, 300.0)),
            layer = layer(verticalLineNumberStart = 5.0, horizontalLineNumberStart = 45.0),
        )

        assertThat(placement.verticalLines.first().id, equalTo(5.0))
        assertThat(placement.verticalLines.first().mapPosition, closeTo(0.0, 0.0001))
        assertThat(placement.horizontalLines.first().id, equalTo(45.0))
        assertThat(placement.horizontalLines.first().mapPosition, closeTo(0.0, 0.0001))
    }

    @Test
    fun `fractional grid starts place next whole grid line from map origin`() {
        val placement = GridLinePlacementBuilder.build(
            viewport = Rectangle(Point(0.0, 0.0), Point(300.0, 300.0)),
            layer = layer(verticalLineNumberStart = 5.5, horizontalLineNumberStart = 45.25),
        )

        assertThat(placement.verticalLines.first().id, equalTo(6.0))
        assertThat(placement.verticalLines.first().mapPosition, closeTo(50.0, 0.0001))
        assertThat(placement.horizontalLines.first().id, equalTo(45.0))
        assertThat(placement.horizontalLines.first().mapPosition, closeTo(25.0, 0.0001))
    }

    @Test
    fun `viewport filters map-local line positions without using render gutters`() {
        val placement = GridLinePlacementBuilder.build(
            viewport = Rectangle(Point(120.0, 220.0), Point(360.0, 460.0)),
            layer = layer(),
        )

        assertThat(placement.verticalLines.map { it.id }, equalTo(listOf(7.0, 8.0)))
        assertThat(placement.verticalLines.map { it.mapPosition }, equalTo(listOf(200.0, 300.0)))
        assertThat(placement.horizontalLines.map { it.id }, equalTo(listOf(42.0, 41.0)))
        assertThat(placement.horizontalLines.map { it.mapPosition }, equalTo(listOf(300.0, 400.0)))
    }

    private fun layer(
        verticalLineNumberStart: Double = 5.0,
        horizontalLineNumberStart: Double = 45.0,
    ) = GridLayer(
        mapId = "25F XK",
        gridScale = 1000,
        metresPerPixel = 10.0,
        detailLevel = 2,
        pixelsPerLine = 100.0,
        verticalLineNumberStart = MgrsCoord.fromLineNumber(verticalLineNumberStart, 1000),
        horizontalLineNumberStart = MgrsCoord.fromLineNumber(horizontalLineNumberStart, 1000),
        verticalLineCount = 20,
        horizontalLineCount = 20,
    )
}
