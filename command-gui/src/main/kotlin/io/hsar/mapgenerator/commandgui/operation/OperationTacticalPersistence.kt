package io.hsar.mapgenerator.commandgui.operation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson

/** Neutral ledgered unit-property representation of otherwise transient tactical state. */
internal object OperationTacticalTurnMarker {
    const val PROPERTY_KEY = "tacticalTurnMarker"

    fun hasMarker(properties: Map<String, String>) = PROPERTY_KEY in properties
    fun usage(properties: Map<String, String>, turnNumber: Int) = records(properties)[turnNumber]?.usage
    fun usages(properties: Map<String, String>, unitId: OperationObjectId) = records(properties).mapKeys { OperationTacticalFireKey(it.key, unitId) }.mapValues { it.value.usage }
    fun activations(properties: Map<String, String>, emitterId: OperationObjectId) = records(properties).values.flatMap { record ->
        record.activations.map { OperationEcmActivation(record.turnNumber, emitterId, it.moduleKeys, it.bearing, it.rangeU, it.widthDegrees, it.intensity) }
    }

    fun update(properties: Map<String, String>, turnNumber: Int, usage: OperationTacticalFireUsage, activation: OperationEcmActivation? = null): Map<String, String> {
        val updated = records(properties).toMutableMap()
        val existing = updated[turnNumber] ?: Record(turnNumber, OperationTacticalFireUsage())
        updated[turnNumber] = existing.copy(usage = usage, activations = activation?.let { existing.activations + it.marker() } ?: existing.activations)
        return properties + (PROPERTY_KEY to serialize(updated.values))
    }

    private fun records(properties: Map<String, String>): Map<Int, Record> = properties[PROPERTY_KEY]
        ?.let { runCatching { OperationSaveJson.objectMapper.readTree(it) }.getOrNull() }
        ?.let { when { it.isArray -> it; it.path("turns").isArray -> it.path("turns"); else -> null } }
        ?.mapNotNull(::parseRecord)?.associateBy(Record::turnNumber).orEmpty()

    private fun parseRecord(node: JsonNode): Record? {
        val turn = node.path("turn").intValue().takeIf { it > 0 } ?: return null
        val usage = OperationTacticalFireUsage(
            (node.get("firedWeapons") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty(),
            node.path("fireControlUsed").intValue().coerceAtLeast(0),
        )
        return Record(turn, usage, (node.get("ecmActivations") as? ArrayNode)?.mapNotNull(::parseActivation).orEmpty())
    }

    private fun parseActivation(node: JsonNode) = Marker(
        (node.get("moduleKeys") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty(),
        node.path("bearing").doubleValue(), node.path("rangeU").doubleValue(), node.path("widthDegrees").doubleValue(), node.path("intensity").intValue(),
    ).takeIf { it.moduleKeys.isNotEmpty() }

    private fun serialize(records: Collection<Record>) = OperationSaveJson.objectMapper.writeValueAsString(
        OperationSaveJson.objectMapper.createArrayNode().also { turns -> records.sortedBy(Record::turnNumber).forEach { record ->
            turns.addObject().apply {
                put("turn", record.turnNumber); put("fireControlUsed", record.usage.fireControlUsed)
                putArray("firedWeapons").also { values -> record.usage.firedWeapons.sorted().forEach(values::add) }
                putArray("ecmActivations").also { values -> record.activations.forEach { activation -> values.addObject().apply {
                    putArray("moduleKeys").also { keys -> activation.moduleKeys.sorted().forEach(keys::add) }
                    put("bearing", activation.bearing); put("rangeU", activation.rangeU); put("widthDegrees", activation.widthDegrees); put("intensity", activation.intensity)
                } } }
            }
        } },
    )

    private data class Record(val turnNumber: Int, val usage: OperationTacticalFireUsage, val activations: List<Marker> = emptyList())
    private data class Marker(val moduleKeys: Set<String>, val bearing: Double, val rangeU: Double, val widthDegrees: Double, val intensity: Int)
    private fun OperationEcmActivation.marker() = Marker(moduleKeys, relativeBearingDegrees, rangeU, widthDegrees, intensity)
}
