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
        COMPLEX(listOf(SMALL, SMALL, SMALL)),
        INTERMEDIATE(listOf(MEDIUM, SMALL, MEDIUM)),
        BULK(listOf(LARGE, SMALL))
    }

    /**
     * Buildings are generated randomly but their internal area has a range defined here in square metres.
     */
    private enum class BuildingSize(val sizeRange: Pair<Double, Double>) {
        LARGE(50_000.0 to 200_000.0),
        MEDIUM(10_000.0 to 40_000.0),
        SMALL(1_000.0 to 4_000.0),
    }

    fun drawFacility(cell: Cell) {
        val complexity = when (cell.adjacentCells.size) {
            1, 2, 3 -> COMPLEX
            4, 5 -> INTERMEDIATE
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

        val buildingsBoundingBox = buildings.flatten().getBoundingBox()
        val buildingsCentreDeltaX = cell.site.x - buildingsBoundingBox.centre.x
        val buildingsCentreDeltaY = cell.site.y - buildingsBoundingBox.centre.y
        val facility = buildings.map { it.translate(buildingsCentreDeltaX, buildingsCentreDeltaY) }

        val perimeter = generatePerimeter(buildingsBoundingBox).translate(buildingsCentreDeltaX, buildingsCentreDeltaY)

        imageBuilder.drawShapeFill(perimeter, color = Palette.Colours.BACKING)

        imageBuilder.drawShapeFills(facility, color = Palette.Colours.LIGHT)
    }

    /**
     * One corner is always at [0,0] while the other is random but obeys the generated floor area.
     */
    private fun generateBuilding(buildingSize: BuildingSize): Rectangle {
        val (areaMin, areaMax) = buildingSize.sizeRange
        val area = RandomGenerator.generateUniform(areaMin, areaMax)

        val squareLength = sqrt(area)
        val buildingWidth = RandomGenerator.generateGaussian(average = squareLength, stdDev = squareLength / 5)
        val buildingHeight = area / buildingWidth
        return Rectangle(ORIGIN, Point(buildingWidth / metresPerPixel, buildingHeight / metresPerPixel))
    }

    /**
     * Create an octagon around the bounding box of the buildings.
     */
    private fun generatePerimeter(buildingsBoundingBox: Rectangle) = buildingsBoundingBox
        .let { box ->
            val padding = PERIMETER_PADDING / metresPerPixel
            listOf(
                box.topLeft.translate(-padding, 0.0),
                box.topLeft.translate(0.0, -padding),
                box.topRight.translate(0.0, -padding),
                box.topRight.translate(padding, 0.0),
                box.bottomRight.translate(padding, 0.0),
                box.bottomRight.translate(0.0, padding),
                box.bottomLeft.translate(0.0, padding),
                box.bottomLeft.translate(-padding, 0.0),
            )
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

    companion object {
        const val PERIMETER_PADDING = 25.0
    }
}