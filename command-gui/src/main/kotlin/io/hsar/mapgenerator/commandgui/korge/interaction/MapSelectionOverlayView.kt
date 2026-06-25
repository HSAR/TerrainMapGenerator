package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect

internal class MapSelectionOverlayView(
    private val coordinateSpace: MapCoordinateSpace,
    private val viewportProvider: () -> CameraViewport,
) : Container() {
    private val content = container()

    fun update(state: MapSelectionState) {
        content.removeChildren()
        state.selected.forEach(::drawHighlight)
        state.box?.let(::drawSelectionBox)
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

    private companion object {
        private const val SELECTION_STROKE_WIDTH = 2.0
        private const val HIGHLIGHT_STROKE_WIDTH = 2.0
        private const val HIGHLIGHT_MAP_PADDING = 0.002
        private val selectionStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 170)
        private val highlightStroke = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 210)
    }
}
