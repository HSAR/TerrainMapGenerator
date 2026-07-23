package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.math.hypot

internal object MoveLimit {
    const val PROPERTY_KEY = "moveLimit"

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

internal fun MapActionHistory.turnOriginFor(unitId: MapPlacedObjectId): MapCoordinate? {
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
