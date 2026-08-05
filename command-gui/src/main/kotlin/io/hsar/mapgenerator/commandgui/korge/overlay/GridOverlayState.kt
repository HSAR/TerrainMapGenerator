package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayRequest
import io.hsar.mapgenerator.commandgui.MapViewerGridReference
import io.hsar.mapgenerator.commandgui.MapViewerMapBounds
import io.hsar.mapgenerator.commandgui.MapViewerMeasurement
import io.hsar.mapgenerator.commandgui.MapViewerScreenBounds
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.roundToInt

internal data class GridOverlayState(
    val viewport: Rectangle,
    val mapWorldX: Double,
    val mapWorldY: Double,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val scale: Double,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val visibleCanvasLeft: Double,
    val visibleCanvasTop: Double,
    val visibleCanvasRight: Double,
    val visibleCanvasBottom: Double,
    val activeReference: MapViewerGridReference?,
    val measurement: MapViewerMeasurement?,
    val gridLayer: GridLayer,
)

internal fun GridOverlayState.toNeutralRequest(): MapViewerGridOverlayRequest {
    val viewportWidth = visibleCanvasRight.roundToInt().coerceAtLeast(1)
    val viewportHeight = visibleCanvasBottom.roundToInt().coerceAtLeast(1)
    val left = mapWorldX - mapOffsetX / scale
    val top = mapWorldY - mapOffsetY / scale
    return MapViewerGridOverlayRequest(
        gridLayer = gridLayer,
        viewport = MapViewport(
            center = ViewerPoint(left + viewportWidth / scale / 2.0, top + viewportHeight / scale / 2.0),
            zoom = scale,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            worldWidth = backgroundWidth.toDouble(),
            worldHeight = backgroundHeight.toDouble(),
        ),
        mapOffsetX = mapWorldX,
        mapOffsetY = mapWorldY,
        mapWidth = pixelWidth,
        mapHeight = pixelHeight,
        visibleBounds = MapViewerScreenBounds(visibleCanvasLeft, visibleCanvasTop, visibleCanvasRight, visibleCanvasBottom),
        visibleMapBounds = MapViewerMapBounds(viewport.topLeft.x, viewport.topLeft.y, viewport.bottomRight.x, viewport.bottomRight.y),
        activeReference = activeReference,
        measurement = measurement,
    )
}
