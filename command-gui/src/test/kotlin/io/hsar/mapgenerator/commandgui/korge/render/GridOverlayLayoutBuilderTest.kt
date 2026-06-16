package io.hsar.mapgenerator.commandgui.korge.render

import io.hsar.mapgenerator.commandgui.korge.interaction.GridOverlayState
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Font

class GridOverlayLayoutBuilderTest {

    @Test
    fun `top labels clamp to gutter while map is panned upward`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = -120.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(GridStyles.MAP_ID_PADDING.y - 5.0, 0.0001))
    }

    @Test
    fun `top labels clamp to visible canvas when background extends above the window`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = -120.0, visibleCanvasTop = 176.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(221.0, 0.0001))
    }

    @Test
    fun `top labels release back to the map edge when the map top is visible`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = 80.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(125.0, 0.0001))
    }

    @Test
    fun `left labels clamp to gutter while map is panned left`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = -140.0),
            measureText = ::measureText,
        )

        val yAxisLabel = layout.labels.first { it.text == "44" }

        assertThat(yAxisLabel.x, closeTo(GridStyles.GRID_LINE_ID_PADDING.x - 10.0, 0.0001))
    }

    @Test
    fun `map id remains top-left aligned rather than centered`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = -140.0, mapOffsetY = -120.0),
            measureText = ::measureText,
        )

        val mapId = layout.labels.first { it.text == "25F XK" }

        assertThat(mapId.x, equalTo(GridStyles.MAP_ID_PADDING.x))
        assertThat(mapId.y, equalTo(GridStyles.MAP_ID_PADDING.y))
    }

    private fun state(
        mapOffsetX: Double = -140.0,
        mapOffsetY: Double = -120.0,
        visibleCanvasTop: Double = 0.0,
    ) = GridOverlayState(
        viewport = Rectangle(Point(0.0, 0.0), Point(800.0, 600.0)),
        mapOffsetX = mapOffsetX,
        mapOffsetY = mapOffsetY,
        scale = 1.0,
        pixelWidth = 1000,
        pixelHeight = 800,
        backgroundWidth = 1200,
        backgroundHeight = 900,
        visibleCanvasLeft = 0.0,
        visibleCanvasTop = visibleCanvasTop,
        visibleCanvasRight = 1200.0,
        visibleCanvasBottom = 900.0,
        gridLayer = GridLayer(
            mapId = "25F XK",
            gridScale = 1000,
            metresPerPixel = 20.0,
            detailLevel = 2,
            pixelsPerLine = 100.0,
            verticalLineNumberStart = 5,
            horizontalLineNumberStart = 45,
            verticalLineCount = 20,
            horizontalLineCount = 20,
        ),
    )

    private fun measureText(text: String, font: Font): Rectangle =
        when (font.name) {
            GridStyles.MAP_ID_FONT.first.name -> Rectangle(Point.ORIGIN, Point(60.0, 16.0))
            else -> Rectangle(Point.ORIGIN, Point(20.0, 10.0))
        }
}
