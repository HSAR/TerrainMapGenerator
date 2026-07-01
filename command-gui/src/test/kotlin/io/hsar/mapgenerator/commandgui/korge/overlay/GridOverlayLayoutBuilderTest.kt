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
import kotlin.math.abs

class GridOverlayLayoutBuilderTest {

    @Test
    fun `top labels clamp to gutter while map is panned upward`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = -120.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(GridStyles.mapIdPadding(1200, 900).y, 0.0001))
    }

    @Test
    fun `top labels clamp to visible canvas when background extends above the window`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = -120.0, visibleCanvasTop = 176.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(176.0 + GridStyles.mapIdPadding(1200, 724).y, 0.0001))
    }

    @Test
    fun `top labels release back to the map edge when the map top is visible`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetY = 80.0),
            measureText = ::measureText,
        )

        val xAxisLabel = layout.labels.first { it.text == "06" }

        assertThat(xAxisLabel.y, closeTo(80.0 + GridStyles.mapIdPadding(1200, 900).y, 0.0001))
    }

    @Test
    fun `left labels clamp to gutter while map is panned left`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = -140.0),
            measureText = ::measureText,
        )

        val yAxisLabel = layout.labels.first { it.text == "44" }

        assertThat(yAxisLabel.x, closeTo(GridStyles.gridLineIdPadding(1200, 900).x, 0.0001))
    }

    @Test
    fun `map id remains top-left aligned rather than centered`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = -140.0, mapOffsetY = -120.0),
            measureText = ::measureText,
        )

        val mapId = layout.labels.first { it.text == "25F XK" }

        assertThat(mapId.x, equalTo(GridStyles.gridLineIdPadding(1200, 900).x))
        assertThat(mapId.y, equalTo(GridStyles.mapIdPadding(1200, 900).y))
        assertThat(mapId.style.role, equalTo(io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG))
        assertThat(mapId.style.size, equalTo(GridStyles.mapIdFont(1200, 900).first.size2D.toDouble()))
        assertThat(mapId.style.color, equalTo(io.hsar.mapgenerator.grid.GridPalettes.DEFAULT.lineColour))
        assertThat(mapId.shadows.size, equalTo(0))
    }

    @Test
    fun `high zoom switches to hundred metre labels and thicker major lines`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(pixelsPerLine = 100.0, scale = 4.0),
            measureText = ::measureText,
        )

        assertThat(layout.labels.any { it.text.length == 3 }, equalTo(true))
        assertThat(layout.lineRects.maxOf { it.width } > layout.lineRects.minOf { it.width }, equalTo(true))
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
    fun `hundred metre labels keep even cadence when x grid start is fractional`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                pixelsPerLine = 100.0,
                scale = 4.0,
                gridStartX = 61.3,
                gridStartY = 45.0,
            ),
            measureText = ::measureText,
        )

        val verticalLineCentres = layout.lineRects
            .filter { it.height > it.width }
            .groupBy { it.x + it.width / 2.0 }
            .keys
            .sorted()

        assertThat(layout.labels.any { it.text == "613" }, equalTo(false))
        assertThat(layout.labels.any { it.text == "614" }, equalTo(true))
        assertThat(layout.labels.any { it.text == "615" }, equalTo(false))
        assertThat(layout.labels.any { it.text == "616" }, equalTo(true))
        assertThat(layout.labels.any { it.text == "620" }, equalTo(true))
        assertThat(
            verticalLineCentres.zipWithNext().take(4).all { (first, second) ->
                abs(second - first - 10.0) < 0.0001
            },
            equalTo(true),
        )
    }

    @Test
    fun `major grid labels use the strong font`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = 0.0, mapOffsetY = 0.0),
            measureText = ::measureText,
        )

        val majorLabel = layout.labels.first { it.text == "10" }

        assertThat(majorLabel.style.role, equalTo(io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG))
        assertThat(majorLabel.style.size, equalTo(GridStyles.majorGridLineIdFont(1200, 900).first.size2D.toDouble()))
        assertThat(majorLabel.style.color.red, equalTo(io.hsar.mapgenerator.grid.GridPalettes.DEFAULT.lineColour.red))
        assertThat(majorLabel.style.color.green, equalTo(io.hsar.mapgenerator.grid.GridPalettes.DEFAULT.lineColour.green))
        assertThat(majorLabel.style.color.blue, equalTo(io.hsar.mapgenerator.grid.GridPalettes.DEFAULT.lineColour.blue))
    }

    @Test
    fun `grid colour drives map id labels axis labels and grid lines`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(gridColour = Palette.Colours.LIGHT),
            measureText = ::measureText,
        )

        val mapId = layout.labels.first { it.text == "25F XK" }
        val axisLabel = layout.labels.first { it.text == "06" }
        val line = layout.lineRects.first()

        assertThat(mapId.style.color, equalTo(Palette.Colours.LIGHT))
        assertThat(axisLabel.style.color.red, equalTo(Palette.Colours.LIGHT.red))
        assertThat(axisLabel.style.color.green, equalTo(Palette.Colours.LIGHT.green))
        assertThat(axisLabel.style.color.blue, equalTo(Palette.Colours.LIGHT.blue))
        assertThat(line.color.red, equalTo(Palette.Colours.LIGHT.red))
        assertThat(line.color.green, equalTo(Palette.Colours.LIGHT.green))
        assertThat(line.color.blue, equalTo(Palette.Colours.LIGHT.blue))
    }


    @Test
    fun `first visible vertical line renders from map origin`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(mapOffsetX = 0.0, mapOffsetY = 0.0),
            measureText = ::measureText,
        )

        val firstVerticalLine = layout.lineRects
            .filter { it.height > it.width }
            .minBy { it.x }

        assertThat(
            firstVerticalLine.x,
            closeTo(-0.5, 0.0001),
        )
    }

    @Test
    fun `hundred metre lines render when they land on the axis fade boundary`() {
        val gridLinePadding = GridStyles.gridLineIdPadding(1200, 900)
        val mapIdPadding = GridStyles.mapIdPadding(1200, 900)
        val axisRightEdge = gridLinePadding.x + 10.0
        val axisBottomEdge = mapIdPadding.y + 5.0
        val scale = 3.0
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = gridLinePadding.x,
                mapOffsetY = gridLinePadding.y,
                pixelsPerLine = 100.0,
                scale = scale,
                gridStartX = 61.3,
                gridStartY = 32.9,
            ),
            measureText = ::measureText,
        )

        val boundaryVerticalLine = layout.lineRects
            .filter { it.height > it.width }
            .groupBy { it.x + it.width / 2.0 }
            .values
            .first { segments ->
                abs(segments.first().x + segments.first().width / 2.0) < 0.0001
            }
            .maxBy { it.height }
        val boundaryHorizontalLine = layout.lineRects
            .filter { it.width > it.height }
            .groupBy { it.y + it.height / 2.0 }
            .values
            .first { segments ->
                abs(segments.first().y + segments.first().height / 2.0) < 0.0001
            }
            .maxBy { it.width }

        assertThat(boundaryVerticalLine.color.alpha, equalTo(255))
        assertThat(boundaryHorizontalLine.color.alpha, equalTo(255))
    }

    @Test
    fun `fractional grid starts shift line positions by the fractional remainder`() {
        val integerLayout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 200.0,
                mapOffsetY = 100.0,
                gridStartX = 5.0,
                gridStartY = 45.0,
            ),
            measureText = ::measureText,
        )

        val fractionalLayout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 200.0,
                mapOffsetY = 100.0,
                gridStartX = 5.5,
                gridStartY = 45.25,
            ),
            measureText = ::measureText,
        )

        val integerFirstVerticalLine = integerLayout.lineRects
            .filter { it.height > it.width }
            .minBy { it.x }
        val fractionalFirstVerticalLine = fractionalLayout.lineRects
            .filter { it.height > it.width }
            .minBy { it.x }

        val integerFirstHorizontalLine = integerLayout.lineRects
            .filter { it.width > it.height }
            .minBy { it.y }
        val fractionalFirstHorizontalLine = fractionalLayout.lineRects
            .filter { it.width > it.height }
            .minBy { it.y }

        assertThat(
            fractionalFirstVerticalLine.x - integerFirstVerticalLine.x,
            closeTo(5.0, 0.0001),
        )
        assertThat(
            fractionalFirstHorizontalLine.y - integerFirstHorizontalLine.y,
            closeTo(2.5, 0.0001),
        )
        assertThat(fractionalLayout.labels.any { it.text == "06" }, equalTo(true))
        assertThat(fractionalLayout.labels.any { it.text == "45" }, equalTo(true))
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
            .first { it.first().height > it.first().width && it.first().x > 100.0 }
            .sortedBy { it.y }

        val horizontalLineSegments = layout.lineRects
            .groupBy { it.y to it.height }
            .values
            .first { it.first().width > it.first().height && it.first().y > 100.0 }
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
            .first { it.first().height > it.first().width && it.first().x > 100.0 }

        assertThat(verticalLineSegments.size, equalTo(8))
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
        assertThat(referenceLabel.x, equalTo(282.0))
        assertThat(referenceLabel.y, equalTo(263.0))
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

        assertThat(distance.x, equalTo(282.0))
        assertThat(distance.y, equalTo(297.0))
        assertThat(distance.style.color, equalTo(Palette.Colours.RED))
        assertThat(bearing.x, equalTo(282.0))
        assertThat(bearing.y, equalTo(311.0))
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

    @Test
    fun `active reference colour drives move preview arrow and labels`() {
        val layout = GridOverlayLayoutBuilder.build(
            state = state(
                mapOffsetX = 0.0,
                mapOffsetY = 0.0,
                activeReference = GridCursorReference(
                    screenX = 260.0,
                    screenY = 280.0,
                    text = "0730 3900",
                    color = Palette.Colours.BLUE,
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

        val referenceLabel = layout.labels.first { it.text == "0730 3900" }
        val distance = layout.labels.first { it.text == "344m" }
        val bearing = layout.labels.first { it.text == "125\u00b0" }
        val arrow = layout.overlayArrows.single()

        assertThat(referenceLabel.style.color, equalTo(Palette.Colours.BLUE))
        assertThat(distance.style.color, equalTo(Palette.Colours.BLUE))
        assertThat(bearing.style.color, equalTo(Palette.Colours.BLUE))
        assertThat(arrow.color, equalTo(Palette.Colours.BLUE))
    }

    private fun state(
        mapOffsetX: Double = -140.0,
        mapOffsetY: Double = -120.0,
        visibleCanvasTop: Double = 0.0,
        pixelsPerLine: Double = 10.0,
        scale: Double = 1.0,
        gridStartX: Double = 5.0,
        gridStartY: Double = 45.0,
        gridColour: java.awt.Color = io.hsar.mapgenerator.grid.GridPalettes.DEFAULT.lineColour,
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
            verticalLineNumberStart = gridStartX,
            horizontalLineNumberStart = gridStartY,
            verticalLineCount = 20,
            horizontalLineCount = 20,
            colour = gridColour,
        ),
    )

    private fun measureText(text: String, style: GridLabelStyle): Rectangle =
        when (style.role) {
            io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG ->
                if (style.size > 24.0) Rectangle(Point.ORIGIN, Point(60.0, 16.0)) else Rectangle(Point.ORIGIN, Point(24.0, 10.0))
            else -> Rectangle(Point.ORIGIN, Point(20.0, 10.0))
        }
}
