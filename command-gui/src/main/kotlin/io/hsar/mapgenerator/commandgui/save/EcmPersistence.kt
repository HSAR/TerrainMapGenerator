package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.operation.OperationEcmActivation
import io.hsar.mapgenerator.commandgui.operation.OperationEcmRules
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import java.util.*
import java.util.Collections.emptySet

internal class EcmPersistence private constructor(
    private val root: ObjectNode,
    private val activations: MutableList<OperationEcmActivation>,
) {
    private var markedKeys = emptySet<Pair<Int, OperationObjectId>>()
    fun forTurn(turnNumber: Int): List<OperationEcmActivation> = activations.filter { it.turnNumber == turnNumber }

    fun add(activation: OperationEcmActivation) {
        activations += activation
    }

    fun clearBefore(turnNumber: Int) {
        activations.removeAll { it.turnNumber < turnNumber }
    }

    fun reconcileMarked(updated: List<OperationEcmActivation>) {
        val updatedKeys = updated.mapTo(linkedSetOf()) { it.turnNumber to it.emitterId }
        activations.removeAll { (it.turnNumber to it.emitterId) in markedKeys || (it.turnNumber to it.emitterId) in updatedKeys }
        activations += updated
        markedKeys = updatedKeys
    }

    fun toJson(): ByteArray {
        if (activations.isEmpty()) root.remove(FIELD_NAME) else root.putArray(FIELD_NAME).also { records ->
            activations.sortedWith(compareBy<OperationEcmActivation>(OperationEcmActivation::turnNumber).thenBy { it.emitterId.value.toString() })
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
                val emitter = record.path("emitterId").textValue()?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() } ?: return@mapNotNull null
                val keys = (record.get("moduleKeys") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty()
                OperationEcmActivation(
                    record.path("turn").intValue(), emitter, keys,
                    record.path("relativeBearingDegrees").doubleValue(), record.path("rangeU").doubleValue(),
                    record.path("widthDegrees").doubleValue(), record.path("intensity").intValue(),
                ).takeIf { keys.isNotEmpty() && OperationEcmRules.valid(keys.size, it.rangeU, it.widthDegrees, it.intensity) }
            }?.toMutableList() ?: mutableListOf(),
        )

        fun fromJson(json: ByteArray?): EcmPersistence = fromRoot(
            runCatching { json?.takeIf { it.isNotEmpty() }?.let(OperationSaveJson.objectMapper::readTree)?.takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>() }
                .getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )
    }
}
