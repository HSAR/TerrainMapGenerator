package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.grid.GridLayerBuilder
import java.awt.image.BufferedImage
import java.io.File
import java.util.*

internal enum class MapViewerLaunchState {
    LAUNCHPAD,
    NEW_OPERATION,
    VIEWER,
}

internal data class OperationMapImage(
    val name: String,
    val imagePath: String,
    val image: BufferedImage,
    val assetSource: AssetSource? = null,
)

internal data class NewOperationDraft(
    val operationName: String = "",
    val mapImage: OperationMapImage? = null,
    val pixelsPerMetre: Double = 20.0,
    val gridConfiguration: GridConfiguration? = null,
    val unitTemplates: List<MapViewerUnitTemplate> = emptyList(),
)

internal data class GridConfiguration(
    val mapId: String,
    val startX: Double,
    val startY: Double,
)

internal data class OperationLaunchConfiguration(
    val settings: MapViewerSettings,
    val unitTemplates: List<MapViewerUnitTemplate>,
    val initialLedgerEntries: List<LedgerEntry>,
    val mapImage: OperationMapImage?,
)

internal class MapViewerLaunchFlow {
    var state = MapViewerLaunchState.LAUNCHPAD
        private set

    fun createNewOperation() {
        state = MapViewerLaunchState.NEW_OPERATION
    }

    fun returnToLaunchpad() {
        state = MapViewerLaunchState.LAUNCHPAD
    }

    fun launchExistingOperation() {
    }

    fun launchNewOperation(draft: NewOperationDraft): Boolean =
        draft.isLaunchable().also { launchable ->
            if (launchable) {
                state = MapViewerLaunchState.VIEWER
            }
        }
}

internal fun NewOperationDraft.isLaunchable(): Boolean =
    operationName.isNotBlank() && mapImage != null && pixelsPerMetre > 0.0

internal fun defaultGridConfiguration(
    mapImage: BufferedImage,
    metresPerPixel: Double,
    seed: Long,
): GridConfiguration =
    GridLayerBuilder.build(
        width = mapImage.width,
        height = mapImage.height,
        gridScale = 1_000,
        metresPerPixel = metresPerPixel,
        random = Random(seed),
    ).let { gridLayer ->
        GridConfiguration(
            mapId = gridLayer.mapId,
            startX = gridLayer.verticalLineNumberStart,
            startY = gridLayer.horizontalLineNumberStart,
        )
    }

internal object OperationLaunchConfigurationFactory {
    fun create(
        draft: NewOperationDraft,
        baseSettings: MapViewerSettings,
    ): OperationLaunchConfiguration {
        val mapImage = draft.mapImage
            ?: return OperationLaunchConfiguration(
                settings = baseSettings,
                unitTemplates = draft.unitTemplates,
                initialLedgerEntries = draft.seededUnitTemplateEntries() + AddTurnLedgerEntry("Operation Start"),
                mapImage = null,
            )

        val staticLayer = StaticImageLayer(
            id = "operation-map-${File(mapImage.imagePath).absolutePath}",
            displayName = mapImage.name.ifBlank { File(mapImage.imagePath).name },
        ) to mapImage.image
        val availableLayers = baseSettings.availableLayers + staticLayer.first
        val layerImages = baseSettings.staticLayerImages + (staticLayer.first.id to staticLayer.second)
        val gridConfiguration = draft.gridConfiguration
            ?: defaultGridConfiguration(mapImage.image, draft.pixelsPerMetre, baseSettings.seed)

        return OperationLaunchConfiguration(
            settings = baseSettings.copy(
                operationName = draft.operationName.trim(),
                availableLayers = availableLayers,
                enabledLayerIds = baseSettings.enabledLayerIds + staticLayer.first.id,
                staticLayerImages = layerImages,
                width = mapImage.image.width,
                height = mapImage.image.height,
                metresPerPixel = draft.pixelsPerMetre,
                mapId = gridConfiguration.mapId,
                gridStartX = gridConfiguration.startX,
                gridStartY = gridConfiguration.startY,
            ),
            unitTemplates = draft.unitTemplates,
            initialLedgerEntries = draft.seededUnitTemplateEntries() + AddTurnLedgerEntry("Operation Start"),
            mapImage = mapImage,
        )
    }
}

private fun NewOperationDraft.seededUnitTemplateEntries(): List<LedgerEntry> =
    unitTemplates.mapIndexed { index, template ->
        CreateUnitTemplateAction(
            UnitTemplateSnapshot(
                id = template.id,
                template = template,
                index = index,
            ),
        )
    }
