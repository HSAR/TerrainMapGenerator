package io.hsar.mapgenerator.commandgui
import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.EpsilonGridDesignators
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class GridReferenceFormatterTest {

    @Test
    fun `formats an eight digit grid reference from map coordinates`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(reference, equalTo("0840 3820"))
    }

    @Test
    fun `formats a mixed sign grid reference when one axis is negative`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(verticalLineNumberStart = -1.0, horizontalLineNumberStart = 2.0),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(reference, equalTo("0240 -0480"))
    }

    @Test
    fun `formatting ignores viewport gutters because coordinates are map local`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
            gutterWidth = 1200,
            gutterHeight = 900,
        )

        assertThat(reference, equalTo("0840 3820"))
    }

    @Test
    fun `formats a fully negative grid reference when both axes are negative`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(verticalLineNumberStart = -1.0, horizontalLineNumberStart = -2.0),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(reference, equalTo("0240 -0880"))
    }

    @Test
    fun `formats an epsilon reference with square designator and thousandth offsets`() {
        val reference = GridReferenceFormatter.format(
            layer = epsilonLayer(),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 15.0,
            mapY = 30.0,
        )

        assertThat(reference, equalTo("CD4 300-400"))
    }

    @Test
    fun `epsilon thousandth offsets are truncated rather than rounded`() {
        val reference = GridReferenceFormatter.format(
            layer = epsilonLayer(),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 15.49,
            mapY = 29.51,
        )

        assertThat(reference, equalTo("CD4 309-409"))
    }

    @Test
    fun `epsilon y designators advance through single and double letters`() {
        assertThat(EpsilonGridDesignators.yDesignator(0), equalTo("A"))
        assertThat(EpsilonGridDesignators.yDesignator(25), equalTo("Z"))
        assertThat(EpsilonGridDesignators.yDesignator(26), equalTo("AA"))
        assertThat(EpsilonGridDesignators.yDesignator(27), equalTo("AB"))
        assertThat(EpsilonGridDesignators.yDesignator(701), equalTo("ZZ"))
    }

    @Test
    fun `epsilon x designators are one to one hundred`() {
        assertThat(EpsilonGridDesignators.xDesignator(0), equalTo("1"))
        assertThat(EpsilonGridDesignators.xDesignator(99), equalTo("100"))
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
