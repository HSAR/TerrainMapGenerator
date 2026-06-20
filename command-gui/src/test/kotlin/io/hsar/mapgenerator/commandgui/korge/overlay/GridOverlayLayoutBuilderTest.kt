package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test

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
        assertThat(mapId.y, equalTo(42.0))
        assertThat(mapId.style.role, equalTo(io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG))
        assertThat(mapId.style.size, equalTo(26.0))
        assertThat(mapId.style.color, equalTo(GridStyles.MAP_ID_FONT.second))
        assertThat(mapId.shadows.size, equalTo(0))
    }

    @Test
    fun `high zoom switches to hundred metre labels and thicker major lines`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(pixelsPerLine = 100.0, scale = 4.0),
            measureText = ::measureText,
        )

        assertThat(layout.labels.any { it.text.length == 3 }, equalTo(true))
        assertThat(layout.lineRects.any { it.width > 1.0 }, equalTo(true))
    }

    @Test
    fun `hundred metre mode skips every other minor x-axis label`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(pixelsPerLine = 100.0, scale = 4.0),
            measureText = ::measureText,
        )

        assertThat(layout.labels.any { it.text == "051" }, equalTo(false))
        assertThat(layout.labels.any { it.text == "052" }, equalTo(true))
        assertThat(layout.labels.any { it.text == "053" }, equalTo(false))
        assertThat(layout.labels.any { it.text == "054" }, equalTo(true))
    }

    @Test
    fun `major grid labels use the strong font`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = 0.0, mapOffsetY = 0.0),
            measureText = ::measureText,
        )

        val majorLabel = layout.labels.first { it.text == "10" }

        assertThat(majorLabel.style.role, equalTo(io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG))
        assertThat(majorLabel.style.size, equalTo(22.0))
        assertThat(majorLabel.style.color.red, equalTo(GridStyles.MAJOR_GRID_LINE_ID_FONT.second.red))
        assertThat(majorLabel.style.color.green, equalTo(GridStyles.MAJOR_GRID_LINE_ID_FONT.second.green))
        assertThat(majorLabel.style.color.blue, equalTo(GridStyles.MAJOR_GRID_LINE_ID_FONT.second.blue))
    }

    @Test
    fun `grid lines stay solid toward the bottom and right edges`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = 0.0, mapOffsetY = 0.0),
            measureText = ::measureText,
        )

        val verticalLineSegments = layout.lineRects
            .groupBy { it.x to it.width }
            .values
            .first { it.first().height > it.first().width && it.first().x > 200.0 }
            .sortedBy { it.y }

        val horizontalLineSegments = layout.lineRects
            .groupBy { it.y to it.height }
            .values
            .first { it.first().width > it.first().height && it.first().y > 200.0 }
            .sortedBy { it.x }

        assertThat(verticalLineSegments.first().color.alpha, lessThan(verticalLineSegments.last().color.alpha))
        assertThat(verticalLineSegments.last().color.alpha, equalTo(255))
        assertThat(horizontalLineSegments.first().color.alpha, lessThan(horizontalLineSegments.last().color.alpha))
        assertThat(horizontalLineSegments.last().color.alpha, equalTo(255))
    }

    @Test
    fun `long faded lines use a compact number of rects`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = 0.0, mapOffsetY = 0.0),
            measureText = ::measureText,
        )

        val verticalLineSegments = layout.lineRects
            .groupBy { it.x to it.width }
            .values
            .first { it.first().height > it.first().width && it.first().x > 200.0 }

        assertThat(verticalLineSegments.size, equalTo(11))
    }

    @Test
    fun `active reference draws red crosshair and label`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 0.0,
                mapOffsetY = 0.0,
                activeReference = GridCursorReference(
                    screenX = 260.0,
                    screenY = 280.0,
                    text = "0730-3900",
                ),
            ),
            measureText = ::measureText,
        )

        val referenceLabel = layout.labels.first { it.text == "0730-3900" }
        val verticalReferenceSegments = layout.lineRects
            .groupBy { it.x to it.width }
            .values
            .first {
                it.first().height > it.first().width &&
                    it.first().width == 2.0 &&
                    it.first().color.red == Palette.Colours.RED.red &&
                    it.first().color.green == Palette.Colours.RED.green &&
                    it.first().color.blue == Palette.Colours.RED.blue
            }
            .sortedBy { it.y }

        assertThat(referenceLabel.style.color, equalTo(Palette.Colours.RED))
        assertThat(referenceLabel.x, equalTo(272.0))
        assertThat(referenceLabel.y, equalTo(258.0))
        assertThat(referenceLabel.shadows.size, equalTo(1))
        assertThat(referenceLabel.shadows.first().color, equalTo(Palette.Colours.MAIN))
        assertThat(referenceLabel.shadows.first().offsetX, equalTo(1.0))
        assertThat(referenceLabel.shadows.first().offsetY, equalTo(1.0))
        assertThat(verticalReferenceSegments.first().color.alpha, lessThan(verticalReferenceSegments.last().color.alpha))
        assertThat(verticalReferenceSegments.last().color.alpha, equalTo(255))
    }

    @Test
    fun `measurement draws overlay line and tooltip below active reference`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 0.0,
                mapOffsetY = 0.0,
                activeReference = GridCursorReference(
                    screenX = 260.0,
                    screenY = 280.0,
                    text = "0730 3900",
                ),
                measurement = GridMeasurement(
                    originMapX = 120.0,
                    originMapY = 180.0,
                    destinationMapX = 260.0,
                    destinationMapY = 280.0,
                    distanceMetres = 344,
                    bearingDegrees = 125,
                ),
            ),
            measureText = ::measureText,
        )

        val distance = layout.labels.first { it.text == "344m" }
        val bearing = layout.labels.first { it.text == "125\u00b0" }
        val arrow = layout.overlayArrows.single()

        assertThat(distance.x, equalTo(272.0))
        assertThat(distance.y, equalTo(292.0))
        assertThat(distance.style.color, equalTo(Palette.Colours.RED))
        assertThat(bearing.x, equalTo(272.0))
        assertThat(bearing.y, equalTo(316.0))
        assertThat(bearing.style.color, equalTo(Palette.Colours.RED))
        assertThat(arrow.x1, equalTo(120.0))
        assertThat(arrow.y1, equalTo(180.0))
        assertThat(arrow.x2, equalTo(260.0))
        assertThat(arrow.y2, equalTo(280.0))
        assertThat(arrow.color.red, equalTo(Palette.Colours.RED.red))
    }

    @Test
    fun `measurement line follows zoom rather than stale screen coordinates`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 20.0,
                mapOffsetY = 30.0,
                scale = 2.0,
                activeReference = GridCursorReference(
                    screenX = 260.0,
                    screenY = 280.0,
                    text = "0730 3900",
                ),
                measurement = GridMeasurement(
                    originMapX = 50.0,
                    originMapY = 60.0,
                    destinationMapX = 70.0,
                    destinationMapY = 90.0,
                    distanceMetres = 0,
                    bearingDegrees = 0,
                ),
            ),
            measureText = ::measureText,
        )

        val line = layout.overlayArrows.single()

        assertThat(line.x1, equalTo(120.0))
        assertThat(line.y1, equalTo(150.0))
        assertThat(line.x2, equalTo(160.0))
        assertThat(line.y2, equalTo(210.0))
    }

    private fun state(
        mapOffsetX: Double = -140.0,
        mapOffsetY: Double = -120.0,
        visibleCanvasTop: Double = 0.0,
        pixelsPerLine: Double = 10.0,
        scale: Double = 1.0,
        activeReference: GridCursorReference? = null,
        measurement: GridMeasurement? = null,
    ) = GridOverlayState(
        viewport = Rectangle(Point(0.0, 0.0), Point(800.0, 600.0)),
        mapWorldX = 0.0,
        mapWorldY = 0.0,
        mapOffsetX = mapOffsetX,
        mapOffsetY = mapOffsetY,
        scale = scale,
        pixelWidth = 1000,
        pixelHeight = 800,
        backgroundWidth = 1200,
        backgroundHeight = 900,
        visibleCanvasLeft = 0.0,
        visibleCanvasTop = visibleCanvasTop,
        visibleCanvasRight = 1200.0,
        visibleCanvasBottom = 900.0,
        activeReference = activeReference,
        measurement = measurement,
        gridLayer = GridLayer(
            mapId = "25F XK",
            gridScale = 1000,
            metresPerPixel = 20.0,
            detailLevel = 2,
            pixelsPerLine = pixelsPerLine,
            verticalLineNumberStart = 5,
            horizontalLineNumberStart = 45,
            verticalLineCount = 20,
            horizontalLineCount = 20,
        ),
    )

    private fun measureText(text: String, style: GridLabelStyle): Rectangle =
        when (style.role) {
            io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG ->
                if (style.size > 24.0) Rectangle(Point.ORIGIN, Point(60.0, 16.0)) else Rectangle(Point.ORIGIN, Point(24.0, 10.0))
            else -> Rectangle(Point.ORIGIN, Point(20.0, 10.0))
        }
}
