package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal object GridReferenceFormatter {
    fun format(
        layer: GridLayer,
        mapWidth: Int,
        mapHeight: Int,
        mapX: Double,
        mapY: Double,
        gutterWidth: Int = mapWidth,
        gutterHeight: Int = mapHeight,
    ): String {
        val reference = formatReference(layer, gutterWidth, gutterHeight, mapX, mapY)
        return "${reference.easting} ${reference.northing}"
    }

    fun formatFields(
        layer: GridLayer,
        mapWidth: Int,
        mapHeight: Int,
        mapX: Double,
        mapY: Double,
        gutterWidth: Int = mapWidth,
        gutterHeight: Int = mapHeight,
    ): GridReferenceFields =
        formatReference(layer, gutterWidth, gutterHeight, mapX, mapY)

    fun parseFields(
        layer: GridLayer,
        mapWidth: Int,
        mapHeight: Int,
        easting: String,
        northing: String,
        gutterWidth: Int = mapWidth,
        gutterHeight: Int = mapHeight,
    ): MapReferencePixels? {
        val parsedEasting = parseCoordinate(easting) ?: return null
        val parsedNorthing = parseCoordinate(northing) ?: return null
        val eastingMetres = parsedEasting * METRES_PER_REFERENCE_DIGIT
        val northingMetres = parsedNorthing * METRES_PER_REFERENCE_DIGIT
        return MapReferencePixels(
            x = (eastingMetres - layer.verticalLineNumberStart * layer.gridScale) / layer.metresPerPixel,
            y = (layer.horizontalLineNumberStart * layer.gridScale - northingMetres) / layer.metresPerPixel,
        )
    }

    private fun formatReference(layer: GridLayer, gutterWidth: Int, gutterHeight: Int, mapX: Double, mapY: Double): GridReferenceFields {
        val eastingMetres = (
            layer.verticalLineNumberStart * layer.gridScale +
                    (mapX * layer.metresPerPixel)
            )
            .roundToInt()
            .coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES)
        val northingMetres = (
            layer.horizontalLineNumberStart * layer.gridScale -
                    (mapY * layer.metresPerPixel)
            )
            .roundToInt()
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
