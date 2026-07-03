package io.hsar.mapgenerator.grid

import kotlin.math.roundToInt

data class EpsilonCoord(
    override val units: Int,
) : Coord {
    override fun format(): String =
        units.toString()

    override fun toString(): String =
        format()

    companion object {
        private val PATTERN = Regex("[+-]?\\d+")

        fun parse(value: String): EpsilonCoord? =
            value.trim()
                .takeIf { it.matches(PATTERN) }
                ?.toIntOrNull()
                ?.let(::fromUnits)

        fun parseDesignator(value: String, axis: CoordAxis, gridScale: Int): EpsilonCoord? =
            when (axis) {
                CoordAxis.X -> EpsilonGridDesignators.xIndex(value)
                CoordAxis.Y -> EpsilonGridDesignators.yIndex(value)
            }?.let { index -> fromUnits(index * gridScale) }

        fun formatDesignator(coord: Coord, axis: CoordAxis, gridScale: Int): String =
            when (axis) {
                CoordAxis.X -> EpsilonGridDesignators.xDesignator(Math.floorDiv(coord.units, gridScale))
                CoordAxis.Y -> EpsilonGridDesignators.yDesignator(Math.floorDiv(coord.units, gridScale))
            }

        fun fromUnits(units: Int): EpsilonCoord =
            EpsilonCoord(units)

        fun fromLineNumber(lineNumber: Double, gridScale: Int): EpsilonCoord =
            fromUnits((lineNumber * gridScale).roundToInt())
    }
}
