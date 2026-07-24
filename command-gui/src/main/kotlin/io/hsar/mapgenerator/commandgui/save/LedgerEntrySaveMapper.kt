package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.ReorderNotesAction
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import java.awt.Color

internal class LedgerEntrySaveMapper(
    private val assets: OperationAssetRegistry,
) {
    fun toDto(entry: LedgerEntry): LedgerEntrySaveDto =
        when (entry) {
            is CreateObjectAction -> CreateObjectEntrySaveDto(
                objectId = entry.snapshot.id.value,
                objectData = entry.snapshot.toDto(),
            )

            is DeleteObjectAction -> DeleteObjectEntrySaveDto(
                objectId = entry.snapshot.id.value,
                objectData = entry.snapshot.toDto(),
            )

            is EditObjectAction -> EditObjectEntrySaveDto(
                objectId = entry.objectId.value,
                forward = entry.forward,
                backward = entry.backward,
            )

            is BatchLedgerEntry -> BatchLedgerEntrySaveDto(
                entries = entry.entries.map(::toDto),
            )

            is AddTurnLedgerEntry -> AddTurnEntrySaveDto(
                name = entry.name,
                thumbnailAssetId = entry.thumbnailAssetId,
            )

            is CreateUnitTemplateAction -> CreateUnitTemplateEntrySaveDto(
                unitTemplate = entry.snapshot.toDto(),
            )

            is DeleteUnitTemplateAction -> DeleteUnitTemplateEntrySaveDto(
                unitTemplate = entry.snapshot.toDto(),
            )

            is EditUnitTemplateAction -> EditUnitTemplateEntrySaveDto(
                id = entry.id.value,
                forward = entry.saveForwardPatch(),
                backward = entry.saveBackwardPatch(),
            )

            is CreateGraphicTemplateAction -> CreateGraphicTemplateEntrySaveDto(entry.snapshot.toDto())
            is DeleteGraphicTemplateAction -> DeleteGraphicTemplateEntrySaveDto(entry.snapshot.toDto())
            is EditGraphicTemplateAction -> EditGraphicTemplateEntrySaveDto(entry.id.value, entry.saveForwardPatch(), entry.saveBackwardPatch())

            is CreateNoteAction -> CreateNoteEntrySaveDto(
                note = entry.snapshot.toDto(),
            )

            is EditNoteAction -> EditNoteEntrySaveDto(
                id = entry.id.value,
                forward = entry.forward,
                backward = entry.backward,
            )

            is DeleteNoteAction -> DeleteNoteEntrySaveDto(
                note = entry.snapshot.toDto(),
            )

            is ReorderNotesAction -> ReorderNotesEntrySaveDto(
                forward = entry.forward.map { it.value },
                backward = entry.backward.map { it.value },
            )

            else -> error("Unsupported ledger entry type: ${entry::class.qualifiedName}")
        }

    private fun MapPlacedObjectSnapshot.toDto(): SavedMapObjectDto =
        when (this) {
            is UnitSnapshot -> SavedUnitObjectDto(
                unitTemplateId = unitTemplateId.value,
                instanceName = instanceName,
                destroyed = destroyed,
                facingDegrees = facingDegrees,
                affiliation = template.affiliation,
                mapCoordinate = mapCoordinate.toDto(),
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
                customProperties = customProperties,
            )

            is DrawingSnapshot -> SavedDrawingObjectDto(
                drawing = drawing.toDto(),
                customProperties = customProperties,
            )
            is GraphicSnapshot -> SavedGraphicObjectDto(
                graphicTemplateId = graphicTemplateId.value,
                instanceName = instanceName,
                mapCoordinate = mapCoordinate.toDto(),
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
                customProperties = customProperties,
            )
        }

    private fun GraphicTemplateSnapshot.toDto() = GraphicTemplateSaveDto(
        id = id.value,
        name = template.name,
        assetId = assets.registerGraphicTemplate(template).id.toString(),
        customProperties = template.customProperties,
    )

    private fun UnitTemplateSnapshot.toDto() =
        UnitTemplateSnapshotSaveDto(
            id = id.value,
            template = template.toDto(),
            index = index,
        )

    private fun MapNoteSnapshot.toDto() =
        NoteSaveDto(
            id = id.value,
            title = title,
            body = body,
            referencedObjectIds = referencedObjectIds.map { it.value },
        )

    private fun MapViewerUnitTemplate.toDto() =
        UnitTemplateSaveDto(
            name = name,
            assetId = assets.registerTemplate(this).id.toString(),
            affiliation = affiliation,
            customProperties = customProperties,
        )

    private fun EditGraphicTemplateAction.saveForwardPatch(): Map<String, com.fasterxml.jackson.databind.JsonNode?> =
        if (beforeSnapshot == null || afterSnapshot == null) forward else graphicPatch(beforeSnapshot, afterSnapshot, assets)

    private fun EditGraphicTemplateAction.saveBackwardPatch(): Map<String, com.fasterxml.jackson.databind.JsonNode?> =
        if (beforeSnapshot == null || afterSnapshot == null) backward else graphicPatch(afterSnapshot, beforeSnapshot, assets)

    private fun EditUnitTemplateAction.saveForwardPatch(): Map<String, com.fasterxml.jackson.databind.JsonNode?> =
        if (beforeSnapshot == null || afterSnapshot == null) {
            forward
        } else {
            val beforeAssetId = assets.registerTemplate(beforeSnapshot.template).id.toString()
            val afterAssetId = assets.registerTemplate(afterSnapshot.template).id.toString()
            buildMap {
                if (beforeSnapshot.template.name != afterSnapshot.template.name) {
                    put("name", TextNode(afterSnapshot.template.name))
                }
                if (beforeSnapshot.template.affiliation != afterSnapshot.template.affiliation) {
                    put("affiliation", afterSnapshot.template.affiliation.toNode())
                }
                if (beforeAssetId != afterAssetId) {
                    put("assetId", TextNode(afterAssetId))
                }
                putAll(
                    CustomPropertyJson.diff(
                        before = beforeSnapshot.template.customProperties,
                        after = afterSnapshot.template.customProperties,
                        reservedKeys = UNIT_TEMPLATE_PATCH_FIELDS,
                    ),
                )
            }
        }

    private fun EditUnitTemplateAction.saveBackwardPatch(): Map<String, com.fasterxml.jackson.databind.JsonNode?> =
        if (beforeSnapshot == null || afterSnapshot == null) {
            backward
        } else {
            val beforeAssetId = assets.registerTemplate(beforeSnapshot.template).id.toString()
            val afterAssetId = assets.registerTemplate(afterSnapshot.template).id.toString()
            buildMap {
                if (beforeSnapshot.template.name != afterSnapshot.template.name) {
                    put("name", TextNode(beforeSnapshot.template.name))
                }
                if (beforeSnapshot.template.affiliation != afterSnapshot.template.affiliation) {
                    put("affiliation", beforeSnapshot.template.affiliation.toNode())
                }
                if (beforeAssetId != afterAssetId) {
                    put("assetId", TextNode(beforeAssetId))
                }
                putAll(
                    CustomPropertyJson.diff(
                        before = afterSnapshot.template.customProperties,
                        after = beforeSnapshot.template.customProperties,
                        reservedKeys = UNIT_TEMPLATE_PATCH_FIELDS,
                    ),
                )
            }
        }

    private fun MapDrawing.toDto(): DrawingSaveDto =
        when (this) {
            is FreeformDrawing -> DrawingSaveDto(
                type = "freeform",
                color = color.toArgbHex(),
                points = points.map { it.toDto() },
            )

            is LineDrawing -> DrawingSaveDto(
                type = "line",
                color = color.toArgbHex(),
                start = start.toDto(),
                end = end.toDto(),
            )

            is RectangleDrawing -> DrawingSaveDto(
                type = "rectangle",
                color = color.toArgbHex(),
                start = start.toDto(),
                end = end.toDto(),
                rotationDegrees = rotationDegrees,
            )

            is OvalDrawing -> DrawingSaveDto(
                type = "oval",
                color = color.toArgbHex(),
                center = center.toDto(),
                xRadius = xRadius,
                yRadius = yRadius,
                rotationDegrees = rotationDegrees,
            )
        }

    private fun MapCoordinate.toDto() =
        MapCoordinateSaveDto(x = x, y = y)

    private fun MapDrawingPoint.toDto() =
        MapDrawingPointSaveDto(x = x, y = y)

    private fun Color.toArgbHex() =
        "#%02X%02X%02X%02X".format(alpha, red, green, blue)

    private fun io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation?.toNode(): com.fasterxml.jackson.databind.JsonNode =
        this?.let { TextNode(it.name) } ?: com.fasterxml.jackson.databind.node.NullNode.instance

    private companion object {
        val UNIT_TEMPLATE_PATCH_FIELDS = setOf("name", "affiliation", "assetId")
    }
}

private fun graphicPatch(
    before: GraphicTemplateSnapshot,
    after: GraphicTemplateSnapshot,
    assets: OperationAssetRegistry,
): Map<String, com.fasterxml.jackson.databind.JsonNode?> = buildMap {
    if (before.template.name != after.template.name) put("name", TextNode(after.template.name))
    val beforeAsset = assets.registerGraphicTemplate(before.template).id.toString()
    val afterAsset = assets.registerGraphicTemplate(after.template).id.toString()
    if (beforeAsset != afterAsset) put("assetId", TextNode(afterAsset))
    putAll(CustomPropertyJson.diff(before.template.customProperties, after.template.customProperties, setOf("name", "assetId")))
}
