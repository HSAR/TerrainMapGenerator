package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.toPoint
import io.hsar.mapgenerator.map.TerrainGenerator
import uk.yetanother.conrec.business.ContourGenerator
import java.awt.image.BufferedImage

object ContourRenderer {

    fun createImage(
        width: Int,
        height: Int,
        contourLevels: List<Double> = (0..255 step 5).map { v -> v / 255.0 }
    ): BufferedImage {
        val heightData = TerrainGenerator.generateTerrain(width, height)
        val xSteps = (0..width step X_STEP).map { it.toDouble() }.toDoubleArray()
        val ySteps = (0..height step Y_STEP).map { it.toDouble() }.toDoubleArray()
//        val contourPolygons = generatePolygons(heightData, xSteps, ySteps, contourLevels.toDoubleArray())
        val contourPolygons = generateClassic(heightData, xSteps, ySteps, contourLevels.toDoubleArray())

        return ImageBuilder(width = height, height = width).drawPolyLines(contourPolygons).build()
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

    const val X_STEP = 3
    const val Y_STEP = 3

}