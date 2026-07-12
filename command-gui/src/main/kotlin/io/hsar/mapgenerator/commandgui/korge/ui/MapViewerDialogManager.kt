package io.hsar.mapgenerator.commandgui.korge.ui

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import korlibs.math.geom.Vector2D

internal data class MapViewerPersistedDialog(
    val type: String,
    val targetId: String? = null,
    val bounds: MapViewerDialogBounds,
    val draft: JsonNode? = null,
) {
    fun toJson(): ObjectNode = OperationSaveJson.objectMapper.createObjectNode().apply {
        put("type", type)
        targetId?.let { put("targetId", it) }
        putObject("bounds").apply {
            put("left", bounds.left)
            put("top", bounds.top)
            put("width", bounds.width)
            put("height", bounds.height)
        }
        draft?.let { set<JsonNode>("draft", it) }
    }

    companion object {
        fun fromJson(node: JsonNode): MapViewerPersistedDialog? {
            val bounds = node.path("bounds").takeIf { it.isObject } ?: return null
            val values = listOf("left", "top", "width", "height").map { bounds.path(it) }
            if (node.path("type").textValue() == null || values.any { !it.isNumber }) return null
            return MapViewerPersistedDialog(
                type = node.path("type").textValue(),
                targetId = node.path("targetId").textValue(),
                bounds = MapViewerDialogBounds(values[0].doubleValue(), values[1].doubleValue(), values[2].doubleValue(), values[3].doubleValue()),
                draft = node.get("draft")?.deepCopy(),
            )
        }
    }
}

internal class MapViewerDialogManager {
    private val dialogs = linkedMapOf<Any, DialogEntry>()

    fun register(
        owner: Any,
        targetKey: Any = owner,
        blocksScreenPoint: (Vector2D) -> Boolean,
        isTextInputActive: () -> Boolean = { false },
        sync: (Any) -> Unit = {},
        persistedDialog: (() -> MapViewerPersistedDialog?)? = null,
        close: () -> Unit,
    ) {
        dialogs[owner] = DialogEntry(
            targetKey = targetKey,
            blocksScreenPoint = blocksScreenPoint,
            isTextInputActive = isTextInputActive,
            sync = sync,
            persistedDialog = persistedDialog,
            close = close,
        )
    }

    fun unregister(owner: Any) {
        dialogs.remove(owner)
    }

    fun closeTarget(targetKey: Any) {
        dialogs
            .filterValues { it.targetKey == targetKey }
            .keys
            .toList()
            .forEach { owner -> dialogs[owner]?.close?.invoke() }
    }

    fun closeAll() {
        dialogs.keys.toList().forEach { owner -> dialogs[owner]?.close?.invoke() }
    }

    fun updateTarget(targetKey: Any, payload: Any) {
        dialogs
            .filterValues { it.targetKey == targetKey }
            .values
            .forEach { it.sync(payload) }
    }

    fun broadcast(payload: Any) {
        dialogs.values.forEach { it.sync(payload) }
    }

    fun hasTarget(targetKey: Any): Boolean =
        dialogs.values.any { it.targetKey == targetKey }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        dialogs.values.any { it.blocksScreenPoint(point) }

    fun isTextInputActive(): Boolean =
        dialogs.values.any { it.isTextInputActive() }

    fun hasOpenDialogs(): Boolean =
        dialogs.isNotEmpty()

    fun persistedDialogs(): List<MapViewerPersistedDialog> =
        dialogs.values.mapNotNull { it.persistedDialog?.invoke() }

    private data class DialogEntry(
        val targetKey: Any,
        val blocksScreenPoint: (Vector2D) -> Boolean,
        val isTextInputActive: () -> Boolean,
        val sync: (Any) -> Unit,
        val persistedDialog: (() -> MapViewerPersistedDialog?)?,
        val close: () -> Unit,
    )
}

internal data class MapViewerDialogPositionSync(
    val positionX: String,
    val positionY: String,
)
