package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
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
        surface.drawString(
            Point(
                textX(GridStyles.MAP_ID_PADDING.x, viewport.topLeft.x, textMode),
                textY(GridStyles.MAP_ID_PADDING.y, viewport.topLeft.y, textMode),
            ),
            layer.mapId,
            GridStyles.MAP_ID_FONT,
        )

        drawGridLines(surface, layer, viewport, textMode, GridAxis.VERTICAL)
        drawGridLines(surface, layer, viewport, textMode, GridAxis.HORIZONTAL)
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
    ) {
        val (startIndex, endIndex) = axis.lineIndexRange(viewport, layer)

        if (startIndex > endIndex) {
            return
        }

        (startIndex..endIndex).forEach { lineIndex ->
            val lineID = axis.lineId(layer, lineIndex)
            val lineIDString = lineID.padZeroes(layer.detailLevel)
            val linePosition = axis.linePosition(layer, lineIndex)
            val labelLayout = labelLayout(surface, viewport, textMode, axis, linePosition, lineIDString)
            val labelColour = axis.labelColour(linePosition, viewport, textMode)
            val lineBounds = axis.lineBounds(surface, viewport, linePosition, lineIDString)
            val width = if ((lineID % 10) == 0) 1.0f else 0.5f
            val lineNormalFadeStart = axis.normalFadeStart(surface, viewport, textMode, linePosition, lineIDString)

            if (lineBounds.isVisible(axis)) {
                surface.drawCenteredString(
                    point = labelLayout.point,
                    text = lineIDString,
                    fontAndColour = GridStyles.GRID_LINE_ID_FONT.first to labelColour,
                )
                drawLineWithOptionalFade(
                    surface = surface,
                    start = lineBounds.start,
                    end = lineBounds.end,
                    width = width,
                    color = GridStyles.GRID_LINE_COLOUR,
                    textMode = textMode,
                    fadeStart = labelLayout.gutterEdge,
                    lineNormal = linePosition,
                    lineNormalFadeStart = lineNormalFadeStart,
                    axis = axis,
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
                val normalOpacity = lineNormalOpacity(lineNormal, lineNormalFadeStart)

                val length = lineEnd - lineStart
                val segmentCount = max(1, (length / LINE_SEGMENT_LENGTH).roundToInt())
                val step = length / segmentCount

                (0 until segmentCount).forEach { segmentIndex ->
                    val segmentStart = lineStart + segmentIndex * step
                    val segmentEnd = if (segmentIndex == segmentCount - 1) lineEnd else segmentStart + step
                    val segmentCenter = (segmentStart + segmentEnd) / 2
                    val opacity = lineSegmentOpacity(segmentCenter, lineStart, lineEnd) * normalOpacity

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
        max(1, ceil((viewportPosition - padding) / pixelsPerLine).toInt())

    private fun endIndexRange(viewportPosition: Double, count: Int, pixelsPerLine: Double, padding: Double): Int =
        min(count, floor((viewportPosition - padding) / pixelsPerLine).toInt())

    internal fun labelLayout(
        surface: GridRenderSurface,
        viewport: Rectangle,
        textMode: TextMode,
        axis: GridAxis,
        linePosition: Double,
        lineIDString: String,
    ): LabelLayout {
        val textSize = surface.getTextSize(lineIDString, GridStyles.GRID_LINE_ID_FONT.first)
        return when (axis) {
            GridAxis.VERTICAL -> {
                val labelY = textY(
                    GridStyles.MAP_ID_PADDING.y,
                    viewport.topLeft.y,
                    textMode,
                )
                LabelLayout(
                    point = Point(linePosition, labelY),
                    gutterEdge = labelY + textSize.height / 2,
                )
            }

            GridAxis.HORIZONTAL -> {
                val labelX = textX(GridStyles.GRID_LINE_ID_PADDING.x, viewport.topLeft.x, textMode)
                LabelLayout(
                    point = Point(labelX, linePosition),
                    gutterEdge = labelX + textSize.width / 2,
                )
            }
        }
    }

    private fun axisOpacity(value: Double, stickyEdge: Double): Double =
        edgeOpacity(value - stickyEdge)

    internal fun edgeOpacity(distanceFromEdge: Double): Double =
        1.0 - (1.0 - (distanceFromEdge / EDGE_FADE_DISTANCE).coerceIn(0.0, 1.0)).let { it * it }

    private fun lineSegmentOpacity(position: Double, start: Double, end: Double): Double {
        val distanceFromStart = position - start
        val distanceFromEnd = end - position
        return min(edgeOpacity(distanceFromStart), edgeOpacity(distanceFromEnd))
    }

    internal fun lineNormalOpacity(lineNormal: Double, lineNormalFadeStart: Double): Double =
        edgeOpacity(lineNormal - lineNormalFadeStart)

    private fun GridAxis.opposite(): GridAxis =
        when (this) {
            GridAxis.VERTICAL -> GridAxis.HORIZONTAL
            GridAxis.HORIZONTAL -> GridAxis.VERTICAL
        }

    private fun GridAxis.lineIndexRange(viewport: Rectangle, layer: GridLayer): Pair<Int, Int> =
        when (this) {
            GridAxis.VERTICAL -> startIndexRange(
                viewportPosition = viewport.topLeft.x,
                pixelsPerLine = layer.pixelsPerLine,
                padding = GridStyles.GRID_LINE_ID_PADDING.x,
            ) to endIndexRange(
                viewportPosition = viewport.topRight.x,
                count = layer.verticalLineCount,
                pixelsPerLine = layer.pixelsPerLine,
                padding = GridStyles.GRID_LINE_ID_PADDING.x,
            )

            GridAxis.HORIZONTAL -> startIndexRange(
                viewportPosition = viewport.topLeft.y,
                pixelsPerLine = layer.pixelsPerLine,
                padding = GridStyles.GRID_LINE_ID_PADDING.y,
            ) to endIndexRange(
                viewportPosition = viewport.bottomRight.y,
                count = layer.horizontalLineCount,
                pixelsPerLine = layer.pixelsPerLine,
                padding = GridStyles.GRID_LINE_ID_PADDING.y,
            )
        }

    private fun GridAxis.lineId(layer: GridLayer, lineIndex: Int): Int =
        when (this) {
            GridAxis.VERTICAL -> layer.verticalLineNumberStart + lineIndex
            GridAxis.HORIZONTAL -> layer.horizontalLineNumberStart - lineIndex
        }

    private fun GridAxis.linePosition(layer: GridLayer, lineIndex: Int): Double =
        when (this) {
            GridAxis.VERTICAL -> GridStyles.GRID_LINE_ID_PADDING.x + lineIndex * layer.pixelsPerLine
            GridAxis.HORIZONTAL -> GridStyles.GRID_LINE_ID_PADDING.y + lineIndex * layer.pixelsPerLine
        }

    private fun GridAxis.lineBounds(
        surface: GridRenderSurface,
        viewport: Rectangle,
        linePosition: Double,
        lineIDString: String,
    ): LineBounds =
        when (this) {
            GridAxis.VERTICAL -> LineBounds(
                start = Point(
                    linePosition,
                    max(
                        viewport.topLeft.y,
                        GridStyles.GRID_LINE_ID_PADDING.y + surface.getTextSize(lineIDString, GridStyles.GRID_LINE_ID_FONT.first).height,
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
                        GridStyles.GRID_LINE_ID_PADDING.x + surface.getTextSize(lineIDString, GridStyles.GRID_LINE_ID_FONT.first).width,
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
    ): Double =
        labelLayout(surface, viewport, textMode, opposite(), linePosition, lineIDString).gutterEdge

    internal fun GridAxis.labelColour(linePosition: Double, viewport: Rectangle, textMode: TextMode): Color =
        when (this) {
            GridAxis.VERTICAL -> verticalLabelColour(
                color = GridStyles.GRID_LINE_ID_FONT.second,
                screenX = linePosition - viewport.topLeft.x,
                textMode = textMode,
            )

            GridAxis.HORIZONTAL -> horizontalLabelColour(
                color = GridStyles.GRID_LINE_ID_FONT.second,
                screenY = linePosition - viewport.topLeft.y,
                textMode = textMode,
            )
        }

    private fun verticalLabelColour(color: Color, screenX: Double, textMode: TextMode): Color =
        when (textMode) {
            TextMode.IMAGE -> color
            TextMode.STICKY_AXES -> color.withAlpha(axisOpacity(screenX, GridStyles.GRID_LINE_ID_PADDING.x))
        }

    private fun horizontalLabelColour(color: Color, screenY: Double, textMode: TextMode): Color =
        when (textMode) {
            TextMode.IMAGE -> color
            TextMode.STICKY_AXES -> color.withAlpha(axisOpacity(screenY, GridStyles.GRID_LINE_ID_PADDING.y))
        }

    private fun Color.withAlpha(opacity: Double): Color =
        Color(red, green, blue, (alpha * opacity).toInt().coerceIn(0, 255))

    private const val EDGE_FADE_DISTANCE = 60.0
    private const val LINE_SEGMENT_LENGTH = 1.0
}
