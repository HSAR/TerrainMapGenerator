package io.hsar.mapgenerator.commandgui

/**
 * Engine-neutral camera for a 2D map world. Screen deltas are expressed in physical pixels and
 * world coordinates are expressed in source-image pixels.
 */
data class MapViewport(
    val center: ViewerPoint,
    val zoom: Double,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val worldWidth: Double,
    val worldHeight: Double,
) {
    val visibleWidth: Double get() = viewportWidth / zoom
    val visibleHeight: Double get() = viewportHeight / zoom
    val left: Double get() = center.x - visibleWidth / 2.0
    val top: Double get() = center.y - visibleHeight / 2.0

    fun resized(width: Int, height: Int): MapViewport =
        copy(viewportWidth = width.coerceAtLeast(1), viewportHeight = height.coerceAtLeast(1)).clamped()

    fun pan(screenDelta: ViewerPoint): MapViewport =
        copy(
            center = ViewerPoint(
                center.x - screenDelta.x / zoom,
                center.y - screenDelta.y / zoom,
            ),
        ).clamped()

    fun zoomAt(screenPosition: ViewerPoint, factor: Double): MapViewport {
        val nextZoom = (zoom * factor).coerceIn(MapViewerSettings.MIN_ZOOM, MapViewerSettings.MAX_ZOOM)
        val anchorWorldX = left + screenPosition.x / zoom
        val anchorWorldY = top + screenPosition.y / zoom
        val nextLeft = anchorWorldX - screenPosition.x / nextZoom
        val nextTop = anchorWorldY - screenPosition.y / nextZoom

        return copy(
            center = ViewerPoint(
                nextLeft + viewportWidth / nextZoom / 2.0,
                nextTop + viewportHeight / nextZoom / 2.0,
            ),
            zoom = nextZoom,
        ).clamped()
    }

    fun screenX(worldX: Double): Double = (worldX - left) * zoom
    fun screenY(worldY: Double): Double = (worldY - top) * zoom

    private fun clamped(): MapViewport =
        copy(
            center = ViewerPoint(
                clampCenter(center.x, visibleWidth, worldWidth),
                clampCenter(center.y, visibleHeight, worldHeight),
            ),
        )

    private fun clampCenter(candidate: Double, visibleSize: Double, worldSize: Double): Double =
        if (visibleSize >= worldSize) worldSize / 2.0
        else candidate.coerceIn(visibleSize / 2.0, worldSize - visibleSize / 2.0)

    companion object {
        fun centered(
            worldWidth: Double,
            worldHeight: Double,
            viewportWidth: Int = 1,
            viewportHeight: Int = 1,
            zoom: Double = 1.0,
        ) = MapViewport(
            center = ViewerPoint(worldWidth / 2.0, worldHeight / 2.0),
            zoom = zoom,
            viewportWidth = viewportWidth.coerceAtLeast(1),
            viewportHeight = viewportHeight.coerceAtLeast(1),
            worldWidth = worldWidth,
            worldHeight = worldHeight,
        ).clamped()
    }
}
