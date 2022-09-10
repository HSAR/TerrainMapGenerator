package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.Path
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.roundToInt


class ImageBuilder(val width: Int, val height: Int) {

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private val g2d = image.createGraphics()
        .also {
            it.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            it.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        }

    fun build() = image
        .also {
            g2d.dispose()
        }

    fun fillTransparent(): ImageBuilder {
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR);
        g2d.fillRect(0, 0, width, height)
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        return this
    }

    fun fillColour(color: Color): ImageBuilder {
        g2d.color = color
        g2d.fillRect(0, 0, width, height)
        return this
    }

    fun drawLines(edges: Collection<Line>, color: Color = Color.BLACK): ImageBuilder {
        edges.forEach { (point1, point2, vertices) ->
            drawLine(point1, point2, color)

            vertices.forEach { vPoint ->
                drawPoint(point = vPoint, diameter = 2, color = Color.BLUE)
            }
        }
        return this
    }

    fun drawLine(line: Line, color: Color, width: Float = 3.0f) = drawLine(line.site1, line.site2, color, width)

    fun drawLine(point1: Point, point2: Point, color: Color, width: Float = 3.0f) {
        val (p1x, p1y) = point1
        val (p2x, p2y) = point2

        g2d.color = color
        g2d.stroke = BasicStroke(width)
        g2d.drawLine(p1x.roundToInt(), p1y.roundToInt(), p2x.roundToInt(), p2y.roundToInt())
    }

    fun drawDiagonalPaths(lines: Collection<Line>, color: Color): ImageBuilder = lines
        .map { line ->
            drawDiagonalPath(line = line, color = color)
        }.last()

    fun drawDiagonalPath(line: Line, color: Color) = drawDiagonalPath(line.site1, line.site2, color)

    fun drawDiagonalPath(point1: Point, point2: Point, color: Color): ImageBuilder {
        val pathLines = Path.createAnglePath(point1, point2)
        drawLines(pathLines, color)
        return this
    }

    fun drawPoints(points: Collection<Point>, color: Color = Color.RED): ImageBuilder {
        points.forEach { point ->
            drawPoint(point = point, diameter = 6, color = color)
        }
        return this
    }

    fun drawPoint(point: Point, diameter: Int = 6, color: Color = Color.RED) {
        val radius = diameter / 2.0
        val startX = (point.x - radius).roundToInt()
        val startY = (point.y - radius).roundToInt()

        g2d.color = color
        g2d.stroke = BasicStroke(1.0f)
        g2d.drawOval(startX, startY, diameter, diameter)
    }

    fun drawPolyLines(polyLines: Collection<List<Point>>, color: Color = Color.GRAY): ImageBuilder {
        polyLines.forEach {
            drawShapeOutline(it, color)
        }
        return this
    }

    fun drawPolyLine(polyLine: List<Point>, color: Color = Color.RED, width: Float = 1.0f) {
        g2d.color = color
        g2d.stroke = BasicStroke(width)
        val (xPoints, yPoints) = createPolyPoints(polyLine)
        g2d.drawPolyline(xPoints, yPoints, xPoints.size)
    }

    fun drawShapeOutlines(shapes: Collection<List<Point>>, color: Color = Color.GRAY): ImageBuilder {
        shapes.forEach {
            drawShapeOutline(it, color)
        }
        return this
    }

    fun drawShapeOutline(shape: List<Point>, color: Color = Color.RED) {
        g2d.color = color
        g2d.stroke = BasicStroke(1.0f)
        val (xPoints, yPoints) = createPolyPoints(shape)
        g2d.drawPolygon(xPoints, yPoints, xPoints.size)
    }

    fun drawShapeFills(shapes: Collection<List<Point>>, color: Color = Color.GRAY): ImageBuilder {
        shapes.forEach {
            drawShapeFill(it, color)
        }
        return this
    }

    fun drawShapeFill(shape: List<Point>, color: Color = Color.RED) {
        g2d.color = color
        val (xPoints, yPoints) = createPolyPoints(shape)
        g2d.fillPolygon(xPoints, yPoints, xPoints.size)
    }

    private fun createPolyPoints(shape: List<Point>): Pair<IntArray, IntArray> {
        val xPoints = shape.map { min(it.x.roundToInt(), width) }.toIntArray()
        val yPoints = shape.map { min(it.y.roundToInt(), height) }.toIntArray()
        return xPoints to yPoints
    }

    fun drawString(point: Point, string: String, fontAndColour: Pair<Font, Color> = Palette.Fonts.BASE) {
        g2d.font = fontAndColour.first
        g2d.color = fontAndColour.second
        g2d.drawString(string, point.x.toFloat(), point.y.toFloat())
    }

    /**
     * Draw a String centered in the middle of a Rectangle.
     */
    fun drawCenteredString(rect: Rectangle, text: String, fontAndColour: Pair<Font, Color> = Palette.Fonts.BASE) {
        // Get the FontMetrics
        val metrics = g2d.getFontMetrics(fontAndColour.first)
        // Determine the X coordinate for the text
        val x = rect.centre.x + (rect.width - metrics.stringWidth(text)) / 2
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        val y = rect.centre.y + (rect.height - metrics.height) / 2 + metrics.ascent
        drawString(Point(x, y), text, fontAndColour)
    }
}
