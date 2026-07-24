package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class LedgerReplayTest {
    @Test
    fun `replay returns only the final note state without UI controller calls`() {
        val id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val before = MapNoteSnapshot(id, "Before", "first")
        val after = before.copy(title = "After", body = "last")

        val result = LedgerReplay.replay(
            listOf(
                CreateNoteAction(before),
                EditNoteAction(id, MapNotePatch.diff(before, after), MapNotePatch.diff(after, before)),
            ),
        )

        assertThat(result.notes, equalTo(listOf(after)))
    }

    @Test
    fun `replay preserves reordered notes`() {
        val first = MapNoteSnapshot(MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000001")), "First", "")
        val second = MapNoteSnapshot(MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000002")), "Second", "")

        val result = LedgerReplay.replay(
            listOf(
                CreateNoteAction(first),
                CreateNoteAction(second),
                ReorderNotesAction(listOf(second.id, first.id), listOf(first.id, second.id)),
            ),
        )

        assertThat(result.notes, equalTo(listOf(second, first)))
    }
}
