package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

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
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                name = "Unit 1",
                bitmap = Bitmap32(4, 4),
            ),
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

    @Test
    fun `unit template edits participate in undo and redo`() {
        val history = MapActionHistory()
        val controller = TestLedgerController()
        val before = UnitTemplateSnapshot(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                name = "SP",
                bitmap = Bitmap32(4, 4),
            ),
            index = 0,
        )
        val after = before.copy(
            template = before.template.copy(name = "S"),
        )

        controller.createUnitTemplate(before)
        history.record(
            EditUnitTemplateAction(
                id = before.id,
                forward = mapOf("name" to TextNode("S")),
                backward = mapOf("name" to TextNode("SP")),
                beforeSnapshot = before,
                afterSnapshot = after,
            ),
        )

        history.undo(controller)
        assertThat(controller.templates.single().template.name, equalTo("SP"))

        history.redo(controller)
        assertThat(controller.templates.single().template.name, equalTo("S"))
    }

    @Test
    fun `undo entry returns the entry that changed state`() {
        val history = MapActionHistory()
        val controller = TestLedgerController()
        val entry = TestLedgerEntry("first")

        history.record(entry)

        val undone = history.undoEntry(controller)

        assertThat(undone, equalTo(entry))
        assertThat(history.canRedo, equalTo(true))
    }

    @Test
    fun `batch ledger entries undo and redo children as one action`() {
        val history = MapActionHistory()
        val controller = TestLedgerController()
        val first = TestLedgerEntry("first")
        val second = TestLedgerEntry("second")

        history.record(BatchLedgerEntry(listOf(first, second)))
        history.undo(controller)
        history.redo(controller)

        assertThat(controller.events, equalTo(listOf("undo second", "undo first", "redo first", "redo second")))
    }

    private data class TestLedgerEntry(
        val name: String,
    ) : LedgerEntry {
        override fun undo(controller: LedgerController) {
            (controller as? TestLedgerController)?.events?.add("undo $name")
        }

        override fun redo(controller: LedgerController) {
            (controller as? TestLedgerController)?.events?.add("redo $name")
        }
    }

    private class TestLedgerController : LedgerController {
        val templates = mutableListOf<UnitTemplateSnapshot>()
        val events = mutableListOf<String>()

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

        override fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot) {
            val index = templates.indexOfFirst { it.id == snapshot.id }
            if (index >= 0) {
                templates[index] = snapshot
            }
        }

        override fun applyUnitTemplateEdit(id: UnitTemplateId, changes: Map<String, JsonNode?>): UnitTemplateSnapshot? {
            val index = templates.indexOfFirst { it.id == id }
            if (index < 0) return null
            val updated = templates[index].copy(
                template = templates[index].template.copy(
                    name = changes["name"]?.takeIf { !it.isNull }?.asText() ?: templates[index].template.name,
                ),
            )
            templates[index] = updated
            return updated
        }
    }
}
