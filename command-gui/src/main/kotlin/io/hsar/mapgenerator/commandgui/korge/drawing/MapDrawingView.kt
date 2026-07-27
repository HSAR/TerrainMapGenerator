package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.displayName
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.radians
import kotlin.math.atan2
import kotlin.math.hypot

internal class MapDrawingView(
    private val coordinateSpace: MapCoordinateSpace,
) : Container() {
    private val segmentContainer = container()

    init {
        addChild(segmentContainer)
    }

    fun update(state: MapDrawingState, zoom: Double) {
        val strokeWidth = SCREEN_STROKE_WIDTH / zoom.coerceAtLeast(0.0001)
        segmentContainer.removeChildren()
        state.drawings.forEach { placed ->
            val drawing = placed.drawing
            MapDrawingOutlineBuilder.build(drawing, zoom, coordinateSpace)
                .forEach { drawSegment(it, strokeWidth) }
            placed.snapshot().displayName()?.let { drawLabel(drawing, it, zoom) }
        }
        state.preview?.let { drawing ->
            MapDrawingOutlineBuilder.build(drawing, zoom, coordinateSpace)
                .forEach { drawSegment(it, strokeWidth) }
        }
    }

    private fun drawSegment(segment: MapDrawingSegment, strokeWidth: Double) {
        val worldStart = coordinateSpace.toWorld(MapCoordinate(segment.start.x, segment.start.y))
        val worldEnd = coordinateSpace.toWorld(MapCoordinate(segment.end.x, segment.end.y))
        val dx = worldEnd.x - worldStart.x
        val dy = worldEnd.y - worldStart.y
        val length = hypot(dx, dy)
        if (length <= 0.0) {
            return
        }

        val rotatedSegment = segmentContainer.container()
        rotatedSegment.xy(worldStart.x, worldStart.y)
        rotatedSegment.rotation = atan2(dy, dx).toFloat().radians
        rotatedSegment.solidRect(
            width = length,
            height = strokeWidth,
            color = RGBA(segment.color.red, segment.color.green, segment.color.blue, segment.color.alpha),
        ) {
            y = -strokeWidth * 0.5
        }
    }

    private fun drawLabel(drawing: MapDrawing, label: String, zoom: Double) {
        val points = MapDrawingOutlineBuilder.build(drawing, zoom, coordinateSpace)
            .flatMap { listOf(it.start, it.end) }
        if (points.isEmpty()) return
        val left = points.minOf(MapDrawingPoint::x)
        val right = points.maxOf(MapDrawingPoint::x)
        val top = points.minOf(MapDrawingPoint::y)
        val center = coordinateSpace.toWorld(MapCoordinate((left + right) * 0.5, top))
        val size = LABEL_TEXT_SIZE / zoom.coerceAtLeast(0.0001)
        val bounds = MapViewerTextViews.measure(label, size = size)
        MapViewerTextViews.box(
            parent = segmentContainer,
            text = label,
            width = bounds.width,
            height = bounds.height,
            size = size,
            alignment = TextAlignment.MIDDLE_CENTER,
        ).xy(center.x - bounds.width * 0.5, center.y - bounds.height - LABEL_GAP / zoom.coerceAtLeast(0.0001))
    }

    private companion object {
        private const val SCREEN_STROKE_WIDTH = 3.0
        private const val LABEL_TEXT_SIZE = 24.0
        private const val LABEL_GAP = 4.0
    }
}
