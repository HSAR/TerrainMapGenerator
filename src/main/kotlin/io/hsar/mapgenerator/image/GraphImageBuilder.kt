package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.Point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

class GraphImageBuilder(val height: Int, val width: Int) {

    val image = BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB)
    private val g2d = image.createGraphics()
        .also {
            it.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )
        }

    fun build() = image
        .also {
            g2d.dispose()
        }

    fun drawLines(edges: Collection<Line>, color: Color = Color.BLACK): GraphImageBuilder {
        edges.forEach { (point1, point2, vertices) ->
            drawLine(point1, point2, color)

            vertices.forEach { vPoint ->
                drawDot(point = vPoint, diameter = 2, color = Color.BLUE)
            }
        }
        return this
    }

    private fun drawLine(point1: Point, point2: Point, color: Color) {
        val (p1x, p1y) = point1
        val (p2x, p2y) = point2

        g2d.color = color
        g2d.stroke = BasicStroke(3.0f)
        g2d.drawLine(p1x.roundToInt(), p1y.roundToInt(), p2x.roundToInt(), p2y.roundToInt())
    }

    fun drawPoints(points: Collection<Point>, color: Color = Color.RED): GraphImageBuilder {
        points.forEach { point ->
            drawDot(point = point, diameter = 6, color = color)
        }
        return this
    }

    private fun drawDot(point: Point, diameter: Int = 6, color: Color = Color.RED) {
        val radius = diameter / 2.0
        val startX = (point.x - radius).roundToInt()
        val startY = (point.y - radius).roundToInt()

        g2d.color = color
        g2d.stroke = BasicStroke(1.0f)
        g2d.drawOval(startX, startY, diameter, diameter)
    }

    fun drawShapes(shapes: Collection<List<Point>>, color: Color = Color.GRAY): GraphImageBuilder {
        shapes.forEach {
            drawShape(it, color)
        }
        return this
    }

    private fun drawShape(shape: List<Point>, color: Color = Color.RED) {
        g2d.color = color
        g2d.stroke = BasicStroke(1.0f)
        shape.forEachIndexed { index, point ->
            val nextPoint = shape[(index + 1) % shape.size]
            drawLine(point, nextPoint, Color.LIGHT_GRAY)
        }
    }
}
