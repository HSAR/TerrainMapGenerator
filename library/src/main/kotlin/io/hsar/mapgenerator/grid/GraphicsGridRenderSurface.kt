package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import kotlin.math.roundToInt

class GraphicsGridRenderSurface(
    private val graphics: Graphics2D,
    override val width: Int,
    override val height: Int,
) : GridRenderSurface {

    override fun drawString(point: Point, string: String, fontAndColour: Pair<Font, Color>) {
        graphics.font = fontAndColour.first
        graphics.color = fontAndColour.second
        graphics.drawString(string, point.x.toFloat(), point.y.toFloat())
    }

    override fun drawLine(point1: Point, point2: Point, width: Float, color: Color) {
        val (p1x, p1y) = point1
        val (p2x, p2y) = point2

        graphics.color = color
        graphics.stroke = BasicStroke(width)
        graphics.drawLine(p1x.roundToInt(), p1y.roundToInt(), p2x.roundToInt(), p2y.roundToInt())
    }

    override fun getTextSize(text: String, font: Font): Rectangle = graphics.getFontMetrics(font)
        .let {
            Rectangle(Point.ORIGIN, Point(it.getStringBounds(text, graphics).width, -it.ascent.toDouble()))
        }
}
