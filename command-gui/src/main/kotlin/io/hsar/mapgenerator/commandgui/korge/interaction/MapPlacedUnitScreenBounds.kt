package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D

internal object MapPlacedUnitScreenBounds {
    fun boundsFor(
        unit: MapPlacedUnitSnapshot,
        viewport: CameraViewport,
        coordinateSpace: MapCoordinateSpace,
    ): ScreenBounds =
        coordinateSpace.toWorld(unit.mapCoordinate).let { worldPoint ->
            val worldWidth = coordinateSpace.toWorldWidth(unit.normalizedWidth)
            val worldHeight = coordinateSpace.toWorldHeight(unit.normalizedHeight)
            ScreenBounds(
                left = viewport.screenX(worldPoint.x) - worldWidth * viewport.zoom * 0.5,
                top = viewport.screenY(worldPoint.y) - worldHeight * viewport.zoom * 0.5,
                width = worldWidth * viewport.zoom,
                height = worldHeight * viewport.zoom,
            )
        }

    fun contains(
        point: Vector2D,
        unit: MapPlacedUnitSnapshot,
        viewport: CameraViewport,
        coordinateSpace: MapCoordinateSpace,
    ): Boolean =
        boundsFor(unit, viewport, coordinateSpace).contains(point)
}

internal data class MapPlacedUnitSnapshot(
    val mapCoordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
)

internal data class ScreenBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    val right: Double
        get() = left + width

    val bottom: Double
        get() = top + height

    fun contains(point: Vector2D): Boolean =
        point.x in left..right && point.y in top..bottom
}
