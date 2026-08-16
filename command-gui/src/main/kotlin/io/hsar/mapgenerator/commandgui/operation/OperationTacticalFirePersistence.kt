package io.hsar.mapgenerator.commandgui.operation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import java.util.UUID

internal class OperationTacticalFirePersistence private constructor(
    private val root: ObjectNode,
    private val fired: MutableMap<OperationTacticalFireKey, OperationTacticalFireUsage>,
) {
    private var markedKeys = emptySet<OperationTacticalFireKey>()
    fun usage(key: OperationTacticalFireKey) = fired[key] ?: OperationTacticalFireUsage()
    fun markFired(key: OperationTacticalFireKey, weaponKeys: Set<String>) {
        if (weaponKeys.isEmpty()) return
        val current = usage(key)
        fired[key] = current.copy(firedWeapons = current.firedWeapons + weaponKeys, fireControlUsed = current.fireControlUsed + 1)
    }
    fun replace(key: OperationTacticalFireKey, usage: OperationTacticalFireUsage) {
        if (usage.firedWeapons.isEmpty() && usage.fireControlUsed <= 0) fired.remove(key) else fired[key] = usage
    }
    fun clearTurn(turnNumber: Int) = fired.keys.filter { it.turnNumber == turnNumber }.toList().forEach(fired::remove)
    fun reconcileMarked(entries: Map<OperationTacticalFireKey, OperationTacticalFireUsage>) {
        fired.keys.filter { it in markedKeys }.toList().forEach(fired::remove)
        entries.forEach(::replace)
        markedKeys = entries.keys
    }
    fun toJson(): ByteArray {
        if (fired.isEmpty()) root.remove(FIELD_NAME) else root.putArray(FIELD_NAME).also { records -> fired.entries
            .sortedWith(compareBy<Map.Entry<OperationTacticalFireKey, OperationTacticalFireUsage>> { it.key.turnNumber }.thenBy { it.key.unitId.value.toString() })
            .forEach { (key, usage) -> records.addObject().apply {
                put("turn", key.turnNumber); put("unitId", key.unitId.value.toString()); put("fireControlUsed", usage.fireControlUsed)
                putArray("firedWeapons").also { values -> usage.firedWeapons.sorted().forEach(values::add) }
            } }
        }
        return OperationSaveJson.objectMapper.writeValueAsBytes(root)
    }
    companion object {
        const val FIELD_NAME = "tacticalFire"
        fun fromRoot(root: ObjectNode): OperationTacticalFirePersistence {
            val state = mutableMapOf<OperationTacticalFireKey, OperationTacticalFireUsage>()
            (root.get(FIELD_NAME) as? ArrayNode)?.forEach { record ->
                val turn = record.path("turn").intValue()
                val unit = record.path("unitId").textValue()?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() } ?: return@forEach
                val weapons = (record.get("firedWeapons") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toSet().orEmpty()
                val controls = record.path("fireControlUsed").intValue().coerceAtLeast(0)
                if (weapons.isNotEmpty() || controls > 0) state[OperationTacticalFireKey(turn, unit)] = OperationTacticalFireUsage(weapons, controls)
            }
            return OperationTacticalFirePersistence(root, state)
        }
        fun fromJson(json: ByteArray?) = fromRoot(runCatching { json?.takeIf { it.isNotEmpty() }?.let(OperationSaveJson.objectMapper::readTree)?.takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>() }.getOrNull() ?: JsonNodeFactory.instance.objectNode())
    }
}
