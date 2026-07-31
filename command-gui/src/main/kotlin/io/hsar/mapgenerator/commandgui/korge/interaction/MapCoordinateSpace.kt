package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.toMapPixels
import io.hsar.mapgenerator.commandgui.toWorldPoint
import korlibs.math.geom.Vector2D
import io.hsar.mapgenerator.commandgui.MapCoordinate as SharedMapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds as SharedMapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapCoordinateDelta as SharedMapCoordinateDelta

internal typealias MapCoordinate = SharedMapCoordinate
internal typealias MapCoordinateDelta = SharedMapCoordinateDelta
internal typealias MapCoordinateBounds = SharedMapCoordinateBounds

internal class MapCoordinateSpace(
    private val geometry: ViewerGeometry,
    private val mapWidth: Int,
    private val mapHeight: Int,
) {
    val aspectRatio = mapWidth.toDouble() / mapHeight.toDouble()

    val mapBounds = MapCoordinateBounds(
        left = 0.0,
        top = 0.0,
        right = 1.0,
        bottom = 1.0,
    )

    val backgroundBounds = MapCoordinateBounds(
        left = -geometry.mapOffsetX.toDouble() / mapWidth,
        top = -geometry.mapOffsetY.toDouble() / mapHeight,
        right = (geometry.backgroundWidth - geometry.mapOffsetX).toDouble() / mapWidth,
        bottom = (geometry.backgroundHeight - geometry.mapOffsetY).toDouble() / mapHeight,
    )

    fun toWorld(point: MapCoordinate) = point.toWorldPoint(
        mapOffsetX = geometry.mapOffsetX.toDouble(),
        mapOffsetY = geometry.mapOffsetY.toDouble(),
        mapWidth = mapWidth,
        mapHeight = mapHeight,
    ).let { worldPoint -> Vector2D(worldPoint.x, worldPoint.y) }

    fun toMapCoordinate(worldPoint: Vector2D) = MapCoordinate(
        x = (worldPoint.x - geometry.mapOffsetX) / mapWidth,
        y = (worldPoint.y - geometry.mapOffsetY) / mapHeight,
    )

    fun toMapDelta(worldDelta: Vector2D) = MapCoordinateDelta(
        x = worldDelta.x / mapWidth,
        y = worldDelta.y / mapHeight,
    )

    fun toWorld(delta: MapCoordinateDelta) = Vector2D(
        x = delta.x * mapWidth,
        y = delta.y * mapHeight,
    )

    fun toMapPixels(point: MapCoordinate) = point.toMapPixels(mapWidth, mapHeight)
        .let { mapPixels -> Vector2D(mapPixels.x, mapPixels.y) }

    fun fromMapPixels(point: Vector2D) = MapCoordinate(
        x = point.x / mapWidth,
        y = point.y / mapHeight,
    )

    fun toWorldWidth(normalizedWidth: Double) =
        normalizedWidth * mapWidth

    fun toWorldHeight(normalizedHeight: Double) =
        normalizedHeight * mapHeight

    fun circleRadiusPixels(normalizedRadius: Double) =
        normalizedRadius * mapHeight

    fun screenToWorld(screenPoint: Vector2D, viewport: CameraViewport) = Vector2D(
        x = viewport.left + screenPoint.x / viewport.zoom,
        y = viewport.top + screenPoint.y / viewport.zoom,
    )

    fun worldToScreen(worldPoint: Vector2D, viewport: CameraViewport) = Vector2D(
        x = viewport.screenX(worldPoint.x),
        y = viewport.screenY(worldPoint.y),
    )

    fun screenToMap(screenPoint: Vector2D, viewport: CameraViewport) =
        toMapCoordinate(screenToWorld(screenPoint, viewport))

    fun resolveScreenPoint(
        screenPoint: Vector2D,
        viewport: CameraViewport,
        bounds: MapCoordinateBounds = mapBounds,
    ) = screenToMap(screenPoint, viewport)
        .takeIf(bounds::contains)

    fun resolveScreenPointClamped(
        screenPoint: Vector2D,
        viewport: CameraViewport,
        bounds: MapCoordinateBounds = mapBounds,
    ) = bounds.clamp(screenToMap(screenPoint, viewport))

    fun screenToleranceToMapTolerance(screenTolerance: Double, viewport: CameraViewport) =
        screenTolerance / (viewport.zoom.coerceAtLeast(0.0001) * minOf(mapWidth, mapHeight))
}
