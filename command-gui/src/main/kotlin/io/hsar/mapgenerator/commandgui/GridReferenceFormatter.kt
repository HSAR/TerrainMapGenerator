package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.EpsilonGridDesignators
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object GridReferenceFormatter {
    fun format(
        layer: GridLayer, mapWidth: Int, mapHeight: Int, mapX: Double, mapY: Double,
        gutterWidth: Int = mapWidth, gutterHeight: Int = mapHeight
    ): String =
        formatFields(layer, mapWidth, mapHeight, mapX, mapY, gutterWidth, gutterHeight).let { "${it.easting} ${it.northing}" }

    fun formatFields(
        layer: GridLayer, mapWidth: Int, mapHeight: Int, mapX: Double, mapY: Double,
        gutterWidth: Int = mapWidth, gutterHeight: Int = mapHeight
    ): GridReferenceFields =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) formatEpsilon(layer, mapX, mapY) else GridReferenceFields(
            formatCoordinate(
                (layer.verticalLineNumberStart.units + mapX * layer.unitsPerPixel).roundToInt().coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES) / UNITS_PER_REFERENCE_DIGIT
            ),
            formatCoordinate(
                (layer.horizontalLineNumberStart.units - mapY * layer.unitsPerPixel).roundToInt().coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES) / UNITS_PER_REFERENCE_DIGIT
            ),
        )

    fun parseFields(
        layer: GridLayer, mapWidth: Int, mapHeight: Int, easting: String, northing: String,
        gutterWidth: Int = mapWidth, gutterHeight: Int = mapHeight
    ): MapReferencePixels? =
        if (layer.referenceSystem == ReferenceSystem.EPSILON) parseEpsilon(layer, "$easting $northing")
        else {
            val east = layer.referenceSystem.parseCoord(easting) ?: return null
            val north = layer.referenceSystem.parseCoord(northing) ?: return null
            MapReferencePixels(
                (east.preciseUnits - layer.verticalLineNumberStart.preciseUnits) / layer.unitsPerPixel,
                (layer.horizontalLineNumberStart.preciseUnits - north.preciseUnits) / layer.unitsPerPixel,
            )
        }

    private fun formatEpsilon(layer: GridLayer, mapX: Double, mapY: Double): GridReferenceFields {
        val easting = layer.verticalLineNumberStart.preciseUnits + mapX * layer.unitsPerPixel
        val northing = layer.horizontalLineNumberStart.preciseUnits - mapY * layer.unitsPerPixel
        val xSquare = kotlin.math.floor(easting / layer.gridScale).toInt()
        val ySquare = kotlin.math.floor(northing / layer.gridScale).toInt()
        return GridReferenceFields(
            "${EpsilonGridDesignators.yDesignator(ySquare)}${EpsilonGridDesignators.xDesignator(xSquare)}",
            "${thousandthsWithinSquare(easting, layer.gridScale).toString().padStart(3, '0')}-${thousandthsWithinSquare(northing, layer.gridScale).toString().padStart(3, '0')}",
        )
    }

    private fun parseEpsilon(layer: GridLayer, reference: String): MapReferencePixels? {
        val match = EPSILON_PATTERN.matchEntire(reference.trim().uppercase()) ?: return null
        val yDesignator = match.groupValues[1]
        val xDesignator = match.groupValues[2].toIntOrNull() ?: return null
        val eastOffset = match.groupValues[3].toIntOrNull()?.takeIf { it in 0..999 } ?: return null
        val northOffset = match.groupValues[4].toIntOrNull()?.takeIf { it in 0..999 } ?: return null
        val xSquare = (xDesignator - 1).takeIf { it in 0 until EpsilonGridDesignators.MAX_X_SQUARES } ?: return null
        val ySquare = EpsilonGridDesignators.yIndex(yDesignator) ?: return null
        val easting = xSquare * layer.gridScale + eastOffset * layer.gridScale / 1_000.0
        val northing = ySquare * layer.gridScale + northOffset * layer.gridScale / 1_000.0
        return MapReferencePixels(
            (easting - layer.verticalLineNumberStart.preciseUnits) / layer.unitsPerPixel,
            (layer.horizontalLineNumberStart.preciseUnits - northing) / layer.unitsPerPixel,
        )
    }

    private fun thousandthsWithinSquare(units: Double, scale: Int) = ((((units % scale) + scale) % scale / scale) * 1_000).toInt().coerceIn(0, 999)
    private fun formatCoordinate(value: Int) =
        if (value < 0) "-${value.absoluteValue.toString().padStart(REFERENCE_DIGITS, '0')}" else value.toString().padStart(REFERENCE_DIGITS, '0')

    private const val REFERENCE_DIGITS = 4
    private const val UNITS_PER_REFERENCE_DIGIT = 10
    private const val MIN_REFERENCE_METRES = -99_990
    private const val MAX_REFERENCE_METRES = 99_990
    private val EPSILON_PATTERN = Regex("([A-Z]{1,2})(\\d{1,3})\\s+(\\d{1,3})-(\\d{1,3})")
}

data class GridReferenceFields(val easting: String, val northing: String)
data class MapReferencePixels(val x: Double, val y: Double)
