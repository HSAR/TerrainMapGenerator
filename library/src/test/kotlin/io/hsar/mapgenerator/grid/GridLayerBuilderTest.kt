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
            unitsPerPixel = 2.0,
            random = Random(seed),
        )
        val second = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 100,
            unitsPerPixel = 2.0,
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
            unitsPerPixel = 20.0,
            random = Random(42L),
        )

        assertThat(layer.verticalLineNumberStart.lineNumber(layer.gridScale) >= layer.verticalLineCount.toDouble(), equalTo(true))
        assertThat(layer.horizontalLineNumberStart.lineNumber(layer.gridScale) >= layer.horizontalLineCount.toDouble(), equalTo(true))
        assertThat(layer.verticalLineNumberStart.lineNumber(layer.gridScale) + layer.verticalLineCount <= 99.0, equalTo(true))
        assertThat(layer.horizontalLineNumberStart.lineNumber(layer.gridScale) - layer.horizontalLineCount >= 0.0, equalTo(true))
    }

    @Test
    fun `grid layer uses supplied colour`() {
        val layer = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 1000,
            unitsPerPixel = 20.0,
            random = Random(42L),
            colour = Color.MAGENTA,
        )

        assertThat(layer.colour, equalTo(Color.MAGENTA))
    }

    @Test
    fun `epsilon grid uses twenty unit squares and representable generated starts`() {
        val layer = GridLayerBuilder.build(
            width = 1_000,
            height = 1_000,
            gridScale = ReferenceSystem.EPSILON.gridScale,
            unitsPerPixel = ReferenceSystem.EPSILON.scaleInputToUnitsPerPixel(20.0),
            referenceSystem = ReferenceSystem.EPSILON,
            random = Random(42L),
        )

        assertThat(layer.gridScale, equalTo(20))
        assertThat(layer.pixelsPerLine, equalTo(400.0))
        assertThat(EpsilonGridDesignators.xIndex(ReferenceSystem.EPSILON.formatCoord(layer.verticalLineNumberStart, CoordAxis.X, layer.gridScale)) != null, equalTo(true))
        assertThat(EpsilonGridDesignators.yIndex(ReferenceSystem.EPSILON.formatCoord(layer.horizontalLineNumberStart, CoordAxis.Y, layer.gridScale)) != null, equalTo(true))
    }
}
