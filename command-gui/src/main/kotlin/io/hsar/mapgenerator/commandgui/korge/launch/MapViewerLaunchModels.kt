package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.GridColourSelector
import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.OPERATION_START_TURN_NAME
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.grid.Coord
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.grid.ReferenceSystem
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
    val unitsPerPixel: Double = 20.0,
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val gridConfiguration: GridConfiguration? = null,
    val unitTemplates: List<MapViewerUnitTemplate> = emptyList(),
)

internal data class GridConfiguration(
    val mapId: String,
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val startX: Coord,
    val startY: Coord,
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
    operationName.isNotBlank() && mapImage != null && unitsPerPixel > 0.0 && gridConfiguration != null

internal object OperationNameGenerator {
    private val adjectives = listOf(
        "Amber",
        "Argent",
        "Aureate",
        "Azure",
        "Aquiline",
        "Brass",
        "Coronal",
        "Corvine",
        "Forgotten",
        "Forsaken",
        "Onyx",
        "Penumbral",
        "Rampant",
        "Roseate",
        "Raven",
        "Sable",
        "Sanguine",
        "Stalwart",
        "Steel",
    )
    private val nouns = listOf(
        "Bastion",
        "Blaze",
        "Citadel",
        "Dawn",
        "Ember",
        "Frontier",
        "Gateway",
        "Horizon",
        "Lance",
        "Meridian",
        "Night",
        "Sentinel",
        "Shield",
        "Summit",
        "Tempest",
        "Twilight",
        "Vanguard",
    )

    fun generate(random: Random = Random()): String =
        "${adjectives[random.nextInt(adjectives.size)]} ${nouns[random.nextInt(nouns.size)]}"
}

internal fun defaultGridConfiguration(
    mapImage: BufferedImage,
    unitsPerPixel: Double,
    seed: Long,
    referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
): GridConfiguration =
    defaultGridConfiguration(mapImage.width, mapImage.height, unitsPerPixel, seed, referenceSystem)

internal fun defaultGridConfiguration(
    width: Int,
    height: Int,
    unitsPerPixel: Double,
    seed: Long,
    referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
): GridConfiguration =
    GridLayerBuilder.build(
        width = width,
        height = height,
        gridScale = referenceSystem.gridScale,
        unitsPerPixel = unitsPerPixel,
        referenceSystem = referenceSystem,
        random = Random(seed),
    ).let { gridLayer ->
        GridConfiguration(
            mapId = gridLayer.mapId,
            referenceSystem = gridLayer.referenceSystem,
            startX = gridLayer.verticalLineNumberStart,
            startY = gridLayer.horizontalLineNumberStart,
        )
    }

internal fun previewUnitsPerPixel(unitsPerPixel: Double, previewScale: Double): Double {
    require(previewScale > 0.0) { "Preview scale must be positive." }
    return unitsPerPixel / previewScale
}

internal fun translateGridConfiguration(
    gridConfiguration: GridConfiguration,
    mapId: String,
    referenceSystem: ReferenceSystem,
): GridConfiguration =
    GridConfiguration(
        mapId = mapId,
        referenceSystem = referenceSystem,
        startX = referenceSystem.coordFromLineNumber(
            lineNumber = gridConfiguration.startX.lineNumber(gridConfiguration.referenceSystem.gridScale),
            gridScale = referenceSystem.gridScale,
        ),
        startY = referenceSystem.coordFromLineNumber(
            lineNumber = gridConfiguration.startY.lineNumber(gridConfiguration.referenceSystem.gridScale),
            gridScale = referenceSystem.gridScale,
        ),
    )

internal object OperationLaunchConfigurationFactory {
    fun create(
        draft: NewOperationDraft,
        baseSettings: MapViewerSettings,
    ): OperationLaunchConfiguration {
        val mapImage = draft.mapImage
            ?: return OperationLaunchConfiguration(
                settings = baseSettings,
                unitTemplates = draft.unitTemplates,
                initialLedgerEntries = draft.seededUnitTemplateEntries() + AddTurnLedgerEntry(OPERATION_START_TURN_NAME),
                mapImage = null,
            )

        val staticLayer = StaticImageLayer(
            id = "operation-map-${File(mapImage.imagePath).absolutePath}",
            displayName = mapImage.name.ifBlank { File(mapImage.imagePath).name },
        ) to mapImage.image
        val availableLayers = baseSettings.availableLayers + staticLayer.first
        val layerImages = baseSettings.staticLayerImages + (staticLayer.first.id to staticLayer.second)
        val gridConfiguration = draft.gridConfiguration
            ?: defaultGridConfiguration(mapImage.image, draft.unitsPerPixel, baseSettings.seed, draft.referenceSystem)

        return OperationLaunchConfiguration(
            settings = baseSettings.copy(
                operationName = draft.operationName.trim(),
                availableLayers = availableLayers,
                enabledLayerIds = setOf(staticLayer.first.id, MapLayer.GRID.id),
                staticLayerImages = layerImages,
                width = mapImage.image.width,
                height = mapImage.image.height,
                unitsPerPixel = draft.unitsPerPixel,
                mapId = gridConfiguration.mapId,
                referenceSystem = gridConfiguration.referenceSystem,
                gridStartX = gridConfiguration.startX,
                gridStartY = gridConfiguration.startY,
                gridColour = GridColourSelector.bestPresetFor(mapImage.image),
            ),
            unitTemplates = draft.unitTemplates,
            initialLedgerEntries = draft.seededUnitTemplateEntries() + AddTurnLedgerEntry(OPERATION_START_TURN_NAME),
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
