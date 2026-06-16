package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import korlibs.math.geom.Vector2D

internal object MapViewerUnitPlacementResolver {
    fun resolve(
        screenPoint: Vector2D,
        viewport: CameraViewport,
        geometry: ViewerGeometry,
        mapWidth: Int,
        mapHeight: Int,
    ): Vector2D? {
        val bounds = mapScreenBounds(viewport, geometry, mapWidth, mapHeight)
        if (screenPoint.x !in bounds.left..bounds.right || screenPoint.y !in bounds.top..bounds.bottom) {
            return null
        }

        return toWorld(screenPoint, viewport, geometry, bounds.left, bounds.top)
    }

    fun resolveClamped(
        screenPoint: Vector2D,
        viewport: CameraViewport,
        geometry: ViewerGeometry,
        mapWidth: Int,
        mapHeight: Int,
    ): Vector2D {
        val bounds = mapScreenBounds(viewport, geometry, mapWidth, mapHeight)
        val clampedPoint = Vector2D(
            x = screenPoint.x.coerceIn(bounds.left, bounds.right),
            y = screenPoint.y.coerceIn(bounds.top, bounds.bottom),
        )

        return toWorld(clampedPoint, viewport, geometry, bounds.left, bounds.top)
    }

    private fun toWorld(
        screenPoint: Vector2D,
        viewport: CameraViewport,
        geometry: ViewerGeometry,
        mapLeft: Double,
        mapTop: Double,
    ): Vector2D =
        Vector2D(
            x = geometry.mapOffsetX + (screenPoint.x - mapLeft) / viewport.zoom,
            y = geometry.mapOffsetY + (screenPoint.y - mapTop) / viewport.zoom,
        )

    private fun mapScreenBounds(
        viewport: CameraViewport,
        geometry: ViewerGeometry,
        mapWidth: Int,
        mapHeight: Int,
    ): ScreenBounds =
        ScreenBounds(
            left = viewport.screenX(geometry.mapOffsetX.toDouble()),
            top = viewport.screenY(geometry.mapOffsetY.toDouble()),
            right = viewport.screenX(geometry.mapOffsetX.toDouble()) + mapWidth * viewport.zoom,
            bottom = viewport.screenY(geometry.mapOffsetY.toDouble()) + mapHeight * viewport.zoom,
        )

    private data class ScreenBounds(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double,
    )
}
