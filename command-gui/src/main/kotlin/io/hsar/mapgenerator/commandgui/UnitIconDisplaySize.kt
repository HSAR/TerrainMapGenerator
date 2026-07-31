package io.hsar.mapgenerator.commandgui

import kotlin.math.roundToInt

/** Global, screen-space unit-icon display size shared by map viewer backends. */
object UnitIconDisplaySize {
    const val DEFAULT_PX = 32.0
    const val MIN_PX = 12.0
    const val MAX_PX = 128.0
    const val SCROLL_STEP = 1.12

    fun coerce(sizePx: Double?): Double =
        (sizePx ?: DEFAULT_PX).coerceIn(MIN_PX, MAX_PX)

    fun scroll(sizePx: Double, scrollDeltaY: Float): Double =
        coerce(coerce(sizePx) * if (scrollDeltaY < 0f) SCROLL_STEP else 1.0 / SCROLL_STEP)

    fun format(sizePx: Double): String =
        coerce(sizePx).roundToInt().toString()

    fun parse(text: String): Double? =
        text.trim().toDoubleOrNull()?.let(::coerce)
}
