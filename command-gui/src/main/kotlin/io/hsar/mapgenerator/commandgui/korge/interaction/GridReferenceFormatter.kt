package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import kotlin.math.absoluteValue

internal object GridReferenceFormatter {
    fun format(layer: GridLayer, mapX: Double, mapY: Double): String {
        val eastingMetres = (
            layer.verticalLineNumberStart * layer.gridScale +
                ((mapX - GridStyles.GRID_LINE_ID_PADDING.x) * layer.metresPerPixel).toInt()
            )
            .coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES)
        val northingMetres = (
            layer.horizontalLineNumberStart * layer.gridScale -
                ((mapY - GridStyles.GRID_LINE_ID_PADDING.y) * layer.metresPerPixel).toInt()
            )
            .coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES)
        val easting = formatCoordinate(eastingMetres / METRES_PER_REFERENCE_DIGIT)
        val northing = formatCoordinate(northingMetres / METRES_PER_REFERENCE_DIGIT)

        return "$easting $northing"
    }

    private fun formatCoordinate(value: Int): String =
        if (value < 0) {
            "-${value.absoluteValue.toString().padStart(REFERENCE_DIGITS, '0')}"
        } else {
            value.toString().padStart(REFERENCE_DIGITS, '0')
        }

    private const val REFERENCE_DIGITS = 4
    private const val METRES_PER_REFERENCE_DIGIT = 10
    private const val MIN_REFERENCE_METRES = -99_990
    private const val MAX_REFERENCE_METRES = 99_990
}
