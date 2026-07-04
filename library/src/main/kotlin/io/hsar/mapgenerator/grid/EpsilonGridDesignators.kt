package io.hsar.mapgenerator.grid

object EpsilonGridDesignators {
    private const val LETTERS = 26
    const val MAX_X_SQUARES = 100
    const val MAX_Y_SQUARES = LETTERS + (LETTERS * LETTERS)

    fun yDesignator(index: Int): String =
        when {
            index in 0 until LETTERS -> ('A'.code + index).toChar().toString()
            index in LETTERS until MAX_Y_SQUARES -> {
                val pairIndex = index - LETTERS
                "${('A'.code + (pairIndex / LETTERS)).toChar()}${('A'.code + (pairIndex % LETTERS)).toChar()}"
            }

            else -> error("Epsilon Y square index $index is outside supported designators.")
        }

    fun yIndex(designator: String): Int? =
        designator
            .trim()
            .uppercase()
            .takeIf { it.matches(Y_PATTERN) }
            ?.let { value ->
                if (value.length == 1) {
                    value.first().code - 'A'.code
                } else {
                    LETTERS + ((value[0].code - 'A'.code) * LETTERS) + (value[1].code - 'A'.code)
                }
            }

    fun xDesignator(index: Int): String =
        (index + 1)
            .takeIf { it in 1..MAX_X_SQUARES }
            ?.toString()
            ?: error("Epsilon X square index $index is outside supported designators.")

    fun xIndex(designator: String): Int? =
        designator
            .trim()
            .takeIf { it.matches(X_PATTERN) }
            ?.toIntOrNull()
            ?.let { value -> (value - 1).takeIf { it in 0 until MAX_X_SQUARES } }

    private val Y_PATTERN = Regex("[A-Z]{1,2}")
    private val X_PATTERN = Regex("\\d{1,3}")
}
