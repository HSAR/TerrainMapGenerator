package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerNoteReferenceDropTargetTest {
    @Test
    fun `accepts a drop over the target when it adds a new reference`() {
        val existing = MapPlacedObjectId(UUID(0, 1))
        val new = MapPlacedObjectId(UUID(0, 2))

        val accepted = MapViewerNoteReferenceDropTarget.canCreateReference(
            readOnly = false,
            draggedObjectIds = setOf(existing, new),
            referencedObjectIds = listOf(existing),
            pointerInDropTarget = true,
        )

        assertThat(accepted, equalTo(true))
    }

    @Test
    fun `rejects a drop that would not create a reference`() {
        val existing = MapPlacedObjectId(UUID(0, 1))

        val accepted = MapViewerNoteReferenceDropTarget.canCreateReference(
            readOnly = false,
            draggedObjectIds = setOf(existing),
            referencedObjectIds = listOf(existing),
            pointerInDropTarget = true,
        )

        assertThat(accepted, equalTo(false))
    }
}
