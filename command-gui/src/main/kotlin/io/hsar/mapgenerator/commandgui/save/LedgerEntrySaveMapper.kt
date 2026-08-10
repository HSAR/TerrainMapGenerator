package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.operation.*

internal class LedgerEntrySaveMapper(private val assets: OperationAssetRegistry) {
    fun toDtos(timeline: OperationTimeline): List<LedgerEntrySaveDto> {
        var content = OperationContent.EMPTY
        return timeline.appliedEntries.mapNotNull { entry ->
            val dto = entry.toDto(content)
            content = entry.changeSet.applyTo(content)
            dto
        }
    }

    private fun OperationTimelineEntry.toDto(before: OperationContent): LedgerEntrySaveDto? {
        if (turnName != null) return AddTurnEntrySaveDto(turnName, turnThumbnailAssetId)
        if (kind == io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind.BATCH) {
            var current = before
            val entries = componentChangeSets.flatMap { component ->
                val after = component.applyTo(current)
                val actions = component.toDtos(current, after)
                current = after
                actions
            }
            return BatchLedgerEntrySaveDto(entries)
        }
        val after = changeSet.applyTo(before)
        val actions = changeSet.toDtos(before, after)
        return when (actions.size) {
            0 -> null
            1 -> actions.single()
            else -> BatchLedgerEntrySaveDto(actions)
        }
    }

    private fun OperationChangeSet.toDtos(before: OperationContent, after: OperationContent) = buildList {
        unitTemplates.mapNotNullTo(this) { it.unitTemplateDto(assets) }
        graphicTemplates.mapNotNullTo(this) { it.graphicTemplateDto(assets) }
        objects.mapNotNullTo(this) { it.toDto(before, after) }
        notes.mapNotNullTo(this) { it.toDto() }
        if (noteOrderBefore != null && noteOrderAfter != null) {
            add(ReorderNotesEntrySaveDto(noteOrderAfter.map { it.value }, noteOrderBefore.map { it.value }))
        }
    }
}

private fun OperationValueChange<OperationObject>.toDto(beforeContent: OperationContent, afterContent: OperationContent): LedgerEntrySaveDto? = when {
    before == null -> CreateObjectEntrySaveDto(after!!.id.value, after.toDto(afterContent))
    after == null -> DeleteObjectEntrySaveDto(before.id.value, before.toDto(beforeContent))
    else -> operationObjectPatch(before, after).let { EditObjectEntrySaveDto(before.id.value, it.forward, it.backward) }
}

private fun OperationValueChange<OperationUnitTemplate>.unitTemplateDto(assets: OperationAssetRegistry): LedgerEntrySaveDto? = when {
    before == null -> CreateUnitTemplateEntrySaveDto(after!!.toDto(assets))
    after == null -> DeleteUnitTemplateEntrySaveDto(before.toDto(assets))
    else -> {
        val patch = operationUnitTemplatePatch(before, after)
        val beforeAsset = assets.registerTemplate(before)
        val afterAsset = assets.registerTemplate(after)
        EditUnitTemplateEntrySaveDto(
            before.id.value,
            patch.forward.withAssetChange(beforeAsset, afterAsset),
            patch.backward.withAssetChange(afterAsset, beforeAsset),
        )
    }
}

private fun OperationValueChange<OperationGraphicTemplate>.graphicTemplateDto(assets: OperationAssetRegistry): LedgerEntrySaveDto? = when {
    before == null -> CreateGraphicTemplateEntrySaveDto(after!!.toDto(assets))
    after == null -> DeleteGraphicTemplateEntrySaveDto(before.toDto(assets))
    else -> {
        val patch = operationGraphicTemplatePatch(before, after)
        val beforeAsset = assets.registerGraphicTemplate(before)
        val afterAsset = assets.registerGraphicTemplate(after)
        EditGraphicTemplateEntrySaveDto(
            before.id.value,
            patch.forward.withAssetChange(beforeAsset, afterAsset),
            patch.backward.withAssetChange(afterAsset, beforeAsset),
        )
    }
}

private fun OperationValueChange<OperationNote>.toDto(): LedgerEntrySaveDto? = when {
    before == null -> CreateNoteEntrySaveDto(after!!.toDto())
    after == null -> DeleteNoteEntrySaveDto(before.toDto())
    else -> operationNotePatch(before, after).let { EditNoteEntrySaveDto(before.id.value, it.forward, it.backward) }
}

private fun Map<String, JsonNode?>.withAssetChange(before: RegisteredAsset, after: RegisteredAsset) =
    if (before.id == after.id) this else this + ("assetId" to TextNode(after.id.toString()))

private fun OperationObject.toDto(content: OperationContent): SavedMapObjectDto = when (this) {
    is OperationUnit -> SavedUnitObjectDto(
        unitTemplateId = unitTemplateId.value,
        instanceName = instanceName,
        destroyed = destroyed,
        facingDegrees = facingDegrees,
        affiliation = (affiliation ?: content.unitTemplate(unitTemplateId)?.affiliation)?.toSaveDto(),
        mapCoordinate = coordinate.toDto(),
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is OperationGraphic -> SavedGraphicObjectDto(
        graphicTemplateId = graphicTemplateId.value,
        instanceName = instanceName,
        mapCoordinate = coordinate.toDto(),
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is OperationDrawingObject -> SavedDrawingObjectDto(drawing.toDto(), customProperties)
}

private fun OperationUnitTemplate.toDto(assets: OperationAssetRegistry) = UnitTemplateSnapshotSaveDto(
    id = id.value,
    template = UnitTemplateSaveDto(
        name = name,
        assetId = assets.registerTemplate(this).id.toString(),
        affiliation = affiliation?.toSaveDto(),
        customProperties = customProperties,
    ),
    index = index,
)

private fun OperationGraphicTemplate.toDto(assets: OperationAssetRegistry) = GraphicTemplateSaveDto(
    id = id.value,
    name = name,
    assetId = assets.registerGraphicTemplate(this).id.toString(),
    customProperties = customProperties,
)

private fun OperationNote.toDto() = NoteSaveDto(id.value, title, body, referencedObjectIds.map { it.value })

private fun OperationDrawing.toDto(): DrawingSaveDto = when (this) {
    is OperationFreeformDrawing -> DrawingSaveDto("freeform", colour.toHex(), points = points.map { it.toDto() })
    is OperationLineDrawing -> DrawingSaveDto("line", colour.toHex(), start = start.toDto(), end = end.toDto())
    is OperationRectangleDrawing -> DrawingSaveDto("rectangle", colour.toHex(), start = start.toDto(), end = end.toDto(), rotationDegrees = rotationDegrees)
    is OperationOvalDrawing -> DrawingSaveDto("oval", colour.toHex(), center = centre.toDto(), xRadius = xRadius, yRadius = yRadius, rotationDegrees = rotationDegrees)
}

private fun MapCoordinate.toDto() = MapCoordinateSaveDto(x, y)
private fun OperationDrawingPoint.toDto() = MapDrawingPointSaveDto(x, y)
private fun OperationArgb.toHex() = "#%02X%02X%02X%02X".format(alpha, red, green, blue)
private fun OperationUnitAffiliation.toSaveDto() = UnitAffiliationSaveDto.valueOf(saveName())
