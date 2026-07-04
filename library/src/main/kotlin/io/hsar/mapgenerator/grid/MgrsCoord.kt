package io.hsar.mapgenerator.grid

data class MgrsCoord(
    override val preciseUnits: Double,
) : Coord {
    val metres: Double
        get() = preciseUnits

    override fun format(): String =
        if (preciseUnits < 0) {
            "-${(-preciseUnits).formatDigits()}"
        } else {
            preciseUnits.formatDigits()
        }

    private fun Double.formatDigits(): String =
        formatNumber()
            .padStart(MAX_DIGITS, '0')
            .trimEnd('0')
            .removeSuffix(".")
            .ifBlank { "0" }

    private fun Double.formatNumber(): String =
        if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            toString().trimEnd('0').removeSuffix(".")
        }

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

        fun fromUnits(units: Double): MgrsCoord =
            MgrsCoord(units)

        fun fromUnits(units: Int): MgrsCoord =
            fromUnits(units.toDouble())

        fun fromMetres(metres: Int): MgrsCoord =
            fromUnits(metres)

        fun fromLineNumber(lineNumber: Double, gridScale: Int): MgrsCoord =
            fromUnits(lineNumber * gridScale)
    }
}
