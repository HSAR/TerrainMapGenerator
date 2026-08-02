package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import io.hsar.mapgenerator.commandgui.operation.changesFrom
import io.hsar.mapgenerator.commandgui.operation.delete
import io.hsar.mapgenerator.commandgui.operation.editorState
import java.util.*
import java.util.Collections.emptyMap
import java.util.Collections.emptySet

enum class ComposeSaveStatus { CLEAN, DIRTY, SAVING, FAILED }

data class ComposeOperationState(
    val document: OperationDocument,
    val viewport: MapViewport? = null,
    val activeTool: MapViewerTool = document.uiState.selectedTool,
    val selectedObjectIds: Set<OperationObjectId> = document.uiState.selectedObjectIds,
    val workspace: OperationWorkspaceState = document.uiState.workspace.hydrate(document.content),
    val enabledLayerIds: Set<String> = document.uiState.enabledLayerIds.ifEmpty { document.settings.enabledLayerIds },
    val validationErrors: Map<OperationEditorId, String> = emptyMap(),
    val historyPreviewCount: Int? = null,
    val previewContent: OperationContent? = null,
    val revision: Long = if (document.persisted) 0L else 1L,
    val savingRevision: Long? = null,
    val savedRevision: Long = 0L,
    val saveStatus: ComposeSaveStatus = if (document.persisted) ComposeSaveStatus.CLEAN else ComposeSaveStatus.DIRTY,
    val saveFailure: String? = null,
) {
    val isReadOnly get() = historyPreviewCount != null
    val visibleContent
        get() = previewContent
            ?: historyPreviewCount?.let(document.timeline::contentAt)
            ?: document.content
    val hasUnsavedChanges get() = revision > savedRevision
    val workspaceMode get() = workspace.mode
    val openEditors get() = workspace.editors

    fun selectTool(tool: MapViewerTool) =
        if (tool == activeTool) this else persistentCopy(activeTool = tool)

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

    fun openNotes() = updateWorkspace(workspace.open(OperationWorkspaceState.default().editors.single()))

    fun openUnitEditor(id: OperationObjectId): ComposeOperationState {
        val unit = document.content.objectById(id) as? OperationUnit ?: return this
        val cascade = workspace.editors.count { it.kind == OperationEditorKind.UNIT } * 32.0
        return updateWorkspace(workspace.open(unit.editorState(OperationEditorBounds(96.0 + cascade, 96.0 + cascade, 520.0, 560.0))))
    }

    fun activateEditor(id: OperationEditorId) = updateWorkspace(workspace.activate(id))

    fun closeEditor(id: OperationEditorId) = updateWorkspace(workspace.close(id))

    fun updateEditorDraft(id: OperationEditorId, draft: OperationEditorDraft) = updateWorkspace(
        workspace.updateEditor(id) { it.copy(draft = draft) },
    )

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
        workspace.updateDock(region) { it.copy(size = size.coerceIn(240.0, 720.0)) },
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

    fun preview(content: OperationContent) =
        if (isReadOnly) this else copy(previewContent = content)

    fun cancelPreview() = copy(previewContent = null)

    fun commitPreview(
        kind: MapViewerLedgerEntryKind = MapViewerLedgerEntryKind.OBJECT,
        description: String,
    ): ComposeOperationState {
        val after = previewContent ?: return this
        val changes = after.changesFrom(document.content)
        if (changes.isEmpty || isReadOnly) return cancelPreview()
        return withTimelineEntry(OperationTimelineEntry(changes, kind, description)).copy(previewContent = null)
    }

    fun deleteSelection(): ComposeOperationState {
        if (isReadOnly || selectedObjectIds.isEmpty()) return this
        val after = document.content.delete(selectedObjectIds)
        return preview(after).commitPreview(
            description = if (selectedObjectIds.size == 1) "deleted an item" else "deleted ${selectedObjectIds.size} items",
        ).copy(selectedObjectIds = emptySet())
    }

    fun editUnit(updated: OperationUnit): ComposeOperationState {
        if (isReadOnly) return this
        val before = document.content.objectById(updated.id) as? OperationUnit ?: return this
        if (before == updated) return this
        val after = document.content.copy(objects = document.content.objects.map { value -> if (value.id == updated.id) updated else value })
        return preview(after).commitPreview(description = "edited ${updated.instanceName.ifBlank { "unit" }}")
    }

    fun addDrawing(drawing: OperationDrawing): ComposeOperationState {
        if (isReadOnly) return this
        val drawingObject = OperationDrawingObject(OperationObjectId(UUID.randomUUID()), drawing)
        return preview(document.content.copy(objects = document.content.objects + drawingObject))
            .commitPreview(description = "created ${drawing.displayName.lowercase()}")
            .copy(selectedObjectIds = setOf(drawingObject.id))
    }

    fun undo(): ComposeOperationState {
        if (!document.timeline.canUndo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.undo()),
            previewContent = null,
        ).pruneSelection().pruneEditors()
    }

    fun redo(): ComposeOperationState {
        if (!document.timeline.canRedo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.redo()),
            previewContent = null,
        ).pruneSelection().pruneEditors()
    }

    fun previewHistory(entryIndex: Int?) = copy(
        historyPreviewCount = entryIndex?.plus(1)?.coerceIn(0, document.timeline.appliedCount),
        previewContent = null,
        selectedObjectIds = emptySet(),
    )

    fun returnToCurrent() = copy(historyPreviewCount = null, previewContent = null).pruneSelection().pruneEditors()

    fun saveStarted(targetRevision: Long = revision) = copy(
        savingRevision = targetRevision,
        saveStatus = ComposeSaveStatus.SAVING,
        saveFailure = null,
    )

    fun saveSucceeded(savedDocument: OperationDocument, targetRevision: Long) = copy(
        document = if (revision == targetRevision) savedDocument else document.copy(
            updatedAt = savedDocument.updatedAt,
            sourcePath = savedDocument.sourcePath,
            additionalDataJson = savedDocument.additionalDataJson,
        ),
        savingRevision = null,
        savedRevision = maxOf(savedRevision, targetRevision),
        saveStatus = if (revision > targetRevision) ComposeSaveStatus.DIRTY else ComposeSaveStatus.CLEAN,
        saveFailure = null,
    )

    fun saveFailed(message: String) = copy(
        savingRevision = null,
        saveStatus = ComposeSaveStatus.FAILED,
        saveFailure = message,
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

    private fun withTimelineEntry(entry: OperationTimelineEntry) = persistentCopy(
        document = document.copy(timeline = document.timeline.record(entry)),
    ).pruneSelection().pruneEditors()

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
    ) = copy(
        document = document,
        viewport = viewport,
        activeTool = activeTool,
        selectedObjectIds = selectedObjectIds,
        workspace = workspace,
        enabledLayerIds = enabledLayerIds,
        previewContent = previewContent,
        revision = revision + 1,
        saveStatus = if (saveStatus == ComposeSaveStatus.SAVING) saveStatus else ComposeSaveStatus.DIRTY,
        saveFailure = null,
    )

    private fun markUiDirtyIfChanged(previousSelection: Set<OperationObjectId>) =
        if (selectedObjectIds == previousSelection) this else copy(revision = revision + 1, saveStatus = ComposeSaveStatus.DIRTY, saveFailure = null)

    private fun pruneSelection() = copy(
        selectedObjectIds = selectedObjectIds.intersect(visibleContent.objects.mapTo(hashSetOf()) { it.id }),
    )

    private fun pruneEditors(): ComposeOperationState {
        val liveIds = document.content.objects.mapTo(hashSetOf()) { it.id }
        val invalid = workspace.editors.filter { editor ->
            (editor.target as? OperationEditorTarget.Object)?.objectId?.let { it !in liveIds } == true
        }.map(OperationEditorState::id)
        return if (invalid.isEmpty()) this else copy(workspace = invalid.fold(workspace, OperationWorkspaceState::close))
    }
}

private fun OperationWorkspaceState.hydrate(content: OperationContent): OperationWorkspaceState {
    val hydrated = editors.filter { editor ->
        (editor.target as? OperationEditorTarget.Object)?.objectId?.let(content::objectById) != null || editor.target !is OperationEditorTarget.Object
    }.map { editor ->
        val target = editor.target as? OperationEditorTarget.Object
        val unit = target?.objectId?.let(content::objectById) as? OperationUnit
        val draft = editor.draft as? OperationEditorDraft.Unit
        if (unit != null && draft != null && draft.name.isEmpty() && draft.positionX.isEmpty() && draft.positionY.isEmpty()) {
            editor.copy(draft = unit.editorState(editor.floating.bounds).draft)
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
