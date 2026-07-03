package io.hsar.mapgenerator.grid

import kotlin.math.roundToInt

data class MgrsCoord(
    override val units: Int,
) : Coord {
    val metres: Int
        get() = units

    override fun format(): String =
        if (units < 0) {
            "-${(-units).formatDigits()}"
        } else {
            units.formatDigits()
        }

    private fun Int.formatDigits(): String =
        toString()
            .padStart(MAX_DIGITS, '0')
            .trimEnd('0')
            .ifBlank { "0" }

    fun formatForGridScale(gridScale: Int): String =
        lineNumber(gridScale)
            .let { lineNumber ->
                if (lineNumber % 1.0 == 0.0) {
                    lineNumber.toInt().toString()
                } else {
                    lineNumber.toString().trimEnd('0').removeSuffix(".")
                }
            }

    override fun toString(): String =
        format()

    companion object {
        private const val MAX_DIGITS = 5
        private val PATTERN = Regex("[+-]?\\d{1,$MAX_DIGITS}")

        fun parse(value: String): MgrsCoord? =
            value.trim()
                .takeIf { it.matches(PATTERN) }
                ?.let { trimmed ->
                    val sign = if (trimmed.startsWith("-")) -1 else 1
                    val digits = trimmed.removePrefix("-").removePrefix("+")
                    sign * digits.padEnd(MAX_DIGITS, '0').toInt()
                }
                ?.let(::fromUnits)

        fun fromUnits(units: Int): MgrsCoord =
            MgrsCoord(units)

        fun fromMetres(metres: Int): MgrsCoord =
            fromUnits(metres)

        fun fromLineNumber(lineNumber: Double, gridScale: Int): MgrsCoord =
            fromUnits((lineNumber * gridScale).roundToInt())
    }
}
