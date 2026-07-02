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
        metresPerPixel: Double,
        random: Random = Random(),
        mapId: String? = null,
        referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
        verticalLineNumberStart: MgrsCoord? = null,
        horizontalLineNumberStart: MgrsCoord? = null,
        colour: Color = GridPalettes.DEFAULT.lineColour,
    ): GridLayer {
        val pixelsPerLine = gridScale / metresPerPixel
        val detailLevel = getDetailLevel(gridScale)
        val verticalLineCount = (width / pixelsPerLine).toInt()
        val horizontalLineCount = (height / pixelsPerLine).toInt()

        return GridLayer(
            mapId = mapId ?: GridGenerator.randomMGRSMapId(random),
            referenceSystem = referenceSystem,
            gridScale = gridScale,
            metresPerPixel = metresPerPixel,
            detailLevel = detailLevel,
            pixelsPerLine = pixelsPerLine,
            verticalLineNumberStart = verticalLineNumberStart ?: generateLineNumberStart(detailLevel, verticalLineCount, random),
            horizontalLineNumberStart = horizontalLineNumberStart ?: generateLineNumberStart(detailLevel, horizontalLineCount, random),
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

    private fun generateLineNumberStart(detailLevel: Int, totalLines: Int, random: Random): MgrsCoord {
        val maxCoordinate = 10.0.pow(detailLevel).toInt() - 1
        val minLineNumberStart = totalLines
        val maxLineNumberStart = maxCoordinate - totalLines

        require(minLineNumberStart <= maxLineNumberStart) {
            "Grid has too many visible lines ($totalLines) for detail level $detailLevel."
        }

        return MgrsCoord.fromLineNumber(
            lineNumber = RandomGenerator.generateUniform(minLineNumberStart, maxLineNumberStart, random).toDouble(),
            gridScale = 10.0.pow(5 - detailLevel).toInt(),
        )
    }
}
