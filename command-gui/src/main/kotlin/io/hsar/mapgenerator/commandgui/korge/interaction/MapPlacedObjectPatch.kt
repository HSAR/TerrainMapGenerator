package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import java.awt.Color
import java.util.*

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
        if (before.unitTemplateId != after.unitTemplateId) {
            put("unitTemplateId", TextNode(after.unitTemplateId.value.toString()))
        }
        putIfChanged("instanceName", before.instanceName, after.instanceName)
        putIfChanged("destroyed", before.destroyed, after.destroyed)
        putIfChanged("facingDegrees", before.facingDegrees, after.facingDegrees)
        putIfChanged("affiliation", before.template.affiliation, after.template.affiliation)
        putIfChanged("mapCoordinate", before.mapCoordinate, after.mapCoordinate)
        putIfChanged("normalizedWidth", before.normalizedWidth, after.normalizedWidth)
        putIfChanged("normalizedHeight", before.normalizedHeight, after.normalizedHeight)
        putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, UNIT_PATCH_FIELDS))
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
                    putIfChanged("rotationDegrees", before.drawing.rotationDegrees, after.drawing.rotationDegrees)
                    putIfChanged("color", before.drawing.color, after.drawing.color)
                }

                before.drawing is OvalDrawing && after.drawing is OvalDrawing -> {
                    putIfChanged("center", before.drawing.center, after.drawing.center)
                    putIfChanged("xRadius", before.drawing.xRadius, after.drawing.xRadius)
                    putIfChanged("yRadius", before.drawing.yRadius, after.drawing.yRadius)
                    putIfChanged("rotationDegrees", before.drawing.rotationDegrees, after.drawing.rotationDegrees)
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
                "facingDegrees" -> current.copy(facingDegrees = value?.takeUnless { it.isNull }?.asInt())
                "affiliation" -> current.copy(template = current.template.copy(affiliation = value?.takeUnless { it.isNull }?.let { UnitAffiliation.valueOf(it.asText()) }))
                "unitTemplateId" -> current.copy(unitTemplateId = UnitTemplateId(UUID.fromString(value.requireValue().asText())))
                "mapCoordinate" -> current.copy(mapCoordinate = fromNode(value.requireValue()))
                "normalizedWidth" -> current.copy(normalizedWidth = value.requireValue().asDouble())
                "normalizedHeight" -> current.copy(normalizedHeight = value.requireValue().asDouble())
                else -> current.copy(customProperties = current.customProperties.applyCustomPropertyPatch(key, value))
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
                current is RectangleDrawing && key == "rotationDegrees" -> current.copy(rotationDegrees = value.requireValue().asDouble())
                current is RectangleDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
                current is OvalDrawing && key == "center" -> current.copy(center = fromNode(value.requireValue()))
                current is OvalDrawing && key == "xRadius" -> current.copy(xRadius = value.requireValue().asDouble())
                current is OvalDrawing && key == "yRadius" -> current.copy(yRadius = value.requireValue().asDouble())
                current is OvalDrawing && key == "rotationDegrees" -> current.copy(rotationDegrees = value.requireValue().asDouble())
                current is OvalDrawing && key == "color" -> current.copy(color = value.requireValue().asText().toColor())
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

    private fun Map<String, String>.applyCustomPropertyPatch(key: String, value: JsonNode?): Map<String, String> =
        if (value == null || value.isNull) {
            this - key
        } else {
            this + (key to CustomPropertyJson.toStringValue(value))
        }

    private val UNIT_PATCH_FIELDS = setOf(
        "unitTemplateId",
        "instanceName",
        "destroyed",
        "facingDegrees",
        "affiliation",
        "mapCoordinate",
        "normalizedWidth",
        "normalizedHeight",
    )
}
