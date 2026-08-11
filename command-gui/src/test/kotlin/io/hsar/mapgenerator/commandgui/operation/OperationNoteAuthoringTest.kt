package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class OperationNoteAuthoringTest {
    private val noteId = OperationNoteId(UUID(0, 1))
    private val firstObject = OperationObjectId(UUID(0, 2))
    private val missingObject = OperationObjectId(UUID(0, 3))

    @Test
    fun `normalizes a blank title while preserving body and ordered distinct references`() {
        val draft = OperationNoteDraft(noteId, "   ", "Line one\r\nLine two", listOf(firstObject, missingObject, firstObject))

        val note = OperationNoteAuthoring.note(draft)

        assertThat(note.title, equalTo("Untitled Note"))
        assertThat(note.body, equalTo("Line one\r\nLine two"))
        assertThat(note.referencedObjectIds, equalTo(listOf(firstObject, missingObject)))
    }

    @Test
    fun `create edit and delete produce exact note changes and suppress no ops`() {
        val content = OperationContent()
        val draft = OperationNoteDraft(noteId, "Orders", "Hold", listOf(missingObject))
        val created = OperationNoteAuthoring.createChange(content, draft).applyTo(content)
        val before = created.notes.single()
        val editedDraft = draft.copy(body = "Advance")

        val edited = OperationNoteAuthoring.editChange(before, editedDraft).applyTo(created)
        val deleted = OperationNoteAuthoring.deleteChange(edited, noteId).applyTo(edited)

        assertThat(edited.notes.single().body, equalTo("Advance"))
        assertThat(deleted.notes, equalTo(emptyList()))
        assertThat(OperationNoteAuthoring.editChange(before, draft).isEmpty, equalTo(true))
    }

    @Test
    fun `reorder accepts only a changed exact permutation`() {
        val first = OperationNote(noteId, "First", "")
        val second = OperationNote(OperationNoteId(UUID(0, 4)), "Second", "")
        val content = OperationContent(notes = listOf(first, second))

        val reordered = OperationNoteAuthoring.reorderChange(content, listOf(second.id, first.id))

        assertThat(reordered.applyTo(content).notes, equalTo(listOf(second, first)))
        assertThat(OperationNoteAuthoring.reorderChange(content, listOf(first.id, second.id)).isEmpty, equalTo(true))
        assertThat(OperationNoteAuthoring.reorderChange(content, listOf(first.id, first.id)).isEmpty, equalTo(true))
        assertThat(OperationNoteAuthoring.reorderChange(content, listOf(first.id)).isEmpty, equalTo(true))
    }

    @Test
    fun `note entries undo redo and truncate a redo branch`() {
        val draft = OperationNoteDraft(noteId, "Orders", "Hold")
        val create = OperationTimelineEntry(
            OperationNoteAuthoring.createChange(OperationContent(), draft), MapViewerLedgerEntryKind.NOTE, "created note Orders",
        )
        val created = OperationTimeline().record(create)
        val undone = created.undo()
        val redone = undone.redo()
        val replacement = OperationTimelineEntry(
            OperationNoteAuthoring.createChange(undone.contentAt(), draft.copy(id = OperationNoteId(UUID(0, 9)), title = "Replacement")),
            MapViewerLedgerEntryKind.NOTE, "created note Replacement",
        )

        val branched = undone.record(replacement)

        assertThat(undone.contentAt().notes, equalTo(emptyList()))
        assertThat(redone.contentAt().notes.single().title, equalTo("Orders"))
        assertThat(branched.canRedo, equalTo(false))
        assertThat(branched.contentAt().notes.single().title, equalTo("Replacement"))
    }
}
