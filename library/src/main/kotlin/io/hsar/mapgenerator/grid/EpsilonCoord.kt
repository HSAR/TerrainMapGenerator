package io.hsar.mapgenerator.grid

import kotlin.math.floor

data class EpsilonCoord(
    override val preciseUnits: Double,
) : Coord {
    override fun format(): String =
        if (preciseUnits % 1.0 == 0.0) {
            preciseUnits.toInt().toString()
        } else {
            preciseUnits.toString().trimEnd('0').removeSuffix(".")
        }

    override fun toString(): String =
        format()

    companion object {
        private val PATTERN = Regex("[+-]?\\d+(\\.\\d+)?")

        fun parse(value: String): EpsilonCoord? =
            value.trim()
                .takeIf { it.matches(PATTERN) }
                ?.toDoubleOrNull()
                ?.let(::fromUnits)

        fun parseDesignator(value: String, axis: CoordAxis, gridScale: Int): EpsilonCoord? =
            when (axis) {
                CoordAxis.X -> EpsilonGridDesignators.xIndex(value)
                CoordAxis.Y -> EpsilonGridDesignators.yIndex(value)
            }?.let { index -> fromUnits(index * gridScale) }

        fun formatDesignator(coord: Coord, axis: CoordAxis, gridScale: Int): String =
            when (axis) {
                CoordAxis.X -> EpsilonGridDesignators.xDesignator(floor(coord.preciseUnits / gridScale).toInt())
                CoordAxis.Y -> EpsilonGridDesignators.yDesignator(floor(coord.preciseUnits / gridScale).toInt())
            }

        fun fromUnits(units: Double): EpsilonCoord =
            EpsilonCoord(units)

        fun fromUnits(units: Int): EpsilonCoord =
            fromUnits(units.toDouble())

        fun fromLineNumber(lineNumber: Double, gridScale: Int): EpsilonCoord =
            fromUnits(lineNumber * gridScale)
    }
}
