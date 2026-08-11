package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import java.awt.image.BufferedImage
import java.util.UUID

const val GRAPHIC_TEMPLATE_WIDTH_PROPERTY = "normalizedWidth"
const val GRAPHIC_TEMPLATE_HEIGHT_PROPERTY = "normalizedHeight"
const val GRAPHIC_DISPLAY_NAME_PROPERTY = OPERATION_DISPLAY_NAME_PROPERTY

data class OperationGraphicTemplateDraft(
    val name: String = "",
    val widthPercent: String = DEFAULT_GRAPHIC_WIDTH_PERCENT.toString(),
    val heightPercent: String = "",
    val replacementImage: OperationImageDraft? = null,
    val properties: List<OperationPropertyDraft> = emptyList(),
)

data class OperationGraphicDraft(
    val graphicTemplateId: OperationGraphicTemplateId? = null,
    val displayName: String = "",
    val positionX: String = "",
    val positionY: String = "",
    val properties: List<OperationPropertyDraft> = emptyList(),
)

data class OperationGraphicDraftValidation(val errors: Map<String, String> = emptyMap()) {
    val valid get() = errors.isEmpty()
}

object OperationGraphicAuthoring {
    val reservedTemplatePropertyKeys = setOf("name", "assetId", GRAPHIC_TEMPLATE_WIDTH_PROPERTY, GRAPHIC_TEMPLATE_HEIGHT_PROPERTY)
    val reservedGraphicPropertyKeys = setOf(
        "graphicTemplateId", "instanceName", "mapCoordinate", GRAPHIC_DISPLAY_NAME_PROPERTY,
        GRAPHIC_TEMPLATE_WIDTH_PROPERTY, GRAPHIC_TEMPLATE_HEIGHT_PROPERTY,
    )

    fun validateTemplateDraft(draft: OperationGraphicTemplateDraft, hasExistingImage: Boolean = false) = buildMap {
        if (draft.name.isBlank()) put("name", "Name is required")
        if (!hasExistingImage && draft.replacementImage == null) put("image", "Image is required")
        if (parsePercent(draft.widthPercent) == null) put("width", "Width must be at least 0.01%")
        if (parsePercent(draft.heightPercent) == null) put("height", "Height must be at least 0.01%")
        putAll(validateProperties(draft.properties, reservedTemplatePropertyKeys))
    }.let(::OperationGraphicDraftValidation)

    fun validateGraphicDraft(draft: OperationGraphicDraft, content: OperationContent, renderData: MapViewerRenderData) = buildMap {
        if (draft.graphicTemplateId?.let(content::graphicTemplate) == null) put("template", "Graphic type is required")
        if (OperationUnitAuthoring.parsePosition(renderData, draft.positionX, draft.positionY) == null) put("position", "Position is invalid")
        putAll(validateProperties(draft.properties, reservedGraphicPropertyKeys))
    }.let(::OperationGraphicDraftValidation)

    fun defaultHeightPercent(image: BufferedImage, mapWidth: Int, mapHeight: Int, widthPercent: Double = DEFAULT_GRAPHIC_WIDTH_PERCENT) =
        widthPercent * mapWidth.coerceAtLeast(1).toDouble() / mapHeight.coerceAtLeast(1) *
            image.height.toDouble() / image.width.coerceAtLeast(1)

    fun createTemplate(
        content: OperationContent,
        draft: OperationGraphicTemplateDraft,
        image: BufferedImage,
        id: OperationGraphicTemplateId = OperationGraphicTemplateId(UUID.randomUUID()),
    ): OperationGraphicTemplate {
        require(validateTemplateDraft(draft).valid)
        return OperationGraphicTemplate(
            id = id,
            name = draft.name.trim(),
            image = image,
            index = (content.graphicTemplates.maxOfOrNull(OperationGraphicTemplate::index) ?: -1) + 1,
            normalizedWidth = requirePercent(draft.widthPercent),
            normalizedHeight = requirePercent(draft.heightPercent),
            assetSource = draft.replacementImage?.assetSource(),
            customProperties = properties(draft.properties),
        )
    }

    fun editTemplate(before: OperationGraphicTemplate, draft: OperationGraphicTemplateDraft, replacementImage: BufferedImage? = null): OperationGraphicTemplate {
        require(validateTemplateDraft(draft, hasExistingImage = true).valid)
        return before.copy(
            name = draft.name.trim(),
            image = replacementImage ?: before.image,
            normalizedWidth = requirePercent(draft.widthPercent),
            normalizedHeight = requirePercent(draft.heightPercent),
            assetSource = draft.replacementImage?.assetSource() ?: before.assetSource,
            customProperties = properties(draft.properties),
        )
    }

    fun createTemplateChange(template: OperationGraphicTemplate) =
        OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = template)))

    fun editTemplateChange(content: OperationContent, before: OperationGraphicTemplate, after: OperationGraphicTemplate): OperationChangeSet {
        if (before == after) return OperationChangeSet()
        val objectChanges = content.objects.filterIsInstance<OperationGraphic>()
            .filter { it.graphicTemplateId == before.id }
            .map { graphic ->
                OperationValueChange<OperationObject>(
                    graphic,
                    graphic.copy(normalizedWidth = after.normalizedWidth, normalizedHeight = after.normalizedHeight),
                )
            }.filter { it.before != it.after }
        return OperationChangeSet(
            objects = objectChanges,
            graphicTemplates = listOf(OperationValueChange(before, after)),
        )
    }

    fun canDeleteTemplate(content: OperationContent, id: OperationGraphicTemplateId) =
        content.objects.none { it is OperationGraphic && it.graphicTemplateId == id }

    fun deleteTemplateChange(content: OperationContent, id: OperationGraphicTemplateId): OperationChangeSet? =
        content.graphicTemplate(id)?.takeIf { canDeleteTemplate(content, id) }
            ?.let { OperationChangeSet(graphicTemplates = listOf(OperationValueChange(before = it))) }

    fun placeGraphic(
        template: OperationGraphicTemplate,
        coordinate: MapCoordinate,
        id: OperationObjectId = OperationObjectId(UUID.randomUUID()),
    ): OperationGraphic {
        require(coordinate.x in 0.0..1.0 && coordinate.y in 0.0..1.0) { "Graphics must be placed inside the map" }
        return OperationGraphic(
            id = id,
            graphicTemplateId = template.id,
            instanceName = template.name,
            coordinate = coordinate,
            normalizedWidth = template.normalizedWidth,
            normalizedHeight = template.normalizedHeight,
        )
    }

    fun placeGraphicChange(graphic: OperationGraphic) = OperationChangeSet(objects = listOf(OperationValueChange(after = graphic)))

    fun editGraphic(before: OperationGraphic, draft: OperationGraphicDraft, content: OperationContent, renderData: MapViewerRenderData): OperationGraphic {
        require(validateGraphicDraft(draft, content, renderData).valid)
        val template = content.graphicTemplate(requireNotNull(draft.graphicTemplateId))!!
        val coordinate = OperationUnitAuthoring.parsePosition(renderData, draft.positionX, draft.positionY)!!
        val customProperties = properties(draft.properties).let { properties ->
            draft.displayName.trim().takeIf(String::isNotEmpty)
                ?.let { properties + (GRAPHIC_DISPLAY_NAME_PROPERTY to it) }
                ?: properties
        }
        val retargeted = before.graphicTemplateId != template.id
        return before.copy(
            graphicTemplateId = template.id,
            instanceName = if (retargeted) template.name else before.instanceName,
            coordinate = coordinate,
            normalizedWidth = if (retargeted) template.normalizedWidth else before.normalizedWidth,
            normalizedHeight = if (retargeted) template.normalizedHeight else before.normalizedHeight,
            customProperties = customProperties,
        )
    }

    fun editGraphicChange(before: OperationGraphic, after: OperationGraphic) =
        if (before == after) OperationChangeSet() else OperationChangeSet(objects = listOf(OperationValueChange(before, after)))

    fun properties(rows: List<OperationPropertyDraft>) = rows.associate { it.key.trim() to it.value }

    private fun parsePercent(value: String) = value.trim().toDoubleOrNull()
        ?.takeIf(Double::isFinite)
        ?.takeIf { it / 100.0 >= MIN_GRAPHIC_NORMALIZED_SIZE }
        ?.div(100.0)

    private fun requirePercent(value: String) = requireNotNull(parsePercent(value))

    private fun validateProperties(rows: List<OperationPropertyDraft>, reserved: Set<String>) = buildMap {
        val keys = mutableSetOf<String>()
        rows.forEachIndexed { index, row ->
            val key = row.key.trim()
            when {
                key.isEmpty() -> put("property:$index", "Property key is required")
                key in reserved -> put("property:$index", "$key is reserved")
                !keys.add(key) -> put("property:$index", "$key is duplicated")
            }
        }
    }
}

internal fun OperationGraphicTemplate.archiveCustomProperties() = customProperties + mapOf(
    GRAPHIC_TEMPLATE_WIDTH_PROPERTY to normalizedWidth.toString(),
    GRAPHIC_TEMPLATE_HEIGHT_PROPERTY to normalizedHeight.toString(),
)

internal fun Map<String, String>.graphicTemplateDimensions() =
    get(GRAPHIC_TEMPLATE_WIDTH_PROPERTY)?.toDoubleOrNull()?.let { width ->
        get(GRAPHIC_TEMPLATE_HEIGHT_PROPERTY)?.toDoubleOrNull()?.let { height -> width to height }
    }

internal fun Map<String, String>.withoutGraphicTemplateDimensions() = this - GRAPHIC_TEMPLATE_WIDTH_PROPERTY - GRAPHIC_TEMPLATE_HEIGHT_PROPERTY

const val DEFAULT_GRAPHIC_WIDTH_PERCENT = 3.2
const val MIN_GRAPHIC_NORMALIZED_SIZE = 0.0001
