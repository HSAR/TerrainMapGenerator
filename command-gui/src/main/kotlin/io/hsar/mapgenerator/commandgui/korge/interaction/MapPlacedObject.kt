package io.hsar.mapgenerator.commandgui.korge.interaction

internal interface MapPlacedObject : MapContextMenuTarget {
    val id: MapPlacedObjectId

    fun snapshot(): MapPlacedObjectSnapshot

    fun containsMapCoordinate(
        mapCoordinate: MapCoordinate,
        tolerance: Double = 0.0,
        coordinateSpace: MapCoordinateSpace? = null,
    ): Boolean

    fun moveBy(mapDelta: MapCoordinateDelta)
}
