package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class GridReferenceFormatterFieldsTest {

    @Test
    fun `formats separate easting and northing fields from map coordinates`() {
        val fields = GridReferenceFormatter.formatFields(
            layer = layer(),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(fields.easting, equalTo("0840"))
        assertThat(fields.northing, equalTo("3820"))
    }

    @Test
    fun `parses separate easting and northing fields back to map pixels`() {
        val pixels = GridReferenceFormatter.parseFields(
            layer = layer(),
            mapWidth = 2560,
            mapHeight = 1440,
            easting = "0840",
            northing = "3820",
        )

        assertThat(pixels?.x, closeTo(170.0, 0.0001))
        assertThat(pixels?.y, closeTo(340.0, 0.0001))
    }

    @Test
    fun `parses separate fields with negative coordinates`() {
        val pixels = GridReferenceFormatter.parseFields(
            layer = layer(verticalLineNumberStart = -1.0, horizontalLineNumberStart = -2.0),
            mapWidth = 2560,
            mapHeight = 1440,
            easting = "0240",
            northing = "-0880",
        )

        assertThat(pixels?.x, closeTo(170.0, 0.0001))
        assertThat(pixels?.y, closeTo(340.0, 0.0001))
    }

    @Test
    fun `parses fields independently of viewport gutters`() {
        val pixels = GridReferenceFormatter.parseFields(
            layer = layer(),
            mapWidth = 2560,
            mapHeight = 1440,
            easting = "0840",
            northing = "3820",
            gutterWidth = 1200,
            gutterHeight = 900,
        )

        assertThat(pixels?.x, closeTo(170.0, 0.0001))
        assertThat(pixels?.y, closeTo(340.0, 0.0001))
    }

    @Test
    fun `epsilon reference fields parse back to map pixels`() {
        val pixels = GridReferenceFormatter.parseFields(
            layer = epsilonLayer(),
            mapWidth = 2560,
            mapHeight = 1440,
            easting = "CD4",
            northing = "30-40",
        )

        assertThat(pixels?.x, closeTo(15.0, 0.0001))
        assertThat(pixels?.y, closeTo(30.0, 0.0001))
    }

    @Test
    fun `epsilon reference fields reject invalid percentages`() {
        val pixels = GridReferenceFormatter.parseFields(
            layer = epsilonLayer(),
            mapWidth = 2560,
            mapHeight = 1440,
            easting = "CD4",
            northing = "100-40",
        )

        assertThat(pixels, equalTo(null))
    }

    @Test
    fun `map pixels round trip through normalized coordinates without offset drift`() {
        val space = MapCoordinateSpace(
            geometry = ViewerGeometry(
                backgroundWidth = 2000,
                backgroundHeight = 2000,
                mapOffsetX = 100,
                mapOffsetY = 150,
            ),
            mapWidth = 1000,
            mapHeight = 800,
        )

        val normalized = space.fromMapPixels(korlibs.math.geom.Vector2D(170.0, 340.0))
        val pixels = space.toMapPixels(normalized)

        assertThat(normalized.x, closeTo(0.17, 0.0001))
        assertThat(normalized.y, closeTo(0.425, 0.0001))
        assertThat(pixels.x, closeTo(170.0, 0.0001))
        assertThat(pixels.y, closeTo(340.0, 0.0001))
    }

    private fun layer(
        verticalLineNumberStart: Double = 5.0,
        horizontalLineNumberStart: Double = 45.0,
    ) = GridLayer(
        mapId = "25F XK",
        gridScale = 1000,
        unitsPerPixel = 20.0,
        detailLevel = 2,
        pixelsPerLine = 100.0,
        verticalLineNumberStart = MgrsCoord.fromLineNumber(verticalLineNumberStart, 1000),
        horizontalLineNumberStart = MgrsCoord.fromLineNumber(horizontalLineNumberStart, 1000),
        verticalLineCount = 20,
        horizontalLineCount = 20,
    )

    private fun epsilonLayer() = GridLayer(
        mapId = "EPSILON",
        referenceSystem = ReferenceSystem.EPSILON,
        gridScale = 1000,
        unitsPerPixel = 20.0,
        detailLevel = 2,
        pixelsPerLine = 50.0,
        verticalLineNumberStart = EpsilonCoord.fromUnits(3_000),
        horizontalLineNumberStart = EpsilonCoord.fromUnits(82_000),
        verticalLineCount = 20,
        horizontalLineCount = 20,
    )
}
