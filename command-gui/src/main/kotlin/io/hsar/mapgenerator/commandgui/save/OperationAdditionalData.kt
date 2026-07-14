package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog
import java.util.*

internal data class UiSessionState(
    val selectedTool: MapViewerTool = MapViewerTool.SELECT,
    val selectedObjectIds: List<MapPlacedObjectId> = emptyList(),
    val dialogs: List<MapViewerPersistedDialog> = emptyList(),
)

/** Owns the complete additive additionalData.json document. */
internal class OperationAdditionalData private constructor(private val root: ObjectNode) {
    val sitrepContacts: SitrepContactPersistence = SitrepContactPersistence.fromRoot(root)
    val tacticalFire: TacticalFirePersistence = TacticalFirePersistence.fromRoot(root)

    fun uiState(): UiSessionState {
        val state = root.path(UI_STATE_FIELD).takeIf { it.isObject } ?: return UiSessionState()
        val tool = state.path("selectedTool").textValue()
            ?.let { value -> MapViewerTool.entries.firstOrNull { it.name == value } }
            ?: MapViewerTool.SELECT
        val selected = state.path("selectedObjectIds").mapNotNull { node ->
            node.textValue()?.let { runCatching { MapPlacedObjectId(UUID.fromString(it)) }.getOrNull() }
        }
        val dialogs = state.path("dialogs").mapNotNull(MapViewerPersistedDialog::fromJson)
        return UiSessionState(tool, selected, dialogs)
    }

    fun updateUiState(state: UiSessionState) {
        val value = root.putObject(UI_STATE_FIELD)
        value.put("selectedTool", state.selectedTool.name)
        value.putArray("selectedObjectIds").also { ids -> state.selectedObjectIds.forEach { ids.add(it.value.toString()) } }
        value.putArray("dialogs").also { dialogs -> state.dialogs.forEach { dialogs.add(it.toJson()) } }
    }

    fun toJsonOrNull(): ByteArray? =
        sitrepContacts.toJson()
            .also { tacticalFire.toJson() }
            .takeIf { root.size() > 0 }

    companion object {
        const val UI_STATE_FIELD = "uiState"

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
