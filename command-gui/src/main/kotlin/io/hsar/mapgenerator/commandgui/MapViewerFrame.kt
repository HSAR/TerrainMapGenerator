package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.korge.scene.MapViewerScene

class MapViewerFrame(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings = MapViewerSettings(),
) {

    fun showWindow() {
        MapViewerScene(renderMap, initialSettings).showWindow()
    }

}
