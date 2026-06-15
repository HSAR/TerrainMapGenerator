package io.hsar.mapgenerator.commandgui

data class MapViewerSettings(
    val layer: MapLayer = MapLayer.GRAPH,
    val metresPerPixel: Double = 2.0,
    val metresPerContour: Double = 10.0,
    val width: Int = 1920,
    val height: Int = 1080,
) {
    fun windowTitle(): String = "Terrain Map Viewer - ${layer.displayName} - ${width}x${height}px"
}
