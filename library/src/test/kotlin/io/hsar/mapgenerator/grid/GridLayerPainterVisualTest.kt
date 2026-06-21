package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.image.ImageBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class GridLayerPainterVisualTest {

    @Test
    fun `sticky grid lines fade near the label gutters`() {
        val image = renderStickyGrid()

        assertThat(alphaAt(image, 100, 75), lessThan(alphaAt(image, 150, 75)))
        assertThat(alphaAt(image, 75, 50), lessThan(alphaAt(image, 75, 100)))
    }

    private fun renderStickyGrid(): BufferedImage {
        val imageBuilder = ImageBuilder(220, 220)
            .fillTransparent()

        GridLayerPainter.draw(
            surface = imageBuilder,
            layer = GridLayer(
                mapId = "TEST",
                gridScale = 1000,
                metresPerPixel = 1.0,
                detailLevel = 1,
                pixelsPerLine = 50.0,
                verticalLineNumberStart = 9.0,
                horizontalLineNumberStart = 11.0,
                verticalLineCount = 3,
                horizontalLineCount = 3,
            ),
            viewport = Rectangle(
                Point.ORIGIN,
                Point(220.0, 220.0),
            ),
            textMode = GridLayerPainter.TextMode.STICKY_AXES,
        )

        return imageBuilder.build()
    }

    private fun alphaAt(image: BufferedImage, x: Int, y: Int): Int =
        Color(image.getRGB(x, y), true).alpha
}
