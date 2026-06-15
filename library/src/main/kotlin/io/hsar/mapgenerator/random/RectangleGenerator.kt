package io.hsar.mapgenerator.random

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.util.Random

object RectangleGenerator {

    /**
     * Generate a Rectangle with a Gaussian distribution of square-ness.
     * The standard deviation will be the square-root of the area by default
     * and can be modified by the given stdDevFactor. A lower value results in a "squarer" result.
     * If given, the top-left point is placed at the argument; otherwise it is at the origin.
     */
    fun generateRectangle(
        area: Double,
        stdDevFactor: Double = 1.0,
        random: Random = Random(),
        topLeft: Point = Point.ORIGIN,
    ): Rectangle {
        val squareLength = kotlin.math.sqrt(area)
        val buildingWidth = RandomGenerator.generateGaussian(
            average = squareLength,
            stdDev = squareLength * stdDevFactor,
            random = random,
        )
        val buildingHeight = area / buildingWidth
        return Rectangle(topLeft, topLeft.translate(buildingWidth, buildingHeight))
    }

    fun generateRectangleCentreOn(area: Double, stdDevFactor: Double = 1.0, centre: Point = Point.ORIGIN) =
        generateRectangle(area, stdDevFactor, topLeft = Point.ORIGIN)
            .let { rectangle -> rectangle.translate(-rectangle.centre.x, -rectangle.centre.y) }
}
