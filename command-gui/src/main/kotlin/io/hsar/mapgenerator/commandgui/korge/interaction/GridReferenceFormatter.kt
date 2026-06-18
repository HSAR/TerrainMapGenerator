package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.GridStyles
import kotlin.math.absoluteValue

internal object GridReferenceFormatter {
    fun format(layer: GridLayer, mapX: Double, mapY: Double): String {
        val reference = formatReference(layer, mapX, mapY)
        return "${reference.easting} ${reference.northing}"
    }

    fun formatFields(layer: GridLayer, mapX: Double, mapY: Double): GridReferenceFields =
        formatReference(layer, mapX, mapY)

    fun parseFields(layer: GridLayer, easting: String, northing: String): MapReferencePixels? {
        val parsedEasting = parseCoordinate(easting) ?: return null
        val parsedNorthing = parseCoordinate(northing) ?: return null
        val eastingMetres = parsedEasting * METRES_PER_REFERENCE_DIGIT
        val northingMetres = parsedNorthing * METRES_PER_REFERENCE_DIGIT
        return MapReferencePixels(
            x = GridStyles.GRID_LINE_ID_PADDING.x +
                    (eastingMetres - layer.verticalLineNumberStart * layer.gridScale) / layer.metresPerPixel,
            y = GridStyles.GRID_LINE_ID_PADDING.y +
                    (layer.horizontalLineNumberStart * layer.gridScale - northingMetres) / layer.metresPerPixel,
        )
    }

    private fun formatReference(layer: GridLayer, mapX: Double, mapY: Double): GridReferenceFields {
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
        return GridReferenceFields(
            easting = formatCoordinate(eastingMetres / METRES_PER_REFERENCE_DIGIT),
            northing = formatCoordinate(northingMetres / METRES_PER_REFERENCE_DIGIT),
        )
    }

    private fun parseCoordinate(value: String): Int? =
        value.trim().toIntOrNull()
            ?.coerceIn(MIN_REFERENCE_DIGITS, MAX_REFERENCE_DIGITS)

    private fun formatCoordinate(value: Int): String =
        if (value < 0) {
            "-${value.absoluteValue.toString().padStart(REFERENCE_DIGITS, '0')}"
        } else {
            value.toString().padStart(REFERENCE_DIGITS, '0')
        }

    private const val REFERENCE_DIGITS = 4
    private const val METRES_PER_REFERENCE_DIGIT = 10
    private const val MIN_REFERENCE_DIGITS = -9_999
    private const val MAX_REFERENCE_DIGITS = 9_999
    private const val MIN_REFERENCE_METRES = -99_990
    private const val MAX_REFERENCE_METRES = 99_990
}

internal data class GridReferenceFields(
    val easting: String,
    val northing: String,
)

internal data class MapReferencePixels(
    val x: Double,
    val y: Double,
)
