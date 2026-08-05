package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.EpsilonGridDesignators
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import io.hsar.mapgenerator.grid.ReferenceSystem
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import java.awt.Color
import java.util.Collections.emptyList
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class MapViewerScreenBounds(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width get() = (right - left).coerceAtLeast(1.0)
    val height get() = (bottom - top).coerceAtLeast(1.0)

    companion object {
        fun viewport(viewport: MapViewport) = MapViewerScreenBounds(0.0, 0.0, viewport.viewportWidth.toDouble(), viewport.viewportHeight.toDouble())
    }
}

data class MapViewerMapBounds(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    companion object {
        fun visible(viewport: MapViewport, mapOffsetX: Double, mapOffsetY: Double, mapWidth: Int, mapHeight: Int) = MapViewerMapBounds(
            left = max(0.0, viewport.left - mapOffsetX),
            top = max(0.0, viewport.top - mapOffsetY),
            right = min(mapWidth.toDouble(), viewport.left + viewport.visibleWidth - mapOffsetX),
            bottom = min(mapHeight.toDouble(), viewport.top + viewport.visibleHeight - mapOffsetY),
        )
    }
}

data class MapViewerGridReference(val coordinate: MapCoordinate, val text: String, val colour: Color? = null)

data class MapViewerGridOverlayRequest(
    val gridLayer: GridLayer,
    val viewport: MapViewport,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val mapWidth: Int,
    val mapHeight: Int,
    val visibleBounds: MapViewerScreenBounds = MapViewerScreenBounds.viewport(viewport),
    val visibleMapBounds: MapViewerMapBounds = MapViewerMapBounds.visible(viewport, mapOffsetX, mapOffsetY, mapWidth, mapHeight),
    val activeReference: MapViewerGridReference? = null,
    val measurement: MapViewerMeasurement? = null,
)

data class MapViewerGridLineSegment(val start: ViewerPoint, val end: ViewerPoint, val thickness: Double, val colour: Color)
data class MapViewerGridArrow(val start: ViewerPoint, val end: ViewerPoint, val colour: Color)
enum class MapViewerGridFontRole { MAIN, STRONG }
data class MapViewerGridLabelShadow(val offset: ViewerPoint, val colour: Color)
data class MapViewerGridLabel(
    val text: String,
    val centre: ViewerPoint,
    val fontSize: Double,
    val role: MapViewerGridFontRole,
    val colour: Color,
    val shadows: List<MapViewerGridLabelShadow> = emptyList(),
) {
    val strong get() = role == MapViewerGridFontRole.STRONG
}

data class MapViewerGridOverlay(
    val lineSegments: List<MapViewerGridLineSegment>,
    val arrows: List<MapViewerGridArrow>,
    val labels: List<MapViewerGridLabel>,
)

/** Authoritative, renderer-neutral grid layout. All returned geometry is in screen space. */
object MapViewerGridOverlayLayoutBuilder {
    fun build(
        request: MapViewerGridOverlayRequest,
        measureText: (text: String, fontSize: Double, role: MapViewerGridFontRole) -> ViewerPoint = ::approximateTextSize,
    ): MapViewerGridOverlay {
        val viewport = request.viewport
        val visible = request.visibleBounds
        val layer = displayLayer(request)
        val mapLeft = viewport.screenX(request.mapOffsetX)
        val mapTop = viewport.screenY(request.mapOffsetY)
        val mapRight = viewport.screenX(request.mapOffsetX + request.mapWidth)
        val mapBottom = viewport.screenY(request.mapOffsetY + request.mapHeight)
        if (mapRight <= visible.left || mapBottom <= visible.top || mapLeft >= visible.right || mapTop >= visible.bottom) {
            return MapViewerGridOverlay(emptyList(), emptyList(), emptyList())
        }

        val styleWidth = visible.width.roundToInt()
        val styleHeight = visible.height.roundToInt()
        val fadeDistance = GridStyles.fadeDistance(styleWidth, styleHeight)
        val mapIdPadding = GridStyles.mapIdPadding(styleWidth, styleHeight)
        val gridPadding = GridStyles.gridLineIdPadding(styleWidth, styleHeight)
        val mapIdFontSize = GridStyles.mapIdFont(styleWidth, styleHeight).first.size2D.toDouble()
        val minorFontSize = GridStyles.gridLineIdFont(styleWidth, styleHeight).first.size2D.toDouble()
        val majorFontSize = GridStyles.majorGridLineIdFont(styleWidth, styleHeight).first.size2D.toDouble()
        val topGutterY = max(visible.top + mapIdPadding.y, mapTop + mapIdPadding.y)
        val leftGutterX = max(visible.left + gridPadding.x, mapLeft + gridPadding.x)
        val mapIdSize = measureText(layer.mapId, mapIdFontSize, MapViewerGridFontRole.STRONG)
        val axisSize = measureText(axisMeasurementText(layer), minorFontSize, MapViewerGridFontRole.MAIN)
        val yAxisRightEdge = leftGutterX + axisSize.x / 2.0
        val xAxisBottomEdge = topGutterY + axisSize.y / 2.0
        val xLabelFadeStart = max(yAxisRightEdge, leftGutterX + mapIdSize.x / 2.0)
        val yLabelFadeStart = max(xAxisBottomEdge, topGutterY + mapIdSize.y / 2.0)
        val placement = visibleLines(request, layer)
        val fineMode = layer.gridScale < request.gridLayer.gridScale
        val segments = mutableListOf<MapViewerGridLineSegment>()
        val arrows = mutableListOf<MapViewerGridArrow>()
        val labels = mutableListOf(label(layer.mapId, leftGutterX, topGutterY, mapIdFontSize, MapViewerGridFontRole.STRONG, layer.colour))

        placement.vertical.forEach { line ->
            val screenX = viewport.screenX(request.mapOffsetX + line.position)
            val major = isMajorLine(line.id)
            if (shouldRenderLabel(layer.referenceSystem, fineMode, line.id, major)) {
                labels += label(
                    verticalLineText(layer, request.gridLayer, line.id), screenX, topGutterY,
                    if (major) majorFontSize else minorFontSize,
                    if (major) MapViewerGridFontRole.STRONG else MapViewerGridFontRole.MAIN,
                    layer.colour.withOpacity(edgeOpacity(screenX - xLabelFadeStart, fadeDistance)),
                )
            }
            segments += fadedLine(
                max(mapTop, xAxisBottomEdge), min(mapBottom, visible.bottom), screenX, true,
                if (major) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                layer.colour.withOpacity(edgeOpacity(screenX - yAxisRightEdge, fadeDistance)), fadeDistance,
            )
        }

        placement.horizontal.forEach { line ->
            val screenY = viewport.screenY(request.mapOffsetY + line.position)
            val major = isMajorLine(line.id)
            if (shouldRenderLabel(layer.referenceSystem, fineMode, line.id, major)) {
                labels += label(
                    horizontalLineText(layer, request.gridLayer, line.id), leftGutterX, screenY,
                    if (major) majorFontSize else minorFontSize,
                    if (major) MapViewerGridFontRole.STRONG else MapViewerGridFontRole.MAIN,
                    layer.colour.withOpacity(edgeOpacity(screenY - yLabelFadeStart, fadeDistance)),
                )
            }
            segments += fadedLine(
                max(mapLeft, yAxisRightEdge), min(mapRight, visible.right), screenY, false,
                if (major) MAJOR_LINE_THICKNESS else MINOR_LINE_THICKNESS,
                layer.colour.withOpacity(edgeOpacity(screenY - xAxisBottomEdge, fadeDistance)), fadeDistance,
            )
        }

        request.activeReference?.let { reference ->
            val referenceColour = reference.colour ?: layer.colour
            val point = reference.coordinate.toScreen(request)
            val referenceSize = measureText(reference.text, minorFontSize, MapViewerGridFontRole.MAIN)
            segments += fadedLine(
                max(mapTop, xAxisBottomEdge), min(mapBottom, visible.bottom), point.x, true,
                ACTIVE_REFERENCE_LINE_THICKNESS, referenceColour.withOpacity(edgeOpacity(point.x - yAxisRightEdge, fadeDistance)), fadeDistance
            )
            segments += fadedLine(
                max(mapLeft, yAxisRightEdge), min(mapRight, visible.right), point.y, false,
                ACTIVE_REFERENCE_LINE_THICKNESS, referenceColour.withOpacity(edgeOpacity(point.y - xAxisBottomEdge, fadeDistance)), fadeDistance
            )
            labels += label(
                reference.text,
                point.x + ACTIVE_REFERENCE_LABEL_OFFSET_X + referenceSize.x / 2.0,
                point.y - ACTIVE_REFERENCE_LABEL_OFFSET_Y - referenceSize.y / 2.0,
                minorFontSize,
                MapViewerGridFontRole.MAIN,
                referenceColour,
                accentShadows(referenceColour),
            )
            request.measurement?.let { measurement ->
                val origin = measurement.origin.toScreen(request)
                val destination = measurement.destination.toScreen(request)
                arrows += MapViewerGridArrow(origin, destination, referenceColour)
                val distanceSize = measureText(measurement.distanceText, minorFontSize, MapViewerGridFontRole.MAIN)
                val bearingSize = measureText(measurement.bearingText, minorFontSize, MapViewerGridFontRole.MAIN)
                val tooltipX = point.x + ACTIVE_REFERENCE_LABEL_OFFSET_X
                val tooltipY = point.y + ACTIVE_REFERENCE_LABEL_OFFSET_Y
                labels += label(
                    measurement.distanceText, tooltipX + distanceSize.x / 2.0, tooltipY + distanceSize.y / 2.0,
                    minorFontSize, MapViewerGridFontRole.MAIN, referenceColour, accentShadows(referenceColour)
                )
                labels += label(
                    measurement.bearingText, tooltipX + bearingSize.x / 2.0,
                    tooltipY + distanceSize.y.absoluteValue + MEASUREMENT_TOOLTIP_LINE_GAP + bearingSize.y / 2.0,
                    minorFontSize, MapViewerGridFontRole.MAIN, referenceColour, accentShadows(referenceColour)
                )
            }
        }

        return MapViewerGridOverlay(segments, arrows, labels)
    }

    fun edgeOpacity(distanceFromEdge: Double, fadeDistance: Double) =
        if (fadeDistance <= 0.0) 1.0 else 1.0 - (1.0 - (distanceFromEdge / fadeDistance).coerceIn(0.0, 1.0)).let { it * it }

    private fun MapCoordinate.toScreen(request: MapViewerGridOverlayRequest) = ViewerPoint(
        request.viewport.screenX(request.mapOffsetX + x * request.mapWidth),
        request.viewport.screenY(request.mapOffsetY + y * request.mapHeight),
    )

    private fun visibleLines(request: MapViewerGridOverlayRequest, layer: GridLayer): GridPlacement {
        val visible = request.visibleMapBounds
        return GridPlacement(
            placedLines(visible.left, visible.right, layer.verticalLineCount, verticalLineStart(layer), layer, false),
            placedLines(visible.top, visible.bottom, layer.horizontalLineCount, horizontalLineStart(layer), layer, true),
        )
    }

    private fun placedLines(start: Double, end: Double, count: Int, first: Double, layer: GridLayer, descending: Boolean): List<PlacedLine> {
        val startIndex = max(0, ceil((start - first) / layer.pixelsPerLine).toInt())
        val endIndex = min(count, floor((end - first) / layer.pixelsPerLine).toInt())
        if (startIndex > endIndex) return emptyList()
        val initial = if (descending) layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale) else layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale)
        return (startIndex..endIndex).map { PlacedLine(initial + if (descending) -it else it, first + it * layer.pixelsPerLine) }
    }

    private fun fadedLine(start: Double, end: Double, fixed: Double, vertical: Boolean, thickness: Double, colour: Color, fade: Double): List<MapViewerGridLineSegment> {
        if (end <= start || colour.alpha <= 0) return emptyList()
        val fadeEnd = min(end, start + fade)
        val length = fadeEnd - start
        val count = max(1, (length / LINE_SEGMENT_LENGTH).roundToInt())
        val step = length / count
        val faded = (0 until count).mapNotNull { index ->
            val a = start + index * step
            val b = if (index == count - 1) fadeEnd else a + step
            colour.withOpacity(edgeOpacity((a + b) / 2.0 - start, fade)).takeIf { it.alpha > 0 }
                ?.let { line(a, b, fixed, vertical, thickness, it) }
        }
        return if (fadeEnd >= end) faded else faded + line(fadeEnd, end, fixed, vertical, thickness, colour)
    }

    private fun line(a: Double, b: Double, fixed: Double, vertical: Boolean, thickness: Double, colour: Color) = MapViewerGridLineSegment(
        if (vertical) ViewerPoint(fixed, a) else ViewerPoint(a, fixed),
        if (vertical) ViewerPoint(fixed, b) else ViewerPoint(b, fixed), thickness, colour,
    )

    private fun label(
        text: String, x: Double, y: Double, size: Double, role: MapViewerGridFontRole, colour: Color,
        shadows: List<MapViewerGridLabelShadow> = emptyList()
    ) =
        MapViewerGridLabel(text, ViewerPoint(x, y), size, role, colour, shadows)

    private fun accentShadows(colour: Color) = listOf(MapViewerGridLabelShadow(ViewerPoint(1.0, 1.0), colour))
    private fun displayLayer(request: MapViewerGridOverlayRequest): GridLayer {
        val layer = request.gridLayer
        val threshold = if (layer.referenceSystem == ReferenceSystem.EPSILON) FINE_GRID_THRESHOLD_PX * 2.0 else FINE_GRID_THRESHOLD_PX
        return if (layer.pixelsPerLine * request.viewport.zoom >= threshold) layer.copy(
            gridScale = (layer.gridScale / FINE_GRID_SUBDIVISIONS).coerceAtLeast(1), detailLevel = layer.detailLevel + 1,
            pixelsPerLine = layer.pixelsPerLine / FINE_GRID_SUBDIVISIONS,
            verticalLineNumberStart = layer.referenceSystem.coordFromUnits(layer.verticalLineNumberStart.preciseUnits),
            horizontalLineNumberStart = layer.referenceSystem.coordFromUnits(layer.horizontalLineNumberStart.preciseUnits),
            verticalLineCount = layer.verticalLineCount * FINE_GRID_SUBDIVISIONS,
            horizontalLineCount = layer.horizontalLineCount * FINE_GRID_SUBDIVISIONS,
        ) else layer
    }

    private fun verticalLineStart(layer: GridLayer) =
        (layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) - layer.verticalLineNumberStart.lineNumber(layer.gridScale)) * layer.pixelsPerLine

    private fun horizontalLineStart(layer: GridLayer) =
        (layer.horizontalLineNumberStart.lineNumber(layer.gridScale) - layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale)) * layer.pixelsPerLine

    private fun shouldRenderLabel(system: ReferenceSystem, fine: Boolean, id: Double, major: Boolean) = when (system) {
        ReferenceSystem.EPSILON -> !fine || major
        ReferenceSystem.MGRS -> !fine || major || abs(id % 2.0) < EPSILON
    }

    private fun verticalLineText(layer: GridLayer, base: GridLayer, id: Double) =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) EpsilonGridDesignators.xDesignator(squareIndex(layer, base, id)) else id.padZeroes(layer.detailLevel)

    private fun horizontalLineText(layer: GridLayer, base: GridLayer, id: Double) =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) EpsilonGridDesignators.yDesignator(squareIndex(layer, base, id)) else id.padZeroes(layer.detailLevel)

    private fun squareIndex(layer: GridLayer, base: GridLayer, id: Double) = Math.floorDiv((id * layer.gridScale).roundToInt(), base.gridScale)
    private fun axisMeasurementText(layer: GridLayer) = if (layer.referenceSystem == ReferenceSystem.EPSILON) "AA" else "0".repeat(layer.detailLevel)
    private fun isMajorLine(id: Double) = abs(id % MAJOR_LINE_DIVISOR) < EPSILON
    private fun Color.withOpacity(opacity: Double) = Color(red, green, blue, (alpha * opacity).roundToInt().coerceIn(0, 255))
    private fun approximateTextSize(text: String, size: Double, role: MapViewerGridFontRole) =
        ViewerPoint(text.length * size * if (role == MapViewerGridFontRole.STRONG) .58 else .54, size)
    private data class PlacedLine(val id: Double, val position: Double)
    private data class GridPlacement(val vertical: List<PlacedLine>, val horizontal: List<PlacedLine>)

    private const val LINE_SEGMENT_LENGTH = 6.0
    private const val FINE_GRID_SUBDIVISIONS = 10
    private const val FINE_GRID_THRESHOLD_PX = 300.0
    private const val MAJOR_LINE_DIVISOR = 10.0
    private const val MAJOR_LINE_THICKNESS = 2.0
    private const val MINOR_LINE_THICKNESS = 1.0
    private const val ACTIVE_REFERENCE_LINE_THICKNESS = 2.0
    private const val ACTIVE_REFERENCE_LABEL_OFFSET_X = 12.0
    private const val ACTIVE_REFERENCE_LABEL_OFFSET_Y = 12.0
    private const val MEASUREMENT_TOOLTIP_LINE_GAP = 4.0
    private const val EPSILON = 1e-6
}
