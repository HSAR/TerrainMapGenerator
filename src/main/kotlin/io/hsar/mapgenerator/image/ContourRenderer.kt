package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.toPoint
import uk.yetanother.conrec.business.ContourGenerator
import java.awt.image.BufferedImage

object ContourRenderer {

    fun createImage(
        heightData: Array<DoubleArray>,
        contourHeight: Double = 0.02
    ): BufferedImage = createImage(heightData, (0..(1 / contourHeight).toInt()).map { v -> v * contourHeight })

    fun createImage(
        heightData: Array<DoubleArray>,
        contourLevels: List<Double> = (0..50).map { v -> v / 255.0 }
    ): BufferedImage {
        val width = heightData.size
        val height = heightData[0].size
        val xSteps = (0 until width step X_STEP).map { it.toDouble() }.toDoubleArray()
        val ySteps = (0 until height step Y_STEP).map { it.toDouble() }.toDoubleArray()
//        val contourPolygons = generatePolygons(heightData, xSteps, ySteps, contourLevels.toDoubleArray())
        val contourPolygons = generateClassic(heightData, xSteps, ySteps, contourLevels.toDoubleArray())

        return ImageBuilder(width = width, height = height)
            .fillColour(Palette.MAIN)
//            .drawPolyLines(contourPolygons, Color.decode("#FFFFFF"))
            .drawPolyLines(contourPolygons, Palette.LIGHT)
            .build()
    }

    private fun generatePolygons(
        heightData: Array<DoubleArray>,
        xSteps: DoubleArray,
        ySteps: DoubleArray,
        contourLevels: DoubleArray
    ): List<List<Point>> = ContourGenerator.generatePolygons(heightData, xSteps, ySteps, contourLevels)
        .map { contourPolygon -> contourPolygon.points.map { it.toPoint() } }

    private fun generateClassic(
        heightData: Array<DoubleArray>,
        xSteps: DoubleArray,
        ySteps: DoubleArray,
        contourLevels: DoubleArray
    ): List<List<Point>> = ContourGenerator.generateClassic(heightData, xSteps, ySteps, contourLevels)
        .map { contourPolygon -> listOf(contourPolygon.start.toPoint(), contourPolygon.end.toPoint()) }

    const val X_PAD = 4
    const val Y_PAD = 4
    const val X_STEP = 1
    const val Y_STEP = 1

}