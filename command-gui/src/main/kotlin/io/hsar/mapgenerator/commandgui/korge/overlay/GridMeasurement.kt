package io.hsar.mapgenerator.commandgui.korge.overlay

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal data class GridMeasurement(
    val originMapX: Double,
    val originMapY: Double,
    val destinationMapX: Double,
    val destinationMapY: Double,
    val distanceUnits: Double,
    val unitLabel: String,
    val bearingDegrees: Int,
) {
    val distanceText: String
        get() = "${formatDistance(distanceUnits)}$unitLabel"

    val bearingText: String
        get() = "${bearingDegrees.toString().padStart(3, '0')}\u00b0"

    private fun formatDistance(value: Double): String =
        BigDecimal.valueOf(value)
            .round(MathContext(DISTANCE_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
            .setScale(MAX_DISTANCE_DECIMAL_PLACES, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private companion object {
        const val DISTANCE_SIGNIFICANT_DIGITS = 4
        const val MAX_DISTANCE_DECIMAL_PLACES = 1
    }
}
