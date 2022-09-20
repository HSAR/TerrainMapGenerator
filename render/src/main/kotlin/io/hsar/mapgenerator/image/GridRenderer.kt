package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Line
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.random.GridGenerator
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import io.hsar.mapgenerator.random.RandomGenerator
import java.awt.Font
import kotlin.math.log
import kotlin.math.roundToInt

class GridRenderer(
    private val imageBuilder: io.hsar.mapgenerator.image.ImageBuilder
) {

    private val MIN_XY = GRID_LINE_ID_PADDING
    private val MAX_XY = Point(imageBuilder.width.toDouble(), imageBuilder.height.toDouble())

    fun draw(
        GZDand100kmGSID: String = "${GridGenerator.randomGridZone()} ${GridGenerator.random100kmGSID()}",
        gridScale: Int,
        metresPerPixel: Double
    ) {
        imageBuilder.drawString(MAP_ID_PADDING, GZDand100kmGSID, MAP_ID_FONT)

        val pixelsPerLine = gridScale / metresPerPixel
        val getDetailLevel = getDetailLevel(gridScale)

        val numVerticalLines = (imageBuilder.width / pixelsPerLine).toInt()
        val verticalLineNumberStart = generateLineNumberStart(getDetailLevel, numVerticalLines)
        // Vertical lines
        (1..numVerticalLines).forEach { verticalLineIndex ->
            val lineID = (verticalLineNumberStart + verticalLineIndex)
            val lineIDString = lineID.padZeroes(getDetailLevel)

            val xPos = MIN_XY.x + verticalLineIndex * pixelsPerLine
            val yPos = MIN_XY.y + imageBuilder.getTextSize(lineIDString, GRID_LINE_ID_FONT.first).height
            val line = Line(Point(xPos, yPos), Point(xPos, MAX_XY.y))
            val width = if ((lineID % 10) == 0) 1.0f else 0.5f

            imageBuilder.drawCenteredString(
                point = Point(xPos, MIN_XY.y),
                text = lineIDString,
                fontAndColour = GRID_LINE_ID_FONT
            )
            imageBuilder.drawLine(line = line, color = GRID_LINE_COLOUR, width = width)
        }

        // Horizontal lines
        val numHorizontalLines = (imageBuilder.width / pixelsPerLine).toInt()
        val horizontalLineNumberStart = generateLineNumberStart(getDetailLevel, numHorizontalLines)
        (1..numHorizontalLines).map { horizontalLineIndex ->
            val lineID = (horizontalLineNumberStart + horizontalLineIndex)
            val lineIDString = lineID.padZeroes(getDetailLevel)

            val xPos = MIN_XY.x + imageBuilder.getTextSize(lineIDString, GRID_LINE_ID_FONT.first).width
            val yPos = MIN_XY.y + horizontalLineIndex * pixelsPerLine
            val line = Line(Point(xPos, yPos), Point(MAX_XY.x, yPos))
            val width = if (lineID % 10 == 0) 1.0f else 0.5f

            imageBuilder.drawCenteredString(
                point = Point(MIN_XY.x, yPos),
                text = lineIDString,
                fontAndColour = GRID_LINE_ID_FONT
            )
            imageBuilder.drawLine(line = line, color = GRID_LINE_COLOUR, width = width)
        }
    }

    private fun getDetailLevel(gridScale: Int): Int {
        val gridScaleMagnitude = log(gridScale.toDouble(), 10.0).roundToInt()
        require(gridScaleMagnitude < 6) { "Grid scale is too large, needs to be < 100km." }
        return 5 - gridScaleMagnitude
    }

    private fun generateLineNumberStart(detailLevel: Int, totalLines: Int): Int {
        val maxLineNumberStart = Math.pow(10.0, detailLevel.toDouble()) - totalLines
        return RandomGenerator.generateUniform(1, maxLineNumberStart.roundToInt())
    }

    companion object {
        val MAP_ID_PADDING = Point(25.0, 50.0)
        val MAP_ID_FONT = Font("TVNordEF-BoldCon", Font.PLAIN, 24) to Palette.Colours.BACKING

        val GRID_LINE_ID_PADDING = Point(55.0, 40.0)
        val GRID_LINE_ID_FONT = Font("TVNordEF-RegularCon", Font.PLAIN, 18) to Palette.Colours.BACKING

        val GRID_LINE_COLOUR = Palette.Colours.BACKING

        const val ONE_KM_GRID_SQUARES = 1_000
        const val HUNDRED_M_GRID_SQUARES = 100
    }

}