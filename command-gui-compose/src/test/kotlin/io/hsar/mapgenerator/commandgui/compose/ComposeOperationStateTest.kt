package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.operation.OperationArgb
import io.hsar.mapgenerator.commandgui.operation.OperationChangeSet
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationImageDraft
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationValueChange
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*

class ComposeOperationStateTest {
    private val objectId = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val unit = OperationUnit(
        id = objectId,
        unitTemplateId = OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        instanceName = "Unit",
        destroyed = false,
        facingDegrees = 0,
        coordinate = MapCoordinate(0.5, 0.5),
        normalizedWidth = 0.03,
        normalizedHeight = 0.03,
    )

    @Test
    fun `owns tool selection history and dirty revision in one immutable state`() {
        val state = ComposeOperationState(documentWith(unit))
            .selectTool(MapViewerTool.DRAW)
            .selectObject(objectId)
            .previewHistory(0)

        assertThat(state.activeTool, equalTo(MapViewerTool.DRAW))
        assertThat(state.isReadOnly, equalTo(true))
        assertThat(state.revision > 1L, equalTo(true))
    }

    @Test
    fun `editing a unit records an object timeline entry`() {
        val updated = unit.copy(instanceName = "Renamed", facingDegrees = 90)

        val state = ComposeOperationState(documentWith(unit)).editUnit(updated)

        assertThat(state.document.content.objectById(objectId), equalTo(updated))
        assertThat(state.document.timeline.entries.last().description, equalTo("edited Renamed"))
    }

    @Test
    fun `adding a drawing records and selects one operation object`() {
        val drawing = OperationLineDrawing(OperationDrawingPoint(.1, .1), OperationDrawingPoint(.2, .2), OperationArgb.of(255, 255, 0, 0))

        val state = ComposeOperationState(documentWith(unit)).addDrawing(drawing)

        assertThat((state.document.content.objects.last() as OperationDrawingObject).drawing, equalTo(drawing))
        assertThat(state.selectedObjectIds, equalTo(setOf(state.document.content.objects.last().id)))
        assertThat(state.document.timeline.entries.last().description, equalTo("created line drawing"))
    }

    @Test
    fun `keeps several independently identified editors open`() {
        val state = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId).openUnitEditor(objectId)

        assertThat(state.openEditors.map { it.id.value }, equalTo(listOf("operation-notes", "unit:${objectId.value}")))
    }

    @Test
    fun `closing one editor retains its independently identified sibling`() {
        val editorId = OperationEditorId("unit:${objectId.value}")
        val state = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId).closeEditor(editorId)

        assertThat(state.openEditors.map { it.id.value }, equalTo(listOf("operation-notes")))
    }

    @Test
    fun `closing discards a unit draft and reopening hydrates current content`() {
        // Arrange
        val editorId = OperationEditorId("unit:${objectId.value}")
        val opened = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId)
        val draft = opened.workspace.editor(editorId)!!.draft as OperationEditorDraft.Unit

        // Act
        val reopened = opened.updateEditorDraft(editorId, draft.copy(name = "Unsaved"))
            .closeEditor(editorId)
            .openUnitEditor(objectId)

        // Assert
        assertThat((reopened.workspace.editor(editorId)!!.draft as OperationEditorDraft.Unit).name, equalTo("Unit"))
    }

    @Test
    fun `preview does not mutate the timeline until committed`() {
        val initial = ComposeOperationState(documentWith(unit))
        val moved = initial.document.content.copy(objects = listOf(unit.copy(coordinate = MapCoordinate(0.7, 0.5))))

        val preview = initial.preview(moved)
        val committed = preview.commitPreview(description = "moved unit")

        assertThat(preview.document.timeline.entries.size, equalTo(1))
        assertThat((preview.visibleContent.objects.single() as OperationUnit).coordinate.x, equalTo(0.7))
        assertThat(committed.document.timeline.entries.size, equalTo(2))
        assertThat((committed.document.content.objects.single() as OperationUnit).coordinate.x, equalTo(0.7))
    }

    @Test
    fun `undo redo and deletion prune selection`() {
        val state = ComposeOperationState(documentWith(unit)).selectObject(objectId)
        val deleted = state.deleteSelection()
        val restored = deleted.undo()

        assertThat(deleted.document.content.objects.isEmpty(), equalTo(true))
        assertThat(deleted.selectedObjectIds.isEmpty(), equalTo(true))
        assertThat(restored.document.content.objects.single(), equalTo(unit))
        assertThat(restored.redo().document.content.objects.isEmpty(), equalTo(true))
    }

    @Test
    fun `save completion keeps changes made during an in flight save dirty`() {
        val started = ComposeOperationState(documentWith(unit)).saveStarted()
        val changed = started.selectTool(MapViewerTool.DRAW)

        val completed = changed.saveSucceeded(started.document, started.revision)

        assertThat(completed.saveStatus, equalTo(ComposeSaveStatus.DIRTY))
        assertThat(completed.hasUnsavedChanges, equalTo(true))
    }

    @Test
    fun `loaded document restores authoritative workspace layers editors and drafts without becoming dirty`() {
        val uiState = OperationUiState(
            selectedTool = MapViewerTool.MEASURE,
            selectedObjectIds = setOf(objectId),
            enabledLayerIds = setOf("GRID"),
            workspace = OperationWorkspaceState.default("restored").copy(mode = MapViewerWorkspaceMode.FLOATING),
        )

        val state = ComposeOperationState(documentWith(unit).copy(uiState = uiState, persisted = true))

        assertThat(state.activeTool, equalTo(MapViewerTool.MEASURE))
        assertThat(state.workspaceMode, equalTo(MapViewerWorkspaceMode.FLOATING))
        assertThat(state.enabledLayerIds, equalTo(setOf("GRID")))
        assertThat(state.openEditors.single().id, equalTo(OperationWorkspaceState.DEFAULT_NOTES_ID))
        assertThat((state.openEditors.single().draft as OperationEditorDraft.Notes).body, equalTo("restored"))
        assertThat(state.saveStatus, equalTo(ComposeSaveStatus.CLEAN))
        assertThat(state.hasUnsavedChanges, equalTo(false))
    }

    @Test
    fun `pinning stores the legacy compatible top right map anchor`() {
        val viewport = MapViewport.centered(166.0, 166.0, 100, 100, 2.0)
        val editorId = OperationWorkspaceState.DEFAULT_NOTES_ID
        val state = ComposeOperationState(documentWith(unit)).updateViewport(viewport)

        val pinned = state.pinEditor(editorId, ViewerPoint(60.0, 20.0))

        val floating = pinned.workspace.editor(editorId)!!.floating
        assertThat(floating.pinned, equalTo(true))
        assertThat(floating.mapAnchor, equalTo(ViewerPoint(88.0, 68.0)))
    }

    @Test
    fun `history preview retains a target editor but live deletion prunes it`() {
        // Arrange
        val opened = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId)

        // Act
        val historical = opened.previewHistory(-1)
        val deleted = opened.selectObject(objectId).deleteSelection()

        // Assert
        assertThat(historical.openEditors.any { it.kind == OperationEditorKind.UNIT }, equalTo(true))
        assertThat(deleted.openEditors.any { it.kind == OperationEditorKind.UNIT }, equalTo(false))
    }

    @Test
    fun `historical preview is read only and current returns to the applied head`() {
        val live = ComposeOperationState(documentWith(unit)).selectObject(objectId).deleteSelection()
        val historical = live.previewHistory(0)

        val attemptedEdit = historical.selectObject(objectId).deleteSelection()

        assertThat(attemptedEdit.document.timeline, equalTo(live.document.timeline))
        assertThat(attemptedEdit.visibleContent.objects.single(), equalTo(unit))
        assertThat(attemptedEdit.returnToCurrent().visibleContent.objects.isEmpty(), equalTo(true))
    }

    @Test
    fun `failed save remains dirty until a later successful revision is saved`() {
        val dirty = ComposeOperationState(documentWith(unit)).selectTool(MapViewerTool.DRAW)
        val failed = dirty.saveStarted().saveFailed("disk unavailable")
        val retried = failed.saveStarted().saveSucceeded(failed.document.copy(persisted = true), failed.revision)

        assertThat(failed.saveStatus, equalTo(ComposeSaveStatus.FAILED))
        assertThat(failed.hasUnsavedChanges, equalTo(true))
        assertThat(retried.saveStatus, equalTo(ComposeSaveStatus.CLEAN))
        assertThat(retried.hasUnsavedChanges, equalTo(false))
    }

    @Test
    fun `unit placement previews without a ledger entry and commits exactly once`() {
        val template = OperationUnitTemplate(unit.unitTemplateId, "Scout", BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB), 0)
        val templateEntry = OperationTimelineEntry(
            OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = template))),
            MapViewerLedgerEntryKind.UNIT_TEMPLATE,
            "created unit template Scout",
        )
        val document = documentWith(unit).copy(timeline = OperationTimeline().record(templateEntry))
        val viewport = MapViewport.centered(166.0, 166.0, 100, 100, 1.0)

        val preview = ComposeOperationState(document).updateViewport(viewport)
            .armUnitPlacement(template.id)
            .previewUnitPlacement(MapCoordinate(.4, .6), 64.0)
        val committed = preview.commitUnitPlacement()

        assertThat(preview.document.timeline.entries.size, equalTo(1))
        assertThat(preview.visibleContent.objects.size, equalTo(1))
        assertThat(committed.document.timeline.entries.size, equalTo(2))
        assertThat(committed.document.content.objects.filterIsInstance<OperationUnit>().single().coordinate, equalTo(MapCoordinate(.4, .6)))
        assertThat(committed.unitPlacement, equalTo(null))
    }

    @Test
    fun `template deletion is blocked while referenced`() {
        val template = OperationUnitTemplate(unit.unitTemplateId, "Scout", BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB), 0)
        val timeline = OperationTimeline()
            .record(OperationTimelineEntry(OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = template))), MapViewerLedgerEntryKind.UNIT_TEMPLATE, "template"))
            .record(OperationTimelineEntry(OperationChangeSet(objects = listOf(OperationValueChange(after = unit))), MapViewerLedgerEntryKind.OBJECT, "unit"))
        val document = documentWith(unit).copy(timeline = timeline)

        val state = ComposeOperationState(document).deleteUnitTemplate(template.id)

        assertThat(state.document.timeline, equalTo(timeline))
        assertThat(state.document.content.unitTemplate(template.id), equalTo(template))
    }

    @Test
    fun `saving a new template editor records one template entry and closes the draft`() {
        val image = BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB)
        val editorId = OperationEditorId("unit-template:new")
        val opened = ComposeOperationState(documentWith(unit)).openNewUnitTemplateEditor()
        val draft = OperationUnitTemplateDraft(
            name = "Scout",
            replacementImage = OperationImageDraft.fromBytes("scout.png", byteArrayOf(1, 2, 3)),
        )

        val saved = opened.updateEditorDraft(editorId, OperationEditorDraft.UnitTemplate(draft))
            .saveUnitTemplateEditor(editorId, image)

        assertThat(saved.document.content.unitTemplates.single().name, equalTo("Scout"))
        assertThat(saved.document.timeline.entries.last().kind, equalTo(MapViewerLedgerEntryKind.UNIT_TEMPLATE))
        assertThat(saved.workspace.editor(editorId), equalTo(null))
    }

    private fun documentWith(value: OperationUnit): OperationDocument {
        val create = OperationTimelineEntry(
            changeSet = OperationChangeSet(objects = listOf(OperationValueChange(after = value))),
            kind = MapViewerLedgerEntryKind.OBJECT,
            description = "created unit",
        )
        return OperationDocument(
            operationId = UUID.fromString("00000000-0000-0000-0000-000000000010"),
            operationName = "Test",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            settings = MapViewerSettings(operationName = "Test"),
            renderData = MapViewerRenderData(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)),
            timeline = OperationTimeline().record(create),
        )
    }
}
