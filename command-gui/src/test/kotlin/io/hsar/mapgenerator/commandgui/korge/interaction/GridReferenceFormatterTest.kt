package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
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

        assertThat(reference, equalTo("0730 3900"))
    }

    @Test
    fun `formats a mixed sign grid reference when one axis is negative`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(verticalLineNumberStart = -1, horizontalLineNumberStart = 2),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(reference, equalTo("0130 -0400"))
    }

    @Test
    fun `formats a fully negative grid reference when both axes are negative`() {
        val reference = GridReferenceFormatter.format(
            layer = layer(verticalLineNumberStart = -1, horizontalLineNumberStart = -2),
            mapWidth = 2560,
            mapHeight = 1440,
            mapX = 170.0,
            mapY = 340.0,
        )

        assertThat(reference, equalTo("0130 -0800"))
    }

    private fun layer(
        verticalLineNumberStart: Int = 5,
        horizontalLineNumberStart: Int = 45,
    ) = GridLayer(
        mapId = "25F XK",
        gridScale = 1000,
        metresPerPixel = 20.0,
        detailLevel = 2,
        pixelsPerLine = 100.0,
        verticalLineNumberStart = verticalLineNumberStart,
        horizontalLineNumberStart = horizontalLineNumberStart,
        verticalLineCount = 20,
        horizontalLineCount = 20,
    )
}
