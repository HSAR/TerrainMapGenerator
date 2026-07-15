package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class EcmActivation(
    val turnNumber: Int,
    val emitterId: MapPlacedObjectId,
    val moduleKeys: Set<String>,
    val relativeBearingDegrees: Double,
    val rangeU: Double,
    val widthDegrees: Double,
    val intensity: Int,
)

internal object EcmRules {
    const val CAPABILITY_PER_MODULE = 120.0
    const val MINIMUM_RANGE_U = 1.0
    const val MINIMUM_WIDTH_DEGREES = 10.0
    const val MAXIMUM_WIDTH_DEGREES = 360.0
    const val MAXIMUM_INTENSITY = 4

    fun capability(moduleCount: Int, intensity: Int): Double =
        CAPABILITY_PER_MODULE * moduleCount.coerceAtLeast(0) / 2.0.pow((intensity.coerceIn(1, MAXIMUM_INTENSITY) - 1).toDouble())

    fun range(moduleCount: Int, widthDegrees: Double, intensity: Int): Double =
        capability(moduleCount, intensity) / widthDegrees.coerceIn(MINIMUM_WIDTH_DEGREES, MAXIMUM_WIDTH_DEGREES)

    fun valid(moduleCount: Int, rangeU: Double, widthDegrees: Double, intensity: Int): Boolean =
        moduleCount > 0 && intensity in 1..MAXIMUM_INTENSITY && rangeU >= MINIMUM_RANGE_U &&
                widthDegrees in MINIMUM_WIDTH_DEGREES..MAXIMUM_WIDTH_DEGREES &&
                rangeU * widthDegrees <= capability(moduleCount, intensity) + EPSILON

    fun automatic(moduleCount: Int, targetRangeU: Double): EcmShape? {
        val fittedRange = max(MINIMUM_RANGE_U, targetRangeU)
        val intensity = (MAXIMUM_INTENSITY downTo 1).firstOrNull {
            fittedRange * MINIMUM_WIDTH_DEGREES <= capability(moduleCount, it) + EPSILON
        } ?: return null
        val width = (capability(moduleCount, intensity) / fittedRange).coerceAtMost(MAXIMUM_WIDTH_DEGREES)
        return EcmShape(fittedRange, width, intensity)
    }

    fun manual(moduleCount: Int, widthDegrees: Double = 60.0, intensity: Int = 1): EcmShape? =
        range(moduleCount, widthDegrees, intensity)
            .takeIf { it >= MINIMUM_RANGE_U }
            ?.let { EcmShape(it, widthDegrees.coerceIn(MINIMUM_WIDTH_DEGREES, MAXIMUM_WIDTH_DEGREES), intensity) }

    fun closestManualIntensity(moduleCount: Int, widthDegrees: Double, desiredRangeU: Double): EcmShape? =
        (1..MAXIMUM_INTENSITY).mapNotNull { intensity -> manual(moduleCount, widthDegrees, intensity) }
            .minByOrNull { kotlin.math.abs(it.rangeU - desiredRangeU) }

    fun resizedManual(moduleCount: Int, desiredWidthDegrees: Double, intensity: Int): EcmShape? {
        val usableCapability = capability(moduleCount, intensity)
        val maximumValidWidth = min(MAXIMUM_WIDTH_DEGREES, usableCapability / MINIMUM_RANGE_U)
        if (maximumValidWidth < MINIMUM_WIDTH_DEGREES) return null
        val desiredWidth = desiredWidthDegrees.coerceIn(MINIMUM_WIDTH_DEGREES, maximumValidWidth)
        val maximumRange = usableCapability / MINIMUM_WIDTH_DEGREES
        val snappedRange = ((usableCapability / desiredWidth) / RANGE_SNAP_U).roundToInt() * RANGE_SNAP_U
        val fittedRange = snappedRange.coerceIn(MINIMUM_RANGE_U, maximumRange)
        return EcmShape(fittedRange, usableCapability / fittedRange, intensity)
    }

    fun contains(activation: EcmActivation, emitter: UnitSnapshot, point: MapCoordinate, unitQueryService: UnitQueryService): Boolean {
        val distance = unitQueryService.distanceBetween(emitter.mapCoordinate, point)
        if (distance > activation.rangeU + EPSILON) return false
        if (activation.widthDegrees >= MAXIMUM_WIDTH_DEGREES - EPSILON) return true
        val absoluteBearing = activation.relativeBearingDegrees.mod(360.0)
        val pointBearing = bearing(emitter.mapCoordinate, point, unitQueryService)
        val delta = ((pointBearing - absoluteBearing + 540.0).mod(360.0)) - 180.0
        return kotlin.math.abs(delta) <= activation.widthDegrees * 0.5 + EPSILON
    }

    fun absoluteBearing(emitter: UnitSnapshot, point: MapCoordinate, unitQueryService: UnitQueryService): Double =
        bearing(emitter.mapCoordinate, point, unitQueryService)

    private fun bearing(origin: MapCoordinate, point: MapCoordinate, unitQueryService: UnitQueryService): Double =
        Math.toDegrees(
            atan2(
                (point.x - origin.x) * unitQueryService.mapWidth,
                -(point.y - origin.y) * unitQueryService.mapHeight,
            )
        ).mod(360.0)

    private const val EPSILON = 0.000001
    private const val RANGE_SNAP_U = 0.25
}

internal data class EcmShape(val rangeU: Double, val widthDegrees: Double, val intensity: Int)
