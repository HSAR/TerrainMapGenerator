package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnitContent
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipSystem
import io.hsar.mapgenerator.commandgui.operation.OperationUnitQueryService

internal data class UnitQueryMatch(
    val unit: UnitSnapshot,
    val distanceUnits: Double,
    val operationalSystems: List<OlympiadDawnShipSystem>,
)

/** Temporary KorGE boundary view over the operation-native query service. */
internal class KorgeUnitQueryAdapter(
    units: List<UnitSnapshot>,
    internal val mapWidth: Int,
    internal val mapHeight: Int,
    private val mapUnitsPerPixel: Double,
) {
    private val snapshots = units.toList()
    private val snapshotsById = snapshots.associateBy { it.id.value }
    internal val operation = OperationUnitQueryService(snapshots.toOperationUnitContent(), mapWidth, mapHeight, mapUnitsPerPixel)

    fun allUnits(): List<UnitSnapshot> = snapshots

    fun replacing(updatedUnit: UnitSnapshot): KorgeUnitQueryAdapter =
        KorgeUnitQueryAdapter(snapshots.map { if (it.id == updatedUnit.id) updatedUnit else it }, mapWidth, mapHeight, mapUnitsPerPixel)

    fun distanceBetween(first: UnitSnapshot, second: UnitSnapshot): Double =
        operation.distanceBetween(first.toOperationUnit(), second.toOperationUnit())

    fun distanceBetween(first: MapCoordinate, second: MapCoordinate): Double =
        operation.distanceBetween(first, second)

    fun unitsWithin(center: UnitSnapshot, radiusUnits: Double): List<UnitQueryMatch> =
        unitsWithin(center.mapCoordinate, radiusUnits)

    fun unitsWithin(center: MapCoordinate, radiusUnits: Double): List<UnitQueryMatch> =
        operation.unitsWithin(center, radiusUnits).mapNotNull { match ->
            snapshotsById[match.unit.id.value]?.let { UnitQueryMatch(it, match.distanceUnits, match.operationalSystems) }
        }

    fun countWithin(center: MapCoordinate, radiusUnits: Double): Int =
        unitsWithin(center, radiusUnits).size

    fun operationalSystemsFor(unit: UnitSnapshot): List<OlympiadDawnShipSystem> =
        operation.operationalSystemsFor(unit.toOperationUnit())
}
