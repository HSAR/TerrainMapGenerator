package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class OperationTimelineTest {
    private val templateId = OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
    private val objectId = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
    private val unit = OperationUnit(objectId, templateId, "Unit", false, 0, MapCoordinate(0.2, 0.3), 0.03, 0.03)

    @Test
    fun `records replays undoes and redoes entries`() {
        val create = entry(OperationChangeSet(objects = listOf(OperationValueChange(after = unit))), "created")
        val moved = unit.copy(coordinate = MapCoordinate(0.5, 0.6))
        val edit = entry(OperationChangeSet(objects = listOf(OperationValueChange(unit, moved))), "moved")

        val timeline = OperationTimeline().record(create).record(edit)
        val undone = timeline.undo()

        assertThat((timeline.contentAt().objects.single() as OperationUnit).coordinate, equalTo(moved.coordinate))
        assertThat((undone.contentAt().objects.single() as OperationUnit).coordinate, equalTo(unit.coordinate))
        assertThat(undone.redo().contentAt(), equalTo(timeline.contentAt()))
    }

    @Test
    fun `recording after undo truncates the redo branch`() {
        val created = OperationTimeline().record(entry(OperationChangeSet(objects = listOf(OperationValueChange(after = unit))), "created"))
        val deleted = created.record(entry(OperationChangeSet(objects = listOf(OperationValueChange(before = unit))), "deleted"))
        val renamed = unit.copy(instanceName = "Renamed")

        val branched = deleted.undo().record(entry(OperationChangeSet(objects = listOf(OperationValueChange(unit, renamed))), "renamed"))

        assertThat(branched.canRedo, equalTo(false))
        assertThat(branched.entries.size, equalTo(2))
        assertThat((branched.contentAt().objects.single() as OperationUnit).instanceName, equalTo("Renamed"))
    }

    @Test
    fun `batch recording combines sequential changes into one undoable entry`() {
        val moved = unit.copy(coordinate = MapCoordinate(0.7, 0.8))

        val timeline = OperationTimeline().recordBatch(
            listOf(
                OperationChangeSet(objects = listOf(OperationValueChange(after = unit))),
                OperationChangeSet(objects = listOf(OperationValueChange(unit, moved))),
            ),
            description = "created and moved unit",
        )

        assertThat(timeline.entries.size, equalTo(1))
        assertThat((timeline.contentAt().objects.single() as OperationUnit).coordinate, equalTo(moved.coordinate))
        assertThat(timeline.undo().contentAt(), equalTo(OperationContent.EMPTY))
    }

    private fun entry(changes: OperationChangeSet, description: String) =
        OperationTimelineEntry(changes, MapViewerLedgerEntryKind.OBJECT, description)
}
