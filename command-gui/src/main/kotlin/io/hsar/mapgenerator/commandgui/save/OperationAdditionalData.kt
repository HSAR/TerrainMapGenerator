package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import java.util.Collections.emptySet

internal data class UiSessionState(
    val selectedTool: MapViewerTool = MapViewerTool.SELECT,
    val selectedObjectIds: List<OperationObjectId> = emptyList(),
    val dialogs: List<MapViewerPersistedDialog> = emptyList(),
    val workspaceMode: MapViewerWorkspaceMode = MapViewerWorkspaceMode.FLOATING,
    val dockPanels: List<DockPanelSessionState> = emptyList(),
    val enabledLayerIds: Set<String> = emptySet(),
    val openEditorIds: List<String> = emptyList(),
    val drafts: Map<String, String> = emptyMap(),
)

internal data class DockPanelSessionState(
    val region: String,
    val size: Double,
    val tabIds: List<String> = emptyList(),
    val activeTabId: String? = null,
    val collapsed: Boolean = false,
)

/** Owns the complete additive additionalData.json document. */
internal class OperationAdditionalData private constructor(private val root: ObjectNode) {
    val sitrepContacts: SitrepContactPersistence = SitrepContactPersistence.fromRoot(root)
    val tacticalFire: TacticalFirePersistence = TacticalFirePersistence.fromRoot(root)
    val ecm: EcmPersistence = EcmPersistence.fromRoot(root)

    fun uiState(): UiSessionState {
        val state = root.path(UI_STATE_FIELD).takeIf { it.isObject } ?: return UiSessionState()
        val tool = state.path("selectedTool").textValue()
            ?.let { value -> MapViewerTool.entries.firstOrNull { it.name == value } }
            ?: MapViewerTool.SELECT
        val selected = state.path("selectedObjectIds").mapNotNull { node ->
            node.textValue()?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() }
        }
        val dialogs = state.path("dialogs").mapNotNull(MapViewerPersistedDialog::fromJson)
        val workspaceMode = state.path("workspaceMode").textValue()
            ?.let { value -> MapViewerWorkspaceMode.entries.firstOrNull { it.name == value } }
            ?: MapViewerWorkspaceMode.FLOATING
        val dockPanels = state.path("dockPanels").mapNotNull { panel ->
            val region = panel.path("region").textValue() ?: return@mapNotNull null
            val size = panel.path("size").doubleValue().takeIf { panel.path("size").isNumber && it >= 0.0 }
                ?: return@mapNotNull null
            DockPanelSessionState(
                region = region,
                size = size,
                tabIds = panel.path("tabIds").mapNotNull(JsonNode::textValue),
                activeTabId = panel.path("activeTabId").textValue(),
                collapsed = panel.path("collapsed").booleanValue(),
            )
        }
        val enabledLayerIds = state.path("enabledLayerIds").mapNotNull(JsonNode::textValue).toSet()
        val openEditorIds = state.path("openEditorIds").mapNotNull(JsonNode::textValue)
        val drafts = state.path("drafts").takeIf(JsonNode::isObject)?.properties()?.asSequence()
            ?.mapNotNull { (id, value) -> value.textValue()?.let { id to it } }
            ?.toMap().orEmpty()
        return UiSessionState(tool, selected, dialogs, workspaceMode, dockPanels, enabledLayerIds, openEditorIds, drafts)
    }

    fun updateUiState(state: UiSessionState) {
        val value = root.putObject(UI_STATE_FIELD)
        value.put("schemaVersion", CURRENT_UI_SCHEMA_VERSION)
        value.put("selectedTool", state.selectedTool.name)
        value.putArray("selectedObjectIds").also { ids -> state.selectedObjectIds.forEach { ids.add(it.value.toString()) } }
        value.putArray("dialogs").also { dialogs -> state.dialogs.forEach { dialogs.add(it.toJson()) } }
        value.put("workspaceMode", state.workspaceMode.name)
        value.putArray("dockPanels").also { panels ->
            state.dockPanels.forEach { panel ->
                panels.addObject().apply {
                    put("region", panel.region)
                    put("size", panel.size)
                    putArray("tabIds").also { tabs -> panel.tabIds.forEach(tabs::add) }
                    panel.activeTabId?.let { put("activeTabId", it) }
                    put("collapsed", panel.collapsed)
                }
            }
        }
        value.putArray("enabledLayerIds").also { layers -> state.enabledLayerIds.forEach(layers::add) }
        value.putArray("openEditorIds").also { editors -> state.openEditorIds.forEach(editors::add) }
        value.putObject("drafts").also { drafts -> state.drafts.forEach(drafts::put) }
    }

    fun updateOperationUiState(state: OperationUiState) = updateUiState(
        uiState().copy(
            selectedTool = state.selectedTool,
            selectedObjectIds = state.selectedObjectIds.toList(),
            workspaceMode = state.workspaceMode,
            enabledLayerIds = state.enabledLayerIds,
            openEditorIds = state.openEditorIds,
            drafts = state.drafts,
        ),
    )

    fun operationUiState() = uiState().let { state ->
        OperationUiState(
            selectedTool = state.selectedTool,
            selectedObjectIds = state.selectedObjectIds.toCollection(linkedSetOf()),
            workspaceMode = state.workspaceMode,
            enabledLayerIds = state.enabledLayerIds,
            openEditorIds = state.openEditorIds,
            drafts = state.drafts,
        )
    }

    fun toJsonOrNull(): ByteArray? =
        sitrepContacts.toJson()
            .let { tacticalFire.toJson() }
            .let { ecm.toJson() }
            .takeIf { root.size() > 0 }

    companion object {
        const val UI_STATE_FIELD = "uiState"
        const val CURRENT_UI_SCHEMA_VERSION = 2

        fun fromJson(json: ByteArray?): OperationAdditionalData = OperationAdditionalData(
            runCatching {
                json?.takeIf { it.isNotEmpty() }
                    ?.let(OperationSaveJson.objectMapper::readTree)
                    ?.takeIf(JsonNode::isObject)
                    ?.deepCopy<ObjectNode>()
            }.getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )
    }
}
