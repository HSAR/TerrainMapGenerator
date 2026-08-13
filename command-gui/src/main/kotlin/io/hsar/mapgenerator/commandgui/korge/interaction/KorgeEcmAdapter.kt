package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationEcmActivation
import io.hsar.mapgenerator.commandgui.operation.OperationEcmRules
import io.hsar.mapgenerator.commandgui.operation.OperationEcmShape
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId

internal data class EcmActivation(
    val turnNumber: Int,
    val emitterId: MapPlacedObjectId,
    val moduleKeys: Set<String>,
    val relativeBearingDegrees: Double,
    val rangeU: Double,
    val widthDegrees: Double,
    val intensity: Int,
)

internal data class EcmShape(val rangeU: Double, val widthDegrees: Double, val intensity: Int)

internal object EcmRules {
    const val CAPABILITY_PER_MODULE = OperationEcmRules.CAPABILITY_PER_MODULE
    const val MINIMUM_RANGE_U = OperationEcmRules.MINIMUM_RANGE_U
    const val MINIMUM_WIDTH_DEGREES = OperationEcmRules.MINIMUM_WIDTH_DEGREES
    const val MAXIMUM_WIDTH_DEGREES = OperationEcmRules.MAXIMUM_WIDTH_DEGREES
    const val MAXIMUM_INTENSITY = OperationEcmRules.MAXIMUM_INTENSITY

    fun capability(moduleCount: Int, intensity: Int) = OperationEcmRules.capability(moduleCount, intensity)
    fun range(moduleCount: Int, widthDegrees: Double, intensity: Int) = OperationEcmRules.range(moduleCount, widthDegrees, intensity)
    fun valid(moduleCount: Int, rangeU: Double, widthDegrees: Double, intensity: Int) = OperationEcmRules.valid(moduleCount, rangeU, widthDegrees, intensity)
    fun automatic(moduleCount: Int, targetRangeU: Double) = OperationEcmRules.automatic(moduleCount, targetRangeU)?.korge()
    fun manual(moduleCount: Int, widthDegrees: Double = 60.0, intensity: Int = 1) = OperationEcmRules.manual(moduleCount, widthDegrees, intensity)?.korge()
    fun closestManualIntensity(moduleCount: Int, widthDegrees: Double, desiredRangeU: Double) =
        OperationEcmRules.closestManualIntensity(moduleCount, widthDegrees, desiredRangeU)?.korge()
    fun resizedManual(moduleCount: Int, desiredWidthDegrees: Double, intensity: Int) =
        OperationEcmRules.resizedManual(moduleCount, desiredWidthDegrees, intensity)?.korge()
    fun contains(activation: EcmActivation, emitter: UnitSnapshot, point: MapCoordinate, unitQueryService: KorgeUnitQueryAdapter) =
        OperationEcmRules.contains(activation.operation(), emitter.toOperationUnit(), point, unitQueryService.operation)
    fun absoluteBearing(emitter: UnitSnapshot, point: MapCoordinate, unitQueryService: KorgeUnitQueryAdapter) =
        OperationEcmRules.absoluteBearing(emitter.toOperationUnit(), point, unitQueryService.operation)
}

internal fun EcmActivation.operation() = OperationEcmActivation(turnNumber, OperationObjectId(emitterId.value), moduleKeys,
    relativeBearingDegrees, rangeU, widthDegrees, intensity)
internal fun OperationEcmActivation.korge() = EcmActivation(turnNumber, MapPlacedObjectId(emitterId.value), moduleKeys,
    relativeBearingDegrees, rangeU, widthDegrees, intensity)
private fun OperationEcmShape.korge() = EcmShape(rangeU, widthDegrees, intensity)
