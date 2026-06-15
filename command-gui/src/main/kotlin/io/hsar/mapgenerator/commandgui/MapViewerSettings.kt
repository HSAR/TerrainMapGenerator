package io.hsar.mapgenerator.commandgui

data class MapViewerSettings(
    val enabledLayers: Set<MapLayer> = MapLayer.values().toSet(),
    val zoom: Double = 1.0,
    val metresPerPixel: Double = 2.0,
    val metresPerContour: Double = 10.0,
    val width: Int = 1920,
    val height: Int = 1080,
    val seed: Long = System.currentTimeMillis(),
) {
    fun windowTitle(): String = "Terrain Map Viewer - ${width}x${height}px - ${formatZoom(zoom)}"

    fun withZoom(factor: Double): MapViewerSettings =
        copy(zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM))

    companion object {
        const val MIN_ZOOM = 0.5
        const val MAX_ZOOM = 4.0

        private fun formatZoom(value: Double) = "${(value * 100).toInt()}%"
    }
}
