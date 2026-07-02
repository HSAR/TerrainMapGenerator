package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.grid.ReferenceSystem
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
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val mapId: String? = null,
    val gridStartX: String? = null,
    val gridStartY: String? = null,
    val gridColour: String? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateObjectEntrySaveDto::class, name = "create-object"),
    JsonSubTypes.Type(value = DeleteObjectEntrySaveDto::class, name = "delete-object"),
    JsonSubTypes.Type(value = EditObjectEntrySaveDto::class, name = "edit-object"),
    JsonSubTypes.Type(value = BatchLedgerEntrySaveDto::class, name = "batch"),
    JsonSubTypes.Type(value = CreateUnitTemplateEntrySaveDto::class, name = "create-unit-template"),
    JsonSubTypes.Type(value = DeleteUnitTemplateEntrySaveDto::class, name = "delete-unit-template"),
    JsonSubTypes.Type(value = EditUnitTemplateEntrySaveDto::class, name = "edit-unit-template"),
    JsonSubTypes.Type(value = AddTurnEntrySaveDto::class, name = "add-turn"),
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

internal data class BatchLedgerEntrySaveDto(
    val entries: List<LedgerEntrySaveDto>,
) : LedgerEntrySaveDto

internal data class AddTurnEntrySaveDto(
    val name: String,
    val thumbnailAssetId: UUID? = null,
) : LedgerEntrySaveDto

internal data class CreateUnitTemplateEntrySaveDto(
    val unitTemplate: UnitTemplateSnapshotSaveDto,
) : LedgerEntrySaveDto

internal data class DeleteUnitTemplateEntrySaveDto(
    val unitTemplate: UnitTemplateSnapshotSaveDto,
) : LedgerEntrySaveDto

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class EditUnitTemplateEntrySaveDto(
    val id: UUID,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
) : LedgerEntrySaveDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SavedUnitObjectDto::class, name = "unit"),
    JsonSubTypes.Type(value = SavedDrawingObjectDto::class, name = "drawing"),
)
internal sealed interface SavedMapObjectDto

internal class SavedUnitObjectDto(
    val unitTemplateId: UUID,
    val instanceName: String,
    val destroyed: Boolean,
    val facingDegrees: Int? = null,
    val mapCoordinate: MapCoordinateSaveDto,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
) : SavedMapObjectDto {
    private val customPropertiesByKey = linkedMapOf<String, String>()

    constructor(
        unitTemplateId: UUID,
        instanceName: String,
        destroyed: Boolean,
        facingDegrees: Int? = null,
        mapCoordinate: MapCoordinateSaveDto,
        normalizedWidth: Double,
        normalizedHeight: Double,
        customProperties: Map<String, String>,
    ) : this(
        unitTemplateId = unitTemplateId,
        instanceName = instanceName,
        destroyed = destroyed,
        facingDegrees = facingDegrees,
        mapCoordinate = mapCoordinate,
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
    ) {
        customPropertiesByKey.putAll(customProperties.filterKeys { it !in KNOWN_PROPERTIES })
    }

    @get:JsonIgnore
    val customProperties: Map<String, String>
        get() = customPropertiesByKey.filterKeys { it !in KNOWN_PROPERTIES }

    @JsonAnySetter
    fun setCustomProperty(key: String, value: JsonNode?) {
        if (key !in KNOWN_PROPERTIES) {
            customPropertiesByKey[key] = CustomPropertyJson.toStringValue(value)
        }
    }

    @JsonAnyGetter
    fun customPropertiesForSerialization(): Map<String, String> =
        customProperties

    private companion object {
        val KNOWN_PROPERTIES = setOf(
            "type",
            "unitTemplateId",
            "instanceName",
            "destroyed",
            "facingDegrees",
            "mapCoordinate",
            "normalizedWidth",
            "normalizedHeight",
        )
    }
}

internal data class SavedDrawingObjectDto(
    val drawing: DrawingSaveDto,
) : SavedMapObjectDto

internal data class UnitTemplateSnapshotSaveDto(
    val id: UUID,
    val template: UnitTemplateSaveDto,
    val index: Int,
)

internal class UnitTemplateSaveDto(
    val name: String,
    val assetId: String,
    val affiliation: UnitAffiliation? = null,
) {
    private val customPropertiesByKey = linkedMapOf<String, String>()

    constructor(
        name: String,
        assetId: String,
        affiliation: UnitAffiliation? = null,
        customProperties: Map<String, String>,
    ) : this(
        name = name,
        assetId = assetId,
        affiliation = affiliation,
    ) {
        customPropertiesByKey.putAll(customProperties.filterKeys { it !in KNOWN_PROPERTIES })
    }

    @get:JsonIgnore
    val customProperties: Map<String, String>
        get() = customPropertiesByKey.filterKeys { it !in KNOWN_PROPERTIES }

    @JsonAnySetter
    fun setCustomProperty(key: String, value: JsonNode?) {
        if (key !in KNOWN_PROPERTIES) {
            customPropertiesByKey[key] = CustomPropertyJson.toStringValue(value)
        }
    }

    @JsonAnyGetter
    fun customPropertiesForSerialization(): Map<String, String> =
        customProperties

    private companion object {
        val KNOWN_PROPERTIES = setOf("name", "assetId", "affiliation")
    }
}

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
