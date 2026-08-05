package io.hsar.mapgenerator.commandgui.korge.persistence

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import java.util.*
import java.util.Collections.emptySet

internal data class TacticalFireKey(val turnNumber: Int, val unitId: MapPlacedObjectId)
internal data class TacticalFireUsage(val firedWeapons: Set<String> = emptySet(), val fireControlUsed: Int = 0)

internal class TacticalFirePersistence private constructor(
    private val root: ObjectNode,
    private val fired: MutableMap<TacticalFireKey, TacticalFireUsage>,
) {
    private var markedKeys = emptySet<TacticalFireKey>()
    fun usage(key: TacticalFireKey): TacticalFireUsage = fired[key] ?: TacticalFireUsage()

    fun markFired(key: TacticalFireKey, weaponKeys: Set<String>) {
        if (weaponKeys.isEmpty()) return
        val current = usage(key)
        fired[key] = current.copy(firedWeapons = current.firedWeapons + weaponKeys, fireControlUsed = current.fireControlUsed + 1)
    }

    fun replace(key: TacticalFireKey, usage: TacticalFireUsage) {
        if (usage.firedWeapons.isEmpty() && usage.fireControlUsed <= 0) fired.remove(key) else fired[key] = usage
    }

    fun clearTurn(turnNumber: Int) = fired.keys.filter { it.turnNumber == turnNumber }.toList().forEach(fired::remove)

    fun reconcileMarked(entries: Map<TacticalFireKey, TacticalFireUsage>) {
        fired.keys.filter { it in markedKeys }.toList().forEach(fired::remove)
        entries.forEach(::replace)
        markedKeys = entries.keys
    }

    fun toJson(): ByteArray {
        if (fired.isEmpty()) root.remove(FIELD_NAME) else {
            root.putArray(FIELD_NAME).also { records ->
                fired.entries.sortedWith(compareBy<Map.Entry<TacticalFireKey, TacticalFireUsage>> { it.key.turnNumber }.thenBy { it.key.unitId.value.toString() })
                    .forEach { (key, usage) ->
                        records.addObject().apply {
                            put("turn", key.turnNumber)
                            put("unitId", key.unitId.value.toString())
                            put("fireControlUsed", usage.fireControlUsed)
                            putArray("firedWeapons").also { array -> usage.firedWeapons.sorted().forEach(array::add) }
                        }
                    }
            }
        }
        return OperationSaveJson.objectMapper.writeValueAsBytes(root)
    }

    companion object {
        const val FIELD_NAME = "tacticalFire"

        fun fromRoot(root: ObjectNode): TacticalFirePersistence {
            val state = mutableMapOf<TacticalFireKey, TacticalFireUsage>()
            (root.get(FIELD_NAME) as? ArrayNode)?.forEach { record ->
                val turn = record.path("turn").intValue()
                val unit = record.path("unitId").textValue()?.let { runCatching { MapPlacedObjectId(UUID.fromString(it)) }.getOrNull() } ?: return@forEach
                val weapons = (record.get("firedWeapons") as? ArrayNode)?.mapNotNull(JsonNode::textValue)?.toMutableSet().orEmpty().toMutableSet()
                val controls = record.path("fireControlUsed").intValue().coerceAtLeast(0)
                if (weapons.isNotEmpty() || controls > 0) state[TacticalFireKey(turn, unit)] = TacticalFireUsage(weapons, controls)
            }
            return TacticalFirePersistence(root, state)
        }

        fun fromJson(json: ByteArray?): TacticalFirePersistence = TacticalFirePersistence.fromRoot(
            runCatching { json?.takeIf { it.isNotEmpty() }?.let(OperationSaveJson.objectMapper::readTree)?.takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>() }
                .getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )
    }
}
