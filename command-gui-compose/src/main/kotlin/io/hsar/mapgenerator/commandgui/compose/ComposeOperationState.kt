package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerPanel
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationGeneratedImage
import io.hsar.mapgenerator.commandgui.operation.OperationLedgerTarget
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationNote
import io.hsar.mapgenerator.commandgui.operation.OperationNoteAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationNoteDraft
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntries
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import io.hsar.mapgenerator.commandgui.operation.changesFrom
import io.hsar.mapgenerator.commandgui.operation.delete
import io.hsar.mapgenerator.commandgui.operation.editorState
import io.hsar.mapgenerator.commandgui.operation.nextTurnName
import java.util.*
import java.util.Collections.emptySet

data class ComposeUnitPlacement(
    val templateId: OperationUnitTemplateId,
    val objectId: OperationObjectId = OperationObjectId(UUID.randomUUID()),
    val coordinate: io.hsar.mapgenerator.commandgui.MapCoordinate? = null,
)

data class ComposeGraphicPlacement(
    val templateId: OperationGraphicTemplateId,
    val objectId: OperationObjectId = OperationObjectId(UUID.randomUUID()),
    val coordinate: io.hsar.mapgenerator.commandgui.MapCoordinate? = null,
)

data class ComposeNoteReferencePick(
    val editorId: OperationEditorId,
    val hoveredObjectId: OperationObjectId? = null,
)

data class ComposeOperationState(
    val document: OperationDocument,
    val viewport: MapViewport? = null,
    val activeTool: MapViewerTool = document.uiState.selectedTool,
    val selectedObjectIds: Set<OperationObjectId> = document.uiState.selectedObjectIds,
    val workspace: OperationWorkspaceState = document.uiState.workspace.hydrate(document.content, document.renderData),
    val enabledLayerIds: Set<String> = document.uiState.enabledLayerIds.ifEmpty { document.settings.enabledLayerIds },
    val historyPreviewCount: Int? = null,
    val previewContent: OperationContent? = null,
    val unitPlacement: ComposeUnitPlacement? = null,
    val graphicPlacement: ComposeGraphicPlacement? = null,
    val noteReferencePick: ComposeNoteReferencePick? = null,
    val revision: Long = if (document.persisted) 0L else 1L,
) {
    val isReadOnly get() = historyPreviewCount != null
    val visibleContent
        get() = previewContent
            ?: historyPreviewCount?.let(document.timeline::contentAt)
            ?: document.content
    val workspaceMode get() = workspace.mode
    val openEditors get() = workspace.editors

    fun selectTool(tool: MapViewerTool) =
        if (tool == activeTool) cancelPlacements() else persistentCopy(activeTool = tool).cancelPlacements()

    fun selectObject(id: OperationObjectId?, additive: Boolean = false) =
        copy(selectedObjectIds = selectedObjectIds.withSelection(id, additive)).markUiDirtyIfChanged(selectedObjectIds)

    fun selectBox(ids: Set<OperationObjectId>, additive: Boolean) =
        copy(selectedObjectIds = selectedObjectIds.withBoxSelection(ids, additive)).markUiDirtyIfChanged(selectedObjectIds)

    fun updateViewport(value: MapViewport) =
        if (value == viewport) this else persistentCopy(viewport = value)

    fun updateWorkspaceMode(value: MapViewerWorkspaceMode) =
        if (value == workspace.mode) this else persistentCopy(workspace = workspace.withMode(value))

    fun updateEnabledLayers(value: Set<String>) =
        if (value == enabledLayerIds) this else persistentCopy(
            document = document.copy(settings = document.settings.copy(enabledLayerIds = value)),
            enabledLayerIds = value,
        )

    fun openNotes() = updateWorkspace(workspace.open(OperationWorkspaceState.notesEditor(), OperationDockRegion.LEFT))

    fun togglePanel(panel: MapViewerPanel): ComposeOperationState {
        val editor = when (panel) {
            MapViewerPanel.GAME -> OperationWorkspaceState.gameEditor()
            MapViewerPanel.NOTES -> OperationWorkspaceState.notesEditor()
            MapViewerPanel.GRAPHICS -> OperationWorkspaceState.graphicTypesEditor()
            MapViewerPanel.UNITS -> OperationWorkspaceState.unitTypesEditor()
        }
        workspace.editor(editor.id)?.let { return closeEditor(it.id) }
        val region = if (panel == MapViewerPanel.GAME || panel == MapViewerPanel.NOTES) OperationDockRegion.LEFT else OperationDockRegion.RIGHT
        return updateWorkspace(workspace.open(editor, region))
    }

    fun openNewNoteEditor(): ComposeOperationState {
        if (isReadOnly) return this
        val draft = OperationNoteDraft()
        return updateWorkspace(workspace.openFloating(OperationEditorState(
            id = OperationEditorId("note:new:${draft.id.value}"),
            kind = OperationEditorKind.NOTE,
            target = OperationEditorTarget.NewNote(draft.id),
            draft = OperationEditorDraft.Note(draft),
            floating = io.hsar.mapgenerator.commandgui.operation.OperationFloatingEditorState(OperationEditorBounds(128.0, 96.0, 700.0, 720.0)),
        )))
    }

    fun openNoteEditor(id: OperationNoteId): ComposeOperationState {
        val note = visibleContent.notes.firstOrNull { it.id == id } ?: return this
        return updateWorkspace(workspace.openFloating(note.editorState()))
    }

    fun openUnitEditor(id: OperationObjectId): ComposeOperationState {
        val unit = visibleContent.objectById(id) as? OperationUnit ?: return this
        val cascade = workspace.editors.count { it.kind == OperationEditorKind.UNIT } * 32.0
        return updateWorkspace(workspace.open(unit.editorState(OperationEditorBounds(96.0 + cascade, 96.0 + cascade, 520.0, 560.0), document.renderData)))
    }

    fun openNewUnitTemplateEditor(): ComposeOperationState {
        val existing = workspace.editors.firstOrNull { it.target == OperationEditorTarget.NewUnitTemplate }
        if (existing != null) return activateEditor(existing.id)
        val editor = OperationEditorState(
            id = OperationEditorId("unit-template:new"),
            kind = OperationEditorKind.UNIT_TEMPLATE,
            target = OperationEditorTarget.NewUnitTemplate,
            draft = OperationEditorDraft.UnitTemplate(),
            floating = io.hsar.mapgenerator.commandgui.operation.OperationFloatingEditorState(OperationEditorBounds(96.0, 96.0, 520.0, 600.0)),
        )
        return updateWorkspace(workspace.open(editor))
    }

    fun openUnitTemplateEditor(id: OperationUnitTemplateId): ComposeOperationState {
        val template = document.content.unitTemplate(id) ?: return this
        return updateWorkspace(workspace.open(template.editorState()))
    }

    fun openGraphicEditor(id: OperationObjectId): ComposeOperationState {
        val graphic = visibleContent.objectById(id) as? OperationGraphic ?: return this
        val cascade = workspace.editors.count { it.kind == OperationEditorKind.GRAPHIC } * 32.0
        return updateWorkspace(workspace.open(graphic.editorState(document.renderData, OperationEditorBounds(112.0 + cascade, 112.0 + cascade, 520.0, 560.0))))
    }

    fun openNewGraphicTemplateEditor(): ComposeOperationState {
        val existing = workspace.editors.firstOrNull { it.target == OperationEditorTarget.NewGraphicTemplate }
        if (existing != null) return activateEditor(existing.id)
        return updateWorkspace(workspace.open(OperationEditorState(
            id = OperationEditorId("graphic-template:new"),
            kind = OperationEditorKind.GRAPHIC_TEMPLATE,
            target = OperationEditorTarget.NewGraphicTemplate,
            draft = OperationEditorDraft.GraphicTemplate(),
            floating = io.hsar.mapgenerator.commandgui.operation.OperationFloatingEditorState(OperationEditorBounds(112.0, 112.0, 520.0, 620.0)),
        )))
    }

    fun openGraphicTemplateEditor(id: OperationGraphicTemplateId): ComposeOperationState {
        val template = document.content.graphicTemplate(id) ?: return this
        return updateWorkspace(workspace.open(template.editorState()))
    }

    fun activateEditor(id: OperationEditorId) = updateWorkspace(workspace.activate(id))

    fun closeEditor(id: OperationEditorId) = updateWorkspace(workspace.close(id)).let { state ->
        if (state.noteReferencePick?.editorId == id) state.copy(noteReferencePick = null) else state
    }

    fun updateEditorDraft(id: OperationEditorId, draft: OperationEditorDraft) = updateWorkspace(
        workspace.updateEditor(id) { it.copy(draft = draft) },
    )

    fun saveNoteEditor(id: OperationEditorId): ComposeOperationState {
        if (isReadOnly) return this
        val editor = workspace.editor(id) ?: return this
        val draft = (editor.draft as? OperationEditorDraft.Note)?.value ?: return this
        val before = (editor.target as? OperationEditorTarget.Note)?.noteId
            ?.let { noteId -> document.content.notes.firstOrNull { it.id == noteId } }
        val changes = before?.let { OperationNoteAuthoring.editChange(it, draft) }
            ?: OperationNoteAuthoring.createChange(document.content, draft)
        if (changes.isEmpty) return closeEditor(id)
        return withChangeSet(changes).closeEditor(id)
    }

    fun deleteNote(id: OperationNoteId): ComposeOperationState {
        if (isReadOnly) return this
        document.content.notes.firstOrNull { it.id == id } ?: return this
        val changes = OperationNoteAuthoring.deleteChange(document.content, id)
        if (changes.isEmpty) return this
        val nextWorkspace = workspace.editors
            .filter { (it.target as? OperationEditorTarget.Note)?.noteId == id }
            .fold(workspace) { value, editor -> value.close(editor.id) }
        return withChangeSet(changes)
            .copy(workspace = nextWorkspace, noteReferencePick = noteReferencePick?.takeUnless { nextWorkspace.editor(it.editorId) == null })
    }

    fun reorderNotes(orderedIds: List<OperationNoteId>): ComposeOperationState {
        if (isReadOnly) return this
        val changes = OperationNoteAuthoring.reorderChange(document.content, orderedIds)
        return if (changes.isEmpty) this else withChangeSet(changes)
    }

    fun armNoteReferencePick(editorId: OperationEditorId): ComposeOperationState {
        if (isReadOnly || workspace.editor(editorId)?.draft !is OperationEditorDraft.Note) return this
        return cancelPlacements().copy(noteReferencePick = ComposeNoteReferencePick(editorId))
    }

    fun previewNoteReference(objectId: OperationObjectId?): ComposeOperationState {
        val pick = noteReferencePick ?: return this
        val valid = objectId?.takeIf { id -> visibleContent.objectById(id) != null }
        return if (pick.hoveredObjectId == valid) this else copy(noteReferencePick = pick.copy(hoveredObjectId = valid))
    }

    fun addPickedNoteReference(objectId: OperationObjectId): ComposeOperationState {
        val pick = noteReferencePick ?: return this
        if (visibleContent.objectById(objectId) == null) return this
        val editor = workspace.editor(pick.editorId) ?: return cancelNoteReferencePick()
        val draft = (editor.draft as? OperationEditorDraft.Note)?.value ?: return cancelNoteReferencePick()
        if (objectId in draft.referencedObjectIds) return this
        return updateEditorDraft(editor.id, OperationEditorDraft.Note(
            draft.copy(referencedObjectIds = draft.referencedObjectIds + objectId),
        )).copy(noteReferencePick = pick.copy(hoveredObjectId = objectId))
    }

    fun cancelNoteReferencePick() = copy(noteReferencePick = null)

    fun updateFloatingEditor(id: OperationEditorId, bounds: OperationEditorBounds) = updateWorkspace(
        workspace.updateEditor(id) { editor -> editor.copy(floating = editor.floating.copy(bounds = bounds)) },
    )

    fun toggleEditorMinimized(id: OperationEditorId) = updateWorkspace(
        workspace.updateEditor(id) { editor -> editor.copy(floating = editor.floating.copy(minimized = !editor.floating.minimized)) },
    )

    fun pinEditor(id: OperationEditorId, screenTopRight: ViewerPoint): ComposeOperationState {
        val current = viewport ?: return this
        val anchor = ViewerPoint(
            current.left + screenTopRight.x / current.zoom,
            current.top + screenTopRight.y / current.zoom,
        )
        return updateWorkspace(workspace.updateEditor(id) { editor ->
            editor.copy(floating = editor.floating.copy(pinned = true, mapAnchor = anchor))
        })
    }

    fun unpinEditor(id: OperationEditorId, displayedBounds: OperationEditorBounds) = updateWorkspace(
        workspace.updateEditor(id) { editor ->
            editor.copy(floating = editor.floating.copy(bounds = displayedBounds, pinned = false, mapAnchor = null))
        },
    )

    fun updateDockSize(region: OperationDockRegion, size: Double) = updateWorkspace(
        workspace.updateDock(region) { it.copy(size = size.coerceIn(if (region == OperationDockRegion.BOTTOM) 180.0 else 240.0, if (region == OperationDockRegion.BOTTOM) 600.0 else 720.0)) },
    )

    fun activateDockTab(region: OperationDockRegion, id: OperationEditorId) = updateWorkspace(
        workspace.activate(id).updateDock(region) { dock -> if (id in dock.editorIds) dock.copy(activeEditorId = id, collapsed = false) else dock },
    )

    fun toggleDockCollapsed(region: OperationDockRegion) = updateWorkspace(
        workspace.updateDock(region) { it.copy(collapsed = !it.collapsed) },
    )

    fun dockEditor(id: OperationEditorId, region: OperationDockRegion, index: Int? = null) =
        updateWorkspace(workspace.dock(id, region, index))

    fun reorderDockTabs(region: OperationDockRegion, orderedIds: List<OperationEditorId>) =
        updateWorkspace(workspace.reorderDock(region, orderedIds))

    fun cycleDockTabs(direction: Int) = workspace.activeDock()?.region?.let { region ->
        updateWorkspace(workspace.cycleDock(region, direction))
    } ?: this

    fun preview(content: OperationContent) =
        if (isReadOnly) this else copy(previewContent = content)

    fun cancelPreview() = copy(previewContent = null)

    fun commitPreview(): ComposeOperationState {
        val after = previewContent ?: return this
        val changes = after.changesFrom(document.content)
        if (changes.isEmpty || isReadOnly) return cancelPreview()
        return withChangeSet(changes).copy(previewContent = null)
    }

    fun deleteSelection(): ComposeOperationState {
        if (isReadOnly || selectedObjectIds.isEmpty()) return this
        val after = document.content.delete(selectedObjectIds)
        return preview(after).commitPreview().copy(selectedObjectIds = emptySet())
    }

    fun editUnit(updated: OperationUnit): ComposeOperationState {
        if (isReadOnly) return this
        val before = document.content.objectById(updated.id) as? OperationUnit ?: return this
        if (before == updated) return this
        return withChangeSet(OperationUnitAuthoring.editUnitChange(before, updated))
    }

    fun saveUnitTemplateEditor(id: OperationEditorId, image: java.awt.image.BufferedImage?): ComposeOperationState {
        if (isReadOnly) return this
        val editor = workspace.editor(id) ?: return this
        val draft = (editor.draft as? OperationEditorDraft.UnitTemplate)?.value ?: return this
        val target = editor.target
        val existing = (target as? OperationEditorTarget.UnitTemplate)?.templateId?.let(document.content::unitTemplate)
        if (draft.replacementImage != null && image == null) return this
        val resolvedImage = image ?: existing?.image ?: return this
        if (!validationFor(editor).valid) return this
        val updated = existing?.let { OperationUnitAuthoring.editTemplate(it, draft, image) }
            ?: OperationUnitAuthoring.createTemplate(document.content, draft, resolvedImage)
        val changes = existing?.let { OperationUnitAuthoring.editTemplateChange(it, updated) }
            ?: OperationUnitAuthoring.createTemplateChange(updated)
        if (changes.isEmpty) return closeEditor(id)
        return withChangeSet(changes).closeEditor(id)
    }

    fun deleteUnitTemplate(id: OperationUnitTemplateId): ComposeOperationState {
        if (isReadOnly || !OperationUnitAuthoring.canDeleteTemplate(document.content, id)) return this
        document.content.unitTemplate(id) ?: return this
        val changes = OperationUnitAuthoring.deleteTemplateChange(document.content, id) ?: return this
        return withChangeSet(changes).copy(workspace = workspace.editors.filter { it.target == OperationEditorTarget.UnitTemplate(id) }
            .fold(workspace) { value, editor -> value.close(editor.id) })
    }

    fun saveGraphicTemplateEditor(id: OperationEditorId, image: java.awt.image.BufferedImage?): ComposeOperationState {
        if (isReadOnly) return this
        val editor = workspace.editor(id) ?: return this
        val draft = (editor.draft as? OperationEditorDraft.GraphicTemplate)?.value ?: return this
        val existing = (editor.target as? OperationEditorTarget.GraphicTemplate)?.templateId?.let(document.content::graphicTemplate)
        if (draft.replacementImage != null && image == null) return this
        val resolvedImage = image ?: existing?.image ?: return this
        if (!validationFor(editor).valid) return this
        val updated = existing?.let { OperationGraphicAuthoring.editTemplate(it, draft, image) }
            ?: OperationGraphicAuthoring.createTemplate(document.content, draft, resolvedImage)
        val changes = existing?.let { OperationGraphicAuthoring.editTemplateChange(document.content, it, updated) }
            ?: OperationGraphicAuthoring.createTemplateChange(updated)
        if (changes.isEmpty) return closeEditor(id)
        return withChangeSet(changes).closeEditor(id)
    }

    fun deleteGraphicTemplate(id: OperationGraphicTemplateId): ComposeOperationState {
        if (isReadOnly || !OperationGraphicAuthoring.canDeleteTemplate(document.content, id)) return this
        document.content.graphicTemplate(id) ?: return this
        val changes = OperationGraphicAuthoring.deleteTemplateChange(document.content, id) ?: return this
        return withChangeSet(changes).copy(workspace = workspace.editors.filter { it.target == OperationEditorTarget.GraphicTemplate(id) }
            .fold(workspace) { value, editor -> value.close(editor.id) })
    }

    fun saveGraphicEditor(id: OperationEditorId): ComposeOperationState {
        if (isReadOnly) return this
        val editor = workspace.editor(id) ?: return this
        val objectId = (editor.target as? OperationEditorTarget.Object)?.objectId ?: return this
        val before = document.content.objectById(objectId) as? OperationGraphic ?: return this
        val draft = (editor.draft as? OperationEditorDraft.Graphic)?.value ?: return this
        if (!validationFor(editor).valid) return this
        val after = OperationGraphicAuthoring.editGraphic(before, draft, document.content, document.renderData)
        val changes = OperationGraphicAuthoring.editGraphicChange(before, after)
        if (changes.isEmpty) return closeEditor(id)
        return withChangeSet(changes).closeEditor(id)
    }

    fun armUnitPlacement(id: OperationUnitTemplateId) =
        if (isReadOnly || document.content.unitTemplate(id) == null) this
        else copy(unitPlacement = ComposeUnitPlacement(id), graphicPlacement = null, noteReferencePick = null, previewContent = null)

    fun previewUnitPlacement(
        coordinate: io.hsar.mapgenerator.commandgui.MapCoordinate?,
        unitIconSizePx: Double,
    ): ComposeOperationState {
        val placement = unitPlacement ?: return this
        val template = document.content.unitTemplate(placement.templateId) ?: return cancelUnitPlacement()
        if (coordinate == null) return copy(unitPlacement = placement.copy(coordinate = null), previewContent = null)
        val viewport = viewport ?: return this
        val image = document.renderData.baseImage
        val unit = OperationUnitAuthoring.placeUnit(
            document.content, template, coordinate, unitIconSizePx, viewport.zoom,
            image.width, image.height, placement.objectId,
        )
        return copy(
            unitPlacement = placement.copy(coordinate = coordinate),
            previewContent = document.content.copy(objects = document.content.objects + unit),
        )
    }

    fun commitUnitPlacement(): ComposeOperationState {
        val placement = unitPlacement ?: return this
        val unit = previewContent?.objectById(placement.objectId) as? OperationUnit ?: return cancelUnitPlacement()
        return withChangeSet(OperationUnitAuthoring.placeUnitChange(unit))
            .copy(unitPlacement = null, previewContent = null, selectedObjectIds = setOf(unit.id))
    }

    fun cancelUnitPlacement() = copy(unitPlacement = null, previewContent = null)

    fun armGraphicPlacement(id: OperationGraphicTemplateId) =
        if (isReadOnly || document.content.graphicTemplate(id) == null) this
        else copy(graphicPlacement = ComposeGraphicPlacement(id), unitPlacement = null, noteReferencePick = null, previewContent = null)

    fun previewGraphicPlacement(coordinate: io.hsar.mapgenerator.commandgui.MapCoordinate?): ComposeOperationState {
        val placement = graphicPlacement ?: return this
        val template = document.content.graphicTemplate(placement.templateId) ?: return cancelGraphicPlacement()
        if (coordinate == null) return copy(graphicPlacement = placement.copy(coordinate = null), previewContent = null)
        val graphic = OperationGraphicAuthoring.placeGraphic(template, coordinate, placement.objectId)
        return copy(
            graphicPlacement = placement.copy(coordinate = coordinate),
            previewContent = document.content.copy(objects = document.content.objects + graphic),
        )
    }

    fun commitGraphicPlacement(): ComposeOperationState {
        val placement = graphicPlacement ?: return this
        val graphic = previewContent?.objectById(placement.objectId) as? OperationGraphic ?: return cancelGraphicPlacement()
        return withChangeSet(OperationGraphicAuthoring.placeGraphicChange(graphic))
            .copy(graphicPlacement = null, previewContent = null, selectedObjectIds = setOf(graphic.id))
    }

    fun cancelGraphicPlacement() = copy(graphicPlacement = null, previewContent = null)

    fun cancelPlacements() = copy(unitPlacement = null, graphicPlacement = null, noteReferencePick = null, previewContent = null)

    fun addDrawing(drawing: OperationDrawing): ComposeOperationState {
        if (isReadOnly) return this
        val drawingObject = OperationDrawingObject(OperationObjectId(UUID.randomUUID()), drawing)
        return preview(document.content.copy(objects = document.content.objects + drawingObject))
            .commitPreview()
            .copy(selectedObjectIds = setOf(drawingObject.id))
    }

    fun renameDrawing(id: OperationObjectId, displayName: String): ComposeOperationState {
        if (isReadOnly) return this
        document.content.objectById(id) as? OperationDrawingObject ?: return this
        val changes = OperationDrawingAuthoring.rename(document.content, id, displayName)
        if (changes.isEmpty) return this
        return withChangeSet(changes)
    }

    fun addTurn(name: String, thumbnail: java.awt.image.BufferedImage, assetId: UUID = UUID.randomUUID()): ComposeOperationState {
        if (isReadOnly) return this
        val resolvedName = name.takeIf(String::isNotBlank) ?: document.timeline.nextTurnName()
        val asset = OperationGeneratedImage(assetId, TURN_THUMBNAIL_ROLE, thumbnail)
        return withTimelineEntry(
            OperationTimelineEntries.turn(resolvedName, assetId),
            document.generatedImageAssets + asset,
        ).cancelTransientWorkflows()
    }

    fun revertToTurn(entryIndex: Int): ComposeOperationState {
        document.timeline.entries.getOrNull(entryIndex)?.takeIf { it.turnName != null } ?: return this
        val nextTimeline = document.timeline.withAppliedCount(entryIndex + 1)
        if (nextTimeline == document.timeline && historyPreviewCount == nextTimeline.appliedCount.takeIf { it < nextTimeline.entries.size }) return this
        return persistentCopy(document = document.copy(timeline = nextTimeline), previewContent = null)
            .copy(
                historyPreviewCount = nextTimeline.appliedCount.takeIf { it < nextTimeline.entries.size },
                selectedObjectIds = emptySet(),
            )
            .cancelTransientWorkflows()
    }

    fun openLedgerTarget(target: OperationLedgerTarget): ComposeOperationState {
        if (isReadOnly) return this
        return when (target) {
            is OperationLedgerTarget.Object -> when (document.content.objectById(target.id)) {
                is OperationUnit -> openUnitEditor(target.id)
                is OperationGraphic -> openGraphicEditor(target.id)
                else -> this
            }
            is OperationLedgerTarget.UnitTemplate -> openUnitTemplateEditor(target.id)
            is OperationLedgerTarget.GraphicTemplate -> openGraphicTemplateEditor(target.id)
            is OperationLedgerTarget.Note -> openNoteEditor(target.id)
            OperationLedgerTarget.Notes -> openNotes()
        }
    }

    fun isLedgerTargetAvailable(target: OperationLedgerTarget): Boolean = !isReadOnly && when (target) {
        is OperationLedgerTarget.Object -> document.content.objectById(target.id) != null
        is OperationLedgerTarget.UnitTemplate -> document.content.unitTemplate(target.id) != null
        is OperationLedgerTarget.GraphicTemplate -> document.content.graphicTemplate(target.id) != null
        is OperationLedgerTarget.Note -> document.content.notes.any { it.id == target.id }
        OperationLedgerTarget.Notes -> true
    }

    fun undo(): ComposeOperationState {
        if (!document.timeline.canUndo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.undo()),
            previewContent = null,
        ).cancelTransientWorkflows().pruneSelection().pruneEditors()
    }

    fun redo(): ComposeOperationState {
        if (!document.timeline.canRedo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.redo()),
            previewContent = null,
        ).cancelTransientWorkflows().pruneSelection().pruneEditors()
    }

    fun previewHistory(entryIndex: Int?) = copy(
        historyPreviewCount = entryIndex?.plus(1)?.coerceIn(0, document.timeline.appliedCount),
        previewContent = null,
        selectedObjectIds = emptySet(),
        unitPlacement = null,
        graphicPlacement = null,
        noteReferencePick = null,
    )

    fun returnToCurrent(): ComposeOperationState {
        val timeline = document.timeline
        val restored = if (timeline.appliedCount < timeline.entries.size) {
            persistentCopy(document = document.copy(timeline = timeline.withAppliedCount(timeline.entries.size)))
        } else {
            this
        }
        return restored.copy(historyPreviewCount = null).cancelTransientWorkflows().pruneSelection().pruneEditors()
    }

    fun withPersistedDocument(savedDocument: OperationDocument, targetRevision: Long) = copy(
        document = if (revision == targetRevision) savedDocument else document.copy(
            updatedAt = savedDocument.updatedAt,
            sourcePath = savedDocument.sourcePath,
            persisted = true,
        ),
    )

    fun savedCamera(): MapViewerSavedCamera? = viewport?.let { current ->
        val image = document.renderData.baseImage
        val worldWidth = image.width * 1.66
        val worldHeight = image.height * 1.66
        val mapOffsetX = (worldWidth - image.width) * 0.5
        val mapOffsetY = (worldHeight - image.height) * 0.5
        MapViewerSavedCamera(
            x = (current.center.x - mapOffsetX) / image.width,
            y = (current.center.y - mapOffsetY) / image.height,
            zoom = current.zoom,
        )
    }

    fun operationUiState() = OperationUiState(
        selectedTool = activeTool,
        selectedObjectIds = selectedObjectIds,
        enabledLayerIds = enabledLayerIds,
        workspace = workspace,
    )

    private fun withChangeSet(changeSet: io.hsar.mapgenerator.commandgui.operation.OperationChangeSet): ComposeOperationState =
        OperationTimelineEntries.change(document.content, changeSet)?.let(::withTimelineEntry) ?: this

    private fun withTimelineEntry(
        entry: OperationTimelineEntry,
        generatedImageAssets: List<OperationGeneratedImage> = document.generatedImageAssets,
    ): ComposeOperationState {
        val timeline = document.timeline.record(entry)
        val retainedThumbnailIds = timeline.entries.mapNotNullTo(hashSetOf(), OperationTimelineEntry::turnThumbnailAssetId)
        val retainedAssets = generatedImageAssets.filter { it.role != TURN_THUMBNAIL_ROLE || it.id in retainedThumbnailIds }
        return persistentCopy(document = document.copy(timeline = timeline, generatedImageAssets = retainedAssets))
            .pruneSelection()
            .pruneEditors()
    }

    private fun cancelTransientWorkflows() = copy(
        previewContent = null,
        unitPlacement = null,
        graphicPlacement = null,
        noteReferencePick = null,
    )

    private fun updateWorkspace(value: OperationWorkspaceState) =
        if (value == workspace) this else persistentCopy(workspace = value)

    private fun persistentCopy(
        document: OperationDocument = this.document,
        viewport: MapViewport? = this.viewport,
        activeTool: MapViewerTool = this.activeTool,
        selectedObjectIds: Set<OperationObjectId> = this.selectedObjectIds,
        workspace: OperationWorkspaceState = this.workspace,
        enabledLayerIds: Set<String> = this.enabledLayerIds,
        previewContent: OperationContent? = this.previewContent,
        unitPlacement: ComposeUnitPlacement? = this.unitPlacement,
        graphicPlacement: ComposeGraphicPlacement? = this.graphicPlacement,
    ) = copy(
        document = document,
        viewport = viewport,
        activeTool = activeTool,
        selectedObjectIds = selectedObjectIds,
        workspace = workspace,
        enabledLayerIds = enabledLayerIds,
        previewContent = previewContent,
        unitPlacement = unitPlacement,
        graphicPlacement = graphicPlacement,
        revision = revision + 1,
    )

    private fun markUiDirtyIfChanged(previousSelection: Set<OperationObjectId>) =
        if (selectedObjectIds == previousSelection) this else copy(revision = revision + 1)

    private fun pruneSelection() = copy(
        selectedObjectIds = selectedObjectIds.intersect(visibleContent.objects.mapTo(hashSetOf()) { it.id }),
    )

    private fun pruneEditors(): ComposeOperationState {
        val liveIds = document.content.objects.mapTo(hashSetOf()) { it.id }
        val invalid = workspace.editors.filter { editor ->
            (editor.target as? OperationEditorTarget.Object)?.objectId?.let { it !in liveIds } == true ||
                (editor.target as? OperationEditorTarget.UnitTemplate)?.templateId?.let { document.content.unitTemplate(it) == null } == true
                || (editor.target as? OperationEditorTarget.GraphicTemplate)?.templateId?.let { document.content.graphicTemplate(it) == null } == true
                || (editor.target as? OperationEditorTarget.Note)?.noteId?.let { noteId -> document.content.notes.none { it.id == noteId } } == true
        }.map(OperationEditorState::id)
        return if (invalid.isEmpty()) this else copy(workspace = invalid.fold(workspace, OperationWorkspaceState::close))
    }
}

internal data class ComposeEditorValidation(val errors: Map<String, String>) {
    val valid get() = errors.isEmpty()
}

internal fun ComposeOperationState.validationFor(editor: OperationEditorState): ComposeEditorValidation = when (val draft = editor.draft) {
    is OperationEditorDraft.Unit -> ComposeEditorValidation(
        OperationUnitAuthoring.validateUnitDraft(
            draft.name, draft.positionX, draft.positionY, draft.facing, draft.properties, document.renderData,
        ).errors,
    )
    is OperationEditorDraft.UnitTemplate -> ComposeEditorValidation(
        OperationUnitAuthoring.validateTemplateDraft(
            draft.value,
            (editor.target as? OperationEditorTarget.UnitTemplate)?.templateId?.let(document.content::unitTemplate) != null,
        ).errors,
    )
    is OperationEditorDraft.Graphic -> ComposeEditorValidation(
        OperationGraphicAuthoring.validateGraphicDraft(draft.value, document.content, document.renderData).errors,
    )
    is OperationEditorDraft.GraphicTemplate -> ComposeEditorValidation(
        OperationGraphicAuthoring.validateTemplateDraft(
            draft.value,
            (editor.target as? OperationEditorTarget.GraphicTemplate)?.templateId?.let(document.content::graphicTemplate) != null,
        ).errors,
    )
    else -> ComposeEditorValidation(emptyMap())
}

private const val TURN_THUMBNAIL_ROLE = "turn-thumbnail"

private fun OperationWorkspaceState.hydrate(content: OperationContent, renderData: io.hsar.mapgenerator.commandgui.MapViewerRenderData): OperationWorkspaceState {
    val hydrated = editors.filter { editor ->
        when (val target = editor.target) {
            is OperationEditorTarget.Object -> content.objectById(target.objectId) != null
            is OperationEditorTarget.UnitTemplate -> content.unitTemplate(target.templateId) != null
            is OperationEditorTarget.GraphicTemplate -> content.graphicTemplate(target.templateId) != null
            is OperationEditorTarget.Note -> content.notes.any { it.id == target.noteId }
            else -> true
        }
    }.map { editor ->
        val target = editor.target as? OperationEditorTarget.Object
        val mapObject = target?.objectId?.let(content::objectById)
        val unit = mapObject as? OperationUnit
        val graphic = mapObject as? OperationGraphic
        val draft = editor.draft as? OperationEditorDraft.Unit
        if (unit != null && draft != null && draft.name.isEmpty() && draft.positionX.isEmpty() && draft.positionY.isEmpty()) {
            editor.copy(draft = unit.editorState(editor.floating.bounds, renderData).draft)
        } else if (graphic != null && editor.draft is OperationEditorDraft.Graphic) {
            val draft = (editor.draft as OperationEditorDraft.Graphic).value
            if (draft.positionX.isEmpty() && draft.positionY.isEmpty()) editor.copy(draft = graphic.editorState(renderData, editor.floating.bounds).draft) else editor
        } else editor
    }
    val ids = hydrated.mapTo(hashSetOf()) { it.id }
    return copy(
        editors = hydrated,
        activeEditorId = activeEditorId?.takeIf(ids::contains) ?: hydrated.lastOrNull()?.id,
        dockRegions = dockRegions.map { dock ->
            val dockIds = dock.editorIds.filter(ids::contains)
            dock.copy(editorIds = dockIds, activeEditorId = dock.activeEditorId?.takeIf(ids::contains) ?: dockIds.lastOrNull())
        },
    )
}

private fun <T> Set<T>.withSelection(id: T?, additive: Boolean): Set<T> = when {
    id == null && !additive -> emptySet()
    id == null -> this
    !additive -> setOf(id)
    id in this -> this - id
    else -> this + id
}

private fun <T> Set<T>.withBoxSelection(ids: Set<T>, additive: Boolean): Set<T> =
    if (additive) ids.fold(this) { selection, id -> if (id in selection) selection - id else selection + id } else ids
