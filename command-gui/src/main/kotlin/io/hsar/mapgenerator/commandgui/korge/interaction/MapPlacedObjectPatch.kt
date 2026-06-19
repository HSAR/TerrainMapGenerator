package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import java.awt.Color

internal object MapPlacedObjectPatch {
    private val objectMapper = jacksonObjectMapper()

    fun diff(before: MapPlacedObjectSnapshot, after: MapPlacedObjectSnapshot): Map<String, JsonNode?> {
        require(before.id == after.id) { "Cannot diff different map objects" }
        return when {
            before is UnitSnapshot && after is UnitSnapshot -> unitDiff(before, after)
            before is DrawingSnapshot && after is DrawingSnapshot -> drawingDiff(before, after)
            else -> error("Cannot diff different object types")
        }
    }

    fun apply(snapshot: MapPlacedObjectSnapshot, changes: Map<String, JsonNode?>): MapPlacedObjectSnapshot =
        when (snapshot) {
            is UnitSnapshot -> applyUnit(snapshot, changes)
            is DrawingSnapshot -> applyDrawing(snapshot, changes)
        }

    private fun unitDiff(before: UnitSnapshot, after: UnitSnapshot) = buildMap<String, JsonNode?> {
        putIfChanged("instanceName", before.instanceName, after.instanceName)
        putIfChanged("destroyed", before.destroyed, after.destroyed)
        putIfChanged("mapCoordinate", before.mapCoordinate, after.mapCoordinate)
        putIfChanged("normalizedWidth", before.normalizedWidth, after.normalizedWidth)
        putIfChanged("normalizedHeight", before.normalizedHeight, after.normalizedHeight)
    }

    private fun drawingDiff(before: DrawingSnapshot, after: DrawingSnapshot): Map<String, JsonNode?> {
        if (before.drawing::class != after.drawing::class) {
            return mapOf("drawing" to toNode(after.drawing))
        }

        return buildMap<String, JsonNode?> {
            when {
                before.drawing is FreeformDrawing && after.drawing is FreeformDrawing -> {
                    putIfChanged("points", before.drawing.points, after.drawing.points)
                    putIfChanged("color", before.drawing.color, after.drawing.color)
                }

                before.drawing is LineDrawing && after.drawing is LineDrawing -> {
                    putIfChanged("start", before.drawing.start, after.drawing.start)
                    putIfChanged("end", before.drawing.end, after.drawing.end)
                    putIfChanged("color", before.drawing.color, after.drawing.color)
                }

                before.drawing is RectangleDrawing && after.drawing is RectangleDrawing -> {
                    putIfChanged("start", before.drawing.start, after.drawing.start)
                    putIfChanged("end", before.drawing.end, after.drawing.end)
                    putIfChanged("color", before.drawing.color, after.drawing.color)
                }

                before.drawing is CircleDrawing && after.drawing is CircleDrawing -> {
                    putIfChanged("center", before.drawing.center, after.drawing.center)
                    putIfChanged("radius", before.drawing.radius, after.drawing.radius)
                    putIfChanged("color", before.drawing.color, after.drawing.color)
                }
            }
        }
    }

    private fun applyUnit(snapshot: UnitSnapshot, changes: Map<String, JsonNode?>) =
        changes.entries.fold(snapshot) { current, (key, value) ->
            when (key) {
                "instanceName" -> current.copy(instanceName = value.requireValue().asText())
                "destroyed" -> current.copy(destroyed = value.requireValue().asBoolean())
                "mapCoordinate" -> current.copy(mapCoordinate = fromNode(value.requireValue()))
                "normalizedWidth" -> current.copy(normalizedWidth = value.requireValue().asDouble())
                "normalizedHeight" -> current.copy(normalizedHeight = value.requireValue().asDouble())
                else -> current
            }
        }

    private fun applyDrawing(snapshot: DrawingSnapshot, changes: Map<String, JsonNode?>) =
        snapshot.copy(drawing = changes.entries.fold(snapshot.drawing) { current, (key, value) ->
            when {
                key == "drawing" -> fromNode(value.requireValue())
                current is FreeformDrawing && key == "points" -> current.copy(points = fromNode(value.requireValue()))
                current is FreeformDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
                current is LineDrawing && key == "start" -> current.copy(start = fromNode(value.requireValue()))
                current is LineDrawing && key == "end" -> current.copy(end = fromNode(value.requireValue()))
                current is LineDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
                current is RectangleDrawing && key == "start" -> current.copy(start = fromNode(value.requireValue()))
                current is RectangleDrawing && key == "end" -> current.copy(end = fromNode(value.requireValue()))
                current is RectangleDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
                current is CircleDrawing && key == "center" -> current.copy(center = fromNode(value.requireValue()))
                current is CircleDrawing && key == "radius" -> current.copy(radius = value.requireValue().asDouble())
                current is CircleDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
                else -> current
            }
        })

    private fun <T> MutableMap<String, JsonNode?>.putIfChanged(key: String, before: T, after: T) {
        if (before != after) {
            put(key, toNode(after))
        }
    }

    private fun JsonNode?.requireValue() =
        this ?: error("Patch values must be present for supported object fields")

    private fun toNode(value: Any?): JsonNode? =
        when (value) {
            null -> null
            is Color -> TextNode(value.toArgbHex())
            else -> objectMapper.valueToTree(value)
        }

    private inline fun <reified T> fromNode(node: JsonNode): T =
        objectMapper.treeToValue(node, T::class.java)

    private fun Color.toArgbHex() =
        "#%02X%02X%02X%02X".format(alpha, red, green, blue)

    private fun String.toColor(): Color {
        val hex = removePrefix("#")
        val alpha = hex.substring(0, 2).toInt(16)
        val red = hex.substring(2, 4).toInt(16)
        val green = hex.substring(4, 6).toInt(16)
        val blue = hex.substring(6, 8).toInt(16)
        return Color(red, green, blue, alpha)
    }
}
