package io.hsar.mapgenerator.contourgenerator

import io.hsar.mapgenerator.graph.Point
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import java.awt.Color

class TopographyRendererTest {
    @Test
    fun `renders deterministic elevation bands and contours at field dimensions`() {
        val field = HeightField(8, 6, DoubleArray(48) { it / 47.0 })
        val map = TopographyMap(
            field,
            listOf(
                ContourPath(0.25, listOf(Point(0.0, 1.0), Point(7.0, 1.0)), closed = false),
                ContourPath(0.50, listOf(Point(0.0, 3.0), Point(7.0, 3.0)), closed = false),
            ),
        )

        val first = TopographyRenderer.render(map)
        val second = TopographyRenderer.render(map)

        assertThat(first.width, equalTo(8))
        assertThat(first.height, equalTo(6))
        assertThat(first.getRGB(0, 0), equalTo(second.getRGB(0, 0)))
        assertThat((0 until first.width).flatMap { x -> (0 until first.height).map { y -> first.getRGB(x, y) } }.toSet().size, greaterThan(2))
    }

    @Test
    fun `major and minor contour styling are configurable`() {
        val field = HeightField(20, 20, DoubleArray(400) { 0.5 })
        val map = TopographyMap(
            field,
            (0..5).map { index ->
                ContourPath(index / 10.0, listOf(Point(1.0, (index + 2).toDouble()), Point(18.0, (index + 2).toDouble())), false)
            },
        )
        val style = TopographyStyle(
            bandColours = listOf(Color.BLACK),
            minorContourColour = Color.RED,
            majorContourColour = Color.GREEN,
            minorContourWidth = 1.0f,
            majorContourWidth = 1.0f,
            majorContourInterval = 5,
        )

        val image = TopographyRenderer.render(map, style)

        assertThat(Color(image.getRGB(10, 2), true).green > Color(image.getRGB(10, 2), true).red, equalTo(true))
        assertThat(Color(image.getRGB(10, 3), true).red > Color(image.getRGB(10, 3), true).green, equalTo(true))
        assertThat(Color(image.getRGB(10, 7), true).green > Color(image.getRGB(10, 7), true).red, equalTo(true))
    }

    @Test
    fun `renders height values as greyscale`() {
        val map = TopographyMap(HeightField(2, 1, doubleArrayOf(0.0, 1.0)), emptyList())

        val image = TopographyRenderer.renderHeight(map)

        assertThat(Color(image.getRGB(0, 0)).red, equalTo(0))
        assertThat(Color(image.getRGB(1, 0)).red, equalTo(255))
    }
}
