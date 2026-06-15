package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object GridLayerPainter {

    fun draw(
        surface: GridRenderSurface,
        layer: GridLayer,
        viewport: Rectangle = Rectangle(
            Point.ORIGIN,
            Point(surface.width.toDouble(), surface.height.toDouble()),
        ),
    ) {
        surface.drawString(GridStyles.MAP_ID_PADDING, layer.mapId, GridStyles.MAP_ID_FONT)

        drawVerticalLines(surface, layer, viewport)
        drawHorizontalLines(surface, layer, viewport)
    }

    private fun drawVerticalLines(surface: GridRenderSurface, layer: GridLayer, viewport: Rectangle) {
        val startIndex = max(
            1,
            ceil((viewport.topLeft.x - GridStyles.GRID_LINE_ID_PADDING.x) / layer.pixelsPerLine).toInt(),
        )
        val endIndex = min(
            layer.verticalLineCount,
            floor((viewport.topRight.x - GridStyles.GRID_LINE_ID_PADDING.x) / layer.pixelsPerLine).toInt(),
        )

        if (startIndex > endIndex) {
            return
        }

        (startIndex..endIndex).forEach { verticalLineIndex ->
            val lineID = layer.verticalLineNumberStart + verticalLineIndex
            val lineIDString = lineID.padZeroes(layer.detailLevel)
            val xPos = GridStyles.GRID_LINE_ID_PADDING.x + verticalLineIndex * layer.pixelsPerLine
            val yPos = GridStyles.GRID_LINE_ID_PADDING.y + surface.getTextSize(lineIDString, GridStyles.GRID_LINE_ID_FONT.first).height
            val startY = max(viewport.topLeft.y, yPos)
            val endY = min(viewport.bottomRight.y, surface.height.toDouble())
            val width = if ((lineID % 10) == 0) 1.0f else 0.5f

            if (startY < endY) {
                surface.drawCenteredString(
                    point = Point(xPos, GridStyles.GRID_LINE_ID_PADDING.y),
                    text = lineIDString,
                    fontAndColour = GridStyles.GRID_LINE_ID_FONT,
                )
                surface.drawLine(
                    Point(xPos, startY),
                    Point(xPos, endY),
                    width = width,
                    color = GridStyles.GRID_LINE_COLOUR,
                )
            }
        }
    }

    private fun drawHorizontalLines(surface: GridRenderSurface, layer: GridLayer, viewport: Rectangle) {
        val startIndex = max(
            1,
            ceil((viewport.topLeft.y - GridStyles.GRID_LINE_ID_PADDING.y) / layer.pixelsPerLine).toInt(),
        )
        val endIndex = min(
            layer.horizontalLineCount,
            floor((viewport.bottomRight.y - GridStyles.GRID_LINE_ID_PADDING.y) / layer.pixelsPerLine).toInt(),
        )

        if (startIndex > endIndex) {
            return
        }

        (startIndex..endIndex).forEach { horizontalLineIndex ->
            val lineID = layer.horizontalLineNumberStart - horizontalLineIndex
            val lineIDString = lineID.padZeroes(layer.detailLevel)
            val xPos = GridStyles.GRID_LINE_ID_PADDING.x + surface.getTextSize(lineIDString, GridStyles.GRID_LINE_ID_FONT.first).width
            val yPos = GridStyles.GRID_LINE_ID_PADDING.y + horizontalLineIndex * layer.pixelsPerLine
            val startX = max(viewport.topLeft.x, xPos)
            val endX = min(viewport.bottomRight.x, surface.width.toDouble())
            val width = if (lineID % 10 == 0) 1.0f else 0.5f

            if (startX < endX) {
                surface.drawCenteredString(
                    point = Point(GridStyles.GRID_LINE_ID_PADDING.x, yPos),
                    text = lineIDString,
                    fontAndColour = GridStyles.GRID_LINE_ID_FONT,
                )
                surface.drawLine(
                    Point(startX, yPos),
                    Point(endX, yPos),
                    width = width,
                    color = GridStyles.GRID_LINE_COLOUR,
                )
            }
        }
    }
}
