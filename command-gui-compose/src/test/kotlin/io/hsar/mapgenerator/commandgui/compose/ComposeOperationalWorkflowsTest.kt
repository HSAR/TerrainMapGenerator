package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.*
import io.hsar.mapgenerator.commandgui.operation.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.UUID

class ComposeOperationalWorkflowsTest {
    private val unitId = OperationObjectId(UUID(0, 1))

    @AfterEach
    fun clearFeatureFlags() = MapViewerFeatureFlags.initialize(emptySet())

    @Test
    fun `operational editors are unavailable without the experimental flag`() {
        MapViewerFeatureFlags.initialize(emptySet())
        val state = ComposeOperationState(document())

        assertThat(state.openTactical(unitId), equalTo(state))
        assertThat(state.openSensorReport(unitId, OperationWorkflowReportMode.SCIREP), equalTo(state))
        assertThat(state.openOlympiadDawnStatus(unitId), equalTo(state))
        assertThat(state.ecmFields(), equalTo(emptyList()))
    }

    @Test
    fun `enabled operational editors use typed workspace state`() {
        MapViewerFeatureFlags.initialize(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL))
        val state = ComposeOperationState(document())

        val opened = state.openOlympiadDawnStatus(unitId).openTactical(unitId)

        assertThat(opened.workspace.editors.map { it.kind }, equalTo(listOf(
            OperationEditorKind.OLYMPIAD_DAWN_STATUS, OperationEditorKind.TACTICAL,
        )))
    }

    private fun document(): OperationDocument {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val templateId = OperationUnitTemplateId(UUID(0, 2))
        val unit = OperationUnit(unitId, templateId, "Unit", false, 0, MapCoordinate(.5, .5), .02, .02)
        val content = OperationContent(listOf(unit), listOf(OperationUnitTemplate(templateId, "Unit", image, 0)))
        val timeline = OperationTimeline(listOf(OperationTimelineEntry(
            OperationChangeSet(objects = listOf(OperationValueChange(after = unit)), unitTemplates = listOf(OperationValueChange(after = content.unitTemplates.single()))),
            MapViewerLedgerEntryKind.OBJECT, "start", turnName = "Operation Start",
        )), 1)
        return OperationDocument(UUID.randomUUID(), "Test", Instant.EPOCH, Instant.EPOCH, MapViewerSettings(unitsPerPixel = 1.0), MapViewerRenderData(image), timeline = timeline)
    }
}
