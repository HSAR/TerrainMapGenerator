package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.random.GridGenerator
import io.hsar.mapgenerator.random.RandomGenerator
import java.awt.Color
import java.util.*
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

object GridLayerBuilder {

    fun build(
        width: Int,
        height: Int,
        gridScale: Int,
        unitsPerPixel: Double,
        random: Random = Random(),
        mapId: String? = null,
        referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
        verticalLineNumberStart: Coord? = null,
        horizontalLineNumberStart: Coord? = null,
        colour: Color = GridPalettes.DEFAULT.lineColour,
    ): GridLayer {
        val pixelsPerLine = gridScale / unitsPerPixel
        val detailLevel = getDetailLevel(gridScale)
        val verticalLineCount = (width / pixelsPerLine).toInt()
        val horizontalLineCount = (height / pixelsPerLine).toInt()

        return GridLayer(
            mapId = mapId ?: defaultMapId(referenceSystem, random),
            referenceSystem = referenceSystem,
            gridScale = gridScale,
            unitsPerPixel = unitsPerPixel,
            detailLevel = detailLevel,
            pixelsPerLine = pixelsPerLine,
            verticalLineNumberStart = verticalLineNumberStart ?: generateLineNumberStart(referenceSystem, CoordAxis.X, detailLevel, verticalLineCount, gridScale, random),
            horizontalLineNumberStart = horizontalLineNumberStart ?: generateLineNumberStart(referenceSystem, CoordAxis.Y, detailLevel, horizontalLineCount, gridScale, random),
            verticalLineCount = verticalLineCount,
            horizontalLineCount = horizontalLineCount,
            colour = colour,
        )
    }

    private fun getDetailLevel(gridScale: Int): Int {
        val gridScaleMagnitude = log(gridScale.toDouble(), 10.0).roundToInt()
        require(gridScaleMagnitude < 6) { "Grid scale is too large, needs to be < 100km." }
        return 5 - gridScaleMagnitude
    }

    private fun generateLineNumberStart(
        referenceSystem: ReferenceSystem,
        axis: CoordAxis,
        detailLevel: Int,
        totalLines: Int,
        gridScale: Int,
        random: Random,
    ): Coord {
        val maxCoordinate = maxLineNumberStart(referenceSystem, axis, detailLevel)
        val minLineNumberStart = totalLines
        val maxLineNumberStart = maxCoordinate - totalLines

        require(minLineNumberStart <= maxLineNumberStart) {
            "Grid has too many visible lines ($totalLines) for detail level $detailLevel."
        }

        return referenceSystem.coordFromLineNumber(
            lineNumber = RandomGenerator.generateUniform(minLineNumberStart, maxLineNumberStart, random).toDouble(),
            gridScale = gridScale,
        )
    }

    private fun maxLineNumberStart(referenceSystem: ReferenceSystem, axis: CoordAxis, detailLevel: Int): Int =
        when (referenceSystem) {
            ReferenceSystem.MGRS -> 10.0.pow(detailLevel).toInt() - 1
            ReferenceSystem.EPSILON -> when (axis) {
                CoordAxis.X -> EpsilonGridDesignators.MAX_X_SQUARES - 1
                CoordAxis.Y -> EpsilonGridDesignators.MAX_Y_SQUARES - 1
            }
        }

    private fun defaultMapId(referenceSystem: ReferenceSystem, random: Random): String =
        when (referenceSystem) {
            ReferenceSystem.MGRS -> GridGenerator.randomMGRSMapId(random)
            ReferenceSystem.EPSILON -> "EPSILON"
        }
}
