package io.hsar.mapgenerator.grid

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class MgrsCoord(
    val metres: Int,
) {
    fun lineNumber(gridScale: Int): Double =
        metres.toDouble() / gridScale

    fun lineNumberCeil(gridScale: Int): Double =
        ceil(lineNumber(gridScale))

    fun lineNumberFloor(gridScale: Int): Double =
        floor(lineNumber(gridScale))

    fun format(): String =
        if (metres < 0) {
            "-${(-metres).formatDigits()}"
        } else {
            metres.formatDigits()
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
                ?.let(::fromMetres)

        fun fromMetres(metres: Int): MgrsCoord =
            MgrsCoord(metres)

        fun fromLineNumber(lineNumber: Double, gridScale: Int): MgrsCoord =
            fromMetres((lineNumber * gridScale).roundToInt())
    }
}
