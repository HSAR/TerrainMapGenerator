package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode

/** Replays save history without constructing views or notifying UI listeners. */
internal data class ReplayedLedger(
    val objects: List<MapPlacedObjectSnapshot>,
    val unitTemplates: List<UnitTemplateSnapshot>,
    val notes: List<MapNoteSnapshot>,
)

internal object LedgerReplay {
    fun replay(entries: List<LedgerEntry>): ReplayedLedger {
        val objects = linkedMapOf<MapPlacedObjectId, MapPlacedObjectSnapshot>()
        val templates = mutableListOf<UnitTemplateSnapshot>()
        val notes = linkedMapOf<MapNoteId, MapNoteSnapshot>()
        val controller = object : LedgerController {
            override fun createObject(snapshot: MapPlacedObjectSnapshot) {
                objects[snapshot.id] = snapshot
            }

            override fun applyObjectEdit(id: MapPlacedObjectId, changes: Map<String, JsonNode?>) {
                objects[id]?.let { objects[id] = MapPlacedObjectPatch.apply(it, changes) }
            }

            override fun deleteObject(id: MapPlacedObjectId): MapPlacedObjectSnapshot? = objects.remove(id)

            override fun createUnitTemplate(snapshot: UnitTemplateSnapshot) {
                templates.removeAll { it.id == snapshot.id }
                templates.add(snapshot.index.coerceIn(0, templates.size), snapshot)
            }

            override fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot) {
                val index = templates.indexOfFirst { it.id == snapshot.id }
                if (index >= 0) templates[index] = snapshot.copy(index = index)
            }

            override fun applyUnitTemplateEdit(id: UnitTemplateId, changes: Map<String, JsonNode?>): UnitTemplateSnapshot? {
                val index = templates.indexOfFirst { it.id == id }
                return templates.getOrNull(index)
                    ?.let { MapViewerUnitTemplatePatch.apply(it, changes) }
                    ?.also { templates[index] = it.copy(index = index) }
            }

            override fun deleteUnitTemplate(id: UnitTemplateId): UnitTemplateSnapshot? =
                templates.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(templates::removeAt)

            override fun createNote(snapshot: MapNoteSnapshot) {
                notes[snapshot.id] = snapshot
            }

            override fun applyNoteEdit(id: MapNoteId, changes: Map<String, JsonNode?>): MapNoteSnapshot? =
                notes[id]?.let { MapNotePatch.apply(it, changes) }?.also { notes[id] = it }

            override fun deleteNote(id: MapNoteId): MapNoteSnapshot? = notes.remove(id)
        }
        entries.forEach { it.redo(controller) }
        return ReplayedLedger(objects.values.toList(), templates.mapIndexed { index, snapshot -> snapshot.copy(index = index) }, notes.values.toList())
    }
}
