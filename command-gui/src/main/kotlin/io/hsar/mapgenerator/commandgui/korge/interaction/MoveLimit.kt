package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShip

import kotlin.math.hypot

internal object MoveLimit {
    const val PROPERTY_KEY = "moveLimit"
    private const val FIGHTER_MOVE_LIMIT = 12.0
    private const val ACTIVE_WARP_KEY = "activeWarp"
    private const val MAXIMUM_WARP_KEY = "maximumWarp"

    fun parse(properties: Map<String, String>): Double? =
        properties[PROPERTY_KEY]
            ?.trim()
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }

    fun sanitize(properties: Map<String, String>): Map<String, String> =
        properties[PROPERTY_KEY]
            ?.takeIf { parse(mapOf(PROPERTY_KEY to it)) == null }
            ?.let { properties - PROPERTY_KEY }
            ?: properties

    fun withWarpMultiplier(limit: Double, properties: Map<String, String>): Double =
        limit * when {
            properties.booleanProperty(MAXIMUM_WARP_KEY) -> 20.0
            properties.booleanProperty(ACTIVE_WARP_KEY) -> 10.0
            else -> 1.0
        }

    fun forUnit(snapshot: UnitSnapshot): Double? {
        return baseForUnit(snapshot)?.let { limit -> withWarpMultiplier(limit, snapshot.customProperties) }
    }

    fun withCalculatedValue(snapshot: UnitSnapshot): UnitSnapshot =
        baseForUnit(snapshot)
            ?.let { limit -> snapshot.copy(customProperties = snapshot.customProperties + (PROPERTY_KEY to limit.toString())) }
            ?: snapshot

    private fun baseForUnit(snapshot: UnitSnapshot): Double? =
        when {
            snapshot.unitClassLabel() == "F" -> FIGHTER_MOVE_LIMIT
            else -> baseForShip(snapshot) ?: parse(snapshot.template.customProperties)
        }

    private fun baseForShip(snapshot: UnitSnapshot): Double? =
        (OlympiadDawnShip.definition(snapshot.customProperties) ?: OlympiadDawnShip.definition(snapshot.template.customProperties))
            ?.let { definition ->
                val damage = OlympiadDawnShip.instanceDamage(snapshot.customProperties, definition)
                val silentRunningMultiplier = if (snapshot.customProperties.booleanProperty("silentRunning")) 0.5 else 1.0
                definition.systems.count { it.name == "Impulse Drive" && it.id !in damage.destroyedSystems } / 2.0 * silentRunningMultiplier
            }

    fun constrain(
        origin: MapCoordinate,
        requested: MapCoordinate,
        limit: Double,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        overrideLimit: Boolean = false,
    ): MapCoordinate {
        if (overrideLimit) return requested
        val dx = requested.x - origin.x
        val dy = requested.y - origin.y
        val distance = hypot(dx * mapWidth, dy * mapHeight) * mapUnitsPerPixel
        if (distance <= limit || distance == 0.0) return requested
        val scale = limit / distance
        return MapCoordinate(origin.x + dx * scale, origin.y + dy * scale)
    }
}

private fun Map<String, String>.booleanProperty(key: String): Boolean =
    entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        ?.value
        ?.trim()
        ?.lowercase() in setOf("true", "yes", "1", "on")

internal fun MapActionHistory.turnOriginFor(unitId: MapPlacedObjectId): MapCoordinate? {
    overriddenTurnOriginFor(unitId)?.let { return it }
    val entries = appliedEntries
    val markerIndex = entries.indexOfLast { it is AddTurnLedgerEntry }
    val boundaryObjects = LedgerReplay.replay(entries.take(markerIndex + 1)).objects
    return (boundaryObjects.firstOrNull { it.id == unitId } as? UnitSnapshot)?.mapCoordinate
        ?: entries.drop(markerIndex + 1)
            .flatMap(LedgerEntry::flattened)
            .filterIsInstance<CreateObjectAction>()
            .firstOrNull { it.snapshot.id == unitId }
            ?.snapshot
            ?.let { it as? UnitSnapshot }
            ?.mapCoordinate
}

private fun LedgerEntry.flattened(): List<LedgerEntry> =
    when (this) {
        is BatchLedgerEntry -> entries.flatMap(LedgerEntry::flattened)
        else -> listOf(this)
    }
