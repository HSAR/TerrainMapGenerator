package io.hsar.mapgenerator.commandgui.korge.render

import io.hsar.mapgenerator.commandgui.korge.interaction.GridOverlayState
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class GridOverlayLayout(
    val lineRects: List<GridRect>,
    val labels: List<GridLabel>,
)

internal data class GridRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val color: Color,
)

internal data class GridLabel(
    val text: String,
    val x: Double,
    val y: Double,
    val font: Font,
    val color: Color,
)

internal object GridOverlayLayoutBuilder {
    fun build(
        state: GridOverlayState,
        measureText: (String, Font) -> Rectangle = AwtGridTextMeasurer::measure,
    ): GridOverlayLayout {
        val layer = state.gridLayer
        val mapLeft = state.mapOffsetX
        val mapTop = state.mapOffsetY
        val mapRight = mapLeft + state.pixelWidth * state.scale
        val mapBottom = mapTop + state.pixelHeight * state.scale
        val mapIdBounds = measureText(layer.mapId, GridStyles.MAP_ID_FONT.first)
        val axisBounds = measureText("0".repeat(layer.detailLevel), GridStyles.GRID_LINE_ID_FONT.first)

        val topGutterY = max(state.visibleCanvasTop + GridStyles.MAP_ID_PADDING.y, mapTop + GridStyles.MAP_ID_PADDING.y)
        val leftGutterX = max(state.visibleCanvasLeft + GridStyles.GRID_LINE_ID_PADDING.x, mapLeft + GridStyles.GRID_LINE_ID_PADDING.x)
        val mapIdX = max(state.visibleCanvasLeft + GridStyles.MAP_ID_PADDING.x, mapLeft + GridStyles.MAP_ID_PADDING.x)
        val mapIdY = topGutterY
        val yAxisRightEdge = leftGutterX + axisBounds.width / 2
        val xAxisBottomEdge = topGutterY + axisBounds.height / 2
        val xLabelFadeStart = max(yAxisRightEdge, mapIdX + mapIdBounds.width)
        val yLabelFadeStart = max(xAxisBottomEdge, mapIdY + mapIdBounds.height)

        val labels = mutableListOf<GridLabel>()
        val lineRects = mutableListOf<GridRect>()

        labels += GridLabel(
            text = layer.mapId,
            x = mapIdX,
            y = mapIdY,
            font = GridStyles.MAP_ID_FONT.first,
            color = GridStyles.MAP_ID_FONT.second,
        )

        verticalLineIndices(state.viewport, layer).forEach { lineIndex ->
            val lineId = layer.verticalLineNumberStart + lineIndex
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineMapX = GridStyles.GRID_LINE_ID_PADDING.x + lineIndex * layer.pixelsPerLine
            val lineCanvasX = mapLeft + lineMapX * state.scale
            val labelAlpha = edgeOpacity(lineCanvasX - xLabelFadeStart)
            val lineAlpha = edgeOpacity(lineCanvasX - yAxisRightEdge)

            labels += GridLabel(
                text = lineText,
                x = lineCanvasX - axisBounds.width / 2,
                y = topGutterY - axisBounds.height / 2,
                font = GridStyles.GRID_LINE_ID_FONT.first,
                color = GridStyles.GRID_LINE_ID_FONT.second.withAlpha(labelAlpha),
            )

            lineRects += buildVerticalLineRects(
                x = lineCanvasX,
                mapTop = mapTop,
                mapBottom = mapBottom,
                gutterBottom = xAxisBottomEdge,
                color = GridStyles.GRID_LINE_COLOUR.withAlpha(lineAlpha),
                width = if (lineId % 10 == 0) 1.0 else 0.5,
            )
        }

        horizontalLineIndices(state.viewport, layer).forEach { lineIndex ->
            val lineId = layer.horizontalLineNumberStart - lineIndex
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineMapY = GridStyles.GRID_LINE_ID_PADDING.y + lineIndex * layer.pixelsPerLine
            val lineCanvasY = mapTop + lineMapY * state.scale
            val labelAlpha = edgeOpacity(lineCanvasY - yLabelFadeStart)
            val lineAlpha = edgeOpacity(lineCanvasY - xAxisBottomEdge)

            labels += GridLabel(
                text = lineText,
                x = leftGutterX - axisBounds.width / 2,
                y = lineCanvasY - axisBounds.height / 2,
                font = GridStyles.GRID_LINE_ID_FONT.first,
                color = GridStyles.GRID_LINE_ID_FONT.second.withAlpha(labelAlpha),
            )

            lineRects += buildHorizontalLineRects(
                y = lineCanvasY,
                mapLeft = mapLeft,
                mapRight = mapRight,
                gutterRight = yAxisRightEdge,
                color = GridStyles.GRID_LINE_COLOUR.withAlpha(lineAlpha),
                width = if (lineId % 10 == 0) 1.0 else 0.5,
            )
        }

        return GridOverlayLayout(
            lineRects = lineRects,
            labels = labels,
        )
    }

    private fun buildVerticalLineRects(
        x: Double,
        mapTop: Double,
        mapBottom: Double,
        gutterBottom: Double,
        color: Color,
        width: Double,
    ): List<GridRect> {
        val start = max(mapTop, gutterBottom)
        val end = mapBottom
        if (end <= start) {
            return emptyList()
        }

        return segmentedRects(
            lineStart = start,
            lineEnd = end,
            color = color,
            normalAlpha = color.alpha / 255.0,
        ) { segmentStart, segmentEnd, segmentAlpha ->
            GridRect(
                x = x - width / 2,
                y = segmentStart,
                width = width.coerceAtLeast(1.0),
                height = max(1.0, segmentEnd - segmentStart),
                color = color.withAlpha(segmentAlpha),
            )
        }
    }

    private fun buildHorizontalLineRects(
        y: Double,
        mapLeft: Double,
        mapRight: Double,
        gutterRight: Double,
        color: Color,
        width: Double,
    ): List<GridRect> {
        val start = max(mapLeft, gutterRight)
        val end = mapRight
        if (end <= start) {
            return emptyList()
        }

        return segmentedRects(
            lineStart = start,
            lineEnd = end,
            color = color,
            normalAlpha = color.alpha / 255.0,
        ) { segmentStart, segmentEnd, segmentAlpha ->
            GridRect(
                x = segmentStart,
                y = y - width / 2,
                width = max(1.0, segmentEnd - segmentStart),
                height = width.coerceAtLeast(1.0),
                color = color.withAlpha(segmentAlpha),
            )
        }
    }

    private fun segmentedRects(
        lineStart: Double,
        lineEnd: Double,
        color: Color,
        normalAlpha: Double,
        create: (Double, Double, Double) -> GridRect,
    ): List<GridRect> {
        val length = lineEnd - lineStart
        val segmentCount = max(1, (length / LINE_SEGMENT_LENGTH).roundToInt())
        val step = length / segmentCount

        return (0 until segmentCount).mapNotNull { segmentIndex ->
            val segmentStart = lineStart + segmentIndex * step
            val segmentEnd = if (segmentIndex == segmentCount - 1) lineEnd else segmentStart + step
            val segmentCenter = (segmentStart + segmentEnd) / 2
            val alpha = lineSegmentOpacity(segmentCenter, lineStart, lineEnd) * normalAlpha

            if (alpha <= 0.0) {
                null
            } else {
                create(segmentStart, segmentEnd, alpha)
            }
        }
    }

    private fun verticalLineIndices(viewport: Rectangle, layer: GridLayer): IntRange {
        val start = max(1, ceil((viewport.topLeft.x - GridStyles.GRID_LINE_ID_PADDING.x) / layer.pixelsPerLine).toInt())
        val end = min(layer.verticalLineCount, floor((viewport.topRight.x - GridStyles.GRID_LINE_ID_PADDING.x) / layer.pixelsPerLine).toInt())
        return if (start <= end) start..end else IntRange.EMPTY
    }

    private fun horizontalLineIndices(viewport: Rectangle, layer: GridLayer): IntRange {
        val start = max(1, ceil((viewport.topLeft.y - GridStyles.GRID_LINE_ID_PADDING.y) / layer.pixelsPerLine).toInt())
        val end = min(layer.horizontalLineCount, floor((viewport.bottomRight.y - GridStyles.GRID_LINE_ID_PADDING.y) / layer.pixelsPerLine).toInt())
        return if (start <= end) start..end else IntRange.EMPTY
    }

    internal fun edgeOpacity(distanceFromEdge: Double): Double =
        1.0 - (1.0 - (distanceFromEdge / EDGE_FADE_DISTANCE).coerceIn(0.0, 1.0)).let { it * it }

    private fun lineSegmentOpacity(position: Double, start: Double, end: Double): Double {
        val distanceFromStart = position - start
        val distanceFromEnd = end - position
        return min(edgeOpacity(distanceFromStart), edgeOpacity(distanceFromEnd))
    }

    private fun Color.withAlpha(opacity: Double): Color =
        Color(red, green, blue, (alpha * opacity).roundToInt().coerceIn(0, 255))

    private const val EDGE_FADE_DISTANCE = 60.0
    private const val LINE_SEGMENT_LENGTH = 6.0
}

internal object AwtGridTextMeasurer {
    fun measure(text: String, font: Font): Rectangle {
        val graphics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        return try {
            graphics.font = font
            graphics.getFontMetrics(font)
                .let { Rectangle(Point.ORIGIN, Point(it.getStringBounds(text, graphics).width, -it.ascent.toDouble())) }
        } finally {
            graphics.dispose()
        }
    }
}
