package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.ViewerPoint

internal data class MapViewerDialogBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    val right get() = left + width
    val bottom get() = top + height
    fun movedTo(left: Double, top: Double) = copy(left = left, top = top)
    fun resizedTo(width: Double, height: Double) = copy(width = width, height = height)
}

internal data class MapViewerPersistedDialog(
    val type: String,
    val targetId: String? = null,
    val bounds: MapViewerDialogBounds,
    val draft: JsonNode? = null,
    val minimized: Boolean = false,
    val pinned: Boolean = false,
    val pinAnchor: ViewerPoint? = null,
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
        put("minimized", minimized)
        put("pinned", pinned)
        pinAnchor?.let { anchor ->
            putObject("pinAnchor").apply {
                put("x", anchor.x)
                put("y", anchor.y)
            }
        }
    }

    companion object {
        fun fromJson(node: JsonNode): MapViewerPersistedDialog? {
            val bounds = node.path("bounds").takeIf { it.isObject } ?: return null
            val values = listOf("left", "top", "width", "height").map(bounds::path)
            if (node.path("type").textValue() == null || values.any { !it.isNumber }) return null
            val pinAnchor = node.path("pinAnchor")
                .takeIf { it.isObject && it.path("x").isNumber && it.path("y").isNumber }
                ?.let { ViewerPoint(it.path("x").doubleValue(), it.path("y").doubleValue()) }
            return MapViewerPersistedDialog(
                type = node.path("type").textValue(),
                targetId = node.path("targetId").textValue(),
                bounds = MapViewerDialogBounds(values[0].doubleValue(), values[1].doubleValue(), values[2].doubleValue(), values[3].doubleValue()),
                draft = node.get("draft")?.deepCopy(),
                minimized = node.path("minimized").booleanValue(),
                pinned = node.path("pinned").booleanValue() && pinAnchor != null,
                pinAnchor = pinAnchor,
            )
        }
    }
}
