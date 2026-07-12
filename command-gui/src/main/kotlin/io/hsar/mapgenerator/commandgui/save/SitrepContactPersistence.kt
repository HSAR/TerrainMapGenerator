package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepContactState
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepLostContact
import java.util.*

internal data class SitrepContactKey(
    val turnNumber: Int,
    val observerUnitId: MapPlacedObjectId,
)

internal class SitrepContactPersistence private constructor(
    private val root: ObjectNode,
    private val states: MutableMap<SitrepContactKey, SitrepContactState>,
) {
    fun stateFor(key: SitrepContactKey): SitrepContactState =
        states[key] ?: SitrepContactState()

    fun update(key: SitrepContactKey, state: SitrepContactState) {
        if (state.sensorContactIds.isEmpty() && state.lostContacts.isEmpty() &&
            state.usedSierraContactNumbers.isEmpty() && state.sierraContactNumbers.isEmpty()
        ) {
            states.remove(key)
        } else {
            states[key] = state.copy(
                identifiedIds = state.identifiedIds intersect state.sensorContactIds,
                sierraContactNumbers = state.sierraContactNumbers.toMap(),
                usedSierraContactNumbers = state.usedSierraContactNumbers + state.sierraContactNumbers.values,
                lastSeenPositions = state.lastSeenPositions.filterKeys { it in state.sensorContactIds },
                lastSeenContacts = state.lastSeenContacts.filterKeys { it in state.sensorContactIds },
            )
        }
    }

    fun copyTurnState(fromTurn: Int, toTurn: Int) {
        states.entries
            .filter { it.key.turnNumber == fromTurn }
            .toList()
            .forEach { (key, state) ->
                states[SitrepContactKey(toTurn, key.observerUnitId)] = state.copy(
                    sensorContactIds = state.sensorContactIds.toSet(),
                    identifiedIds = state.identifiedIds.toSet(),
                    sierraContactNumbers = state.sierraContactNumbers.toMap(),
                    usedSierraContactNumbers = state.usedSierraContactNumbers.toSet(),
                    lastSeenPositions = state.lastSeenPositions.toMap(),
                    lastSeenContacts = state.lastSeenContacts.toMap(),
                    lostContacts = state.lostContacts.toList(),
                )
            }
    }

    fun toJson(): ByteArray {
        if (states.isEmpty()) {
            root.remove(FIELD_NAME)
        } else {
            val records = root.putArray(FIELD_NAME)
            states.entries
                .sortedWith(compareBy<Map.Entry<SitrepContactKey, SitrepContactState>> { it.key.turnNumber }
                    .thenBy { it.key.observerUnitId.value.toString() })
                .forEach { (key, state) ->
                    records.addObject().apply {
                        put("turn", key.turnNumber)
                        put("observerUnitId", key.observerUnitId.value.toString())
                        val contacts = putArray("contacts")
                        putArray("usedSierraNumbers").also { usedNumbers ->
                            state.usedSierraContactNumbers.sorted().forEach(usedNumbers::add)
                        }
                        (state.sensorContactIds + state.sierraContactNumbers.keys)
                            .sortedBy { state.sierraContactNumbers[it] ?: Int.MAX_VALUE }
                            .forEach { contactId ->
                                contacts.addObject().apply {
                                    put("unitId", contactId.value.toString())
                                    put("active", contactId in state.sensorContactIds)
                                    state.sierraContactNumbers[contactId]?.let { put("sierraNumber", it) }
                                    put("identified", contactId in state.identifiedIds && contactId in state.sensorContactIds)
                                    state.lastSeenPositions[contactId]?.let { position ->
                                        put("x", position.x)
                                        put("y", position.y)
                                    }
                                    state.lastSeenContacts[contactId]?.let { lastSeen ->
                                        put("rangeUnits", lastSeen.lastSeenRangeUnits)
                                        lastSeen.unitTypeName?.let { put("unitTypeName", it) }
                                        lastSeen.unitClassCode?.let { put("unitClassCode", it) }
                                    }
                                }
                            }
                        val lostContacts = putArray("lostContacts")
                        state.lostContacts
                            .sortedBy { it.sierraNumber }
                            .forEach { lost ->
                                lostContacts.addObject().apply {
                                    put("unitId", lost.contactId.value.toString())
                                    put("sierraNumber", lost.sierraNumber)
                                    put("x", lost.lastSeenPosition.x)
                                    put("y", lost.lastSeenPosition.y)
                                    put("rangeUnits", lost.lastSeenRangeUnits)
                                    put("identified", lost.wasIdentified)
                                    lost.unitTypeName?.let { put("unitTypeName", it) }
                                    lost.unitClassCode?.let { put("unitClassCode", it) }
                                }
                            }
                    }
                }
        }
        return OperationSaveJson.objectMapper.writeValueAsBytes(root)
    }

    fun toJsonOrNull(): ByteArray? =
        if (states.isEmpty() && root.size() == 0) null else toJson()

    companion object {
        const val FIELD_NAME = "sitrepContacts"

        fun fromJson(json: ByteArray?): SitrepContactPersistence = load(json)

        internal fun fromRoot(root: ObjectNode): SitrepContactPersistence = load(root)

        fun load(json: ByteArray?): SitrepContactPersistence = load(
            runCatching {
                json?.takeIf { it.isNotEmpty() }
                    ?.let { OperationSaveJson.objectMapper.readTree(it) }
                    ?.takeIf { it.isObject }
                    ?.deepCopy<ObjectNode>()
            }.getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )

        private fun load(root: ObjectNode): SitrepContactPersistence {
            val states = mutableMapOf<SitrepContactKey, SitrepContactState>()
            (root.get(FIELD_NAME) as? ArrayNode)?.forEach { record ->
                val turn = record.path("turn").intValue()
                val observerId = record.path("observerUnitId").textValue()?.toMapObjectId() ?: return@forEach
                val sensorContactIds = mutableSetOf<MapPlacedObjectId>()
                val identifiedIds = mutableSetOf<MapPlacedObjectId>()
                val sierraNumbers = mutableMapOf<MapPlacedObjectId, Int>()
                val usedSierraNumbers = mutableSetOf<Int>()
                (record.get("usedSierraNumbers") as? ArrayNode)?.forEach { number ->
                    number.takeIf { it.isInt }?.intValue()?.let(usedSierraNumbers::add)
                }
                val lastSeenPositions = mutableMapOf<MapPlacedObjectId, MapCoordinate>()
                val lastSeenContacts = mutableMapOf<MapPlacedObjectId, SitrepLostContact>()
                (record.get("contacts") as? ArrayNode)?.forEach { contact ->
                    val unitId = contact.path("unitId").textValue()?.toMapObjectId() ?: return@forEach
                    if (!contact.has("active") || contact.path("active").booleanValue()) sensorContactIds += unitId
                    if (contact.path("identified").booleanValue() && unitId in sensorContactIds) identifiedIds += unitId
                    contact.path("sierraNumber").takeIf { it.isInt }?.intValue()?.let { sierraNumbers[unitId] = it }
                    if (contact.has("x") && contact.has("y")) {
                        val position = MapCoordinate(contact.path("x").doubleValue(), contact.path("y").doubleValue())
                        lastSeenPositions[unitId] = position
                        sierraNumbers[unitId]?.let { number ->
                            lastSeenContacts[unitId] = SitrepLostContact(
                                contactId = unitId,
                                sierraNumber = number,
                                lastSeenPosition = position,
                                lastSeenRangeUnits = contact.path("rangeUnits").doubleValue(),
                                wasIdentified = unitId in identifiedIds,
                                unitTypeName = contact.path("unitTypeName").textValue(),
                                unitClassCode = contact.path("unitClassCode").textValue(),
                            )
                        }
                    }
                }
                val lostContacts = mutableListOf<SitrepLostContact>()
                (record.get("lostContacts") as? ArrayNode)?.forEach { lost ->
                    val contactId = lost.path("unitId").textValue()?.toMapObjectId() ?: return@forEach
                    val number = lost.path("sierraNumber").takeIf { it.isInt }?.intValue() ?: return@forEach
                    if (!lost.has("x") || !lost.has("y") || !lost.has("rangeUnits")) return@forEach
                    lostContacts += SitrepLostContact(
                        contactId = contactId,
                        sierraNumber = number,
                        lastSeenPosition = MapCoordinate(lost.path("x").doubleValue(), lost.path("y").doubleValue()),
                        lastSeenRangeUnits = lost.path("rangeUnits").doubleValue(),
                        wasIdentified = lost.path("identified").booleanValue(),
                        unitTypeName = lost.path("unitTypeName").textValue(),
                        unitClassCode = lost.path("unitClassCode").textValue(),
                    )
                }
                usedSierraNumbers += sierraNumbers.values
                usedSierraNumbers += lostContacts.map { it.sierraNumber }
                states[SitrepContactKey(turn, observerId)] = SitrepContactState(
                    sensorContactIds = sensorContactIds,
                    identifiedIds = identifiedIds,
                    sierraContactNumbers = sierraNumbers,
                    usedSierraContactNumbers = usedSierraNumbers,
                    lastSeenPositions = lastSeenPositions,
                    lastSeenContacts = lastSeenContacts,
                    lostContacts = lostContacts,
                )
            }
            return SitrepContactPersistence(root, states)
        }

        private fun String.toMapObjectId(): MapPlacedObjectId? =
            runCatching { MapPlacedObjectId(UUID.fromString(this)) }.getOrNull()
    }

}
