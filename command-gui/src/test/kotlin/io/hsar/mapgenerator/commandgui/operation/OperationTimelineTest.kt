package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
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
        )

        assertThat(timeline.entries.size, equalTo(1))
        assertThat(timeline.entries.single().componentChangeSets.size, equalTo(2))
        assertThat((timeline.contentAt().objects.single() as OperationUnit).coordinate, equalTo(moved.coordinate))
        assertThat(timeline.undo().contentAt(), equalTo(OperationContent.EMPTY))
    }

    @Test
    fun `canonical entry splits aggregate targets and derives stable metadata`() {
        val second = unit.copy(id = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000012")), instanceName = "Second")

        val entry = OperationTimelineEntries.change(
            OperationContent.EMPTY,
            OperationChangeSet(objects = listOf(OperationValueChange(after = unit), OperationValueChange(after = second))),
        )!!

        assertThat(entry.kind, equalTo(MapViewerLedgerEntryKind.BATCH))
        assertThat(entry.componentChangeSets.size, equalTo(2))
        assertThat(entry.description, equalTo("created unit Unit () and 1 other item"))
    }

    @Test
    fun `turn groups retain unmarked actions future state targets and numbering`() {
        val created = OperationTimeline().recordChange(OperationChangeSet(objects = listOf(OperationValueChange(after = unit))))
        val withStart = created.record(OperationTimelineEntries.turn(OPERATION_START_TURN_NAME))
        val note = OperationNote(OperationNoteId(UUID.fromString("00000000-0000-0000-0000-000000000013")), "Orders", "Hold")
        val complete = withStart.recordChange(OperationChangeSet(notes = listOf(OperationValueChange(after = note))))
            .record(OperationTimelineEntries.turn("Turn 1"))
            .withAppliedCount(2)

        val groups = complete.turnGroups()

        assertThat(groups.map(OperationTurnGroup::name), equalTo(listOf("Unmarked History", OPERATION_START_TURN_NAME, "Turn 1")))
        assertThat(groups.first().actions.single().targets.single(), instanceOf(OperationLedgerTarget.Object::class.java))
        assertThat(groups.last().marker!!.isFuture, equalTo(true))
        assertThat(complete.nextTurnName(), equalTo("Turn 2"))
    }

    private fun entry(changes: OperationChangeSet, description: String) =
        OperationTimelineEntry(changes, MapViewerLedgerEntryKind.OBJECT, description)
}
