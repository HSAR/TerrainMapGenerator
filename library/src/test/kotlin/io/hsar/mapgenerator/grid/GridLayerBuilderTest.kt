package io.hsar.mapgenerator.grid

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class GridLayerBuilderTest {

    @Test
    fun `grid layer layout is stable for the same seed`() {
        val seed = 42L

        val first = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 100,
            metresPerPixel = 2.0,
            random = Random(seed),
        )
        val second = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 100,
            metresPerPixel = 2.0,
            random = Random(seed),
        )

        assertThat(first, equalTo(second))
    }

    @Test
    fun `generated line starts stay within the representable range`() {
        val layer = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 1000,
            metresPerPixel = 20.0,
            random = Random(42L),
        )

        assertThat(layer.verticalLineNumberStart >= layer.verticalLineCount.toDouble(), equalTo(true))
        assertThat(layer.horizontalLineNumberStart >= layer.horizontalLineCount.toDouble(), equalTo(true))
        assertThat(layer.verticalLineNumberStart + layer.verticalLineCount <= 99.0, equalTo(true))
        assertThat(layer.horizontalLineNumberStart - layer.horizontalLineCount >= 0.0, equalTo(true))
    }

    @Test
    fun `grid layer uses supplied colour`() {
        val layer = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 1000,
            metresPerPixel = 20.0,
            random = Random(42L),
            colour = Color.MAGENTA,
        )

        assertThat(layer.colour, equalTo(Color.MAGENTA))
    }
}
