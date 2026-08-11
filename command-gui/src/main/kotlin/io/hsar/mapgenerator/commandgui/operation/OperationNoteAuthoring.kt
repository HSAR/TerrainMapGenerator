package io.hsar.mapgenerator.commandgui.operation

import java.util.*

data class OperationNoteDraft(
    val id: OperationNoteId = OperationNoteId(UUID.randomUUID()),
    val title: String = "",
    val body: String = "",
    val referencedObjectIds: List<OperationObjectId> = emptyList(),
)

object OperationNoteAuthoring {
    const val DEFAULT_TITLE = "Untitled Note"

    fun note(draft: OperationNoteDraft) = OperationNote(
        id = draft.id,
        title = draft.title.trim().ifBlank { DEFAULT_TITLE },
        body = draft.body,
        referencedObjectIds = draft.referencedObjectIds.distinct(),
    )

    fun createChange(content: OperationContent, draft: OperationNoteDraft): OperationChangeSet =
        note(draft)
            .takeUnless { created -> content.notes.any { it.id == created.id } }
            ?.let { OperationChangeSet(notes = listOf(OperationValueChange(after = it))) }
            ?: OperationChangeSet()

    fun editChange(before: OperationNote, draft: OperationNoteDraft): OperationChangeSet {
        if (before.id != draft.id) return OperationChangeSet()
        val after = note(draft)
        return if (before == after) OperationChangeSet()
        else OperationChangeSet(notes = listOf(OperationValueChange(before, after)))
    }

    fun deleteChange(content: OperationContent, id: OperationNoteId): OperationChangeSet =
        content.notes.firstOrNull { it.id == id }
            ?.let { OperationChangeSet(notes = listOf(OperationValueChange(before = it))) }
            ?: OperationChangeSet()

    fun reorderChange(content: OperationContent, orderedIds: List<OperationNoteId>): OperationChangeSet {
        val before = content.notes.map(OperationNote::id)
        return orderedIds
            .takeIf { it.size == before.size && it.toSet().size == it.size && it.toSet() == before.toSet() && it != before }
            ?.let { OperationChangeSet(noteOrderBefore = before, noteOrderAfter = it) }
            ?: OperationChangeSet()
    }
}

fun OperationNote.toDraft() = OperationNoteDraft(id, title, body, referencedObjectIds)
