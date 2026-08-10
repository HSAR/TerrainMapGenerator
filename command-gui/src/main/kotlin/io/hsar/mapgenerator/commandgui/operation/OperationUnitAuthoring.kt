package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.GridReferenceFields
import io.hsar.mapgenerator.commandgui.GridReferenceFormatter
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import java.awt.image.BufferedImage
import java.util.Base64
import java.util.UUID

data class OperationPropertyDraft(val key: String = "", val value: String = "")

data class OperationImageDraft(
    val originalFilename: String,
    val mediaType: String,
    val base64Bytes: String,
) {
    fun bytes(): ByteArray = Base64.getDecoder().decode(base64Bytes)

    fun assetSource() = AssetSource.fromEmbeddedBytes(originalFilename, bytes())

    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024

        fun fromBytes(fileName: String, bytes: ByteArray): OperationImageDraft {
            require(bytes.size <= MAX_BYTES) { "Unit images must be 20 MiB or smaller" }
            return OperationImageDraft(
                originalFilename = fileName,
                mediaType = AssetSource.mediaTypeForExtension(fileName.substringAfterLast('.', "")),
                base64Bytes = Base64.getEncoder().encodeToString(bytes),
            )
        }

        fun fromBase64(fileName: String, mediaType: String, base64Bytes: String): OperationImageDraft {
            require(base64Bytes.length <= ((MAX_BYTES + 2) / 3) * 4) { "Unit images must be 20 MiB or smaller" }
            require(Base64.getDecoder().decode(base64Bytes).size <= MAX_BYTES) { "Unit images must be 20 MiB or smaller" }
            return OperationImageDraft(fileName, mediaType, base64Bytes)
        }
    }
}

data class OperationUnitTemplateDraft(
    val name: String = "",
    val affiliation: OperationUnitAffiliation? = null,
    val replacementImage: OperationImageDraft? = null,
    val properties: List<OperationPropertyDraft> = emptyList(),
)

data class OperationUnitDraftValidation(
    val errors: Map<String, String> = emptyMap(),
) {
    val valid get() = errors.isEmpty()
}

object OperationUnitAuthoring {
    val reservedTemplatePropertyKeys = setOf("name", "assetId", "affiliation")
    val reservedUnitPropertyKeys = setOf("name", "assetId", "affiliation", "position", "destroyed", "facing")

    fun validateTemplateDraft(draft: OperationUnitTemplateDraft, hasExistingImage: Boolean = false) = buildMap {
        if (draft.name.isBlank()) put("name", "Name is required")
        if (!hasExistingImage && draft.replacementImage == null) put("image", "Image is required")
        putAll(validateProperties(draft.properties, reservedTemplatePropertyKeys))
    }.let(::OperationUnitDraftValidation)

    fun validateUnitDraft(
        name: String,
        positionX: String,
        positionY: String,
        facing: String,
        properties: List<OperationPropertyDraft>,
        renderData: MapViewerRenderData,
    ) = buildMap {
        if (name.isBlank()) put("name", "Name is required")
        if (parsePosition(renderData, positionX, positionY) == null) put("position", "Position is invalid")
        if (facing.isNotBlank() && facing.toIntOrNull()?.let { it in 0..359 } != true) put("facing", "Facing must be 0–359 or blank")
        putAll(validateProperties(properties, reservedUnitPropertyKeys))
    }.let(::OperationUnitDraftValidation)

    fun properties(rows: List<OperationPropertyDraft>) = rows.associate { it.key.trim() to it.value }

    fun createTemplate(
        content: OperationContent,
        draft: OperationUnitTemplateDraft,
        image: BufferedImage,
        id: OperationUnitTemplateId = OperationUnitTemplateId(UUID.randomUUID()),
    ): OperationUnitTemplate {
        require(validateTemplateDraft(draft).valid)
        return OperationUnitTemplate(
            id = id,
            name = draft.name.trim(),
            image = image,
            index = (content.unitTemplates.maxOfOrNull(OperationUnitTemplate::index) ?: -1) + 1,
            assetSource = draft.replacementImage?.assetSource(),
            affiliation = draft.affiliation,
            customProperties = properties(draft.properties),
        )
    }

    fun editTemplate(
        before: OperationUnitTemplate,
        draft: OperationUnitTemplateDraft,
        replacementImage: BufferedImage? = null,
    ): OperationUnitTemplate {
        require(validateTemplateDraft(draft, hasExistingImage = true).valid)
        return before.copy(
            name = draft.name.trim(),
            image = replacementImage ?: before.image,
            assetSource = draft.replacementImage?.assetSource() ?: before.assetSource,
            affiliation = draft.affiliation,
            customProperties = properties(draft.properties),
        )
    }

    fun canDeleteTemplate(content: OperationContent, id: OperationUnitTemplateId) =
        content.objects.none { it is OperationUnit && it.unitTemplateId == id }

    fun createTemplateChange(template: OperationUnitTemplate) =
        OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = template)))

    fun editTemplateChange(before: OperationUnitTemplate, after: OperationUnitTemplate) =
        if (before == after) OperationChangeSet() else OperationChangeSet(unitTemplates = listOf(OperationValueChange(before, after)))

    fun deleteTemplateChange(content: OperationContent, id: OperationUnitTemplateId): OperationChangeSet? =
        content.unitTemplate(id)?.takeIf { canDeleteTemplate(content, id) }
            ?.let { OperationChangeSet(unitTemplates = listOf(OperationValueChange(before = it))) }

    fun placeUnitChange(unit: OperationUnit) = OperationChangeSet(objects = listOf(OperationValueChange(after = unit)))

    fun editUnitChange(before: OperationUnit, after: OperationUnit) =
        if (before == after) OperationChangeSet() else OperationChangeSet(objects = listOf(OperationValueChange(before, after)))

    fun nextInstanceName(content: OperationContent, templateName: String): String {
        val names = content.objects.filterIsInstance<OperationUnit>().mapTo(hashSetOf(), OperationUnit::instanceName)
        return generateSequence(1) { it + 1 }.map { "$templateName $it" }.first { it !in names }
    }

    fun dimensions(
        image: BufferedImage,
        unitIconSizePx: Double,
        zoom: Double,
        mapWidth: Int,
        mapHeight: Int,
    ): Pair<Double, Double> {
        val worldWidth = unitIconSizePx / zoom.coerceAtLeast(0.000001)
        val worldHeight = worldWidth * image.height.toDouble() / image.width.coerceAtLeast(1)
        return worldWidth / mapWidth.coerceAtLeast(1) to worldHeight / mapHeight.coerceAtLeast(1)
    }

    fun placeUnit(
        content: OperationContent,
        template: OperationUnitTemplate,
        coordinate: MapCoordinate,
        unitIconSizePx: Double,
        zoom: Double,
        mapWidth: Int,
        mapHeight: Int,
        id: OperationObjectId = OperationObjectId(UUID.randomUUID()),
    ): OperationUnit {
        require(coordinate.x in 0.0..1.0 && coordinate.y in 0.0..1.0) { "Units must be placed inside the map" }
        val (width, height) = dimensions(template.image, unitIconSizePx, zoom, mapWidth, mapHeight)
        return OperationUnit(
            id = id,
            unitTemplateId = template.id,
            instanceName = nextInstanceName(content, template.name),
            destroyed = false,
            facingDegrees = null,
            coordinate = coordinate,
            normalizedWidth = width,
            normalizedHeight = height,
            affiliation = template.affiliation,
        )
    }

    fun formatPosition(renderData: MapViewerRenderData, coordinate: MapCoordinate): GridReferenceFields =
        renderData.gridLayer?.let { layer ->
            GridReferenceFormatter.formatFields(
                layer, renderData.baseImage.width, renderData.baseImage.height,
                coordinate.x * renderData.baseImage.width, coordinate.y * renderData.baseImage.height,
            )
        } ?: GridReferenceFields(coordinate.x.toString(), coordinate.y.toString())

    fun parsePosition(renderData: MapViewerRenderData, positionX: String, positionY: String): MapCoordinate? {
        val coordinate = if (renderData.gridLayer != null) {
            GridReferenceFormatter.parseFields(
                renderData.gridLayer, renderData.baseImage.width, renderData.baseImage.height, positionX, positionY,
            )?.let { MapCoordinate(it.x / renderData.baseImage.width, it.y / renderData.baseImage.height) }
        } else {
            val x = positionX.toDoubleOrNull() ?: return null
            val y = positionY.toDoubleOrNull() ?: return null
            MapCoordinate(x, y)
        } ?: return null
        return coordinate.takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
    }

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
