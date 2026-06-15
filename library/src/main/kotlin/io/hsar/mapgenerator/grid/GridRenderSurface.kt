package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.Color
import java.awt.Font

interface GridRenderSurface {
    val width: Int
    val height: Int

    fun drawString(
        point: Point,
        string: String,
        fontAndColour: Pair<Font, Color>,
    )

    fun drawLine(point1: Point, point2: Point, width: Float, color: Color)

    fun getTextSize(text: String, font: Font): Rectangle

    fun drawCenteredString(
        point: Point,
        text: String,
        fontAndColour: Pair<Font, Color>,
    ) {
        val renderedStringSize = getTextSize(text, fontAndColour.first)
        val x = point.x - renderedStringSize.centre.x
        val y = point.y - renderedStringSize.centre.y
        drawString(Point(x, y), text, fontAndColour)
    }
}
