package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayerPainter.normalFadeStart
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Font

class GridLayerPainterTest {

    @Test
    fun `sticky axis text stays fixed until the viewport reaches the image edge`() {
        assertThat(GridLayerPainter.textX(baseX = 25.0, viewportLeftX = 120.0, GridLayerPainter.TextMode.STICKY_AXES), equalTo(145.0))
        assertThat(GridLayerPainter.textX(baseX = 25.0, viewportLeftX = -30.0, GridLayerPainter.TextMode.STICKY_AXES), equalTo(25.0))
        assertThat(GridLayerPainter.textY(baseY = 25.0, viewportTopY = 120.0, GridLayerPainter.TextMode.STICKY_AXES), equalTo(145.0))
        assertThat(GridLayerPainter.textY(baseY = 25.0, viewportTopY = -30.0, GridLayerPainter.TextMode.STICKY_AXES), equalTo(25.0))
    }

    @Test
    fun `image text mode preserves original positioning`() {
        assertThat(GridLayerPainter.textY(baseY = 25.0, viewportTopY = 120.0, GridLayerPainter.TextMode.IMAGE), equalTo(25.0))
        assertThat(GridLayerPainter.textX(baseX = 25.0, viewportLeftX = 120.0, GridLayerPainter.TextMode.IMAGE), equalTo(25.0))
    }

    @Test
    fun `fade curve stays strong then drops quickly near the edge`() {
        val midFade = GridLayerPainter.edgeOpacity(30.0)
        val nearEdge = GridLayerPainter.edgeOpacity(10.0)

        assertThat(midFade, closeTo(0.75, 0.0001))
        assertThat(nearEdge, closeTo(0.3055555556, 0.0001))
    }

    @Test
    fun `label layout derives gutter edges from rendered text size`() {
        val surface = testSurface()
        val viewport = testViewport()

        val vertical = GridLayerPainter.labelLayout(
            surface = surface,
            viewport = viewport,
            textMode = GridLayerPainter.TextMode.STICKY_AXES,
            axis = GridLayerPainter.GridAxis.VERTICAL,
            linePosition = 123.0,
            lineIDString = "12",
        )
        val horizontal = GridLayerPainter.labelLayout(
            surface = surface,
            viewport = viewport,
            textMode = GridLayerPainter.TextMode.STICKY_AXES,
            axis = GridLayerPainter.GridAxis.HORIZONTAL,
            linePosition = 456.0,
            lineIDString = "34",
        )

        assertThat(vertical.point, equalTo(Point(123.0, 105.55555555555556)))
        assertThat(vertical.gutterEdge, equalTo(110.55555555555556))
        assertThat(horizontal.point, equalTo(Point(104.6875, 456.0)))
        assertThat(horizontal.gutterEdge, equalTo(114.6875))
    }

    @Test
    fun `line normal fade start is derived from the opposite axis labels`() {
        val surface = testSurface()
        val viewport = testViewport()

        assertThat(
            GridLayerPainter.GridAxis.VERTICAL.normalFadeStart(
                surface = surface,
                viewport = viewport,
                textMode = GridLayerPainter.TextMode.STICKY_AXES,
                linePosition = 123.0,
                lineIDString = "12",
                mapIdPadding = GridStyles.mapIdPadding(surface.width, surface.height),
                gridLinePadding = GridStyles.gridLineIdPadding(surface.width, surface.height),
                labelFont = GridStyles.gridLineIdFont(surface.width, surface.height).first,
            ),
            equalTo(114.6875),
        )
        assertThat(
            GridLayerPainter.GridAxis.HORIZONTAL.normalFadeStart(
                surface = surface,
                viewport = viewport,
                textMode = GridLayerPainter.TextMode.STICKY_AXES,
                linePosition = 456.0,
                lineIDString = "34",
                mapIdPadding = GridStyles.mapIdPadding(surface.width, surface.height),
                gridLinePadding = GridStyles.gridLineIdPadding(surface.width, surface.height),
                labelFont = GridStyles.gridLineIdFont(surface.width, surface.height).first,
            ),
            equalTo(110.55555555555556),
        )
    }

    @Test
    fun `epsilon image grid draws major designator labels but omits minor labels`() {
        val drawnText = mutableListOf<String>()
        val surface = testSurface(drawnText)

        GridLayerPainter.draw(
            surface = surface,
            layer = GridLayer(
                mapId = "EPSILON",
                referenceSystem = ReferenceSystem.EPSILON,
                gridScale = ReferenceSystem.EPSILON.gridScale,
                unitsPerPixel = 1.0,
                detailLevel = 2,
                pixelsPerLine = 20.0,
                verticalLineNumberStart = ReferenceSystem.EPSILON.coordFromLineNumber(0.0, ReferenceSystem.EPSILON.gridScale),
                horizontalLineNumberStart = ReferenceSystem.EPSILON.coordFromLineNumber(20.0, ReferenceSystem.EPSILON.gridScale),
                verticalLineCount = 20,
                horizontalLineCount = 20,
            ),
        )

        assertThat(drawnText.contains("EPSILON"), equalTo(true))
        assertThat(drawnText.contains("1"), equalTo(true))
        assertThat(drawnText.contains("U"), equalTo(true))
        assertThat(drawnText.contains("2"), equalTo(false))
        assertThat(drawnText.contains("T"), equalTo(false))
    }

    private fun testSurface(drawnText: MutableList<String> = mutableListOf()): GridRenderSurface =
        object : GridRenderSurface {
            override val width = 200
            override val height = 200

            override fun drawString(point: Point, string: String, fontAndColour: Pair<Font, Color>) {
                drawnText += string
            }
            override fun drawLine(point1: Point, point2: Point, width: Float, color: Color) = Unit
            override fun getTextSize(text: String, font: Font): Rectangle = Rectangle(Point.ORIGIN, Point(20.0, 10.0))
        }

    private fun testViewport(): Rectangle =
        Rectangle(Point(100.0, 100.0), Point(300.0, 300.0))
}
