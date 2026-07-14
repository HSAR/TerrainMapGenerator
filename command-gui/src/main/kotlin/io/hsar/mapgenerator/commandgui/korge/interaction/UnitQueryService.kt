package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.math.floor
import kotlin.math.hypot

internal data class UnitQueryMatch(
    val unit: UnitSnapshot,
    val distanceUnits: Double,
    val operationalSystems: List<OlympiadDawnShipSystem>,
)

internal class UnitQueryService(
    units: List<UnitSnapshot>,
    internal val mapWidth: Int,
    internal val mapHeight: Int,
    private val mapUnitsPerPixel: Double,
) {
    private data class SpatialCell(val x: Int, val y: Int)

    private val snapshots = units.toList()
    private val cells = snapshots.groupBy { cellFor(it.mapCoordinate) }

    fun allUnits(): List<UnitSnapshot> = snapshots

    fun replacing(updatedUnit: UnitSnapshot): UnitQueryService =
        UnitQueryService(snapshots.map { if (it.id == updatedUnit.id) updatedUnit else it }, mapWidth, mapHeight, mapUnitsPerPixel)

    fun distanceBetween(first: UnitSnapshot, second: UnitSnapshot): Double =
        distanceBetween(first.mapCoordinate, second.mapCoordinate)

    fun distanceBetween(first: MapCoordinate, second: MapCoordinate): Double =
        hypot(
            (first.x - second.x) * mapWidth,
            (first.y - second.y) * mapHeight,
        ) * mapUnitsPerPixel

    fun unitsWithin(center: UnitSnapshot, radiusUnits: Double): List<UnitQueryMatch> =
        unitsWithin(center.mapCoordinate, radiusUnits)

    fun unitsWithin(center: MapCoordinate, radiusUnits: Double): List<UnitQueryMatch> {
        require(radiusUnits >= 0.0) { "Radius must be non-negative." }
        val centerX = center.x * mapWidth * mapUnitsPerPixel
        val centerY = center.y * mapHeight * mapUnitsPerPixel
        val minimumCellX = floor((centerX - radiusUnits) / CELL_SIZE_UNITS).toInt()
        val maximumCellX = floor((centerX + radiusUnits) / CELL_SIZE_UNITS).toInt()
        val minimumCellY = floor((centerY - radiusUnits) / CELL_SIZE_UNITS).toInt()
        val maximumCellY = floor((centerY + radiusUnits) / CELL_SIZE_UNITS).toInt()
        return (minimumCellX..maximumCellX).asSequence()
            .flatMap { x -> (minimumCellY..maximumCellY).asSequence().flatMap { y -> cells[SpatialCell(x, y)].orEmpty().asSequence() } }
            .map { unit -> UnitQueryMatch(unit, distanceBetween(center, unit.mapCoordinate), operationalSystemsFor(unit)) }
            .filter { it.distanceUnits <= radiusUnits }
            .sortedWith(
                compareBy<UnitQueryMatch> { it.distanceUnits }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.unit.displayName() }
                    .thenBy { it.unit.id.value },
            )
            .toList()
    }

    fun countWithin(center: MapCoordinate, radiusUnits: Double): Int =
        unitsWithin(center, radiusUnits).size

    fun operationalSystemsFor(unit: UnitSnapshot): List<OlympiadDawnShipSystem> =
        (OlympiadDawnShip.definition(unit.customProperties) ?: OlympiadDawnShip.definition(unit.template.customProperties))
            ?.let { definition ->
                val damage = OlympiadDawnShip.instanceDamage(unit.customProperties, definition)
                definition.systems.filter { it.id !in damage.destroyedSystems }
            }
            .orEmpty()

    private fun cellFor(position: MapCoordinate): SpatialCell =
        SpatialCell(
            floor(position.x * mapWidth * mapUnitsPerPixel / CELL_SIZE_UNITS).toInt(),
            floor(position.y * mapHeight * mapUnitsPerPixel / CELL_SIZE_UNITS).toInt(),
        )

    private companion object {
        const val CELL_SIZE_UNITS = 6.0
    }
}
