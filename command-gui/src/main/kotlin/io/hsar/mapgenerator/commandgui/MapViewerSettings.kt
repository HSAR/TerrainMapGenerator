package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.Coord
import io.hsar.mapgenerator.grid.ReferenceSystem
import java.awt.Color
import java.awt.image.BufferedImage

data class MapViewerSettings(
    val operationName: String? = null,
    val availableLayers: List<MapViewerLayer> = MapLayer.values().toList(),
    val enabledLayerIds: Set<String> = availableLayers.map { it.id }.toSet(),
    val staticLayerImages: Map<String, BufferedImage> = emptyMap(),
    val featureFlags: Set<MapViewerFeatureFlag> = emptySet(),
    val mapId: String? = null,
    val referenceSystem: ReferenceSystem = ReferenceSystem.MGRS,
    val gridStartX: Coord? = null,
    val gridStartY: Coord? = null,
    val gridColour: Color? = null,
    val zoom: Double = 1.0,
    val unitsPerPixel: Double = 20.0,
    val metresPerContour: Double = 10.0,
    val width: Int = 1920,
    val height: Int = 1080,
    val seed: Long = System.currentTimeMillis(),
) {
    fun windowTitle(): String =
        MapViewerWindowTitle.format(operationName?.takeIf { it.isNotBlank() } ?: "Launchpad")

    fun withZoom(factor: Double): MapViewerSettings =
        copy(zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM))

    companion object {
        const val MIN_ZOOM = 0.5
        const val MAX_ZOOM = 4.0
    }
}
