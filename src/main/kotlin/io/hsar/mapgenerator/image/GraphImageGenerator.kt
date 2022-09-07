package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Edge
import io.hsar.mapgenerator.graph.Point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

object GraphImageGenerator {

    fun generate(points: List<Point>, edges: List<Edge>, height: Int, width: Int) =
        BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB)
            .also { image ->
                val g2d = image.createGraphics()
                    .also {
                        it.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                        )
                    }

                drawPoints(g2d, points)
                drawEdges(g2d, edges)

                g2d.dispose()
            }

    private fun drawEdges(g2d: Graphics2D, edges: List<Edge>) {

        edges.forEach { (point1, point2, vertices) ->
            val (p1x, p1y) = point1
            val (p2x, p2y) = point2

            g2d.color = Color.BLACK
            g2d.stroke = BasicStroke(3.0f)
            g2d.drawLine(p1x.roundToInt(), p1y.roundToInt(), p2x.roundToInt(), p2y.roundToInt())

            vertices.forEach { vPoint ->
                drawDot(g2d = g2d, point = vPoint, diameter = 2, color = Color.BLUE)
            }
        }
    }

    private fun drawPoints(g2d: Graphics2D, points: List<Point>) {
        g2d.stroke = BasicStroke(3.0f)

        points.forEach { point ->
            drawDot(g2d = g2d, point = point, diameter = 6, color = Color.RED)
        }
    }

    private fun drawDot(g2d: Graphics2D, point: Point, diameter: Int = 6, color: Color = Color.RED) {
        val radius = diameter / 2.0
        val startX = (point.x - radius).roundToInt()
        val startY = (point.y - radius).roundToInt()

        g2d.color = color
        g2d.drawOval(startX, startY, diameter, diameter)
    }
}
