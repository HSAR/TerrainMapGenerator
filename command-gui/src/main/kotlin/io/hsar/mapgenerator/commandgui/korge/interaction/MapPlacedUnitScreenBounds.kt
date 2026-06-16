package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D

internal object MapPlacedUnitScreenBounds {
    fun boundsFor(
        unit: MapPlacedUnitSnapshot,
        viewport: CameraViewport,
    ): ScreenBounds =
        ScreenBounds(
            left = viewport.screenX(unit.mapPoint.x) - unit.screenWidth * 0.5,
            top = viewport.screenY(unit.mapPoint.y) - unit.screenHeight * 0.5,
            width = unit.screenWidth,
            height = unit.screenHeight,
        )

    fun contains(
        point: Vector2D,
        unit: MapPlacedUnitSnapshot,
        viewport: CameraViewport,
    ): Boolean =
        boundsFor(unit, viewport).contains(point)
}

internal data class MapPlacedUnitSnapshot(
    val mapPoint: Vector2D,
    val screenWidth: Double,
    val screenHeight: Double,
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
