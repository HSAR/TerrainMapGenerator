package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Circle
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
        val drawings = state.drawings.map { it.drawing } + listOfNotNull(state.preview)

        segmentContainer.removeChildren()
        drawings.forEach { drawing ->
            when (drawing) {
                is CircleDrawing -> drawCircle(drawing, strokeWidth)
                else -> MapDrawingOutlineBuilder.build(drawing, zoom, coordinateSpace)
                    .forEach { drawSegment(it, strokeWidth) }
            }
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

    private fun drawCircle(circle: CircleDrawing, strokeWidth: Double) {
        if (circle.radius <= 0.0) {
            return
        }

        val worldCenter = coordinateSpace.toWorld(MapCoordinate(circle.center.x, circle.center.y))
        val worldRadius = coordinateSpace.circleRadiusPixels(circle.radius)
        val outlineColor = RGBA(circle.color.red, circle.color.green, circle.color.blue, circle.color.alpha)
        segmentContainer.graphics(build = {
            strokeStyle = outlineColor
            lineWidth = strokeWidth
            circle(Circle(worldCenter.x, worldCenter.y, worldRadius))
            stroke()
        }
        )
    }

    private companion object {
        private const val SCREEN_STROKE_WIDTH = 3.0
    }
}
