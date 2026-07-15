package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.EcmActivation
import io.hsar.mapgenerator.commandgui.korge.interaction.EcmRules
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import java.util.*

internal class EcmPersistence private constructor(
    private val root: ObjectNode,
    private val activations: MutableList<EcmActivation>,
) {
    private var markedKeys = emptySet<Pair<Int, MapPlacedObjectId>>()
    fun forTurn(turnNumber: Int): List<EcmActivation> = activations.filter { it.turnNumber == turnNumber }

    fun add(activation: EcmActivation) {
        activations += activation
    }

    fun clearBefore(turnNumber: Int) {
        activations.removeAll { it.turnNumber < turnNumber }
    }

    fun reconcileMarked(updated: List<EcmActivation>) {
        val updatedKeys = updated.mapTo(linkedSetOf()) { it.turnNumber to it.emitterId }
        activations.removeAll { (it.turnNumber to it.emitterId) in markedKeys || (it.turnNumber to it.emitterId) in updatedKeys }
        activations += updated
        markedKeys = updatedKeys
    }

    fun toJson(): ByteArray {
        if (activations.isEmpty()) root.remove(FIELD_NAME) else root.putArray(FIELD_NAME).also { records ->
            activations.sortedWith(compareBy<EcmActivation>(EcmActivation::turnNumber).thenBy { it.emitterId.value.toString() })
                .forEach { activation ->
                    records.addObject().apply {
                        put("turn", activation.turnNumber)
                        put("emitterId", activation.emitterId.value.toString())
                        putArray("moduleKeys").also { keys -> activation.moduleKeys.sorted().forEach(keys::add) }
                        put("relativeBearingDegrees", activation.relativeBearingDegrees)
                        put("rangeU", activation.rangeU)
                        put("widthDegrees", activation.widthDegrees)
                        put("intensity", activation.intensity)
                    }
                }
        }
        return OperationSaveJson.objectMapper.writeValueAsBytes(root)
    }

    companion object {
        const val FIELD_NAME = "ecmActivations"

        fun fromRoot(root: ObjectNode): EcmPersistence = EcmPersistence(
            root,
            (root.get(FIELD_NAME) as? ArrayNode)?.mapNotNull { record ->
                val emitter = record.path("emitterId").textValue()?.let { runCatching { MapPlacedObjectId(UUID.fromString(it)) }.getOrNull() } ?: return@mapNotNull null
                val keys = (record.get("moduleKeys") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty()
                EcmActivation(
                    record.path("turn").intValue(), emitter, keys,
                    record.path("relativeBearingDegrees").doubleValue(), record.path("rangeU").doubleValue(),
                    record.path("widthDegrees").doubleValue(), record.path("intensity").intValue(),
                ).takeIf { keys.isNotEmpty() && EcmRules.valid(keys.size, it.rangeU, it.widthDegrees, it.intensity) }
            }?.toMutableList() ?: mutableListOf(),
        )

        fun fromJson(json: ByteArray?): EcmPersistence = fromRoot(
            runCatching { json?.takeIf { it.isNotEmpty() }?.let(OperationSaveJson.objectMapper::readTree)?.takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>() }
                .getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )
    }
}
