package io.hsar.mapgenerator.commandgui.korge.render

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.grid.GridLayer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class MapViewerRendererTest {

    @Test
    fun `renderer returns premultiplied bitmap for KorGE`() {
        val source = BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB).apply {
            val graphics = createGraphics()
            try {
                graphics.color = Color(255, 0, 0, 128)
                graphics.fillRect(0, 0, width, height)
            } finally {
                graphics.dispose()
            }
        }

        val rendered = MapViewerRenderer().render(
            MapViewerRenderData(
                baseImage = source,
                gridLayer = GridLayer(
                    mapId = "25F XK",
                    gridScale = 25,
                    metresPerPixel = 20.0,
                    detailLevel = 2,
                    pixelsPerLine = 100.0,
                    verticalLineNumberStart = 1,
                    horizontalLineNumberStart = 1,
                    verticalLineCount = 1,
                    horizontalLineCount = 1,
                ),
            )
        )

        assertThat(rendered.bitmap.premultiplied, `is`(true))
        assertThat(rendered.geometry.backgroundWidth, equalTo(6))
        assertThat(rendered.geometry.backgroundHeight, equalTo(4))
    }
}
