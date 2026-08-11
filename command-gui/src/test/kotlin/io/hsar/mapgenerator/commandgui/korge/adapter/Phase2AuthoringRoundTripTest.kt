package io.hsar.mapgenerator.commandgui.korge.adapter

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.operation.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.UUID

class Phase2AuthoringRoundTripTest {
    @TempDir
    lateinit var savesDirectory: Path

    @Test
    fun `ordinary phase two content retains ids images turns batches and descriptions across both viewers`() {
        val repository = MapViewerSaveRepository(savesDirectory)
        val unitTemplate = OperationUnitTemplate(
            OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
            "Frigate", image(16, 8), 0, affiliation = OperationUnitAffiliation.FRIEND,
        )
        val graphicTemplate = OperationGraphicTemplate(
            OperationGraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000102")),
            "Objective", image(12, 12), 0, .04, .04,
        )
        val unit = OperationUnit(
            OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000103")), unitTemplate.id,
            "Frigate 1", false, 45, MapCoordinate(.2, .3), .03, .015,
        )
        val graphic = OperationGraphic(
            OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000104")), graphicTemplate.id,
            "Objective", MapCoordinate(.5, .6), .04, .04,
        )
        val drawing = OperationDrawingObject(
            OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000105")),
            OperationLineDrawing(OperationDrawingPoint(.1, .8), OperationDrawingPoint(.7, .8), OperationArgb.of(255, 190, 62, 60)),
            mapOf(OPERATION_DISPLAY_NAME_PROPERTY to "Boundary"),
        )
        val note = OperationNote(
            OperationNoteId(UUID.fromString("00000000-0000-0000-0000-000000000106")),
            "Orders", "Hold the boundary.", listOf(unit.id, graphic.id, drawing.id),
        )
        val turnThumbnailId = UUID.fromString("00000000-0000-0000-0000-000000000107")
        val created = repository.createOperation(
            MapViewerSettings(operationName = "Phase 2"), MapViewerRenderData(image(100, 60)),
        )
        val timeline = created.timeline
            .recordChange(OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = unitTemplate))))
            .recordChange(OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = graphicTemplate))))
            .recordChange(OperationChangeSet(objects = listOf(
                OperationValueChange(after = unit), OperationValueChange(after = graphic), OperationValueChange(after = drawing),
            )))
            .recordChange(OperationChangeSet(notes = listOf(OperationValueChange(after = note))))
            .record(OperationTimelineEntries.turn("Turn 1", turnThumbnailId))
        val saved = repository.saveOperation(created.copy(
            timeline = timeline,
            generatedImageAssets = listOf(OperationGeneratedImage(turnThumbnailId, "turn-thumbnail", image(64, 36))),
        ))

        val loaded = repository.loadOperation(saved.sourcePath!!)
        val adapted = loaded.timeline.toLegacyEntries().toOperationTimeline()

        assertThat(adapted.contentAt().objects.map(OperationObject::id), equalTo(listOf(unit.id, graphic.id, drawing.id)))
        assertThat(adapted.contentAt().notes.single().referencedObjectIds, equalTo(note.referencedObjectIds))
        assertThat(adapted.contentAt().unitTemplates.single().image.width, equalTo(16))
        assertThat(adapted.contentAt().graphicTemplates.single().image.height, equalTo(12))
        assertThat(adapted.entries.map(OperationTimelineEntry::description), equalTo(loaded.timeline.entries.map(OperationTimelineEntry::description)))
        assertThat(adapted.entries.last().turnThumbnailAssetId, equalTo(turnThumbnailId))
        assertThat(adapted.entries.any {
            it.kind == io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind.BATCH && it.componentChangeSets.size == 3
        }, equalTo(true))
        assertThat(adapted.appliedCount, equalTo(loaded.timeline.appliedCount))
    }

    private fun image(width: Int, height: Int) = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
}
