package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction

internal fun noteReferenceContextMenuActions(
    referencedNotes: List<MapNoteSnapshot>,
    openNote: (MapNoteId) -> Unit,
): List<MapPlacedObjectMenuAction> {
    val notes = referencedNotes
        .sortedBy { it.title.ifBlank { "Untitled Note" }.lowercase() }
    if (notes.isEmpty()) {
        return emptyList()
    }

    return listOf(
        MapPlacedObjectMenuAction(
            label = "Notes",
            onSelected = {},
            submenuActions = notes.map { note ->
                MapPlacedObjectMenuAction(
                    label = note.title.ifBlank { "Untitled Note" },
                    onSelected = { openNote(note.id) },
                )
            },
        ),
    )
}
