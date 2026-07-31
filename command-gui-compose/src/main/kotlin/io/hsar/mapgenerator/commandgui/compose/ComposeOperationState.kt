package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.changesFrom
import io.hsar.mapgenerator.commandgui.operation.delete
import java.util.Collections.emptyMap
import java.util.Collections.emptySet

enum class ComposeSaveStatus { CLEAN, DIRTY, SAVING, FAILED }

data class ComposeEditorInstance(
    val id: String,
    val type: String,
    val objectId: OperationObjectId? = null,
)

data class ComposeOperationState(
    val document: OperationDocument,
    val viewport: MapViewport? = null,
    val activeTool: MapViewerTool = document.uiState.selectedTool,
    val selectedObjectIds: Set<OperationObjectId> = document.uiState.selectedObjectIds,
    val workspaceMode: MapViewerWorkspaceMode = document.uiState.workspaceMode,
    val enabledLayerIds: Set<String> = document.uiState.enabledLayerIds.ifEmpty { document.settings.enabledLayerIds },
    val openEditors: List<ComposeEditorInstance> = document.uiState.openEditorIds.map { ComposeEditorInstance(it, "restored") },
    val drafts: Map<String, String> = document.uiState.drafts,
    val validationErrors: Map<String, String> = emptyMap(),
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

    fun selectTool(tool: MapViewerTool) =
        if (tool == activeTool) this else persistentCopy(activeTool = tool)

    fun selectObject(id: OperationObjectId?, additive: Boolean = false) =
        copy(selectedObjectIds = selectedObjectIds.withSelection(id, additive)).markUiDirtyIfChanged(selectedObjectIds)

    fun selectBox(ids: Set<OperationObjectId>, additive: Boolean) =
        copy(selectedObjectIds = selectedObjectIds.withBoxSelection(ids, additive)).markUiDirtyIfChanged(selectedObjectIds)

    fun updateViewport(value: MapViewport) =
        if (value == viewport) this else persistentCopy(viewport = value)

    fun updateWorkspaceMode(value: MapViewerWorkspaceMode) =
        if (value == workspaceMode) this else persistentCopy(workspaceMode = value)

    fun updateEnabledLayers(value: Set<String>) =
        if (value == enabledLayerIds) this else persistentCopy(
            document = document.copy(settings = document.settings.copy(enabledLayerIds = value)),
            enabledLayerIds = value,
        )

    fun openEditor(value: ComposeEditorInstance) =
        if (openEditors.any { it.id == value.id }) this else persistentCopy(openEditors = openEditors + value)

    fun closeEditor(id: String) =
        if (openEditors.none { it.id == id }) this else persistentCopy(openEditors = openEditors.filterNot { it.id == id })

    fun updateDraft(id: String, value: String) =
        if (drafts[id] == value) this else persistentCopy(drafts = drafts + (id to value))

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

    fun undo(): ComposeOperationState {
        if (!document.timeline.canUndo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.undo()),
            previewContent = null,
        ).pruneSelection()
    }

    fun redo(): ComposeOperationState {
        if (!document.timeline.canRedo || isReadOnly) return this
        return persistentCopy(
            document = document.copy(timeline = document.timeline.redo()),
            previewContent = null,
        ).pruneSelection()
    }

    fun previewHistory(entryIndex: Int?) = copy(
        historyPreviewCount = entryIndex?.plus(1)?.coerceIn(0, document.timeline.appliedCount),
        previewContent = null,
        selectedObjectIds = emptySet(),
    )

    fun returnToCurrent() = copy(historyPreviewCount = null, previewContent = null).pruneSelection()

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
        workspaceMode = workspaceMode,
        enabledLayerIds = enabledLayerIds,
        openEditorIds = openEditors.map(ComposeEditorInstance::id),
        drafts = drafts,
    )

    private fun withTimelineEntry(entry: OperationTimelineEntry) = persistentCopy(
        document = document.copy(timeline = document.timeline.record(entry)),
    ).pruneSelection()

    private fun persistentCopy(
        document: OperationDocument = this.document,
        viewport: MapViewport? = this.viewport,
        activeTool: MapViewerTool = this.activeTool,
        selectedObjectIds: Set<OperationObjectId> = this.selectedObjectIds,
        workspaceMode: MapViewerWorkspaceMode = this.workspaceMode,
        enabledLayerIds: Set<String> = this.enabledLayerIds,
        openEditors: List<ComposeEditorInstance> = this.openEditors,
        drafts: Map<String, String> = this.drafts,
        previewContent: OperationContent? = this.previewContent,
    ) = copy(
        document = document,
        viewport = viewport,
        activeTool = activeTool,
        selectedObjectIds = selectedObjectIds,
        workspaceMode = workspaceMode,
        enabledLayerIds = enabledLayerIds,
        openEditors = openEditors,
        drafts = drafts,
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
