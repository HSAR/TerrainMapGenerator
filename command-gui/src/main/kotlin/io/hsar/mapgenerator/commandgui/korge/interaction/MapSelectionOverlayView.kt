package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.solidRect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class MapSelectionOverlayView(
    private val coordinateSpace: MapCoordinateSpace,
    private val viewportProvider: () -> CameraViewport,
) : Container() {
    private val content = container()

    fun update(state: MapSelectionState) {
        content.removeChildren()
        state.groupFrame?.let(::drawGroupFrame)
        state.selected.forEach(::drawHighlight)
        state.box?.let(::drawSelectionBox)
        state.pivotHandle?.let(::drawPivotHandle)
    }

    private fun drawSelectionBox(box: ScreenSelectionBox) {
        if (box.width <= 0.0 || box.height <= 0.0) {
            return
        }
        content.solidRect(box.width, box.height, RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 36)) {
            x = box.left
            y = box.top
        }
        content.solidRect(box.width, SELECTION_STROKE_WIDTH, selectionStroke) {
            x = box.left
            y = box.top
        }
        content.solidRect(box.width, SELECTION_STROKE_WIDTH, selectionStroke) {
            x = box.left
            y = box.bottom - SELECTION_STROKE_WIDTH
        }
        content.solidRect(SELECTION_STROKE_WIDTH, box.height, selectionStroke) {
            x = box.left
            y = box.top
        }
        content.solidRect(SELECTION_STROKE_WIDTH, box.height, selectionStroke) {
            x = box.right - SELECTION_STROKE_WIDTH
            y = box.top
        }
    }

    private fun drawGroupFrame(frame: ScreenSelectionFrame) {
        drawFilledQuad(frame.corners, fill = groupFill)
        drawEdge(frame.topLeft, frame.topRight, groupStroke)
        drawEdge(frame.topRight, frame.bottomRight, groupStroke)
        drawEdge(frame.bottomRight, frame.bottomLeft, groupStroke)
        drawEdge(frame.bottomLeft, frame.topLeft, groupStroke)
    }

    private fun drawHighlight(snapshot: MapPlacedObjectSnapshot) {
        val bounds = snapshot.selectionBounds(coordinateSpace).expanded(HIGHLIGHT_MAP_PADDING)
        val viewport = viewportProvider()
        val topLeft = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.left, bounds.top)), viewport)
        val bottomRight = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.right, bounds.bottom)), viewport)
        val left = minOf(topLeft.x, bottomRight.x)
        val top = minOf(topLeft.y, bottomRight.y)
        val width = kotlin.math.abs(bottomRight.x - topLeft.x)
        val height = kotlin.math.abs(bottomRight.y - topLeft.y)

        if (width <= 0.0 || height <= 0.0) {
            return
        }
        content.solidRect(width, HIGHLIGHT_STROKE_WIDTH, highlightStroke) {
            x = left
            y = top
        }
        content.solidRect(width, HIGHLIGHT_STROKE_WIDTH, highlightStroke) {
            x = left
            y = top + height - HIGHLIGHT_STROKE_WIDTH
        }
        content.solidRect(HIGHLIGHT_STROKE_WIDTH, height, highlightStroke) {
            x = left
            y = top
        }
        content.solidRect(HIGHLIGHT_STROKE_WIDTH, height, highlightStroke) {
            x = left + width - HIGHLIGHT_STROKE_WIDTH
            y = top
        }
    }

    private fun drawFilledQuad(points: List<korlibs.math.geom.Vector2D>, fill: RGBA) {
        content.graphics {
            fill(fill) {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
    }

    private fun drawEdge(start: korlibs.math.geom.Vector2D, end: korlibs.math.geom.Vector2D, color: RGBA) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.hypot(dx, dy)
        if (length <= 0.0) {
            return
        }
        val halfThickness = SELECTION_STROKE_WIDTH * 0.5
        val unitX = dx / length
        val unitY = dy / length
        val perpX = -unitY * halfThickness
        val perpY = unitX * halfThickness
        drawFilledQuad(
            listOf(
                korlibs.math.geom.Vector2D(start.x + perpX, start.y + perpY),
                korlibs.math.geom.Vector2D(end.x + perpX, end.y + perpY),
                korlibs.math.geom.Vector2D(end.x - perpX, end.y - perpY),
                korlibs.math.geom.Vector2D(start.x - perpX, start.y - perpY),
            ),
            fill = color,
        )
    }

    private fun drawPivotHandle(handle: ScreenPivotHandle) {
        content.graphics {
            fill(handleFill) {
                val steps = 24
                val firstX = handle.center.x + cos(0.0) * handle.radius
                val firstY = handle.center.y + sin(0.0) * handle.radius
                moveTo(firstX, firstY)
                (1..steps).forEach { step ->
                    val angle = (step.toDouble() / steps) * PI * 2.0
                    lineTo(
                        handle.center.x + cos(angle) * handle.radius,
                        handle.center.y + sin(angle) * handle.radius,
                    )
                }
                close()
            }
        }
        content.graphics {
            fill(handleCore) {
                val radius = handle.radius * 0.55
                val steps = 20
                val firstX = handle.center.x + radius
                val firstY = handle.center.y
                moveTo(firstX, firstY)
                (1..steps).forEach { step ->
                    val angle = (step.toDouble() / steps) * PI * 2.0
                    lineTo(handle.center.x + cos(angle) * radius, handle.center.y + sin(angle) * radius)
                }
                close()
            }
        }
    }

    private companion object {
        private const val SELECTION_STROKE_WIDTH = 2.0
        private const val HIGHLIGHT_STROKE_WIDTH = 2.0
        private const val HIGHLIGHT_MAP_PADDING = 0.002
        private val selectionStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 170)
        private val highlightStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 210)
        private val groupFill = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 28)
        private val groupStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 150)
        private val handleFill = RGBA(Palette.Colours.WHITE.red, Palette.Colours.WHITE.green, Palette.Colours.WHITE.blue, 230)
        private val handleCore = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 230)
    }
}
