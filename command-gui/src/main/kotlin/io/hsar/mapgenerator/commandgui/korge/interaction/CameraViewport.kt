package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import korlibs.math.geom.Rectangle
import korlibs.math.geom.Vector2D

internal data class CameraViewport(
    val centerX: Double,
    val centerY: Double,
    val zoom: Double,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val worldWidth: Int,
    val worldHeight: Int,
) {
    val visibleWidth: Double
        get() = viewportWidth / zoom

    val visibleHeight: Double
        get() = viewportHeight / zoom

    val left: Double
        get() = centerX - visibleWidth / 2

    val top: Double
        get() = centerY - visibleHeight / 2

    val right: Double
        get() = centerX + visibleWidth / 2

    val bottom: Double
        get() = centerY + visibleHeight / 2

    fun pan(screenDelta: Vector2D): CameraViewport =
        copy(
            centerX = centerX - screenDelta.x / zoom,
            centerY = centerY - screenDelta.y / zoom,
        ).clamped()

    fun zoomAt(screenPosition: Vector2D, factor: Double): CameraViewport {
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val anchorWorldX = left + screenPosition.x / zoom
        val anchorWorldY = top + screenPosition.y / zoom
        val newVisibleWidth = viewportWidth / newZoom
        val newVisibleHeight = viewportHeight / newZoom
        val newLeft = anchorWorldX - screenPosition.x / newZoom
        val newTop = anchorWorldY - screenPosition.y / newZoom

        return copy(
            centerX = newLeft + newVisibleWidth / 2,
            centerY = newTop + newVisibleHeight / 2,
            zoom = newZoom,
        ).clamped()
    }

    fun toCameraRectangle(): Rectangle =
        Rectangle(left, top, visibleWidth, visibleHeight)

    fun screenX(worldX: Double): Double =
        (worldX - left) * zoom

    fun screenY(worldY: Double): Double =
        (worldY - top) * zoom

    private fun clamped(): CameraViewport =
        copy(
            centerX = clampCenter(centerX, visibleWidth, worldWidth.toDouble()),
            centerY = clampCenter(centerY, visibleHeight, worldHeight.toDouble()),
        )

    private fun clampCenter(candidate: Double, visibleSize: Double, worldSize: Double): Double =
        if (visibleSize >= worldSize) {
            worldSize / 2
        } else {
            candidate.coerceIn(visibleSize / 2, worldSize - visibleSize / 2)
        }

    companion object {
        fun centeredOnMap(
            geometry: ViewerGeometry,
            mapWidth: Int,
            mapHeight: Int,
            viewportWidth: Int,
            viewportHeight: Int,
        ): CameraViewport =
            CameraViewport(
                centerX = geometry.mapOffsetX + mapWidth / 2.0,
                centerY = geometry.mapOffsetY + mapHeight / 2.0,
                zoom = 1.0,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                worldWidth = geometry.backgroundWidth,
                worldHeight = geometry.backgroundHeight,
            ).clamped()

        private const val MIN_ZOOM = 0.5
        private const val MAX_ZOOM = 4.0
    }
}
