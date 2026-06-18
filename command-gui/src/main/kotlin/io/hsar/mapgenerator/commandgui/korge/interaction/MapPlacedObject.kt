package io.hsar.mapgenerator.commandgui.korge.interaction

internal data class MapPlacedObjectMenuAction(
    val label: String,
    val onSelected: () -> Unit,
)

internal interface MapPlacedObject {
    fun containsMapCoordinate(
        mapCoordinate: MapCoordinate,
        tolerance: Double = 0.0,
        coordinateSpace: MapCoordinateSpace? = null,
    ): Boolean

    fun contextMenuActions(): List<MapPlacedObjectMenuAction>

    fun moveBy(mapDelta: MapCoordinateDelta)
}
