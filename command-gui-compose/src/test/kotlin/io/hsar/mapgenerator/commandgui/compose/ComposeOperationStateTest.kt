package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerPanel
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
import io.hsar.mapgenerator.commandgui.operation.OperationLedgerTarget
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationNote
import io.hsar.mapgenerator.commandgui.operation.OperationNoteDraft
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationImageDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
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
        assertThat(state.document.timeline.entries.last().description, equalTo("unit Renamed (): renamed to Renamed, facing 90°"))
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
    fun `renaming a drawing records one undoable edit and suppresses no ops`() {
        val shape = OperationLineDrawing(
            OperationDrawingPoint(.1, .1), OperationDrawingPoint(.2, .2), OperationArgb.of(255, 255, 0, 0),
        )
        val initial = ComposeOperationState(documentWith(unit)).addDrawing(shape)
        val drawing = initial.document.content.objects.last() as OperationDrawingObject

        val renamed = initial.renameDrawing(drawing.id, "  Objective  ")

        assertThat((renamed.document.content.objectById(drawing.id) as OperationDrawingObject).customProperties["displayName"], equalTo("Objective"))
        assertThat(renamed.document.timeline.entries.last().description, equalTo("line drawing: displayName set to Objective"))
        assertThat(renamed.renameDrawing(drawing.id, "Objective"), equalTo(renamed))
        assertThat(renamed.undo().document.content.objectById(drawing.id), equalTo(drawing))
    }

    @Test
    fun `keeps several independently identified editors open`() {
        val state = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId).openUnitEditor(objectId)

        assertThat(state.openEditors.map { it.id.value }, equalTo(listOf("unit:${objectId.value}")))
    }

    @Test
    fun `closing one editor retains its independently identified sibling`() {
        val editorId = OperationEditorId("unit:${objectId.value}")
        val state = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId).closeEditor(editorId)

        assertThat(state.openEditors, equalTo(emptyList()))
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
        val committed = preview.commitPreview()

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
    fun `save completion merges metadata without replacing newer content`() {
        val started = ComposeOperationState(documentWith(unit))
        val changed = started.selectObject(objectId).deleteSelection().let { state ->
            state.copy(document = state.document.copy(additionalDataJson = "newer".toByteArray()))
        }
        val saved = started.document.copy(
            persisted = true,
            sourcePath = java.nio.file.Path.of("saved.oct.zip"),
            additionalDataJson = "older".toByteArray(),
        )

        val completed = changed.withPersistedDocument(saved, started.revision)

        assertThat(completed.document.content.objects.isEmpty(), equalTo(true))
        assertThat(completed.document.additionalDataJson?.decodeToString(), equalTo("newer"))
        assertThat(completed.document.sourcePath, equalTo(saved.sourcePath))
        assertThat(completed.document.persisted, equalTo(true))
        assertThat(completed.revision, equalTo(changed.revision))
    }

    @Test
    fun `loaded document restores authoritative workspace layers and editors without becoming dirty`() {
        val uiState = OperationUiState(
            selectedTool = MapViewerTool.MEASURE,
            selectedObjectIds = setOf(objectId),
            enabledLayerIds = setOf("GRID"),
            workspace = OperationWorkspaceState.default().copy(mode = MapViewerWorkspaceMode.FLOATING),
        )

        val state = ComposeOperationState(documentWith(unit).copy(uiState = uiState, persisted = true))

        assertThat(state.activeTool, equalTo(MapViewerTool.MEASURE))
        assertThat(state.workspaceMode, equalTo(MapViewerWorkspaceMode.FLOATING))
        assertThat(state.enabledLayerIds, equalTo(setOf("GRID")))
        assertThat(state.openEditors, equalTo(emptyList()))
        assertThat(state.revision, equalTo(0L))
    }

    @Test
    fun `editor validation is derived from its restored typed draft`() {
        // Arrange
        val opened = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId)
        val editor = opened.openEditors.single { it.kind == OperationEditorKind.UNIT }
        val invalid = opened.updateEditorDraft(editor.id, (editor.draft as OperationEditorDraft.Unit).copy(facing = "north"))

        // Act
        val validation = invalid.validationFor(invalid.workspace.editor(editor.id)!!)

        // Assert
        assertThat(validation.valid, equalTo(false))
        assertThat(validation.errors.containsKey("facing"), equalTo(true))
    }

    @Test
    fun `pinning stores the legacy compatible top right map anchor`() {
        val viewport = MapViewport.centered(166.0, 166.0, 100, 100, 2.0)
        val editorId = OperationWorkspaceState.DEFAULT_NOTES_ID
        val state = ComposeOperationState(documentWith(unit)).openNotes().updateViewport(viewport)

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

    @Test
    fun `graphic placement previews and commits one selected object`() {
        val template = graphicTemplate()
        val document = documentWith(unit).copy(timeline = OperationTimeline().record(OperationTimelineEntry(
            OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = template))),
            MapViewerLedgerEntryKind.GRAPHIC_TEMPLATE, "template",
        )))

        val preview = ComposeOperationState(document).armGraphicPlacement(template.id).previewGraphicPlacement(MapCoordinate(.4, .6))
        val committed = preview.commitGraphicPlacement()

        assertThat(preview.document.timeline.entries.size, equalTo(1))
        assertThat(preview.visibleContent.objects.filterIsInstance<OperationGraphic>().size, equalTo(1))
        assertThat(committed.document.timeline.entries.size, equalTo(2))
        assertThat(committed.selectedObjectIds, equalTo(setOf(committed.document.content.objects.single().id)))
        assertThat(committed.graphicPlacement, equalTo(null))
    }

    @Test
    fun `saving graphic template size updates all instances in one timeline entry`() {
        val template = graphicTemplate()
        val graphic = OperationGraphic(OperationObjectId(UUID(0, 30)), template.id, template.name, MapCoordinate(.5, .5), .032, .032)
        val timeline = OperationTimeline().record(OperationTimelineEntry(
            OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = template)), objects = listOf(OperationValueChange(after = graphic))),
            MapViewerLedgerEntryKind.BATCH, "fixture",
        ))
        val editorId = OperationEditorId("graphic-template:${template.id.value}")
        val state = ComposeOperationState(documentWith(unit).copy(timeline = timeline)).openGraphicTemplateEditor(template.id)
        val draft = OperationGraphicTemplateDraft("Arrow", "8", "4")

        val saved = state.updateEditorDraft(editorId, OperationEditorDraft.GraphicTemplate(draft)).saveGraphicTemplateEditor(editorId, null)

        assertThat(saved.document.timeline.entries.size, equalTo(2))
        assertThat(saved.document.content.graphicTemplate(template.id)!!.normalizedWidth, equalTo(.08))
        assertThat((saved.document.content.objectById(graphic.id) as OperationGraphic).normalizedHeight, equalTo(.04))
    }

    @Test
    fun `history preview cancels armed graphic placement without recording`() {
        val template = graphicTemplate()
        val timeline = OperationTimeline().record(OperationTimelineEntry(
            OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = template))),
            MapViewerLedgerEntryKind.GRAPHIC_TEMPLATE, "template",
        ))
        val state = ComposeOperationState(documentWith(unit).copy(timeline = timeline)).armGraphicPlacement(template.id)

        val historical = state.previewHistory(0)

        assertThat(historical.graphicPlacement, equalTo(null))
        assertThat(historical.document.timeline, equalTo(timeline))
    }

    @Test
    fun `new note remains a draft until one save entry is recorded`() {
        val opened = ComposeOperationState(documentWith(unit)).openNewNoteEditor()
        val editor = opened.openEditors.single { it.kind == OperationEditorKind.NOTE }
        val draft = (editor.draft as OperationEditorDraft.Note).value.copy(title = "", body = "Line one\nLine two")

        val edited = opened.updateEditorDraft(editor.id, OperationEditorDraft.Note(draft))
        val saved = edited.saveNoteEditor(editor.id)

        assertThat(edited.document.content.notes, equalTo(emptyList()))
        assertThat(saved.document.content.notes.single().title, equalTo("Untitled Note"))
        assertThat(saved.document.content.notes.single().body, equalTo("Line one\nLine two"))
        assertThat(saved.document.timeline.entries.size, equalTo(2))
        assertThat(saved.workspace.editor(editor.id), equalTo(null))
    }

    @Test
    fun `reference picking accepts every map object kind updates only the draft and remains armed`() {
        val drawing = OperationDrawingObject(OperationObjectId(UUID(0, 50)), OperationLineDrawing(
            OperationDrawingPoint(.1, .1), OperationDrawingPoint(.2, .2), OperationArgb.of(255, 255, 255, 255),
        ))
        val graphic = OperationGraphic(OperationObjectId(UUID(0, 51)), OperationGraphicTemplateId(UUID(0, 52)), "Marker", MapCoordinate(.3, .3), .03, .03)
        val document = documentWith(unit)
        val entry = OperationTimelineEntry(
            OperationChangeSet(objects = listOf(OperationValueChange(after = drawing), OperationValueChange(after = graphic))),
            MapViewerLedgerEntryKind.BATCH, "objects",
        )
        val opened = ComposeOperationState(document.copy(timeline = document.timeline.record(entry))).openNewNoteEditor()
        val editor = opened.openEditors.single { it.kind == OperationEditorKind.NOTE }
        val armed = opened.armNoteReferencePick(editor.id).previewNoteReference(objectId)

        val picked = armed.addPickedNoteReference(objectId)
            .addPickedNoteReference(drawing.id)
            .addPickedNoteReference(graphic.id)
            .previewNoteReference(objectId)
            .addPickedNoteReference(objectId)

        val draft = (picked.workspace.editor(editor.id)!!.draft as OperationEditorDraft.Note).value
        assertThat(draft.referencedObjectIds, equalTo(listOf(objectId, drawing.id, graphic.id)))
        assertThat(picked.document.content.notes, equalTo(emptyList()))
        assertThat(picked.noteReferencePick?.hoveredObjectId, equalTo(objectId))
    }

    @Test
    fun `history tool change and editor close cancel note reference picking`() {
        val opened = ComposeOperationState(documentWith(unit)).openNewNoteEditor()
        val editor = opened.openEditors.single { it.kind == OperationEditorKind.NOTE }
        val armed = opened.armNoteReferencePick(editor.id)

        assertThat(armed.previewHistory(0).noteReferencePick, equalTo(null))
        assertThat(armed.selectTool(MapViewerTool.DRAW).noteReferencePick, equalTo(null))
        assertThat(armed.closeEditor(editor.id).noteReferencePick, equalTo(null))
    }

    @Test
    fun `note reorder and delete each record one timeline entry`() {
        val first = OperationNote(OperationNoteId(UUID(0, 40)), "First", "")
        val second = OperationNote(OperationNoteId(UUID(0, 41)), "Second", "")
        val notesEntry = OperationTimelineEntry(
            OperationChangeSet(notes = listOf(OperationValueChange(after = first), OperationValueChange(after = second))),
            MapViewerLedgerEntryKind.BATCH, "notes",
        )
        val state = ComposeOperationState(documentWith(unit).copy(timeline = documentWith(unit).timeline.record(notesEntry)))

        val reordered = state.reorderNotes(listOf(second.id, first.id))
        val deleted = reordered.deleteNote(first.id)

        assertThat(reordered.document.content.notes.map(OperationNote::id), equalTo(listOf(second.id, first.id)))
        assertThat(deleted.document.content.notes, equalTo(listOf(second)))
        assertThat(deleted.document.timeline.entries.size, equalTo(state.document.timeline.entries.size + 2))
    }

    @Test
    fun `turn selection moves the applied head read only and current restores the future`() {
        val thumbnail = BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB)
        val assetId = UUID.fromString("00000000-0000-0000-0000-000000000060")
        val opened = ComposeOperationState(documentWith(unit)).openUnitEditor(objectId)
        val withTurn = opened.addTurn("Turn 1", thumbnail, assetId)
        val renamed = withTurn.editUnit(unit.copy(instanceName = "Renamed"))
        val turnIndex = renamed.document.timeline.entries.indexOfFirst { it.turnName == "Turn 1" }

        val reverted = renamed.revertToTurn(turnIndex)
        val current = reverted.returnToCurrent()

        assertThat(reverted.isReadOnly, equalTo(true))
        assertThat(reverted.document.timeline.appliedCount, equalTo(turnIndex + 1))
        assertThat(reverted.openEditors.any { it.kind == OperationEditorKind.UNIT }, equalTo(true))
        assertThat((current.document.content.objectById(objectId) as OperationUnit).instanceName, equalTo("Renamed"))
        assertThat(current.isReadOnly, equalTo(false))
        assertThat(current.revision, equalTo(reverted.revision + 1))
    }

    @Test
    fun `branching after an undone turn prunes its generated thumbnail`() {
        val assetId = UUID.fromString("00000000-0000-0000-0000-000000000061")
        val withTurn = ComposeOperationState(documentWith(unit)).addTurn(
            "Turn 1", BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB), assetId,
        )

        val branched = withTurn.undo().addDrawing(OperationLineDrawing(
            OperationDrawingPoint(.1, .1), OperationDrawingPoint(.2, .2), OperationArgb.of(255, 255, 255, 255),
        ))

        assertThat(branched.document.timeline.entries.any { it.turnThumbnailAssetId == assetId }, equalTo(false))
        assertThat(branched.document.generatedImageAssets.any { it.id == assetId }, equalTo(false))
    }

    @Test
    fun `ledger target opening reuses the ordinary typed editor`() {
        val state = ComposeOperationState(documentWith(unit))
            .openLedgerTarget(OperationLedgerTarget.Object(objectId))
            .openLedgerTarget(OperationLedgerTarget.Object(objectId))

        assertThat(state.openEditors.count { it.kind == OperationEditorKind.UNIT }, equalTo(1))
    }

    @Test
    fun `menu panels toggle closed even when another editor is active`() {
        val opened = ComposeOperationState(documentWith(unit))
            .togglePanel(MapViewerPanel.GAME)
            .togglePanel(MapViewerPanel.NOTES)

        val closedGame = opened.togglePanel(MapViewerPanel.GAME)

        assertThat(closedGame.workspace.editor(io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState.GAME_ID), equalTo(null))
        assertThat(closedGame.workspace.editor(io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState.DEFAULT_NOTES_ID) != null, equalTo(true))
        assertThat(closedGame.workspace.dockContaining(io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState.DEFAULT_NOTES_ID)?.region, equalTo(io.hsar.mapgenerator.commandgui.operation.OperationDockRegion.LEFT))
    }

    private fun graphicTemplate() = OperationGraphicTemplate(
        OperationGraphicTemplateId(UUID(0, 20)), "Arrow", BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB), 0, .032, .032,
    )

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
