package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GridLayerPainter {

    enum class TextMode {
        IMAGE,
        STICKY_AXES,
    }

    fun draw(
        surface: GridRenderSurface,
        layer: GridLayer,
        viewport: Rectangle = Rectangle(
            Point.ORIGIN,
            Point(surface.width.toDouble(), surface.height.toDouble()),
        ),
        textMode: TextMode = TextMode.IMAGE,
    ) {
        val mapIdPadding = GridStyles.mapIdPadding(surface.width, surface.height)
        val gridLinePadding = GridStyles.gridLineIdPadding(surface.width, surface.height)
        val mapIdFont = GridStyles.mapIdFont(surface.width, surface.height).first to layer.colour
        val gridLineFont = GridStyles.gridLineIdFont(surface.width, surface.height).first to layer.colour
        val majorGridLineFont = GridStyles.majorGridLineIdFont(surface.width, surface.height).first to layer.colour
        val edgeFadeDistance = GridStyles.fadeDistance(surface.width, surface.height)

        surface.drawCenteredString(
            Point(
                textX(gridLinePadding.x, viewport.topLeft.x, textMode),
                textY(mapIdPadding.y, viewport.topLeft.y, textMode),
            ),
            layer.mapId,
            mapIdFont,
        )

        drawGridLines(
            surface = surface,
            layer = layer,
            viewport = viewport,
            textMode = textMode,
            axis = GridAxis.VERTICAL,
            mapIdPadding = mapIdPadding,
            gridLinePadding = gridLinePadding,
            gridLineFont = gridLineFont,
            majorGridLineFont = majorGridLineFont,
            edgeFadeDistance = edgeFadeDistance,
        )
        drawGridLines(
            surface = surface,
            layer = layer,
            viewport = viewport,
            textMode = textMode,
            axis = GridAxis.HORIZONTAL,
            mapIdPadding = mapIdPadding,
            gridLinePadding = gridLinePadding,
            gridLineFont = gridLineFont,
            majorGridLineFont = majorGridLineFont,
            edgeFadeDistance = edgeFadeDistance,
        )
    }

    internal fun textX(baseX: Double, viewportLeftX: Double, textMode: TextMode): Double =
        when (textMode) {
            TextMode.IMAGE -> baseX
            TextMode.STICKY_AXES -> max(baseX, viewportLeftX + baseX)
        }

    internal fun textY(baseY: Double, viewportTopY: Double, textMode: TextMode): Double =
        when (textMode) {
            TextMode.IMAGE -> baseY
            TextMode.STICKY_AXES -> max(baseY, viewportTopY + baseY)
        }

    private fun drawGridLines(
        surface: GridRenderSurface,
        layer: GridLayer,
        viewport: Rectangle,
        textMode: TextMode,
        axis: GridAxis,
        mapIdPadding: Point,
        gridLinePadding: Point,
        gridLineFont: Pair<java.awt.Font, Color>,
        majorGridLineFont: Pair<java.awt.Font, Color>,
        edgeFadeDistance: Double,
    ) {
        val (startIndex, endIndex) = axis.lineIndexRange(viewport, layer, gridLinePadding)

        if (startIndex > endIndex) {
            return
        }

        (startIndex..endIndex).forEach { lineIndex ->
            val lineID = axis.lineId(layer, lineIndex)
            val lineIDString = axis.lineText(layer, lineID)
            val linePosition = axis.linePosition(layer, lineIndex, gridLinePadding)
            val isMajorLine = axis.isMajorLine(lineID)
            val shouldDrawLabel = layer.referenceSystem.shouldDrawLabel(isMajorLine)
            val fontAndColour = if (isMajorLine) majorGridLineFont else gridLineFont
            val labelLayout = labelLayout(
                surface = surface,
                viewport = viewport,
                textMode = textMode,
                axis = axis,
                linePosition = linePosition,
                lineIDString = lineIDString,
                mapIdPadding = mapIdPadding,
                gridLinePadding = gridLinePadding,
                labelFont = fontAndColour.first,
            )
            val labelColour = axis.labelColour(
                linePosition = linePosition,
                viewport = viewport,
                textMode = textMode,
                gridLinePadding = gridLinePadding,
                color = fontAndColour.second,
                edgeFadeDistance = edgeFadeDistance,
            )
            val lineBounds = axis.lineBounds(
                surface = surface,
                viewport = viewport,
                linePosition = linePosition,
                lineIDString = lineIDString,
                gridLinePadding = gridLinePadding,
                labelFont = fontAndColour.first,
            )
            val width = if (isMajorLine) 1.0f else 0.5f
            val lineNormalFadeStart = axis.normalFadeStart(
                surface = surface,
                viewport = viewport,
                textMode = textMode,
                linePosition = linePosition,
                lineIDString = lineIDString,
                mapIdPadding = mapIdPadding,
                gridLinePadding = gridLinePadding,
                labelFont = fontAndColour.first,
            )

            if (lineBounds.isVisible(axis)) {
                if (shouldDrawLabel) {
                    surface.drawCenteredString(
                        point = labelLayout.point,
                        text = lineIDString,
                        fontAndColour = fontAndColour.first to labelColour,
                    )
                }
                drawLineWithOptionalFade(
                    surface = surface,
                    start = lineBounds.start,
                    end = lineBounds.end,
                    width = width,
                    color = layer.colour,
                    textMode = textMode,
                    fadeStart = labelLayout.gutterEdge,
                    lineNormal = linePosition,
                    lineNormalFadeStart = lineNormalFadeStart,
                    axis = axis,
                    edgeFadeDistance = edgeFadeDistance,
                )
            }
        }
    }

    private fun drawLineWithOptionalFade(
        surface: GridRenderSurface,
        start: Point,
        end: Point,
        width: Float,
        color: Color,
        textMode: TextMode,
        fadeStart: Double,
        lineNormal: Double,
        lineNormalFadeStart: Double,
        axis: GridAxis,
        edgeFadeDistance: Double,
    ) {
        when (textMode) {
            TextMode.IMAGE -> {
                surface.drawLine(start, end, width = width, color = color)
            }

            TextMode.STICKY_AXES -> {
                val (lineStart, lineEnd) = when (axis) {
                    GridAxis.VERTICAL -> fadeStart to end.y
                    GridAxis.HORIZONTAL -> fadeStart to end.x
                }
                val normalOpacity = lineNormalOpacity(lineNormal, lineNormalFadeStart, edgeFadeDistance)

                val length = lineEnd - lineStart
                val segmentCount = max(1, (length / LINE_SEGMENT_LENGTH).roundToInt())
                val step = length / segmentCount

                (0 until segmentCount).forEach { segmentIndex ->
                    val segmentStart = lineStart + segmentIndex * step
                    val segmentEnd = if (segmentIndex == segmentCount - 1) lineEnd else segmentStart + step
                    val segmentCenter = (segmentStart + segmentEnd) / 2
                    val opacity = lineSegmentOpacity(segmentCenter, lineStart, lineEnd, edgeFadeDistance) * normalOpacity

                    if (opacity > 0.0) {
                        when (axis) {
                            GridAxis.VERTICAL -> surface.drawLine(
                                Point(start.x, segmentStart),
                                Point(end.x, segmentEnd),
                                width = width,
                                color = color.withAlpha(opacity),
                            )

                            GridAxis.HORIZONTAL -> surface.drawLine(
                                Point(segmentStart, start.y),
                                Point(segmentEnd, end.y),
                                width = width,
                                color = color.withAlpha(opacity),
                            )
                        }
                    }
                }
            }
        }
    }

    internal data class LabelLayout(
        val point: Point,
        val gutterEdge: Double,
    )

    internal enum class GridAxis {
        VERTICAL,
        HORIZONTAL,
    }

    private data class LineBounds(val start: Point, val end: Point)

    private fun LineBounds.isVisible(axis: GridAxis): Boolean =
        when (axis) {
            GridAxis.VERTICAL -> start.y < end.y
            GridAxis.HORIZONTAL -> start.x < end.x
        }

    private fun startIndexRange(viewportPosition: Double, pixelsPerLine: Double, padding: Double): Int =
        max(0, ceil((viewportPosition - padding) / pixelsPerLine).toInt())

    private fun endIndexRange(viewportPosition: Double, count: Int, pixelsPerLine: Double, padding: Double): Int =
        min(count, floor((viewportPosition - padding) / pixelsPerLine).toInt())

    internal fun labelLayout(
        surface: GridRenderSurface,
        viewport: Rectangle,
        textMode: TextMode,
        axis: GridAxis,
        linePosition: Double,
        lineIDString: String,
        mapIdPadding: Point = GridStyles.mapIdPadding(surface.width, surface.height),
        gridLinePadding: Point = GridStyles.gridLineIdPadding(surface.width, surface.height),
        labelFont: java.awt.Font = GridStyles.gridLineIdFont(surface.width, surface.height).first,
    ): LabelLayout {
        val textSize = surface.getTextSize(lineIDString, labelFont)
        return when (axis) {
            GridAxis.VERTICAL -> {
                val labelY = textY(
                    mapIdPadding.y,
                    viewport.topLeft.y,
                    textMode,
                )
                LabelLayout(
                    point = Point(linePosition, labelY),
                    gutterEdge = labelY + textSize.height / 2,
                )
            }

            GridAxis.HORIZONTAL -> {
                val labelX = textX(gridLinePadding.x, viewport.topLeft.x, textMode)
                LabelLayout(
                    point = Point(labelX, linePosition),
                    gutterEdge = labelX + textSize.width / 2,
                )
            }
        }
    }

    private fun axisOpacity(value: Double, stickyEdge: Double, edgeFadeDistance: Double): Double =
        edgeOpacity(value - stickyEdge, edgeFadeDistance)

    internal fun edgeOpacity(distanceFromEdge: Double, edgeFadeDistance: Double = EDGE_FADE_DISTANCE): Double =
        1.0 - (1.0 - (distanceFromEdge / edgeFadeDistance).coerceIn(0.0, 1.0)).let { it * it }

    private fun lineSegmentOpacity(position: Double, start: Double, end: Double, edgeFadeDistance: Double): Double {
        val distanceFromStart = position - start
        val distanceFromEnd = end - position
        return min(edgeOpacity(distanceFromStart, edgeFadeDistance), edgeOpacity(distanceFromEnd, edgeFadeDistance))
    }

    internal fun lineNormalOpacity(lineNormal: Double, lineNormalFadeStart: Double, edgeFadeDistance: Double = EDGE_FADE_DISTANCE): Double =
        edgeOpacity(lineNormal - lineNormalFadeStart, edgeFadeDistance)

    private fun GridAxis.opposite(): GridAxis =
        when (this) {
            GridAxis.VERTICAL -> GridAxis.HORIZONTAL
            GridAxis.HORIZONTAL -> GridAxis.VERTICAL
        }

    private fun GridAxis.lineIndexRange(viewport: Rectangle, layer: GridLayer, gridLinePadding: Point): Pair<Int, Int> =
        when (this) {
            GridAxis.VERTICAL -> startIndexRange(
                viewportPosition = viewport.topLeft.x,
                pixelsPerLine = layer.pixelsPerLine,
                padding = lineBasePosition(layer, gridLinePadding),
            ) to endIndexRange(
                viewportPosition = viewport.topRight.x,
                count = layer.verticalLineCount,
                pixelsPerLine = layer.pixelsPerLine,
                padding = lineBasePosition(layer, gridLinePadding),
            )

            GridAxis.HORIZONTAL -> startIndexRange(
                viewportPosition = viewport.topLeft.y,
                pixelsPerLine = layer.pixelsPerLine,
                padding = lineBasePosition(layer, gridLinePadding),
            ) to endIndexRange(
                viewportPosition = viewport.bottomRight.y,
                count = layer.horizontalLineCount,
                pixelsPerLine = layer.pixelsPerLine,
                padding = lineBasePosition(layer, gridLinePadding),
            )
        }

    private fun GridAxis.lineId(layer: GridLayer, lineIndex: Int): Double =
        when (this) {
            GridAxis.VERTICAL -> layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) + lineIndex
            GridAxis.HORIZONTAL -> layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale) - lineIndex
        }

    private fun GridAxis.linePosition(layer: GridLayer, lineIndex: Int, gridLinePadding: Point): Double =
        when (this) {
            GridAxis.VERTICAL -> lineBasePosition(layer, gridLinePadding) + lineIndex * layer.pixelsPerLine
            GridAxis.HORIZONTAL -> lineBasePosition(layer, gridLinePadding) + lineIndex * layer.pixelsPerLine
        }

    private fun GridAxis.lineBasePosition(layer: GridLayer, gridLinePadding: Point): Double =
        when (this) {
            GridAxis.VERTICAL -> gridLinePadding.x + (
                    layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) -
                            layer.verticalLineNumberStart.lineNumber(layer.gridScale)
                    ) * layer.pixelsPerLine

            GridAxis.HORIZONTAL -> gridLinePadding.y + (
                    layer.horizontalLineNumberStart.lineNumber(layer.gridScale) -
                            layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale)
                    ) * layer.pixelsPerLine
        }

    private fun GridAxis.isMajorLine(lineId: Double): Boolean =
        abs(lineId % 10.0) < 1e-6

    private fun GridAxis.lineText(layer: GridLayer, lineId: Double): String =
        when (layer.referenceSystem) {
            ReferenceSystem.MGRS -> lineId.padZeroes(layer.detailLevel)
            ReferenceSystem.EPSILON -> layer.referenceSystem.formatCoord(
                coord = layer.referenceSystem.coordFromLineNumber(lineId, layer.gridScale),
                axis = coordAxis(),
                gridScale = layer.gridScale,
            )
        }

    private fun GridAxis.coordAxis(): CoordAxis =
        when (this) {
            GridAxis.VERTICAL -> CoordAxis.X
            GridAxis.HORIZONTAL -> CoordAxis.Y
        }

    private fun ReferenceSystem.shouldDrawLabel(isMajorLine: Boolean): Boolean =
        when (this) {
            ReferenceSystem.MGRS -> true
            ReferenceSystem.EPSILON -> isMajorLine
        }

    private fun Double.ceilWhole(): Double =
        kotlin.math.ceil(this)

    private fun Double.floorWhole(): Double =
        kotlin.math.floor(this)

    private fun GridAxis.lineBounds(
        surface: GridRenderSurface,
        viewport: Rectangle,
        linePosition: Double,
        lineIDString: String,
        gridLinePadding: Point,
        labelFont: java.awt.Font,
    ): LineBounds =
        when (this) {
            GridAxis.VERTICAL -> LineBounds(
                start = Point(
                    linePosition,
                    max(
                        viewport.topLeft.y,
                        gridLinePadding.y + surface.getTextSize(lineIDString, labelFont).height,
                    ),
                ),
                end = Point(
                    linePosition,
                    min(viewport.bottomRight.y, surface.height.toDouble()),
                ),
            )

            GridAxis.HORIZONTAL -> LineBounds(
                start = Point(
                    max(
                        viewport.topLeft.x,
                        gridLinePadding.x + surface.getTextSize(lineIDString, labelFont).width,
                    ),
                    linePosition,
                ),
                end = Point(
                    min(viewport.bottomRight.x, surface.width.toDouble()),
                    linePosition,
                ),
            )
        }

    internal fun GridAxis.normalFadeStart(
        surface: GridRenderSurface,
        viewport: Rectangle,
        textMode: TextMode,
        linePosition: Double,
        lineIDString: String,
        mapIdPadding: Point,
        gridLinePadding: Point,
        labelFont: java.awt.Font,
    ): Double =
        labelLayout(
            surface = surface,
            viewport = viewport,
            textMode = textMode,
            axis = opposite(),
            linePosition = linePosition,
            lineIDString = lineIDString,
            mapIdPadding = mapIdPadding,
            gridLinePadding = gridLinePadding,
            labelFont = labelFont,
        ).gutterEdge

    internal fun GridAxis.labelColour(
        linePosition: Double,
        viewport: Rectangle,
        textMode: TextMode,
        gridLinePadding: Point,
        color: Color,
        edgeFadeDistance: Double,
    ): Color =
        when (this) {
            GridAxis.VERTICAL -> verticalLabelColour(
                color = color,
                screenX = linePosition - viewport.topLeft.x,
                textMode = textMode,
                gridLinePadding = gridLinePadding,
                edgeFadeDistance = edgeFadeDistance,
            )

            GridAxis.HORIZONTAL -> horizontalLabelColour(
                color = color,
                screenY = linePosition - viewport.topLeft.y,
                textMode = textMode,
                gridLinePadding = gridLinePadding,
                edgeFadeDistance = edgeFadeDistance,
            )
        }

    private fun verticalLabelColour(
        color: Color,
        screenX: Double,
        textMode: TextMode,
        gridLinePadding: Point,
        edgeFadeDistance: Double,
    ): Color =
        when (textMode) {
            TextMode.IMAGE -> color
            TextMode.STICKY_AXES -> color.withAlpha(axisOpacity(screenX, gridLinePadding.x, edgeFadeDistance))
        }

    private fun horizontalLabelColour(
        color: Color,
        screenY: Double,
        textMode: TextMode,
        gridLinePadding: Point,
        edgeFadeDistance: Double,
    ): Color =
        when (textMode) {
            TextMode.IMAGE -> color
            TextMode.STICKY_AXES -> color.withAlpha(axisOpacity(screenY, gridLinePadding.y, edgeFadeDistance))
        }

    private fun Color.withAlpha(opacity: Double): Color =
        Color(red, green, blue, (alpha * opacity).toInt().coerceIn(0, 255))

    private const val EDGE_FADE_DISTANCE = 60.0
    private const val LINE_SEGMENT_LENGTH = 1.0
}
