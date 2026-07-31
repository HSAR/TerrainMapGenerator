package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerReplay
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.toWorldPoint

/** Temporary support for the unused pre-operation-document renderer while it is removed incrementally. */
internal fun MapViewerLedgerReplay.objectAtScreen(
    screenPoint: ViewerPoint,
    viewport: MapViewport,
    mapOffsetX: Double,
    mapOffsetY: Double,
    mapWidth: Int,
    mapHeight: Int,
    hitRadius: Double = 12.0,
) = objects.asReversed().firstOrNull { mapObject ->
    mapObject.x?.let { mapX ->
        mapObject.y?.let { mapY ->
            MapCoordinate(mapX, mapY).toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight).let { worldPoint ->
                val x = viewport.screenX(worldPoint.x) - screenPoint.x
                val y = viewport.screenY(worldPoint.y) - screenPoint.y
                x * x + y * y <= hitRadius * hitRadius
            }
        }
    } == true
}
