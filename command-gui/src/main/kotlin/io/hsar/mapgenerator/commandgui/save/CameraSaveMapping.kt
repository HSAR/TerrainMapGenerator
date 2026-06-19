package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import korlibs.math.geom.Vector2D

internal fun CameraViewport.toSaveCamera(geometry: ViewerGeometry, mapWidth: Int, mapHeight: Int) =
    MapCoordinateSpace(geometry, mapWidth, mapHeight)
        .toMapCoordinate(Vector2D(centerX, centerY))
        .let { center ->
            CameraSaveDto(
                x = center.x,
                y = center.y,
                zoom = zoom,
            )
        }

internal fun CameraSaveDto.toViewport(
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
