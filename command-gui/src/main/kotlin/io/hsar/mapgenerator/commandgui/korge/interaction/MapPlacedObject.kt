package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32

internal interface MapPlacedObject : MapContextMenuTarget {
    val id: MapPlacedObjectId

    val name: String
        get() = snapshot().name

    val icon: Bitmap32
        get() = snapshot().icon

    fun snapshot(): MapPlacedObjectSnapshot

    fun containsMapCoordinate(
        mapCoordinate: MapCoordinate,
        tolerance: Double = 0.0,
        coordinateSpace: MapCoordinateSpace? = null,
    ): Boolean

    fun moveBy(mapDelta: MapCoordinateDelta)
}
