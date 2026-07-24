package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteStore
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerNoteReferenceMenuTest {
    @Test
    fun `note store returns notes referencing an object`() {
        val objectId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val unrelatedId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val store = MapNoteStore()
        val matching = MapNoteSnapshot(
            id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            title = "Alpha",
            body = "",
            referencedObjectIds = listOf(objectId),
        )
        store.upsert(matching)
        store.upsert(
            MapNoteSnapshot(
                id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000011")),
                title = "Bravo",
                body = "",
                referencedObjectIds = listOf(unrelatedId),
            ),
        )

        assertThat(store.snapshotsReferencing(objectId), equalTo(listOf(matching)))
    }

    @Test
    fun `note reference menu builds a submenu for each note and invokes the opener`() {
        val opened = mutableListOf<MapNoteId>()
        val notes = listOf(
            MapNoteSnapshot(
                id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                title = "Zulu",
                body = "",
            ),
            MapNoteSnapshot(
                id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-000000000102")),
                title = "Alpha",
                body = "",
            ),
        )

        val actions = noteReferenceContextMenuActions(
            referencedNotes = notes,
            openNote = opened::add,
        )

        assertThat(actions.size, equalTo(1))
        assertThat(actions.single().label, equalTo("Notes"))
        assertThat(
            actions.single().submenuActions.map { it.label },
            equalTo(listOf("Alpha", "Zulu")),
        )

        actions.single().submenuActions.first().onSelected()
        assertThat(opened, equalTo(listOf(notes[1].id)))
    }
}
