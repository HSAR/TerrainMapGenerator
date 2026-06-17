package io.hsar.mapgenerator.commandgui.korge.drawing

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.radians
import kotlin.math.atan2
import kotlin.math.hypot

internal class MapDrawingView : Container() {
    private val segmentContainer = container()

    init {
        addChild(segmentContainer)
    }

    fun update(state: MapDrawingState, zoom: Double) {
        val strokeWidth = SCREEN_STROKE_WIDTH / zoom.coerceAtLeast(0.0001)
        val drawings = state.drawings + listOfNotNull(state.preview)

        segmentContainer.removeChildren()
        drawings
            .flatMap { MapDrawingOutlineBuilder.build(it, zoom) }
            .forEach { drawSegment(it, strokeWidth) }
    }

    private fun drawSegment(segment: MapDrawingSegment, strokeWidth: Double) {
        val dx = segment.end.x - segment.start.x
        val dy = segment.end.y - segment.start.y
        val length = hypot(dx, dy)
        if (length <= 0.0) {
            return
        }

        val rotatedSegment = segmentContainer.container()
        rotatedSegment.xy(segment.start.x, segment.start.y)
        rotatedSegment.rotation = atan2(dy, dx).toFloat().radians
        rotatedSegment.solidRect(
            width = length,
            height = strokeWidth,
            color = RGBA(segment.color.red, segment.color.green, segment.color.blue, segment.color.alpha),
        ) {
            y = -strokeWidth * 0.5
        }
    }

    private companion object {
        private const val SCREEN_STROKE_WIDTH = 3.0
    }
}
