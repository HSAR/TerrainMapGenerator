package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalTargetingService

internal typealias TacticalWeaponStatus = io.hsar.mapgenerator.commandgui.operation.OperationTacticalWeaponStatus
internal typealias TacticalWeaponContribution = io.hsar.mapgenerator.commandgui.operation.OperationTacticalWeaponContribution

internal data class TacticalTarget(
    val targetId: MapPlacedObjectId,
    val targetName: String,
    val rangeU: Double,
    val arcs: List<String>,
    val firepower: Int,
    val contributions: List<TacticalWeaponContribution>,
    val manualEcm: Boolean = false,
    val shipClass: String? = null,
)

internal data class TacticalTargetingReport(
    val weapons: List<TacticalWeaponStatus>,
    val targets: List<TacticalTarget>,
    val fireControlCapacity: Int = 0,
    val fireControlUsed: Int = 0,
)

internal object TacticalTargetingResolver {
    fun arcForBearing(bearingDegrees: Double, facingDegrees: Int) = OperationTacticalTargetingService.arcForBearing(bearingDegrees, facingDegrees)

    fun resolve(source: UnitSnapshot, unitQueryService: KorgeUnitQueryAdapter, firedWeaponKeys: Set<String> = emptySet(), fireControlUsed: Int = 0): TacticalTargetingReport =
        OperationTacticalTargetingService.resolve(source.toOperationUnit(), unitQueryService.operation, firedWeaponKeys, fireControlUsed).let { report ->
            TacticalTargetingReport(report.weapons, report.targets.map { target ->
                TacticalTarget(MapPlacedObjectId(target.targetId.value), target.targetName, target.rangeU, target.arcs, target.firepower, target.contributions, target.manualEcm, target.shipClass)
            }, report.fireControlCapacity, report.fireControlUsed)
        }

    fun resolve(source: UnitSnapshot, candidates: List<UnitSnapshot>, mapWidth: Int, mapHeight: Int, mapUnitsPerPixel: Double, firedWeaponKeys: Set<String> = emptySet(), fireControlUsed: Int = 0) =
        resolve(source, KorgeUnitQueryAdapter(candidates, mapWidth, mapHeight, mapUnitsPerPixel), firedWeaponKeys, fireControlUsed)
}
