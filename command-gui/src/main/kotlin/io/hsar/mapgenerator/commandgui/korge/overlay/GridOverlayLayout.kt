package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.image.Palette
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class GridOverlayLayout(
    val lineRects: List<GridRect>,
    val overlayArrows: List<GridArrow>,
    val labels: List<GridLabel>,
)

internal data class GridRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val color: Color,
)

internal data class GridArrow(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val color: Color,
)

internal data class GridLabel(
    val text: String,
    val x: Double,
    val y: Double,
    val style: GridLabelStyle,
    val shadows: List<GridLabelShadow> = emptyList(),
)

internal data class GridLabelStyle(
    val role: MapViewerFontRole,
    val size: Double,
    val color: Color,
)

internal data class GridLabelShadow(
    val offsetX: Double,
    val offsetY: Double,
    val color: Color,
)

internal object GridOverlayLayoutBuilder {
    fun build(
        state: GridOverlayState,
        measureText: (String, GridLabelStyle) -> Rectangle = ::measureText,
    ): GridOverlayLayout {
        val layer = displayLayer(state)
        val mapLeft = state.mapOffsetX
        val mapTop = state.mapOffsetY
        val mapRight = mapLeft + state.pixelWidth * state.scale
        val mapBottom = mapTop + state.pixelHeight * state.scale
        val mapIdStyle = GridLabelStyle(MapViewerFontRole.STRONG, GridStyles.MAP_ID_FONT.first.size2D.toDouble(), GridStyles.MAP_ID_FONT.second)
        val minorAxisStyle = GridLabelStyle(MapViewerFontRole.MAIN, GridStyles.GRID_LINE_ID_FONT.first.size2D.toDouble(), GridStyles.GRID_LINE_ID_FONT.second)
        val majorAxisStyle = GridLabelStyle(MapViewerFontRole.STRONG, GridStyles.MAJOR_GRID_LINE_ID_FONT.first.size2D.toDouble(), GridStyles.MAJOR_GRID_LINE_ID_FONT.second)
        val referenceStyle = GridLabelStyle(MapViewerFontRole.MAIN, GridStyles.GRID_LINE_ID_FONT.first.size2D.toDouble(), Palette.Colours.RED)
        val mapIdBounds = measureText(layer.mapId, mapIdStyle)
        val axisBounds = measureText("0".repeat(layer.detailLevel), minorAxisStyle)

        val topGutterY = max(state.visibleCanvasTop + GridStyles.MAP_ID_PADDING.y, mapTop + GridStyles.MAP_ID_PADDING.y)
        val leftGutterX = max(state.visibleCanvasLeft + GridStyles.GRID_LINE_ID_PADDING.x, mapLeft + GridStyles.GRID_LINE_ID_PADDING.x)
        val mapIdX = max(state.visibleCanvasLeft + GridStyles.MAP_ID_PADDING.x, mapLeft + GridStyles.MAP_ID_PADDING.x)
        val labelCenterY = topGutterY
        val mapIdY = labelCenterY - mapIdBounds.height / 2
        val yAxisRightEdge = leftGutterX + axisBounds.width / 2
        val xAxisBottomEdge = topGutterY + axisBounds.height / 2
        val xLabelFadeStart = max(yAxisRightEdge, mapIdX + mapIdBounds.width)
        val yLabelFadeStart = max(xAxisBottomEdge, mapIdY + mapIdBounds.height)
        val fineMode = isFineMode(state, layer)

        val labels = mutableListOf<GridLabel>()
        val lineRects = mutableListOf<GridRect>()
        val overlayArrows = mutableListOf<GridArrow>()

        labels += GridLabel(
            text = layer.mapId,
            x = mapIdX,
            y = mapIdY,
            style = mapIdStyle,
        )

        verticalLineIndices(state.viewport, layer).forEach { lineIndex ->
            val lineId = layer.verticalLineNumberStart + lineIndex
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineMapX = GridStyles.GRID_LINE_ID_PADDING.x + lineIndex * layer.pixelsPerLine
            val lineCanvasX = mapLeft + lineMapX * state.scale
            val labelAlpha = edgeOpacity(lineCanvasX - xLabelFadeStart)
            val lineAlpha = edgeOpacity(lineCanvasX - yAxisRightEdge)
            val isMajorLine = lineId % MAJOR_LINE_DIVISOR == 0

            if (shouldRenderVerticalLabel(fineMode, lineIndex, isMajorLine)) {
                val labelStyle = if (isMajorLine) majorAxisStyle else minorAxisStyle
                val labelBounds = measureText(lineText, labelStyle)
                labels += GridLabel(
                    text = lineText,
                    x = lineCanvasX - labelBounds.width / 2,
                    y = labelCenterY - labelBounds.height / 2,
                    style = labelStyle.copy(color = labelStyle.color.withAlpha(labelAlpha)),
                )
            }

            lineRects += buildVerticalLineRects(
                mapX = lineMapX,
                mapTop = mapTop,
                mapBottom = mapBottom,
                gutterBottom = xAxisBottomEdge,
                color = GridStyles.GRID_LINE_COLOUR.withAlpha(lineAlpha),
                width = if (isMajorLine) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                scale = state.scale,
            )
        }

        horizontalLineIndices(state.viewport, layer).forEach { lineIndex ->
            val lineId = layer.horizontalLineNumberStart - lineIndex
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineMapY = GridStyles.GRID_LINE_ID_PADDING.y + lineIndex * layer.pixelsPerLine
            val lineCanvasY = mapTop + lineMapY * state.scale
            val labelAlpha = edgeOpacity(lineCanvasY - yLabelFadeStart)
            val lineAlpha = edgeOpacity(lineCanvasY - xAxisBottomEdge)
            val isMajorLine = lineId % MAJOR_LINE_DIVISOR == 0
            val labelStyle = if (isMajorLine) majorAxisStyle else minorAxisStyle
            val labelBounds = measureText(lineText, labelStyle)

            labels += GridLabel(
                text = lineText,
                x = leftGutterX - labelBounds.width / 2,
                y = lineCanvasY - labelBounds.height / 2,
                style = labelStyle.copy(color = labelStyle.color.withAlpha(labelAlpha)),
            )

            lineRects += buildHorizontalLineRects(
                mapY = lineMapY,
                mapLeft = mapLeft,
                mapRight = mapRight,
                gutterRight = yAxisRightEdge,
                color = GridStyles.GRID_LINE_COLOUR.withAlpha(lineAlpha),
                width = if (isMajorLine) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                scale = state.scale,
            )
        }

        state.activeReference?.let { reference ->
            val lineAlpha = edgeOpacity(reference.screenX - yAxisRightEdge)
            val rowAlpha = edgeOpacity(reference.screenY - xAxisBottomEdge)
            val referenceBounds = measureText(reference.text, referenceStyle)

            lineRects += buildVerticalLineRects(
                mapX = (reference.screenX - mapLeft) / state.scale,
                mapTop = mapTop,
                mapBottom = mapBottom,
                gutterBottom = xAxisBottomEdge,
                color = Palette.Colours.RED.withAlpha(lineAlpha),
                width = ACTIVE_REFERENCE_LINE_THICKNESS,
                scale = state.scale,
            )
            lineRects += buildHorizontalLineRects(
                mapY = (reference.screenY - mapTop) / state.scale,
                mapLeft = mapLeft,
                mapRight = mapRight,
                gutterRight = yAxisRightEdge,
                color = Palette.Colours.RED.withAlpha(rowAlpha),
                width = ACTIVE_REFERENCE_LINE_THICKNESS,
                scale = state.scale,
            )
            labels += GridLabel(
                text = reference.text,
                x = reference.screenX + ACTIVE_REFERENCE_LABEL_OFFSET_X,
                y = reference.screenY - referenceBounds.height - ACTIVE_REFERENCE_LABEL_OFFSET_Y,
                style = referenceStyle,
                shadows = accentLabelShadows(),
            )

            state.measurement?.let { measurement ->
                val originScreenX = mapLeft + measurement.originMapX * state.scale
                val originScreenY = mapTop + measurement.originMapY * state.scale
                val destinationScreenX = mapLeft + measurement.destinationMapX * state.scale
                val destinationScreenY = mapTop + measurement.destinationMapY * state.scale
                overlayArrows += GridArrow(
                    x1 = originScreenX,
                    y1 = originScreenY,
                    x2 = destinationScreenX,
                    y2 = destinationScreenY,
                    color = Palette.Colours.RED,
                )
                val distanceBounds = measureText(measurement.distanceText, referenceStyle)
                val bearingBounds = measureText(measurement.bearingText, referenceStyle)
                val tooltipX = reference.screenX + ACTIVE_REFERENCE_LABEL_OFFSET_X
                val tooltipY = reference.screenY + ACTIVE_REFERENCE_LABEL_OFFSET_Y
                labels += GridLabel(
                    text = measurement.distanceText,
                    x = tooltipX,
                    y = tooltipY,
                    style = referenceStyle,
                    shadows = accentLabelShadows(),
                )
                labels += GridLabel(
                    text = measurement.bearingText,
                    x = tooltipX,
                    y = tooltipY + distanceBounds.height.absoluteValue + bearingBounds.height.absoluteValue + MEASUREMENT_TOOLTIP_LINE_GAP,
                    style = referenceStyle,
                    shadows = accentLabelShadows(),
                )
            }
        }

        return GridOverlayLayout(
            lineRects = lineRects,
            overlayArrows = overlayArrows,
            labels = labels,
        )
    }

    private fun buildVerticalLineRects(
        mapX: Double,
        mapTop: Double,
        mapBottom: Double,
        gutterBottom: Double,
        color: Color,
        width: Double,
        scale: Double,
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
            val lineWidth = width.coerceAtLeast(1.0) / scale
            GridRect(
                x = mapX - lineWidth / 2,
                y = (segmentStart - mapTop) / scale,
                width = lineWidth,
                height = (segmentEnd - segmentStart) / scale,
                color = color.withAlpha(segmentAlpha),
            )
        }
    }

    private fun buildHorizontalLineRects(
        mapY: Double,
        mapLeft: Double,
        mapRight: Double,
        gutterRight: Double,
        color: Color,
        width: Double,
        scale: Double,
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
            val lineHeight = width.coerceAtLeast(1.0) / scale
            GridRect(
                x = (segmentStart - mapLeft) / scale,
                y = mapY - lineHeight / 2,
                width = (segmentEnd - segmentStart) / scale,
                height = lineHeight,
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
        if (length <= 0.0 || normalAlpha <= 0.0) {
            return emptyList()
        }

        val fadeEnd = min(lineEnd, lineStart + EDGE_FADE_DISTANCE)
        val fadeLength = fadeEnd - lineStart
        val fadeSegmentCount = max(1, (fadeLength / LINE_SEGMENT_LENGTH).roundToInt())
        val fadeStep = fadeLength / fadeSegmentCount
        val fadedRects = (0 until fadeSegmentCount).mapNotNull { segmentIndex ->
            val segmentStart = lineStart + segmentIndex * fadeStep
            val segmentEnd = if (segmentIndex == fadeSegmentCount - 1) fadeEnd else segmentStart + fadeStep
            val segmentCenter = (segmentStart + segmentEnd) / 2
            val alpha = lineSegmentOpacity(segmentCenter, lineStart) * normalAlpha

            if (alpha <= 0.0) {
                null
            } else {
                create(segmentStart, segmentEnd, alpha)
            }
        }

        return if (fadeEnd >= lineEnd) {
            fadedRects
        } else {
            fadedRects + create(fadeEnd, lineEnd, normalAlpha)
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

    private fun lineSegmentOpacity(position: Double, start: Double): Double =
        edgeOpacity(position - start)

    private fun Color.withAlpha(opacity: Double): Color =
        Color(red, green, blue, (alpha * opacity).roundToInt().coerceIn(0, 255))

    private fun displayLayer(state: GridOverlayState): GridLayer {
        val layer = state.gridLayer
        return if (shouldUseFineGrid(state, layer)) {
            GridLayer(
                mapId = layer.mapId,
                gridScale = (layer.gridScale / FINE_GRID_SUBDIVISIONS).coerceAtLeast(1),
                metresPerPixel = layer.metresPerPixel,
                detailLevel = layer.detailLevel + 1,
                pixelsPerLine = layer.pixelsPerLine / FINE_GRID_SUBDIVISIONS,
                verticalLineNumberStart = layer.verticalLineNumberStart * FINE_GRID_SUBDIVISIONS,
                horizontalLineNumberStart = layer.horizontalLineNumberStart * FINE_GRID_SUBDIVISIONS,
                verticalLineCount = layer.verticalLineCount * FINE_GRID_SUBDIVISIONS,
                horizontalLineCount = layer.horizontalLineCount * FINE_GRID_SUBDIVISIONS,
            )
        } else {
            layer
        }
    }

    private fun shouldUseFineGrid(state: GridOverlayState, layer: GridLayer): Boolean =
        layer.pixelsPerLine * state.scale >= FINE_GRID_THRESHOLD_PX

    private fun isFineMode(state: GridOverlayState, layer: GridLayer): Boolean =
        layer.gridScale < state.gridLayer.gridScale

    private fun shouldRenderVerticalLabel(fineMode: Boolean, lineIndex: Int, isMajorLine: Boolean): Boolean =
        !fineMode || isMajorLine || lineIndex % 2 == 0

    private fun accentLabelShadows(): List<GridLabelShadow> =
        listOf(
            GridLabelShadow(1.0, 1.0, Palette.Colours.MAIN),
        )

    private fun measureText(text: String, style: GridLabelStyle): Rectangle =
        MapViewerTextViews.measure(text, style.role, style.size).let { bounds ->
            Rectangle(Point.ORIGIN, Point(bounds.width, bounds.height))
        }

    private const val EDGE_FADE_DISTANCE = 60.0
    private const val LINE_SEGMENT_LENGTH = 6.0
    private const val FINE_GRID_SUBDIVISIONS = 10
    private const val FINE_GRID_THRESHOLD_PX = 300.0
    private const val MAJOR_LINE_DIVISOR = 10
    private const val MAJOR_LINE_THICKNESS = 2.0
    private const val MINOR_LINE_THICKNESS = 1.0
    private const val ACTIVE_REFERENCE_LINE_THICKNESS = 2.0
    private const val ACTIVE_REFERENCE_LABEL_OFFSET_X = 12.0
    private const val ACTIVE_REFERENCE_LABEL_OFFSET_Y = 12.0
    private const val MEASUREMENT_TOOLTIP_LINE_GAP = 4.0
}
