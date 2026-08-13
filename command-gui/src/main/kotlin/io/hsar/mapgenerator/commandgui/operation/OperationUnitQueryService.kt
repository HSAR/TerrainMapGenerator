package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import kotlin.math.floor
import kotlin.math.hypot

internal data class OperationUnitQueryMatch(
    val unit: OperationUnit,
    val distanceUnits: Double,
    val operationalSystems: List<OlympiadDawnShipSystem>,
)

internal class OperationUnitQueryService(
    val content: OperationContent,
    internal val mapWidth: Int,
    internal val mapHeight: Int,
    private val mapUnitsPerPixel: Double,
) {
    private data class SpatialCell(val x: Int, val y: Int)

    private val units = content.objects.filterIsInstance<OperationUnit>()
    private val cells = units.groupBy { cellFor(it.coordinate) }

    fun allUnits(): List<OperationUnit> = units

    fun replacing(updatedUnit: OperationUnit): OperationUnitQueryService =
        OperationUnitQueryService(
            content.copy(objects = content.objects.map { if (it.id == updatedUnit.id) updatedUnit else it }),
            mapWidth,
            mapHeight,
            mapUnitsPerPixel,
        )

    fun templateFor(unit: OperationUnit): OperationUnitTemplate? = content.unitTemplate(unit.unitTemplateId)

    fun distanceBetween(first: OperationUnit, second: OperationUnit): Double = distanceBetween(first.coordinate, second.coordinate)

    fun distanceBetween(first: MapCoordinate, second: MapCoordinate): Double =
        hypot((first.x - second.x) * mapWidth, (first.y - second.y) * mapHeight) * mapUnitsPerPixel

    fun unitsWithin(center: OperationUnit, radiusUnits: Double): List<OperationUnitQueryMatch> = unitsWithin(center.coordinate, radiusUnits)

    fun unitsWithin(center: MapCoordinate, radiusUnits: Double): List<OperationUnitQueryMatch> {
        require(radiusUnits >= 0.0) { "Radius must be non-negative." }
        val centerX = center.x * mapWidth * mapUnitsPerPixel
        val centerY = center.y * mapHeight * mapUnitsPerPixel
        val minimumCellX = floor((centerX - radiusUnits) / CELL_SIZE_UNITS).toInt()
        val maximumCellX = floor((centerX + radiusUnits) / CELL_SIZE_UNITS).toInt()
        val minimumCellY = floor((centerY - radiusUnits) / CELL_SIZE_UNITS).toInt()
        val maximumCellY = floor((centerY + radiusUnits) / CELL_SIZE_UNITS).toInt()
        return (minimumCellX..maximumCellX).asSequence()
            .flatMap { x -> (minimumCellY..maximumCellY).asSequence().flatMap { y -> cells[SpatialCell(x, y)].orEmpty().asSequence() } }
            .map { unit -> OperationUnitQueryMatch(unit, distanceBetween(center, unit.coordinate), operationalSystemsFor(unit)) }
            .filter { it.distanceUnits <= radiusUnits }
            .sortedWith(
                compareBy<OperationUnitQueryMatch> { it.distanceUnits }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { OperationUnitPresentation.displayName(it.unit, content) }
                    .thenBy { it.unit.id.value },
            )
            .toList()
    }

    fun countWithin(center: MapCoordinate, radiusUnits: Double): Int = unitsWithin(center, radiusUnits).size

    fun operationalSystemsFor(unit: OperationUnit): List<OlympiadDawnShipSystem> =
        OlympiadDawnShip.definitionFor(unit, content)
            ?.let { definition ->
                val damage = OlympiadDawnShip.instanceDamage(unit.customProperties, definition)
                definition.systems.filter { it.id !in damage.destroyedSystems }
            }
            .orEmpty()

    private fun cellFor(position: MapCoordinate): SpatialCell = SpatialCell(
        floor(position.x * mapWidth * mapUnitsPerPixel / CELL_SIZE_UNITS).toInt(),
        floor(position.y * mapHeight * mapUnitsPerPixel / CELL_SIZE_UNITS).toInt(),
    )

    private companion object {
        const val CELL_SIZE_UNITS = 6.0
    }
}
