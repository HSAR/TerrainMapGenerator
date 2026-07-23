package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.solidRect
import java.awt.Color
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
        val gridColor = state.gridColor ?: UnitAffiliationUi.unaffiliatedColor()
        val selectionColor = state.selected.selectionAccentColor(gridColor)
        state.groupFrame?.let { drawGroupFrame(it, selectionColor) }
        state.selected.forEach { drawHighlight(it, gridColor) }
        state.box?.let(::drawSelectionBox)
        state.selectionBracketFrame()?.let { drawCornerBrackets(it, selectionColor) }
        state.pivotHandle?.let { drawPivotHandle(it, selectionColor) }
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

    private fun drawGroupFrame(frame: ScreenSelectionFrame, color: Color) {
        drawFrame(frame, color, GROUP_FILL_ALPHA, GROUP_STROKE_ALPHA)
    }

    private fun drawHighlight(snapshot: MapPlacedObjectSnapshot, gridColor: Color) {
        val viewport = viewportProvider()
        val frame = snapshot.selectionFrame(coordinateSpace, viewport, SELECTION_FRAME_SCREEN_PADDING)
        drawFrame(
            frame = frame,
            color = snapshot.selectionAccentColor(gridColor),
            strokeAlpha = HIGHLIGHT_STROKE_ALPHA,
            strokeWidth = HIGHLIGHT_STROKE_WIDTH,
        )
    }

    private fun drawCornerBrackets(frame: ScreenSelectionFrame, color: Color) {
        val bracketColor = MapViewerUiBoxes.toRgba(color, BRACKET_STROKE_ALPHA)
        frame.cornerBrackets(SELECTION_BRACKET_LENGTH_PX, SELECTION_BRACKET_THICKNESS_PX)
            .forEach { drawFilledPolygon(it.points, bracketColor) }
    }

    private fun MapSelectionState.selectionBracketFrame(): ScreenSelectionFrame? =
        takeIf { selected.any(MapPlacedObjectSnapshot::supportsResizeHandles) }
            ?.let {
                groupFrame ?: selected.singleOrNull()?.selectionFrame(
                    coordinateSpace = coordinateSpace,
                    viewport = viewportProvider(),
                    screenPadding = SELECTION_FRAME_SCREEN_PADDING,
                )
            }

    private fun drawFilledQuad(points: List<korlibs.math.geom.Vector2D>, fill: RGBA) =
        drawFilledPolygon(points, fill)

    private fun drawFrame(
        frame: ScreenSelectionFrame,
        color: Color,
        fillAlpha: Int? = null,
        strokeAlpha: Int? = null,
        strokeWidth: Double = SELECTION_STROKE_WIDTH,
    ) {
        if (fillAlpha != null) {
            drawFilledQuad(frame.corners, fill = MapViewerUiBoxes.toRgba(color, fillAlpha))
        }
        if (strokeAlpha != null) {
            val strokeColor = MapViewerUiBoxes.toRgba(color, strokeAlpha)
            drawEdge(frame.topLeft, frame.topRight, strokeColor, strokeWidth)
            drawEdge(frame.topRight, frame.bottomRight, strokeColor, strokeWidth)
            drawEdge(frame.bottomRight, frame.bottomLeft, strokeColor, strokeWidth)
            drawEdge(frame.bottomLeft, frame.topLeft, strokeColor, strokeWidth)
        }
    }

    private fun drawFilledPolygon(points: List<korlibs.math.geom.Vector2D>, fill: RGBA) {
        content.graphics {
            fill(fill) {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
    }

    private fun drawEdge(
        start: korlibs.math.geom.Vector2D,
        end: korlibs.math.geom.Vector2D,
        color: RGBA,
        strokeWidth: Double = SELECTION_STROKE_WIDTH,
    ) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.hypot(dx, dy)
        if (length <= 0.0) {
            return
        }
        val halfThickness = strokeWidth * 0.5
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

    private fun drawPivotHandle(handle: ScreenPivotHandle, color: Color) {
        content.graphics {
            fill(MapViewerUiBoxes.toRgba(color, HANDLE_FILL_ALPHA)) {
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
            fill(MapViewerUiBoxes.toRgba(color, HANDLE_CORE_ALPHA)) {
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
        private const val GROUP_FILL_ALPHA = 28
        private const val GROUP_STROKE_ALPHA = 150
        private const val HIGHLIGHT_STROKE_ALPHA = 210
        private const val BRACKET_STROKE_ALPHA = 230
        private const val HANDLE_FILL_ALPHA = 230
        private const val HANDLE_CORE_ALPHA = 230
        private val selectionStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 170)
    }
}
