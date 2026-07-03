package io.hsar.mapgenerator.commandgui.korge.overlay

internal data class GridMeasurement(
    val originMapX: Double,
    val originMapY: Double,
    val destinationMapX: Double,
    val destinationMapY: Double,
    val distanceUnits: Int,
    val unitLabel: String,
    val bearingDegrees: Int,
) {
    val distanceText: String
        get() = "$distanceUnits$unitLabel"

    val bearingText: String
        get() = "${bearingDegrees.toString().padStart(3, '0')}\u00b0"
}
