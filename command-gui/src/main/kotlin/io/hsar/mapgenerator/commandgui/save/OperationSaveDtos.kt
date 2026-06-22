package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

internal data class GameSaveDto(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val thumbnailAssetId: String? = null,
    val camera: CameraSaveDto? = null,
    val map: MapSaveDto,
)

internal data class LedgerFileDto(
    val entries: List<LedgerEntrySaveDto>,
)

internal data class CameraSaveDto(
    val x: Double,
    val y: Double,
    val zoom: Double,
)

internal data class MapSaveDto(
    val metresPerPixel: Double,
    val assetId: String,
    val mapId: String? = null,
    val gridStartX: Double? = null,
    val gridStartY: Double? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateObjectEntrySaveDto::class, name = "create-object"),
    JsonSubTypes.Type(value = DeleteObjectEntrySaveDto::class, name = "delete-object"),
    JsonSubTypes.Type(value = EditObjectEntrySaveDto::class, name = "edit-object"),
    JsonSubTypes.Type(value = CreateUnitTemplateEntrySaveDto::class, name = "create-unit-template"),
    JsonSubTypes.Type(value = DeleteUnitTemplateEntrySaveDto::class, name = "delete-unit-template"),
)
internal sealed interface LedgerEntrySaveDto

internal data class CreateObjectEntrySaveDto(
    val objectId: UUID,
    val objectData: SavedMapObjectDto,
) : LedgerEntrySaveDto

internal data class DeleteObjectEntrySaveDto(
    val objectId: UUID,
    val objectData: SavedMapObjectDto,
) : LedgerEntrySaveDto

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class EditObjectEntrySaveDto(
    val objectId: UUID,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
) : LedgerEntrySaveDto

internal data class CreateUnitTemplateEntrySaveDto(
    val unitTemplate: UnitTemplateSnapshotSaveDto,
) : LedgerEntrySaveDto

internal data class DeleteUnitTemplateEntrySaveDto(
    val unitTemplate: UnitTemplateSnapshotSaveDto,
) : LedgerEntrySaveDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SavedUnitObjectDto::class, name = "unit"),
    JsonSubTypes.Type(value = SavedDrawingObjectDto::class, name = "drawing"),
)
internal sealed interface SavedMapObjectDto

internal data class SavedUnitObjectDto(
    val unitTemplateId: UUID,
    val instanceName: String,
    val destroyed: Boolean,
    val mapCoordinate: MapCoordinateSaveDto,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
) : SavedMapObjectDto

internal data class SavedDrawingObjectDto(
    val drawing: DrawingSaveDto,
) : SavedMapObjectDto

internal data class UnitTemplateSnapshotSaveDto(
    val id: UUID,
    val template: UnitTemplateSaveDto,
    val index: Int,
)

internal data class UnitTemplateSaveDto(
    val name: String,
    val assetId: String,
)

internal data class DrawingSaveDto(
    val type: String,
    val color: String,
    val points: List<MapDrawingPointSaveDto>? = null,
    val start: MapDrawingPointSaveDto? = null,
    val end: MapDrawingPointSaveDto? = null,
    val center: MapDrawingPointSaveDto? = null,
    val radius: Double? = null,
)

internal data class MapDrawingPointSaveDto(
    val x: Double,
    val y: Double,
)

internal data class MapCoordinateSaveDto(
    val x: Double,
    val y: Double,
)

internal data class ReadOperationSave(
    val game: GameSaveDto,
    val ledger: LedgerFileDto,
)
