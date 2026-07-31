package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.*
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import korlibs.io.lang.toByteArray
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*

class OperationDocumentRoundTripTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `saves and reloads an engine neutral operation with every object kind`() {
        val repository = MapViewerSaveRepository(directory)
        val image = BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB)
        val unitTemplate = OperationUnitTemplate(unitTemplateId, "Unit", icon(), 0)
        val graphicTemplate = OperationGraphicTemplate(graphicTemplateId, "Graphic", icon(), 0, 0.1, 0.12)
        val templates = OperationTimelineEntry(
            OperationChangeSet(
                unitTemplates = listOf(OperationValueChange(after = unitTemplate)),
                graphicTemplates = listOf(OperationValueChange(after = graphicTemplate)),
            ),
            MapViewerLedgerEntryKind.BATCH,
            "created templates",
        )
        val objects = listOf<OperationObject>(
            OperationUnit(id(1), unitTemplateId, "Alpha", false, 90, MapCoordinate(0.2, 0.3), 0.03, 0.04),
            OperationGraphic(id(2), graphicTemplateId, "Marker", MapCoordinate(0.5, 0.5), 0.1, 0.12),
            OperationDrawingObject(id(3), OperationFreeformDrawing(listOf(OperationDrawingPoint(0.1, 0.1), OperationDrawingPoint(0.2, 0.2)), colour)),
            OperationDrawingObject(id(4), OperationLineDrawing(OperationDrawingPoint(0.2, 0.2), OperationDrawingPoint(0.4, 0.4), colour)),
            OperationDrawingObject(id(5), OperationRectangleDrawing(OperationDrawingPoint(0.3, 0.3), OperationDrawingPoint(0.5, 0.5), colour, 30.0)),
            OperationDrawingObject(id(6), OperationOvalDrawing(OperationDrawingPoint(0.7, 0.7), 0.1, 0.05, colour, 45.0)),
        )
        val created = OperationTimelineEntry(
            OperationChangeSet(objects = objects.map { OperationValueChange(after = it) }),
            MapViewerLedgerEntryKind.BATCH,
            "created objects",
        )
        val target = directory.resolve("operation.oct.zip")
        val document = OperationDocument(
            operationId = UUID.fromString("00000000-0000-0000-0000-000000000099"),
            operationName = "Round Trip",
            createdAt = Instant.parse("2026-07-31T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-31T10:00:00Z"),
            settings = MapViewerSettings(operationName = "Round Trip", width = image.width, height = image.height),
            renderData = MapViewerRenderData(image),
            timeline = OperationTimeline().record(templates).record(created),
            additionalDataJson = """{"unknown":{"keep":true}}""".toByteArray(),
            sourcePath = target,
        )

        val uiState = OperationUiState(
            selectedTool = MapViewerTool.DRAW,
            selectedObjectIds = setOf(id(2)),
            workspaceMode = MapViewerWorkspaceMode.FLOATING,
            enabledLayerIds = setOf(MapLayer.GRID.id),
            openEditorIds = listOf("graphic-2"),
            drafts = mapOf("notes" to "draft text"),
        )
        val camera = MapViewerSavedCamera(0.25, 0.75, 2.0)
        repository.saveOperation(document, camera, uiState)
        val loaded = repository.loadOperation(target)

        assertThat(loaded.sourcePath, equalTo(target))
        assertThat(loaded.content.objects.map { it::class }, equalTo(objects.map { it::class }))
        assertThat((loaded.content.objects[4] as OperationDrawingObject).drawing, equalTo((objects[4] as OperationDrawingObject).drawing))
        assertThat(loaded.content.unitTemplates.single().image.width, equalTo(4))
        assertThat(loaded.content.graphicTemplates.single().normalizedHeight, equalTo(0.12))
        assertThat(loaded.camera, equalTo(camera))
        assertThat(loaded.uiState, equalTo(uiState))
        assertThat(OperationSaveJson.objectMapper.readTree(loaded.additionalDataJson).path("unknown").path("keep").booleanValue(), equalTo(true))
    }

    private fun id(number: Int) = OperationObjectId(UUID(0L, number.toLong()))
    private fun icon() = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
    private val unitTemplateId = OperationUnitTemplateId(UUID(0L, 20L))
    private val graphicTemplateId = OperationGraphicTemplateId(UUID(0L, 21L))
    private val colour = OperationArgb(0xff336699.toInt())
}
