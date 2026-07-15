package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.hsar.mapgenerator.commandgui.korge.interaction.EcmActivation
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId

/** Ledgered unit-property representation of the otherwise transient tactical state. */
internal object TacticalTurnMarker {
    const val PROPERTY_KEY = "tacticalTurnMarker"

    fun hasMarker(properties: Map<String, String>): Boolean = properties.containsKey(PROPERTY_KEY)

    fun usage(properties: Map<String, String>, turnNumber: Int): TacticalFireUsage? =
        records(properties)[turnNumber]?.usage

    fun usages(properties: Map<String, String>, unitId: MapPlacedObjectId): Map<TacticalFireKey, TacticalFireUsage> =
        records(properties).mapKeys { (turnNumber, _) -> TacticalFireKey(turnNumber, unitId) }.mapValues { (_, record) -> record.usage }

    fun activations(properties: Map<String, String>, emitterId: MapPlacedObjectId): List<EcmActivation> =
        records(properties).values.flatMap { record ->
            record.activations.map { activation ->
                EcmActivation(record.turnNumber, emitterId, activation.moduleKeys, activation.bearing, activation.rangeU, activation.widthDegrees, activation.intensity)
            }
        }

    fun update(
        properties: Map<String, String>,
        turnNumber: Int,
        usage: TacticalFireUsage,
        activation: EcmActivation? = null,
    ): Map<String, String> {
        val updated = records(properties).toMutableMap()
        val existing = updated[turnNumber] ?: TacticalTurnRecord(turnNumber, TacticalFireUsage())
        updated[turnNumber] = existing.copy(
            usage = usage,
            activations = activation?.let { existing.activations + it.toMarker() } ?: existing.activations,
        )
        return properties + (PROPERTY_KEY to serialize(updated.values))
    }

    private fun records(properties: Map<String, String>): Map<Int, TacticalTurnRecord> =
        properties[PROPERTY_KEY]
            ?.let { source -> runCatching { OperationSaveJson.objectMapper.readTree(source) }.getOrNull() }
            ?.let { root ->
                when {
                    root.isArray -> root
                    root.path("turns").isArray -> root.path("turns")
                    else -> null
                }
            }
            ?.mapNotNull(::parseRecord)
            ?.associateBy(TacticalTurnRecord::turnNumber)
            .orEmpty()

    private fun parseRecord(node: JsonNode): TacticalTurnRecord? {
        val turnNumber = node.path("turn").intValue().takeIf { it > 0 } ?: return null
        val usage = TacticalFireUsage(
            firedWeapons = (node.get("firedWeapons") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty(),
            fireControlUsed = node.path("fireControlUsed").intValue().coerceAtLeast(0),
        )
        val activations = (node.get("ecmActivations") as? ArrayNode)?.mapNotNull(::parseActivation).orEmpty()
        return TacticalTurnRecord(turnNumber, usage, activations)
    }

    private fun parseActivation(node: JsonNode): TacticalEcmMarker? {
        val keys = (node.get("moduleKeys") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty()
        return TacticalEcmMarker(
            keys,
            node.path("bearing").doubleValue(),
            node.path("rangeU").doubleValue(),
            node.path("widthDegrees").doubleValue(),
            node.path("intensity").intValue(),
        ).takeIf { it.moduleKeys.isNotEmpty() }
    }

    private fun serialize(records: Collection<TacticalTurnRecord>): String =
        OperationSaveJson.objectMapper.writeValueAsString(
            OperationSaveJson.objectMapper.createArrayNode().also { turns ->
                records.sortedBy(TacticalTurnRecord::turnNumber).forEach { record ->
                    turns.addObject().apply {
                        put("turn", record.turnNumber)
                        put("fireControlUsed", record.usage.fireControlUsed)
                        putArray("firedWeapons").also { weapons -> record.usage.firedWeapons.sorted().forEach(weapons::add) }
                        putArray("ecmActivations").also { activations ->
                            record.activations.forEach { activation ->
                                activations.addObject().apply {
                                    putArray("moduleKeys").also { keys -> activation.moduleKeys.sorted().forEach(keys::add) }
                                    put("bearing", activation.bearing)
                                    put("rangeU", activation.rangeU)
                                    put("widthDegrees", activation.widthDegrees)
                                    put("intensity", activation.intensity)
                                }
                            }
                        }
                    }
                }
            },
        )

    private data class TacticalTurnRecord(
        val turnNumber: Int,
        val usage: TacticalFireUsage,
        val activations: List<TacticalEcmMarker> = emptyList(),
    )

    private data class TacticalEcmMarker(
        val moduleKeys: Set<String>,
        val bearing: Double,
        val rangeU: Double,
        val widthDegrees: Double,
        val intensity: Int,
    )

    private fun EcmActivation.toMarker() = TacticalEcmMarker(moduleKeys, relativeBearingDegrees, rangeU, widthDegrees, intensity)
}
