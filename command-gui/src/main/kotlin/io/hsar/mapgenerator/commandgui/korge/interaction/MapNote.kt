package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

internal data class MapNoteId(
    val value: UUID,
)

internal data class MapNoteSnapshot(
    val id: MapNoteId,
    val title: String,
    val body: String,
    val referencedObjectIds: List<MapPlacedObjectId> = emptyList(),
)

internal object MapNotePatch {
    private val objectMapper = jacksonObjectMapper()

    fun diff(before: MapNoteSnapshot, after: MapNoteSnapshot): Map<String, JsonNode?> {
        require(before.id == after.id) { "Cannot diff different notes" }
        return buildMap {
            putIfChanged("title", before.title, after.title)
            putIfChanged("body", before.body, after.body)
            if (before.referencedObjectIds != after.referencedObjectIds) {
                put("referencedObjectIds", toNode(after.referencedObjectIds.map { it.value.toString() }))
            }
        }
    }

    fun apply(snapshot: MapNoteSnapshot, changes: Map<String, JsonNode?>): MapNoteSnapshot =
        changes.entries.fold(snapshot) { current, (key, value) ->
            when (key) {
                "title" -> current.copy(title = value.requireValue().asText())
                "body" -> current.copy(body = value.requireValue().asText())
                "referencedObjectIds" -> current.copy(referencedObjectIds = value.requireValue().map { node ->
                    MapPlacedObjectId(UUID.fromString(node.asText()))
                }.distinct())

                else -> current
            }
        }

    private fun <T> MutableMap<String, JsonNode?>.putIfChanged(key: String, before: T, after: T) {
        if (before != after) {
            put(key, toNode(after))
        }
    }

    private fun JsonNode?.requireValue() =
        this ?: error("Patch values must be present for supported note fields")

    private fun toNode(value: Any?): JsonNode? =
        when (value) {
            null -> null
            is String -> TextNode(value)
            else -> objectMapper.valueToTree(value)
        }
}

internal class MapNoteStore(
    private val uuidGenerator: () -> UUID = UUID::randomUUID,
) {
    private val notes = mutableListOf<MapNoteSnapshot>()

    fun createNote(
        title: String = DEFAULT_TITLE,
        body: String = "",
        referencedObjectIds: List<MapPlacedObjectId> = emptyList(),
    ): MapNoteSnapshot =
        MapNoteSnapshot(
            id = MapNoteId(uuidGenerator()),
            title = title,
            body = body,
            referencedObjectIds = referencedObjectIds.distinct(),
        ).also(::upsert)

    fun upsert(snapshot: MapNoteSnapshot) {
        val index = notes.indexOfFirst { it.id == snapshot.id }
        if (index >= 0) {
            notes[index] = snapshot
        } else {
            notes += snapshot
        }
    }

    fun applyEdit(id: MapNoteId, changes: Map<String, JsonNode?>): MapNoteSnapshot? =
        snapshot(id)
            ?.let { MapNotePatch.apply(it, changes) }
            ?.also(::upsert)

    fun delete(id: MapNoteId): MapNoteSnapshot? =
        notes.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }
            ?.let(notes::removeAt)

    fun snapshot(id: MapNoteId): MapNoteSnapshot? =
        notes.firstOrNull { it.id == id }

    fun snapshots(): List<MapNoteSnapshot> =
        notes.toList()

    fun reorder(ids: List<MapNoteId>) {
        val known = notes.associateBy(MapNoteSnapshot::id)
        if (ids.size != notes.size || ids.distinct().size != ids.size || ids.any { it !in known }) return
        notes.clear()
        notes += ids.map(known::getValue)
    }

    fun snapshotsReferencing(objectId: MapPlacedObjectId): List<MapNoteSnapshot> =
        notes.filter { objectId in it.referencedObjectIds }

    private companion object {
        const val DEFAULT_TITLE = "Untitled Note"
    }
}
