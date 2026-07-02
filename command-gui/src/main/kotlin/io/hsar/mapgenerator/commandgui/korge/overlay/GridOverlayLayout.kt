package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.grid.MgrsCoord
import io.hsar.mapgenerator.image.Palette
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import kotlin.math.abs
import kotlin.math.absoluteValue
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
        val viewportWidth = (state.visibleCanvasRight - state.visibleCanvasLeft).coerceAtLeast(1.0)
        val viewportHeight = (state.visibleCanvasBottom - state.visibleCanvasTop).coerceAtLeast(1.0)
        val edgeFadeDistance = GridStyles.fadeDistance(viewportWidth.toInt(), viewportHeight.toInt())
        val mapLeft = state.mapOffsetX
        val mapTop = state.mapOffsetY
        val mapRight = mapLeft + state.pixelWidth * state.scale
        val mapBottom = mapTop + state.pixelHeight * state.scale
        val mapIdPadding = GridStyles.mapIdPadding(viewportWidth.toInt(), viewportHeight.toInt())
        val gridLinePadding = GridStyles.gridLineIdPadding(viewportWidth.toInt(), viewportHeight.toInt())
        val mapIdFont = GridStyles.mapIdFont(viewportWidth.toInt(), viewportHeight.toInt()).first to layer.colour
        val minorAxisFont = GridStyles.gridLineIdFont(viewportWidth.toInt(), viewportHeight.toInt()).first to layer.colour
        val majorAxisFont = GridStyles.majorGridLineIdFont(viewportWidth.toInt(), viewportHeight.toInt()).first to layer.colour
        val mapIdStyle = GridLabelStyle(MapViewerFontRole.STRONG, mapIdFont.first.size2D.toDouble(), mapIdFont.second)
        val minorAxisStyle = GridLabelStyle(MapViewerFontRole.MAIN, minorAxisFont.first.size2D.toDouble(), minorAxisFont.second)
        val majorAxisStyle = GridLabelStyle(MapViewerFontRole.STRONG, majorAxisFont.first.size2D.toDouble(), majorAxisFont.second)
        val referenceColor = state.activeReference?.color ?: Palette.Colours.RED
        val referenceStyle = GridLabelStyle(MapViewerFontRole.MAIN, minorAxisFont.first.size2D.toDouble(), referenceColor)
        val mapIdBounds = measureText(layer.mapId, mapIdStyle)
        val axisBounds = measureText("0".repeat(layer.detailLevel), minorAxisStyle)

        val topGutterY = max(state.visibleCanvasTop + mapIdPadding.y, mapTop + mapIdPadding.y)
        val leftGutterX = max(state.visibleCanvasLeft + gridLinePadding.x, mapLeft + gridLinePadding.x)
        val mapIdCenterX = leftGutterX
        val mapIdCenterY = topGutterY
        val yAxisRightEdge = leftGutterX + axisBounds.width / 2
        val xAxisBottomEdge = topGutterY + axisBounds.height / 2
        val xLabelFadeStart = max(yAxisRightEdge, mapIdCenterX + mapIdBounds.width / 2)
        val yLabelFadeStart = max(xAxisBottomEdge, mapIdCenterY + mapIdBounds.height / 2)
        val fineMode = isFineMode(state, layer)
        val linePlacement = GridLinePlacementBuilder.build(state.viewport, layer)

        val labels = mutableListOf<GridLabel>()
        val lineRects = mutableListOf<GridRect>()
        val overlayArrows = mutableListOf<GridArrow>()

        labels += GridLabel(
            text = layer.mapId,
            x = mapIdCenterX,
            y = mapIdCenterY,
            style = mapIdStyle,
        )

        linePlacement.verticalLines.forEach { line ->
            val lineId = line.id
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineCanvasX = mapLeft + line.mapPosition * state.scale
            val labelAlpha = edgeOpacity(lineCanvasX - xLabelFadeStart, edgeFadeDistance)
            val isMajorLine = isMajorLine(lineId)

            if (shouldRenderVerticalLabel(fineMode, lineId, isMajorLine)) {
                val labelStyle = if (isMajorLine) majorAxisStyle else minorAxisStyle
                labels += GridLabel(
                    text = lineText,
                    x = lineCanvasX,
                    y = mapIdCenterY,
                    style = labelStyle.copy(color = labelStyle.color.withAlpha(labelAlpha)),
                )
            }

            lineRects += buildVerticalLineRects(
                screenX = lineCanvasX,
                mapLeft = mapLeft,
                mapTop = mapTop,
                mapBottom = mapBottom,
                gutterBottom = xAxisBottomEdge,
                color = layer.colour.withAlpha(edgeOpacity(lineCanvasX - yAxisRightEdge, edgeFadeDistance)),
                width = if (isMajorLine) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                scale = state.scale,
                edgeFadeDistance = edgeFadeDistance,
            )
        }

        linePlacement.horizontalLines.forEach { line ->
            val lineId = line.id
            val lineText = lineId.padZeroes(layer.detailLevel)
            val lineCanvasY = mapTop + line.mapPosition * state.scale
            val labelAlpha = edgeOpacity(lineCanvasY - yLabelFadeStart, edgeFadeDistance)
            val isMajorLine = isMajorLine(lineId)
            val labelStyle = if (isMajorLine) majorAxisStyle else minorAxisStyle

            labels += GridLabel(
                text = lineText,
                x = leftGutterX,
                y = lineCanvasY,
                style = labelStyle.copy(color = labelStyle.color.withAlpha(labelAlpha)),
            )

            lineRects += buildHorizontalLineRects(
                screenY = lineCanvasY,
                mapLeft = mapLeft,
                mapTop = mapTop,
                mapRight = mapRight,
                gutterRight = yAxisRightEdge,
                color = layer.colour.withAlpha(edgeOpacity(lineCanvasY - xAxisBottomEdge, edgeFadeDistance)),
                width = if (isMajorLine) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                scale = state.scale,
                edgeFadeDistance = edgeFadeDistance,
            )
        }

        state.activeReference?.let { reference ->
            val lineAlpha = edgeOpacity(reference.screenX - yAxisRightEdge, edgeFadeDistance)
            val rowAlpha = edgeOpacity(reference.screenY - xAxisBottomEdge, edgeFadeDistance)
            val referenceBounds = measureText(reference.text, referenceStyle)

            lineRects += buildVerticalLineRects(
                screenX = reference.screenX,
                mapLeft = mapLeft,
                mapTop = mapTop,
                mapBottom = mapBottom,
                gutterBottom = xAxisBottomEdge,
                color = referenceColor.withAlpha(lineAlpha),
                width = ACTIVE_REFERENCE_LINE_THICKNESS,
                scale = state.scale,
                edgeFadeDistance = edgeFadeDistance,
            )
            lineRects += buildHorizontalLineRects(
                screenY = reference.screenY,
                mapLeft = mapLeft,
                mapTop = mapTop,
                mapRight = mapRight,
                gutterRight = yAxisRightEdge,
                color = referenceColor.withAlpha(rowAlpha),
                width = ACTIVE_REFERENCE_LINE_THICKNESS,
                scale = state.scale,
                edgeFadeDistance = edgeFadeDistance,
            )
            labels += GridLabel(
                text = reference.text,
                x = reference.screenX + ACTIVE_REFERENCE_LABEL_OFFSET_X + referenceBounds.width / 2,
                y = reference.screenY - ACTIVE_REFERENCE_LABEL_OFFSET_Y - referenceBounds.height / 2,
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
                    color = referenceColor,
                )
                val distanceBounds = measureText(measurement.distanceText, referenceStyle)
                val bearingBounds = measureText(measurement.bearingText, referenceStyle)
                val tooltipX = reference.screenX + ACTIVE_REFERENCE_LABEL_OFFSET_X
                val tooltipY = reference.screenY + ACTIVE_REFERENCE_LABEL_OFFSET_Y
                labels += GridLabel(
                    text = measurement.distanceText,
                    x = tooltipX + distanceBounds.width / 2,
                    y = tooltipY + distanceBounds.height / 2,
                    style = referenceStyle,
                    shadows = accentLabelShadows(),
                )
                labels += GridLabel(
                    text = measurement.bearingText,
                    x = tooltipX + bearingBounds.width / 2,
                    y = tooltipY + distanceBounds.height.absoluteValue + MEASUREMENT_TOOLTIP_LINE_GAP + bearingBounds.height / 2,
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
        screenX: Double,
        mapLeft: Double,
        mapTop: Double,
        mapBottom: Double,
        gutterBottom: Double,
        color: Color,
        width: Double,
        scale: Double,
        edgeFadeDistance: Double,
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
            edgeFadeDistance = edgeFadeDistance,
            mapLeft = mapLeft,
            mapTop = mapTop,
            scale = scale,
        ) { segmentStart, segmentEnd, segmentAlpha ->
            val lineWidth = (width / scale).coerceAtLeast(0.25)
            val localX = (screenX - mapLeft) / scale
            GridRect(
                x = localX - lineWidth / 2,
                y = (segmentStart - mapTop) / scale,
                width = lineWidth,
                height = (segmentEnd - segmentStart) / scale,
                color = color.withAlpha(segmentAlpha),
            )
        }
    }

    private fun buildHorizontalLineRects(
        screenY: Double,
        mapLeft: Double,
        mapTop: Double,
        mapRight: Double,
        gutterRight: Double,
        color: Color,
        width: Double,
        scale: Double,
        edgeFadeDistance: Double,
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
            edgeFadeDistance = edgeFadeDistance,
            mapLeft = mapLeft,
            mapTop = mapTop,
            scale = scale,
        ) { segmentStart, segmentEnd, segmentAlpha ->
            val lineHeight = (width / scale).coerceAtLeast(0.25)
            val localY = (screenY - mapTop) / scale
            GridRect(
                x = (segmentStart - mapLeft) / scale,
                y = localY - lineHeight / 2,
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
        edgeFadeDistance: Double,
        mapLeft: Double,
        mapTop: Double,
        scale: Double,
        create: (Double, Double, Double) -> GridRect,
    ): List<GridRect> {
        val length = lineEnd - lineStart
        if (length <= 0.0 || normalAlpha <= 0.0) {
            return emptyList()
        }

        val fadeEnd = min(lineEnd, lineStart + edgeFadeDistance)
        val fadeLength = fadeEnd - lineStart
        val fadeSegmentCount = max(1, (fadeLength / LINE_SEGMENT_LENGTH).roundToInt())
        val fadeStep = fadeLength / fadeSegmentCount
        val fadedRects = (0 until fadeSegmentCount).mapNotNull { segmentIndex ->
            val segmentStart = lineStart + segmentIndex * fadeStep
            val segmentEnd = if (segmentIndex == fadeSegmentCount - 1) fadeEnd else segmentStart + fadeStep
            val segmentCenter = (segmentStart + segmentEnd) / 2
            val alpha = lineSegmentOpacity(segmentCenter, lineStart, edgeFadeDistance) * normalAlpha

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

    private fun isMajorLine(lineId: Double): Boolean =
        abs(lineId % MAJOR_LINE_DIVISOR) < 1e-6

    internal fun edgeOpacity(distanceFromEdge: Double, edgeFadeDistance: Double): Double =
        1.0 - (1.0 - (distanceFromEdge / edgeFadeDistance).coerceIn(0.0, 1.0)).let { it * it }

    private fun lineSegmentOpacity(position: Double, start: Double, edgeFadeDistance: Double): Double =
        edgeOpacity(position - start, edgeFadeDistance)

    private fun Color.withAlpha(opacity: Double): Color =
        Color(red, green, blue, (alpha * opacity).roundToInt().coerceIn(0, 255))

    private fun displayLayer(state: GridOverlayState): GridLayer {
        val layer = state.gridLayer
        return if (shouldUseFineGrid(state, layer)) {
            GridLayer(
                mapId = layer.mapId,
                referenceSystem = layer.referenceSystem,
                gridScale = (layer.gridScale / FINE_GRID_SUBDIVISIONS).coerceAtLeast(1),
                metresPerPixel = layer.metresPerPixel,
                detailLevel = layer.detailLevel + 1,
                pixelsPerLine = layer.pixelsPerLine / FINE_GRID_SUBDIVISIONS,
                verticalLineNumberStart = MgrsCoord.fromMetres(layer.verticalLineNumberStart.metres),
                horizontalLineNumberStart = MgrsCoord.fromMetres(layer.horizontalLineNumberStart.metres),
                verticalLineCount = layer.verticalLineCount * FINE_GRID_SUBDIVISIONS,
                horizontalLineCount = layer.horizontalLineCount * FINE_GRID_SUBDIVISIONS,
                colour = layer.colour,
            )
        } else {
            layer
        }
    }

    private fun shouldUseFineGrid(state: GridOverlayState, layer: GridLayer): Boolean =
        layer.pixelsPerLine * state.scale >= FINE_GRID_THRESHOLD_PX

    private fun isFineMode(state: GridOverlayState, layer: GridLayer): Boolean =
        layer.gridScale < state.gridLayer.gridScale

    private fun shouldRenderVerticalLabel(fineMode: Boolean, lineId: Double, isMajorLine: Boolean): Boolean =
        !fineMode || isMajorLine || isEvenFineLine(lineId)

    private fun isEvenFineLine(lineId: Double): Boolean =
        abs(lineId % 2.0) < 1e-6

    private fun accentLabelShadows(): List<GridLabelShadow> =
        listOf(
            GridLabelShadow(1.0, 1.0, Palette.Colours.MAIN),
        )

    private fun measureText(text: String, style: GridLabelStyle): Rectangle =
        MapViewerTextViews.measure(text, style.role, style.size).let { bounds ->
            Rectangle(Point.ORIGIN, Point(bounds.width, bounds.height))
        }

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
