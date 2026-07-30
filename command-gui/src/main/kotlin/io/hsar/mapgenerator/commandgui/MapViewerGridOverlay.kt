package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.EpsilonGridDesignators
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.grid.ReferenceSystem
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import java.util.Collections.emptyList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class MapViewerGridOverlayRequest(
    val gridLayer: GridLayer,
    val viewport: MapViewport,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val mapWidth: Int,
    val mapHeight: Int,
)

data class MapViewerGridLineSegment(
    val start: ViewerPoint,
    val end: ViewerPoint,
    val thickness: Double,
    val colour: Color,
)

data class MapViewerGridLabel(
    val text: String,
    val centre: ViewerPoint,
    val fontSize: Double,
    val strong: Boolean,
    val colour: Color,
)

data class MapViewerGridOverlay(
    val lineSegments: List<MapViewerGridLineSegment>,
    val labels: List<MapViewerGridLabel>,
)

/**
 * Builds the grid in screen space.
 *
 * Axis labels stay attached to the visible top and left gutters while their corresponding
 * world-space lines continue to pan beneath them. Vertical lines fade in below the top labels;
 * horizontal lines fade in to the right of the left labels. A second perpendicular fade keeps
 * lines and labels from visually colliding with the map ID in the top-left corner.
 */
object MapViewerGridOverlayLayoutBuilder {
    fun build(
        request: MapViewerGridOverlayRequest,
        measureText: (text: String, fontSize: Double, strong: Boolean) -> ViewerPoint = ::approximateTextSize,
    ): MapViewerGridOverlay {
        val viewport = request.viewport
        val layer = displayLayer(request)
        val viewportWidth = viewport.viewportWidth.toDouble()
        val viewportHeight = viewport.viewportHeight.toDouble()
        val mapLeft = viewport.screenX(request.mapOffsetX)
        val mapTop = viewport.screenY(request.mapOffsetY)
        val mapRight = viewport.screenX(request.mapOffsetX + request.mapWidth)
        val mapBottom = viewport.screenY(request.mapOffsetY + request.mapHeight)
        if (mapRight <= 0.0 || mapBottom <= 0.0 || mapLeft >= viewportWidth || mapTop >= viewportHeight) {
            return MapViewerGridOverlay(emptyList(), emptyList())
        }

        val fadeDistance = GridStyles.fadeDistance(viewport.viewportWidth, viewport.viewportHeight)
        val mapIdPadding = GridStyles.mapIdPadding(viewport.viewportWidth, viewport.viewportHeight)
        val gridPadding = GridStyles.gridLineIdPadding(viewport.viewportWidth, viewport.viewportHeight)
        val mapIdFontSize = GridStyles.mapIdFont(viewport.viewportWidth, viewport.viewportHeight).first.size2D.toDouble()
        val minorFontSize = GridStyles.gridLineIdFont(viewport.viewportWidth, viewport.viewportHeight).first.size2D.toDouble()
        val majorFontSize = GridStyles.majorGridLineIdFont(viewport.viewportWidth, viewport.viewportHeight).first.size2D.toDouble()
        val topGutterY = max(mapIdPadding.y, mapTop + mapIdPadding.y)
        val leftGutterX = max(gridPadding.x, mapLeft + gridPadding.x)
        val mapIdSize = measureText(layer.mapId, mapIdFontSize, true)
        val axisSize = measureText(axisMeasurementText(layer), minorFontSize, false)
        val yAxisRightEdge = leftGutterX + axisSize.x / 2.0
        val xAxisBottomEdge = topGutterY + axisSize.y / 2.0
        val xLabelFadeStart = max(yAxisRightEdge, leftGutterX + mapIdSize.x / 2.0)
        val yLabelFadeStart = max(xAxisBottomEdge, topGutterY + mapIdSize.y / 2.0)
        val placement = visibleLines(request, layer)
        val fineMode = layer.gridScale < request.gridLayer.gridScale
        val segments = mutableListOf<MapViewerGridLineSegment>()
        val labels = mutableListOf(
            MapViewerGridLabel(
                text = layer.mapId,
                centre = ViewerPoint(leftGutterX, topGutterY),
                fontSize = mapIdFontSize,
                strong = true,
                colour = layer.colour,
            ),
        )

        placement.vertical.forEach { line ->
            val screenX = viewport.screenX(request.mapOffsetX + line.position)
            val major = isMajorLine(line.id)
            if (shouldRenderLabel(layer.referenceSystem, fineMode, line.id, major)) {
                labels += MapViewerGridLabel(
                    text = verticalLineText(layer, request.gridLayer, line.id),
                    centre = ViewerPoint(screenX, topGutterY),
                    fontSize = if (major) majorFontSize else minorFontSize,
                    strong = major,
                    colour = layer.colour.withOpacity(edgeOpacity(screenX - xLabelFadeStart, fadeDistance)),
                )
            }
            segments += fadedLine(
                start = max(mapTop, xAxisBottomEdge),
                end = min(mapBottom, viewportHeight),
                fixed = screenX,
                vertical = true,
                thickness = if (major) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                colour = layer.colour.withOpacity(edgeOpacity(screenX - yAxisRightEdge, fadeDistance)),
                fadeDistance = fadeDistance,
            )
        }

        placement.horizontal.forEach { line ->
            val screenY = viewport.screenY(request.mapOffsetY + line.position)
            val major = isMajorLine(line.id)
            if (shouldRenderLabel(layer.referenceSystem, fineMode, line.id, major)) {
                labels += MapViewerGridLabel(
                    text = horizontalLineText(layer, request.gridLayer, line.id),
                    centre = ViewerPoint(leftGutterX, screenY),
                    fontSize = if (major) majorFontSize else minorFontSize,
                    strong = major,
                    colour = layer.colour.withOpacity(edgeOpacity(screenY - yLabelFadeStart, fadeDistance)),
                )
            }
            segments += fadedLine(
                start = max(mapLeft, yAxisRightEdge),
                end = min(mapRight, viewportWidth),
                fixed = screenY,
                vertical = false,
                thickness = if (major) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                colour = layer.colour.withOpacity(edgeOpacity(screenY - xAxisBottomEdge, fadeDistance)),
                fadeDistance = fadeDistance,
            )
        }

        return MapViewerGridOverlay(
            lineSegments = segments,
            labels = labels.filter { it.colour.alpha > 0 },
        )
    }

    fun edgeOpacity(distanceFromEdge: Double, fadeDistance: Double): Double =
        if (fadeDistance <= 0.0) {
            1.0
        } else {
            1.0 - (1.0 - (distanceFromEdge / fadeDistance).coerceIn(0.0, 1.0)).let { it * it }
        }

    private fun visibleLines(request: MapViewerGridOverlayRequest, layer: GridLayer): GridPlacement {
        val localLeft = request.viewport.left - request.mapOffsetX
        val localTop = request.viewport.top - request.mapOffsetY
        val localRight = localLeft + request.viewport.visibleWidth
        val localBottom = localTop + request.viewport.visibleHeight
        return GridPlacement(
            vertical = placedLines(localLeft, localRight, layer.verticalLineCount, verticalLineStart(layer), layer, descending = false),
            horizontal = placedLines(localTop, localBottom, layer.horizontalLineCount, horizontalLineStart(layer), layer, descending = true),
        )
    }

    private fun placedLines(
        viewportStart: Double,
        viewportEnd: Double,
        lineCount: Int,
        firstPosition: Double,
        layer: GridLayer,
        descending: Boolean,
    ): List<PlacedLine> {
        val startIndex = max(0, ceil((viewportStart - firstPosition) / layer.pixelsPerLine).toInt())
        val endIndex = min(lineCount, floor((viewportEnd - firstPosition) / layer.pixelsPerLine).toInt())
        if (startIndex > endIndex) return emptyList()
        val initialId = if (descending) {
            layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale)
        } else {
            layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale)
        }
        return (startIndex..endIndex).map { index ->
            PlacedLine(
                id = initialId + if (descending) -index else index,
                position = firstPosition + index * layer.pixelsPerLine,
            )
        }
    }

    private fun fadedLine(
        start: Double,
        end: Double,
        fixed: Double,
        vertical: Boolean,
        thickness: Double,
        colour: Color,
        fadeDistance: Double,
    ): List<MapViewerGridLineSegment> {
        if (end <= start || colour.alpha <= 0) return emptyList()
        val fadeEnd = min(end, start + fadeDistance)
        val fadeLength = fadeEnd - start
        val segmentCount = max(1, (fadeLength / LINE_SEGMENT_LENGTH).roundToInt())
        val step = fadeLength / segmentCount
        val faded = (0 until segmentCount).mapNotNull { index ->
            val segmentStart = start + index * step
            val segmentEnd = if (index == segmentCount - 1) fadeEnd else segmentStart + step
            val opacity = edgeOpacity((segmentStart + segmentEnd) / 2.0 - start, fadeDistance)
            colour.withOpacity(opacity).takeIf { it.alpha > 0 }?.let {
                lineSegment(segmentStart, segmentEnd, fixed, vertical, thickness, it)
            }
        }
        return if (fadeEnd >= end) faded
        else faded + lineSegment(fadeEnd, end, fixed, vertical, thickness, colour)
    }

    private fun lineSegment(
        start: Double,
        end: Double,
        fixed: Double,
        vertical: Boolean,
        thickness: Double,
        colour: Color,
    ) = MapViewerGridLineSegment(
        start = if (vertical) ViewerPoint(fixed, start) else ViewerPoint(start, fixed),
        end = if (vertical) ViewerPoint(fixed, end) else ViewerPoint(end, fixed),
        thickness = thickness,
        colour = colour,
    )

    private fun displayLayer(request: MapViewerGridOverlayRequest): GridLayer {
        val layer = request.gridLayer
        val threshold = if (layer.referenceSystem == ReferenceSystem.EPSILON) FINE_GRID_THRESHOLD_PX * 2.0 else FINE_GRID_THRESHOLD_PX
        return if (layer.pixelsPerLine * request.viewport.zoom >= threshold) {
            layer.copy(
                gridScale = (layer.gridScale / FINE_GRID_SUBDIVISIONS).coerceAtLeast(1),
                detailLevel = layer.detailLevel + 1,
                pixelsPerLine = layer.pixelsPerLine / FINE_GRID_SUBDIVISIONS,
                verticalLineNumberStart = layer.referenceSystem.coordFromUnits(layer.verticalLineNumberStart.preciseUnits),
                horizontalLineNumberStart = layer.referenceSystem.coordFromUnits(layer.horizontalLineNumberStart.preciseUnits),
                verticalLineCount = layer.verticalLineCount * FINE_GRID_SUBDIVISIONS,
                horizontalLineCount = layer.horizontalLineCount * FINE_GRID_SUBDIVISIONS,
            )
        } else {
            layer
        }
    }

    private fun verticalLineStart(layer: GridLayer) =
        (layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) - layer.verticalLineNumberStart.lineNumber(layer.gridScale)) *
                layer.pixelsPerLine

    private fun horizontalLineStart(layer: GridLayer) =
        (layer.horizontalLineNumberStart.lineNumber(layer.gridScale) - layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale)) *
                layer.pixelsPerLine

    private fun shouldRenderLabel(referenceSystem: ReferenceSystem, fineMode: Boolean, lineId: Double, major: Boolean) =
        when (referenceSystem) {
            ReferenceSystem.EPSILON -> !fineMode || major
            ReferenceSystem.MGRS -> !fineMode || major || abs(lineId % 2.0) < EPSILON
        }

    private fun verticalLineText(layer: GridLayer, baseLayer: GridLayer, id: Double) =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) {
            EpsilonGridDesignators.xDesignator(squareIndex(layer, baseLayer, id))
        } else {
            id.padZeroes(layer.detailLevel)
        }

    private fun horizontalLineText(layer: GridLayer, baseLayer: GridLayer, id: Double) =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) {
            EpsilonGridDesignators.yDesignator(squareIndex(layer, baseLayer, id))
        } else {
            id.padZeroes(layer.detailLevel)
        }

    private fun squareIndex(layer: GridLayer, baseLayer: GridLayer, id: Double) =
        Math.floorDiv((id * layer.gridScale).roundToInt(), baseLayer.gridScale)

    private fun axisMeasurementText(layer: GridLayer) =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) "AA" else "0".repeat(layer.detailLevel)

    private fun isMajorLine(id: Double) = abs(id % MAJOR_LINE_DIVISOR) < EPSILON

    private fun Color.withOpacity(opacity: Double) =
        Color(red, green, blue, (alpha * opacity).roundToInt().coerceIn(0, 255))

    private fun approximateTextSize(text: String, fontSize: Double, strong: Boolean) =
        ViewerPoint(text.length * fontSize * if (strong) 0.58 else 0.54, fontSize)

    private data class PlacedLine(val id: Double, val position: Double)
    private data class GridPlacement(val vertical: List<PlacedLine>, val horizontal: List<PlacedLine>)

    private const val LINE_SEGMENT_LENGTH = 6.0
    private const val FINE_GRID_SUBDIVISIONS = 10
    private const val FINE_GRID_THRESHOLD_PX = 300.0
    private const val MAJOR_LINE_DIVISOR = 10.0
    private const val MAJOR_LINE_THICKNESS = 2.0
    private const val MINOR_LINE_THICKNESS = 1.0
    private const val EPSILON = 1e-6
}
