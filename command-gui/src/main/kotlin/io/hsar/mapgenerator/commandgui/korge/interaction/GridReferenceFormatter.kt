package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.EpsilonGridDesignators
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
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
        if (layer.referenceSystem == ReferenceSystem.EPSILON) {
            return parseEpsilon(layer, "$easting $northing")
        }

        val parsedEasting = layer.referenceSystem.parseCoord(easting) ?: return null
        val parsedNorthing = layer.referenceSystem.parseCoord(northing) ?: return null
        return MapReferencePixels(
            x = (parsedEasting.units - layer.verticalLineNumberStart.units) / layer.unitsPerPixel,
            y = (layer.horizontalLineNumberStart.units - parsedNorthing.units) / layer.unitsPerPixel,
        )
    }

    private fun formatReference(layer: GridLayer, gutterWidth: Int, gutterHeight: Int, mapX: Double, mapY: Double): GridReferenceFields {
        if (layer.referenceSystem == ReferenceSystem.EPSILON) {
            return formatEpsilon(layer, mapX, mapY)
        }

        val eastingUnits = (
                layer.verticalLineNumberStart.units +
                        (mapX * layer.unitsPerPixel)
            )
            .roundToInt()
            .coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES)
        val northingUnits = (
                layer.horizontalLineNumberStart.units -
                        (mapY * layer.unitsPerPixel)
            )
            .roundToInt()
            .coerceIn(MIN_REFERENCE_METRES, MAX_REFERENCE_METRES)
        return GridReferenceFields(
            easting = formatCoordinate(eastingUnits / UNITS_PER_REFERENCE_DIGIT),
            northing = formatCoordinate(northingUnits / UNITS_PER_REFERENCE_DIGIT),
        )
    }

    private fun formatEpsilon(layer: GridLayer, mapX: Double, mapY: Double): GridReferenceFields {
        val eastingUnits = layer.verticalLineNumberStart.units + (mapX * layer.unitsPerPixel).roundToInt()
        val northingUnits = layer.horizontalLineNumberStart.units - (mapY * layer.unitsPerPixel).roundToInt()
        val xSquareIndex = Math.floorDiv(eastingUnits, layer.gridScale)
        val ySquareIndex = Math.floorDiv(northingUnits, layer.gridScale)
        val eastPercent = percentWithinSquare(eastingUnits, layer.gridScale)
        val northPercent = percentWithinSquare(northingUnits, layer.gridScale)
        return GridReferenceFields(
            easting = "${EpsilonGridDesignators.yDesignator(ySquareIndex)}${EpsilonGridDesignators.xDesignator(xSquareIndex)}",
            northing = "${eastPercent.toString().padStart(2, '0')}-${northPercent.toString().padStart(2, '0')}",
        )
    }

    private fun parseEpsilon(layer: GridLayer, reference: String): MapReferencePixels? {
        val match = EPSILON_PATTERN.matchEntire(reference.trim().uppercase()) ?: return null
        val yDesignator = match.groupValues[1]
        val xDesignator = match.groupValues[2].toIntOrNull() ?: return null
        val eastPercent = match.groupValues[3].toIntOrNull()?.takeIf { it in 0..99 } ?: return null
        val northPercent = match.groupValues[4].toIntOrNull()?.takeIf { it in 0..99 } ?: return null
        val xSquareIndex = (xDesignator - 1).takeIf { it in 0 until EpsilonGridDesignators.MAX_X_SQUARES } ?: return null
        val ySquareIndex = EpsilonGridDesignators.yIndex(yDesignator) ?: return null
        val eastingUnits = (xSquareIndex * layer.gridScale) + (eastPercent * layer.gridScale / 100.0).roundToInt()
        val northingUnits = (ySquareIndex * layer.gridScale) + (northPercent * layer.gridScale / 100.0).roundToInt()
        return MapReferencePixels(
            x = (eastingUnits - layer.verticalLineNumberStart.units) / layer.unitsPerPixel,
            y = (layer.horizontalLineNumberStart.units - northingUnits) / layer.unitsPerPixel,
        )
    }

    private fun percentWithinSquare(units: Int, gridScale: Int): Int =
        ((Math.floorMod(units, gridScale).toDouble() / gridScale) * 100)
            .roundToInt()
            .coerceIn(0, 99)

    private fun formatCoordinate(value: Int): String =
        if (value < 0) {
            "-${value.absoluteValue.toString().padStart(REFERENCE_DIGITS, '0')}"
        } else {
            value.toString().padStart(REFERENCE_DIGITS, '0')
        }

    private const val REFERENCE_DIGITS = 4
    private const val UNITS_PER_REFERENCE_DIGIT = 10
    private const val MIN_REFERENCE_METRES = -99_990
    private const val MAX_REFERENCE_METRES = 99_990
    private val EPSILON_PATTERN = Regex("([A-Z]{1,2})(\\d{1,3})\\s+(\\d{1,2})-(\\d{1,2})")
}

internal data class GridReferenceFields(
    val easting: String,
    val northing: String,
)

internal data class MapReferencePixels(
    val x: Double,
    val y: Double,
)
