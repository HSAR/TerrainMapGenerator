package io.hsar.mapgenerator.commandgui.korge.adapter

import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import korlibs.math.geom.Vector2D

internal fun CameraViewport.toSavedCamera(geometry: ViewerGeometry, mapWidth: Int, mapHeight: Int) =
    MapCoordinateSpace(geometry, mapWidth, mapHeight)
        .toMapCoordinate(Vector2D(centerX, centerY))
        .let { center ->
            MapViewerSavedCamera(
                x = center.x,
                y = center.y,
                zoom = zoom,
            )
        }

internal fun MapViewerSavedCamera.toViewport(
    geometry: ViewerGeometry,
    mapWidth: Int,
    mapHeight: Int,
    viewportWidth: Int,
    viewportHeight: Int,
) = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    .toWorld(MapCoordinate(x = x, y = y))
    .let { worldCenter ->
        CameraViewport(
            centerX = worldCenter.x,
            centerY = worldCenter.y,
            zoom = zoom,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            worldWidth = geometry.backgroundWidth,
            worldHeight = geometry.backgroundHeight,
        )
    }

internal fun CameraViewport.toMapViewport() = MapViewport(
    center = ViewerPoint(centerX, centerY),
    zoom = zoom,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
    worldWidth = worldWidth.toDouble(),
    worldHeight = worldHeight.toDouble(),
)
