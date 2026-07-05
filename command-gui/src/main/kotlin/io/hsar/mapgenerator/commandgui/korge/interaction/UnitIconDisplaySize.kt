package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.math.roundToInt

internal object UnitIconDisplaySize {
    const val DEFAULT_PX = 32.0
    const val MIN_PX = 12.0
    const val MAX_PX = 128.0
    const val SCROLL_STEP = 1.12

    fun coerce(sizePx: Double?): Double =
        (sizePx ?: DEFAULT_PX).coerceIn(MIN_PX, MAX_PX)

    fun format(sizePx: Double): String =
        coerce(sizePx).roundToInt().toString()

    fun parse(text: String): Double? =
        text.trim()
            .toDoubleOrNull()
            ?.let(::coerce)
}
