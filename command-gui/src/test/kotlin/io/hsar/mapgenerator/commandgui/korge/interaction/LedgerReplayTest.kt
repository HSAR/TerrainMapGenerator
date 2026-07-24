package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
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

    @Test
    fun `replay applies edited template affiliation to existing units`() {
        val templateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
        val template = MapViewerUnitTemplate(
            id = templateId,
            name = "Scout",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val created = UnitSnapshot(
            id = unitId,
            unitTemplateId = templateId,
            template = template,
            instanceName = "Scout 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )
        val editedTemplate = UnitTemplateSnapshot(
            id = templateId,
            template = template.copy(affiliation = UnitAffiliation.Hostile),
            index = 0,
        )

        val result = LedgerReplay.replay(
            listOf(
                CreateUnitTemplateAction(UnitTemplateSnapshot(templateId, template, 0)),
                CreateObjectAction(created),
                EditUnitTemplateAction(
                    id = templateId,
                    forward = mapOf(),
                    backward = mapOf(),
                    beforeSnapshot = UnitTemplateSnapshot(templateId, template, 0),
                    afterSnapshot = editedTemplate,
                ),
            ),
        )

        val replayedUnit = result.objects.single() as UnitSnapshot
        assertThat(replayedUnit.template.affiliation, equalTo(UnitAffiliation.Hostile))
        assertThat(result.unitTemplates.single().template.affiliation, equalTo(UnitAffiliation.Hostile))
    }
}
