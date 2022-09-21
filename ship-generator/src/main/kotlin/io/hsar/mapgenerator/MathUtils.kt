package io.hsar.mapgenerator

import kotlin.math.max
import kotlin.math.roundToInt

object MathUtils {
    fun Double.roundWithMinValue(minValue: Int = 1) = max(minValue, this.roundToInt())
}