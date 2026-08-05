package io.hsar.mapgenerator.commandgui.operation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.org.apache.bcel.internal.util.Args.require
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import korlibs.io.util.buildList
import java.util.*

internal data class OperationPatch(val forward: Map<String, JsonNode?>, val backward: Map<String, JsonNode?>)

internal fun operationObjectPatch(before: OperationObject, after: OperationObject) = OperationPatch(
    forward = operationObjectDiff(before, after),
    backward = operationObjectDiff(after, before),
)

internal fun operationUnitTemplatePatch(before: OperationUnitTemplate, after: OperationUnitTemplate) = OperationPatch(
    forward = operationUnitTemplateDiff(before, after),
    backward = operationUnitTemplateDiff(after, before),
)

internal fun operationGraphicTemplatePatch(before: OperationGraphicTemplate, after: OperationGraphicTemplate) = OperationPatch(
    forward = operationGraphicTemplateDiff(before, after),
    backward = operationGraphicTemplateDiff(after, before),
)

internal fun operationNotePatch(before: OperationNote, after: OperationNote) = OperationPatch(
    forward = operationNoteDiff(before, after),
    backward = operationNoteDiff(after, before),
)

internal fun applyOperationObjectPatch(snapshot: OperationObject, changes: Map<String, JsonNode?>): OperationObject = when (snapshot) {
    is OperationUnit -> changes.entries.fold(snapshot) { current, (key, value) ->
        when (key) {
            "unitTemplateId" -> current.copy(unitTemplateId = OperationUnitTemplateId(UUID.fromString(value.required().asText())))
            "instanceName" -> current.copy(instanceName = value.required().asText())
            "destroyed" -> current.copy(destroyed = value.required().asBoolean())
            "facingDegrees" -> current.copy(facingDegrees = value?.takeUnless(JsonNode::isNull)?.asInt())
            "affiliation" -> current.copy(affiliation = value?.takeUnless(JsonNode::isNull)?.asText()?.toOperationAffiliation())
            "mapCoordinate" -> current.copy(coordinate = value.required().toCoordinate())
            "normalizedWidth" -> current.copy(normalizedWidth = value.required().asDouble())
            "normalizedHeight" -> current.copy(normalizedHeight = value.required().asDouble())
            else -> current.copy(customProperties = current.customProperties.applyProperty(key, value))
        }
    }

    is OperationGraphic -> changes.entries.fold(snapshot) { current, (key, value) ->
        when (key) {
            "graphicTemplateId" -> current.copy(graphicTemplateId = OperationGraphicTemplateId(UUID.fromString(value.required().asText())))
            "instanceName" -> current.copy(instanceName = value.required().asText())
            "mapCoordinate" -> current.copy(coordinate = value.required().toCoordinate())
            "normalizedWidth" -> current.copy(normalizedWidth = value.required().asDouble())
            "normalizedHeight" -> current.copy(normalizedHeight = value.required().asDouble())
            else -> current.copy(customProperties = current.customProperties.applyProperty(key, value))
        }
    }

    is OperationDrawingObject -> {
        val drawingChanges = changes.filterKeys(DRAWING_FIELDS::contains)
        snapshot.copy(
            drawing = drawingChanges.entries.fold(snapshot.drawing) { drawing, (key, value) -> drawing.applyField(key, value) },
            customProperties = changes.filterKeys { it !in DRAWING_FIELDS }.entries
                .fold(snapshot.customProperties) { properties, (key, value) -> properties.applyProperty(key, value) },
        )
    }
}

internal fun applyOperationUnitTemplatePatch(
    template: OperationUnitTemplate,
    changes: Map<String, JsonNode?>,
    resolveAsset: (UUID) -> Pair<java.awt.image.BufferedImage, io.hsar.mapgenerator.commandgui.AssetSource>?,
): OperationUnitTemplate = changes.entries.fold(template) { current, (key, value) ->
    when (key) {
        "name" -> current.copy(name = value.required().asText())
        "affiliation" -> current.copy(affiliation = value?.takeUnless(JsonNode::isNull)?.asText()?.toOperationAffiliation())
        "assetId" -> value?.takeUnless(JsonNode::isNull)?.asText()?.let(UUID::fromString)?.let(resolveAsset)
            ?.let { (image, source) -> current.copy(image = image, assetSource = source) } ?: current

        else -> current.copy(customProperties = current.customProperties.applyProperty(key, value))
    }
}

internal fun applyOperationGraphicTemplatePatch(
    template: OperationGraphicTemplate,
    changes: Map<String, JsonNode?>,
    resolveAsset: (UUID) -> Pair<java.awt.image.BufferedImage, io.hsar.mapgenerator.commandgui.AssetSource>?,
): OperationGraphicTemplate = changes.entries.fold(template) { current, (key, value) ->
    when (key) {
        "name" -> current.copy(name = value.required().asText())
        "assetId" -> value?.takeUnless(JsonNode::isNull)?.asText()?.let(UUID::fromString)?.let(resolveAsset)
            ?.let { (image, source) -> current.copy(image = image, assetSource = source) } ?: current

        else -> current.copy(customProperties = current.customProperties.applyProperty(key, value))
    }
}

internal fun applyOperationNotePatch(note: OperationNote, changes: Map<String, JsonNode?>): OperationNote =
    changes.entries.fold(note) { current, (key, value) ->
        when (key) {
            "title" -> current.copy(title = value.required().asText())
            "body" -> current.copy(body = value.required().asText())
            "referencedObjectIds" -> current.copy(referencedObjectIds = value.required().map { OperationObjectId(UUID.fromString(it.asText())) }.distinct())
            else -> current
        }
    }

internal fun operationEntryDescription(
    changeSet: OperationChangeSet,
    before: OperationContent,
    turnName: String? = null,
    componentChangeSets: List<OperationChangeSet> = listOf(changeSet),
): Pair<MapViewerLedgerEntryKind, String> {
    if (turnName != null) return MapViewerLedgerEntryKind.TURN to "added $turnName"
    var current = before
    val changes = componentChangeSets.flatMap { component ->
        component.descriptions(current).also { current = component.applyTo(current) }
    }
    val kind = when {
        changes.size != 1 -> MapViewerLedgerEntryKind.BATCH
        changeSet.objects.isNotEmpty() -> MapViewerLedgerEntryKind.OBJECT
        changeSet.unitTemplates.isNotEmpty() -> MapViewerLedgerEntryKind.UNIT_TEMPLATE
        changeSet.graphicTemplates.isNotEmpty() -> MapViewerLedgerEntryKind.GRAPHIC_TEMPLATE
        changeSet.notes.isNotEmpty() || changeSet.noteOrderAfter != null -> MapViewerLedgerEntryKind.NOTE
        else -> MapViewerLedgerEntryKind.BATCH
    }
    val description = changes.firstOrNull()?.let { if (changes.size == 1) it else "$it and ${changes.size - 1} other item${if (changes.size == 2) "" else "s"}" }
        ?: "changed no items"
    return kind to description
}

private fun OperationChangeSet.descriptions(content: OperationContent) = buildList {
    addAll(unitTemplates.map { it.describeTemplate("unit template", ::operationUnitTemplatePatch) })
    addAll(graphicTemplates.map { it.describeTemplate("graphic template", ::operationGraphicTemplatePatch) })
    addAll(objects.map { it.describeObject(content) })
    addAll(notes.map { change ->
        when {
            change.before == null -> "created note ${change.after!!.title}"
            change.after == null -> "deleted note ${change.before.title}"
            else -> describePatch("note", operationNotePatch(change.before, change.after).forward)
        }
    })
    if (noteOrderAfter != null) add("reordered notes")
}

private fun <T> OperationValueChange<T>.describeTemplate(type: String, patch: (T, T) -> OperationPatch): String where T : Any = when {
    before == null -> "created $type ${templateName(after)}".trimEnd()
    after == null -> "deleted $type ${templateName(before)}".trimEnd()
    else -> describePatch("$type ${templateName(after)}".trimEnd(), patch(before, after).forward)
}

private fun OperationValueChange<OperationObject>.describeObject(content: OperationContent): String = when {
    before == null -> "created ${after!!.objectDescription(content)}"
    after == null -> "deleted ${before.objectDescription(content)}"
    else -> after.objectDescription(content) + ": " + after.changeDescriptions(operationObjectPatch(before, after).forward, content).joinToString(", ")
}

private fun OperationObject.changeDescriptions(changes: Map<String, JsonNode?>, content: OperationContent): List<String> = buildList {
    when (this@changeDescriptions) {
        is OperationUnit -> {
            if ("instanceName" in changes) add("renamed to $instanceName")
            if ("unitTemplateId" in changes) add("type set to ${content.unitTemplate(unitTemplateId)?.name.orEmpty()}")
            if ("destroyed" in changes) add(if (destroyed) "marked destroyed" else "marked active")
            if ("facingDegrees" in changes) add("facing ${facingDegrees?.let { "$it°" } ?: "unset"}")
            if ("affiliation" in changes) add("affiliation set to ${(affiliation ?: content.unitTemplate(unitTemplateId)?.affiliation)?.name?.lowercase() ?: "unset"}")
            if ("mapCoordinate" in changes) add("moved to ${coordinate.noticeCoordinate()}")
            if ("normalizedWidth" in changes || "normalizedHeight" in changes) add("size set to ${normalizedWidth.noticeNumber()} × ${normalizedHeight.noticeNumber()}")
            addAll(changes.customDescriptions(UNIT_FIELDS))
        }

        is OperationGraphic -> {
            if ("instanceName" in changes) add("renamed to $instanceName")
            if ("graphicTemplateId" in changes) add("type set to ${content.graphicTemplate(graphicTemplateId)?.name.orEmpty()}")
            if ("mapCoordinate" in changes) add("moved to ${coordinate.noticeCoordinate()}")
            if ("normalizedWidth" in changes || "normalizedHeight" in changes) add("size set to ${normalizedWidth.noticeNumber()} × ${normalizedHeight.noticeNumber()}")
            addAll(changes.customDescriptions(GRAPHIC_FIELDS))
        }

        is OperationDrawingObject -> {
            when (val shape = drawing) {
                is OperationFreeformDrawing -> if ("points" in changes) add("points set to ${shape.points.size}")
                is OperationLineDrawing -> {
                    if ("start" in changes) add("start set to ${shape.start.noticeCoordinate()}")
                    if ("end" in changes) add("end set to ${shape.end.noticeCoordinate()}")
                }

                is OperationRectangleDrawing -> {
                    if ("start" in changes || "end" in changes) add("bounds set to ${shape.start.noticeCoordinate()}–${shape.end.noticeCoordinate()}")
                    if ("rotationDegrees" in changes) add("rotation set to ${shape.rotationDegrees.noticeNumber()}°")
                }

                is OperationOvalDrawing -> {
                    if ("center" in changes) add("centre set to ${shape.centre.noticeCoordinate()}")
                    if ("xRadius" in changes || "yRadius" in changes) add("radii set to ${shape.xRadius.noticeNumber()} × ${shape.yRadius.noticeNumber()}")
                    if ("rotationDegrees" in changes) add("rotation set to ${shape.rotationDegrees.noticeNumber()}°")
                }
            }
            if ("color" in changes) add("colour set to ${drawing.colour.hex()}")
            addAll(changes.customDescriptions(DRAWING_FIELDS))
        }
    }
}.ifEmpty { listOf("updated") }

private fun describePatch(item: String, changes: Map<String, JsonNode?>) = changes.entries.sortedBy { it.key }
    .joinToString(", ") { (key, value) -> value?.takeUnless(JsonNode::isNull)?.let { "$key to ${it.safeDisplay()}" } ?: "removed $key" }
    .takeIf(String::isNotBlank)?.let { "$item: $it" } ?: "edited $item"

private fun Map<String, JsonNode?>.customDescriptions(reserved: Set<String>) = entries.filterNot { it.key in reserved }.sortedBy { it.key }
    .map { (key, value) -> value?.takeUnless(JsonNode::isNull)?.let { "$key set to ${it.safeDisplay()}" } ?: "removed $key" }

private fun JsonNode.safeDisplay() = asText().let { if (UUID_REGEX.containsMatchIn(it)) "redacted" else it }.take(80)
private fun MapCoordinate.noticeCoordinate() = "(${x.noticeNumber()}, ${y.noticeNumber()})"
private fun OperationDrawingPoint.noticeCoordinate() = "(${x.noticeNumber()}, ${y.noticeNumber()})"
private fun Double.noticeNumber() = "%.3f".format(Locale.ROOT, this).trimEnd('0').trimEnd('.')

private fun OperationObject.objectDescription(content: OperationContent): String = when (this) {
    is OperationUnit -> "unit ${instanceName.ifBlank { content.unitTemplate(unitTemplateId)?.name.orEmpty() }} (${content.unitTemplate(unitTemplateId)?.name.orEmpty()})"
    is OperationGraphic -> "graphic ${customProperties["displayName"]?.takeIf(String::isNotBlank) ?: instanceName.ifBlank { content.graphicTemplate(graphicTemplateId)?.name.orEmpty() }} (${
        content.graphicTemplate(
            graphicTemplateId
        )?.name.orEmpty()
    })"

    is OperationDrawingObject -> drawing.displayName.lowercase()
}

private fun templateName(value: Any?): String = when (value) {
    is OperationUnitTemplate -> value.name
    is OperationGraphicTemplate -> value.name
    is OperationNote -> value.title
    else -> ""
}

private fun operationObjectDiff(before: OperationObject, after: OperationObject): Map<String, JsonNode?> {
    require(before.id == after.id) { "Cannot diff different operation objects" }
    return when {
        before is OperationUnit && after is OperationUnit -> buildMap {
            putChanged("unitTemplateId", before.unitTemplateId.value.toString(), after.unitTemplateId.value.toString())
            putChanged("instanceName", before.instanceName, after.instanceName)
            putChanged("destroyed", before.destroyed, after.destroyed)
            putChanged("facingDegrees", before.facingDegrees, after.facingDegrees)
            if (before.affiliation != after.affiliation) put("affiliation", after.affiliation?.let { TextNode(it.saveName()) } ?: NullNode.instance)
            putChanged("mapCoordinate", before.coordinate, after.coordinate)
            putChanged("normalizedWidth", before.normalizedWidth, after.normalizedWidth)
            putChanged("normalizedHeight", before.normalizedHeight, after.normalizedHeight)
            putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, UNIT_FIELDS))
        }

        before is OperationGraphic && after is OperationGraphic -> buildMap {
            putChanged("graphicTemplateId", before.graphicTemplateId.value.toString(), after.graphicTemplateId.value.toString())
            putChanged("instanceName", before.instanceName, after.instanceName)
            putChanged("mapCoordinate", before.coordinate, after.coordinate)
            putChanged("normalizedWidth", before.normalizedWidth, after.normalizedWidth)
            putChanged("normalizedHeight", before.normalizedHeight, after.normalizedHeight)
            putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, GRAPHIC_FIELDS))
        }

        before is OperationDrawingObject && after is OperationDrawingObject -> drawingDiff(before, after)
        else -> error("Cannot diff different operation object types")
    }
}

private fun drawingDiff(before: OperationDrawingObject, after: OperationDrawingObject): Map<String, JsonNode?> = buildMap {
    if (before.drawing::class != after.drawing::class) put("drawing", node(after.drawing)) else when {
        before.drawing is OperationFreeformDrawing && after.drawing is OperationFreeformDrawing -> {
            putChanged("points", before.drawing.points, after.drawing.points); putChanged("color", before.drawing.colour.hex(), after.drawing.colour.hex())
        }

        before.drawing is OperationLineDrawing && after.drawing is OperationLineDrawing -> {
            putChanged("start", before.drawing.start, after.drawing.start); putChanged("end", before.drawing.end, after.drawing.end); putChanged(
                "color",
                before.drawing.colour.hex(),
                after.drawing.colour.hex()
            )
        }

        before.drawing is OperationRectangleDrawing && after.drawing is OperationRectangleDrawing -> {
            putChanged("start", before.drawing.start, after.drawing.start); putChanged("end", before.drawing.end, after.drawing.end); putChanged(
                "rotationDegrees",
                before.drawing.rotationDegrees,
                after.drawing.rotationDegrees
            ); putChanged("color", before.drawing.colour.hex(), after.drawing.colour.hex())
        }

        before.drawing is OperationOvalDrawing && after.drawing is OperationOvalDrawing -> {
            putChanged("center", before.drawing.centre, after.drawing.centre); putChanged("xRadius", before.drawing.xRadius, after.drawing.xRadius); putChanged(
                "yRadius",
                before.drawing.yRadius,
                after.drawing.yRadius
            ); putChanged("rotationDegrees", before.drawing.rotationDegrees, after.drawing.rotationDegrees); putChanged(
                "color",
                before.drawing.colour.hex(),
                after.drawing.colour.hex()
            )
        }
    }
    putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, DRAWING_FIELDS))
}

private fun operationUnitTemplateDiff(before: OperationUnitTemplate, after: OperationUnitTemplate) = buildMap<String, JsonNode?> {
    if (before.name != after.name) put("name", TextNode(after.name))
    if (before.affiliation != after.affiliation) put("affiliation", after.affiliation?.let { TextNode(it.saveName()) } ?: NullNode.instance)
    putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, UNIT_TEMPLATE_FIELDS))
}

private fun operationGraphicTemplateDiff(before: OperationGraphicTemplate, after: OperationGraphicTemplate) = buildMap<String, JsonNode?> {
    if (before.name != after.name) put("name", TextNode(after.name))
    putAll(CustomPropertyJson.diff(before.customProperties, after.customProperties, GRAPHIC_TEMPLATE_FIELDS))
}

private fun operationNoteDiff(before: OperationNote, after: OperationNote) = buildMap<String, JsonNode?> {
    putChanged("title", before.title, after.title)
    putChanged("body", before.body, after.body)
    putChanged("referencedObjectIds", before.referencedObjectIds.map { it.value.toString() }, after.referencedObjectIds.map { it.value.toString() })
}

private fun OperationDrawing.applyField(key: String, value: JsonNode?): OperationDrawing = when {
    key == "drawing" -> value.required().toDrawing()
    this is OperationFreeformDrawing && key == "points" -> copy(points = value.required().map(JsonNode::toDrawingPoint))
    this is OperationFreeformDrawing && key == "color" -> copy(colour = value.required().asText().toArgb())
    this is OperationLineDrawing && key == "start" -> copy(start = value.required().toDrawingPoint())
    this is OperationLineDrawing && key == "end" -> copy(end = value.required().toDrawingPoint())
    this is OperationLineDrawing && key == "color" -> copy(colour = value.required().asText().toArgb())
    this is OperationRectangleDrawing && key == "start" -> copy(start = value.required().toDrawingPoint())
    this is OperationRectangleDrawing && key == "end" -> copy(end = value.required().toDrawingPoint())
    this is OperationRectangleDrawing && key == "rotationDegrees" -> copy(rotationDegrees = value.required().asDouble())
    this is OperationRectangleDrawing && key == "color" -> copy(colour = value.required().asText().toArgb())
    this is OperationOvalDrawing && key == "center" -> copy(centre = value.required().toDrawingPoint())
    this is OperationOvalDrawing && key == "xRadius" -> copy(xRadius = value.required().asDouble())
    this is OperationOvalDrawing && key == "yRadius" -> copy(yRadius = value.required().asDouble())
    this is OperationOvalDrawing && key == "rotationDegrees" -> copy(rotationDegrees = value.required().asDouble())
    this is OperationOvalDrawing && key == "color" -> copy(colour = value.required().asText().toArgb())
    else -> this
}

private fun JsonNode.toDrawing(): OperationDrawing = when (get("type").asText()) {
    "freeform" -> OperationFreeformDrawing(get("points").map(JsonNode::toDrawingPoint), get("color").asText().toArgb())
    "line" -> OperationLineDrawing(get("start").toDrawingPoint(), get("end").toDrawingPoint(), get("color").asText().toArgb())
    "rectangle" -> OperationRectangleDrawing(get("start").toDrawingPoint(), get("end").toDrawingPoint(), get("color").asText().toArgb(), get("rotationDegrees")?.asDouble() ?: 0.0)
    "oval" -> OperationOvalDrawing(
        get("center").toDrawingPoint(),
        get("xRadius").asDouble(),
        get("yRadius").asDouble(),
        get("color").asText().toArgb(),
        get("rotationDegrees")?.asDouble() ?: 0.0
    )

    else -> error("Unsupported drawing type ${get("type")}")
}

private fun JsonNode.toDrawingPoint() = OperationDrawingPoint(get("x").asDouble(), get("y").asDouble())
private fun JsonNode.toCoordinate() = MapCoordinate(get("x").asDouble(), get("y").asDouble())
private fun JsonNode?.required() = this ?: error("Patch values must be present for supported fields")
private fun Map<String, String>.applyProperty(key: String, value: JsonNode?) =
    if (value == null || value.isNull) this - key else this + (key to CustomPropertyJson.toStringValue(value))

private fun <T> MutableMap<String, JsonNode?>.putChanged(key: String, before: T, after: T) {
    if (before != after) put(key, node(after))
}

private val patchObjectMapper = jacksonObjectMapper()
private fun node(value: Any?): JsonNode? = if (value == null) null else patchObjectMapper.valueToTree(value)
private fun OperationArgb.hex() = "#%02X%02X%02X%02X".format(alpha, red, green, blue)
private fun String.toArgb() =
    removePrefix("#").let { OperationArgb.of(it.substring(0, 2).toInt(16), it.substring(2, 4).toInt(16), it.substring(4, 6).toInt(16), it.substring(6, 8).toInt(16)) }

internal fun OperationUnitAffiliation.saveName() = name.lowercase().replaceFirstChar(Char::uppercase)
internal fun String.toOperationAffiliation() = OperationUnitAffiliation.valueOf(uppercase())

private val UNIT_FIELDS = setOf("unitTemplateId", "instanceName", "destroyed", "facingDegrees", "affiliation", "mapCoordinate", "normalizedWidth", "normalizedHeight")
private val GRAPHIC_FIELDS = setOf("graphicTemplateId", "instanceName", "mapCoordinate", "normalizedWidth", "normalizedHeight")
private val DRAWING_FIELDS = setOf("drawing", "points", "color", "start", "end", "rotationDegrees", "center", "xRadius", "yRadius")
private val UNIT_TEMPLATE_FIELDS = setOf("name", "affiliation", "assetId")
private val GRAPHIC_TEMPLATE_FIELDS = setOf("name", "assetId")
private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
