package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Point.Companion.ORIGIN
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.graph.Rectangle.Companion.getBoundingBox
import io.hsar.mapgenerator.graph.translate
import io.hsar.mapgenerator.image.FacilityRenderer.BuildingSize.LARGE
import io.hsar.mapgenerator.image.FacilityRenderer.BuildingSize.MEDIUM
import io.hsar.mapgenerator.image.FacilityRenderer.BuildingSize.SMALL
import io.hsar.mapgenerator.image.FacilityRenderer.Complexity.BULK
import io.hsar.mapgenerator.image.FacilityRenderer.Complexity.COMPLEX
import io.hsar.mapgenerator.image.FacilityRenderer.Complexity.INTERMEDIATE
import io.hsar.mapgenerator.map.Cell
import io.hsar.mapgenerator.randomness.RandomGenerator
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Draws a factory facility using multiple randomly-positioned rectangular buildings, always aligned along the Euclidean plane.
 *
 * Well-connected facilities (5+ adjacent facilities) are storehouses.
 * Less well-connected facilities are specialised end points with additional complexity.
 */
class FacilityRenderer(private val metresPerPixel: Double, private val imageBuilder: ImageBuilder) {

    /**
     * The complexity describes the number of buildings to be generated for it.
     */
    private enum class Complexity(val buildingsToGenerate: List<BuildingSize>) {
        COMPLEX(listOf(MEDIUM, SMALL, SMALL)),
        INTERMEDIATE(listOf(MEDIUM, SMALL)),
        BULK(listOf(LARGE, MEDIUM))
    }

    /**
     * Buildings are generated randomly but their internal area has a range defined here in square metres.
     */
    private enum class BuildingSize(val sizeRange: Pair<Double, Double>) {
        LARGE(200_000.0 to 800_000.0),
        MEDIUM(20_000.0 to 80_000.0),
        SMALL(2_000.0 to 8_000.0),
    }

    fun drawFacility(cell: Cell) {
        val complexity = when (cell.adjacentCells.size) {
            1, 2, 3 -> COMPLEX
            4 -> INTERMEDIATE
            else -> BULK
        }

        val buildings = complexity.buildingsToGenerate
            .map {
                generateBuilding(it)
            }
            .fold(emptyList<List<Point>>()) { acc, rectangle ->
                val joinAt = when (RandomGenerator.generateBoolean()) {
                    true -> JoinAt.TOP
                    false -> JoinAt.BOTTOM
                }
                acc.join(joinAt, rectangle)
            }

        val buildingsCentre = buildings.flatten().getBoundingBox().centre
        val deltaX = cell.site.x - buildingsCentre.x
        val deltaY = cell.site.y - buildingsCentre.y
        val facility = buildings.map { it.translate(deltaX, deltaY) }

        imageBuilder.drawShapeFills(facility, color = Color.YELLOW)
    }

    /**
     * One corner is always at [0,0] while the other is random but obeys the generated floor area.
     */
    private fun generateBuilding(buildingSize: BuildingSize): Rectangle {
        val (areaMin, areaMax) = buildingSize.sizeRange
        val area = RandomGenerator.generateUniform(areaMin, areaMax)

        val squareLength = sqrt(area)
        val buildingWidth = RandomGenerator.generateGaussian(squareLength, squareLength / 4)
        val buildingHeight = area / buildingWidth
        return Rectangle(ORIGIN, Point(buildingWidth / metresPerPixel, buildingHeight / metresPerPixel))
    }

    private enum class JoinAt { TOP, BOTTOM }

    /**
     * If joining two rectangles at the top, the top of both rectangles will fit flush and vice versa.
     */
    private fun List<List<Point>>.join(joinAt: JoinAt, other: Rectangle): List<List<Point>> {
        val currBoundBox = this.flatten().getBoundingBox()
        return when (joinAt) {
            JoinAt.TOP -> {
                this + listOf(other.translate(currBoundBox.width, 0.0).shape)
            }
            JoinAt.BOTTOM -> {
                val deltaX = currBoundBox.width
                val deltaY = abs(currBoundBox.height - other.height)
                if (currBoundBox.height > other.height) {
                    // don't translate current points, translate other in X and Y
                    this + listOf(other.translate(deltaX, deltaY).shape)
                } else {
                    // translate current points in Y, translate other in X
                    this.map { it.translate(0.0, deltaY) } + listOf(other.translate(deltaX, 0.0).shape)
                }
            }
        }
    }


}