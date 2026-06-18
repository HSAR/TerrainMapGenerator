package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import java.awt.image.BufferedImage
import java.io.File

internal enum class MapViewerLaunchState {
    LAUNCHPAD,
    NEW_OPERATION,
    VIEWER,
}

internal data class OperationMapImage(
    val name: String,
    val imagePath: String,
    val image: BufferedImage,
)

internal data class NewOperationDraft(
    val operationName: String = "",
    val mapImage: OperationMapImage? = null,
    val pixelsPerMetre: Double = 20.0,
    val unitTemplates: List<MapViewerUnitTemplate> = emptyList(),
)

internal data class OperationLaunchConfiguration(
    val settings: MapViewerSettings,
    val unitTemplates: List<MapViewerUnitTemplate>,
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

internal object OperationLaunchConfigurationFactory {
    fun create(
        draft: NewOperationDraft,
        baseSettings: MapViewerSettings,
    ): OperationLaunchConfiguration {
        val mapImage = draft.mapImage
            ?: return OperationLaunchConfiguration(settings = baseSettings, unitTemplates = draft.unitTemplates)

        val staticLayer = StaticImageLayer(
            id = "operation-map-${File(mapImage.imagePath).absolutePath}",
            displayName = mapImage.name.ifBlank { File(mapImage.imagePath).name },
        ) to mapImage.image
        val availableLayers = baseSettings.availableLayers + staticLayer.first
        val layerImages = baseSettings.staticLayerImages + (staticLayer.first.id to staticLayer.second)

        return OperationLaunchConfiguration(
            settings = baseSettings.copy(
                operationName = draft.operationName.trim(),
                availableLayers = availableLayers,
                enabledLayerIds = baseSettings.enabledLayerIds + staticLayer.first.id,
                staticLayerImages = layerImages,
                width = mapImage.image.width,
                height = mapImage.image.height,
                metresPerPixel = draft.pixelsPerMetre,
            ),
            unitTemplates = draft.unitTemplates,
        )
    }
}
