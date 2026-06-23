package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
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

            else -> error("Unsupported ledger entry type: ${entry::class.qualifiedName}")
        }

    private fun MapPlacedObjectSnapshot.toDto(): SavedMapObjectDto =
        when (this) {
            is UnitSnapshot -> SavedUnitObjectDto(
                unitTemplateId = unitTemplateId.value,
                instanceName = instanceName,
                destroyed = destroyed,
                mapCoordinate = mapCoordinate.toDto(),
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
            )

            is DrawingSnapshot -> SavedDrawingObjectDto(
                drawing = drawing.toDto(),
            )
        }

    private fun UnitTemplateSnapshot.toDto() =
        UnitTemplateSnapshotSaveDto(
            id = id.value,
            template = template.toDto(),
            index = index,
        )

    private fun MapViewerUnitTemplate.toDto() =
        UnitTemplateSaveDto(
            name = name,
            assetId = assets.registerTemplate(this).id.toString(),
        )

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
                if (beforeAssetId != afterAssetId) {
                    put("assetId", TextNode(afterAssetId))
                }
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
                if (beforeAssetId != afterAssetId) {
                    put("assetId", TextNode(beforeAssetId))
                }
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
            )

            is CircleDrawing -> DrawingSaveDto(
                type = "circle",
                color = color.toArgbHex(),
                center = center.toDto(),
                radius = radius,
            )
        }

    private fun MapCoordinate.toDto() =
        MapCoordinateSaveDto(x = x, y = y)

    private fun MapDrawingPoint.toDto() =
        MapDrawingPointSaveDto(x = x, y = y)

    private fun Color.toArgbHex() =
        "#%02X%02X%02X%02X".format(alpha, red, green, blue)
}
