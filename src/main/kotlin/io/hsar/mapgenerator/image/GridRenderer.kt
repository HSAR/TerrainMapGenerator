package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.randomness.GridGenerator
import io.hsar.mapgenerator.randomness.GridGenerator.padZeroes
import io.hsar.mapgenerator.randomness.RandomGenerator
import java.awt.Font
import java.awt.image.BufferedImage


class GridRenderer(
    private val imageBuilder: ImageBuilder,
    private val GZDand100kmGSID: String = "${GridGenerator.randomGridZone()} ${GridGenerator.random100kmGSID()}"
) {

    private val MIN_XY = Point.ORIGIN
    private val MAX_XY = Point(imageBuilder.width.toDouble(), imageBuilder.height.toDouble())

    fun createImage(metresPerPixel: Double): BufferedImage {
        imageBuilder.drawString(MAP_ID_PADDING, GZDand100kmGSID, MAP_ID_FONT)

        val pixelsPerLine = 1000 / metresPerPixel

        val numVerticalLines = (imageBuilder.width / pixelsPerLine).toInt()
        val verticalLineNumberStart = generateLineNumberStart(numVerticalLines)
        // Vertical lines
        (1..numVerticalLines).forEach { verticalLineIndex ->
            val lineID = (verticalLineNumberStart + verticalLineIndex).padZeroes(2)

            val xPos = verticalLineIndex * pixelsPerLine
            val line = Line(Point(xPos, MIN_XY.y), Point(xPos, MAX_XY.y))

            imageBuilder.drawCenteredString(
                rect = GRID_LINE_ID_BOUNDING_BOX.translate(xPos, GRID_LINE_ID_PADDING.y),
                text = lineID,
                fontAndColour = GRID_LINE_ID_FONT
            )
            imageBuilder.drawLine(line = line, color = GRID_LINE_COLOUR, width = 0.5f)
        }

        // Horizontal lines
        val numHorizontalLines = (imageBuilder.width / pixelsPerLine).toInt()
        val horizontalLineNumberStart = generateLineNumberStart(numHorizontalLines)
        (1..numHorizontalLines).map { horizontalLineIndex ->
            val lineID = (horizontalLineNumberStart + horizontalLineIndex).padZeroes(2)

            val yPos = horizontalLineIndex * pixelsPerLine
            val line = Line(Point(MIN_XY.x, yPos), Point(MAX_XY.x, yPos))

            imageBuilder.drawCenteredString(
                rect = GRID_LINE_ID_BOUNDING_BOX.translate(GRID_LINE_ID_PADDING.x, yPos),
                text = lineID,
                fontAndColour = GRID_LINE_ID_FONT
            )
            imageBuilder.drawLine(line = line, color = GRID_LINE_COLOUR, width = 0.5f)
        }

        return imageBuilder.image
    }

    private fun generateLineNumberStart(totalLines: Int): Int {
        val maxLineNumberStart = 90 - totalLines
        return RandomGenerator.generateUniform(10, maxLineNumberStart)
    }

    companion object {
        val MAP_ID_PADDING = Point(25.0, 45.0)
        val MAP_ID_FONT = Font("TVNordEF-BoldCon", Font.PLAIN, 20) to Palette.Colours.DARK

        val GRID_LINE_ID_PADDING = Point(15.0, 15.0)
        val GRID_LINE_ID_BOUNDING_BOX = Rectangle(Point(-50.0, -15.0), Point(50.0, 15.0))
        val GRID_LINE_ID_FONT = Font("TVNordEF-RegularCon", Font.PLAIN, 16) to Palette.Colours.DARK

        val GRID_LINE_COLOUR = Palette.Colours.BACKING
    }

}