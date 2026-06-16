package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.random.GridGenerator
import io.hsar.mapgenerator.random.RandomGenerator
import java.util.Random
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
    ): GridLayer {
        val pixelsPerLine = gridScale / metresPerPixel
        val detailLevel = getDetailLevel(gridScale)
        val verticalLineCount = (width / pixelsPerLine).toInt()
        val horizontalLineCount = (height / pixelsPerLine).toInt()

        return GridLayer(
            mapId = mapId ?: "${GridGenerator.randomGridZone(random)} ${GridGenerator.random100kmGSID(random)}",
            gridScale = gridScale,
            metresPerPixel = metresPerPixel,
            detailLevel = detailLevel,
            pixelsPerLine = pixelsPerLine,
            verticalLineNumberStart = generateLineNumberStart(detailLevel, verticalLineCount, random),
            horizontalLineNumberStart = generateLineNumberStart(detailLevel, horizontalLineCount, random),
            verticalLineCount = verticalLineCount,
            horizontalLineCount = horizontalLineCount,
        )
    }

    private fun getDetailLevel(gridScale: Int): Int {
        val gridScaleMagnitude = log(gridScale.toDouble(), 10.0).roundToInt()
        require(gridScaleMagnitude < 6) { "Grid scale is too large, needs to be < 100km." }
        return 5 - gridScaleMagnitude
    }

    private fun generateLineNumberStart(detailLevel: Int, totalLines: Int, random: Random): Int {
        val maxCoordinate = 10.0.pow(detailLevel).toInt() - 1
        val minLineNumberStart = totalLines
        val maxLineNumberStart = maxCoordinate - totalLines

        require(minLineNumberStart <= maxLineNumberStart) {
            "Grid has too many visible lines ($totalLines) for detail level $detailLevel."
        }

        return RandomGenerator.generateUniform(minLineNumberStart, maxLineNumberStart, random)
    }
}
