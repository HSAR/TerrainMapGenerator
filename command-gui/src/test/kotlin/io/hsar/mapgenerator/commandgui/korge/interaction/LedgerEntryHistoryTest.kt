package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class LedgerEntryHistoryTest {

    @Test
    fun `record enables undo and clears redo`() {
        val history = MapActionHistory()

        history.record(TestLedgerEntry("first"))
        history.record(TestLedgerEntry("second"))

        assertThat(history.canUndo, equalTo(true))
        assertThat(history.canRedo, equalTo(false))
    }

    @Test
    fun `recording after undo clears redo`() {
        val history = MapActionHistory()
        val controller = TestLedgerController()

        history.record(TestLedgerEntry("first"))
        history.undo(controller)
        assertThat(history.canRedo, equalTo(true))
        history.record(TestLedgerEntry("second"))

        assertThat(history.canRedo, equalTo(false))
    }

    @Test
    fun `unit template create and delete participate in undo and redo`() {
        val history = MapActionHistory()
        val controller = TestLedgerController()
        val snapshot = UnitTemplateSnapshot(
            id = UnitTemplateId(1),
            template = MapViewerUnitTemplate("Unit 1", Bitmap32(4, 4)),
            index = 0,
        )

        controller.createUnitTemplate(snapshot)
        history.record(CreateUnitTemplateAction(snapshot))
        history.undo(controller)
        assertThat(controller.templates, equalTo(emptyList()))

        history.redo(controller)
        assertThat(controller.templates.map { it.template.name }, equalTo(listOf("Unit 1")))

        controller.deleteUnitTemplate(snapshot.id)
        history.record(DeleteUnitTemplateAction(snapshot))
        history.undo(controller)
        assertThat(controller.templates.map { it.template.name }, equalTo(listOf("Unit 1")))
    }

    private data class TestLedgerEntry(
        val name: String,
    ) : LedgerEntry {
        override fun undo(controller: LedgerController) = Unit

        override fun redo(controller: LedgerController) = Unit
    }

    private class TestLedgerController : LedgerController {
        val templates = mutableListOf<UnitTemplateSnapshot>()

        override fun createObject(snapshot: MapPlacedObjectSnapshot) = Unit

        override fun applyObjectEdit(id: MapPlacedObjectId, changes: Map<String, JsonNode?>) = Unit

        override fun deleteObject(id: MapPlacedObjectId): MapPlacedObjectSnapshot? = null

        override fun createUnitTemplate(snapshot: UnitTemplateSnapshot) {
            val insertionIndex = snapshot.index.coerceIn(0, templates.size)
            templates.add(insertionIndex, snapshot)
        }

        override fun deleteUnitTemplate(id: UnitTemplateId): UnitTemplateSnapshot? {
            val index = templates.indexOfFirst { it.id == id }
            if (index < 0) return null
            return templates.removeAt(index)
        }
    }
}
