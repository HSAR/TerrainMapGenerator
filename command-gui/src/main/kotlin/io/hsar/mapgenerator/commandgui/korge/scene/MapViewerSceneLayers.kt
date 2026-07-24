package io.hsar.mapgenerator.commandgui.korge.scene

import korlibs.korge.view.Container
import korlibs.korge.view.container

internal class MapViewerSceneLayers private constructor(
    val background: Container,
    val map: Container,
    val overlay: Container,
    val uiUnderlay: Container,
    val ui: Container,
) {
    fun destroy() {
        ui.parent?.removeChild(ui)
        uiUnderlay.parent?.removeChild(uiUnderlay)
        overlay.parent?.removeChild(overlay)
        map.parent?.removeChild(map)
        background.parent?.removeChild(background)
    }

    companion object {
        fun create(root: Container, viewportWidth: Int, viewportHeight: Int): MapViewerSceneLayers {
            val background = root.container().also { it.zIndex = BACKGROUND_Z_INDEX }
            val map = root.container().also { it.zIndex = MAP_Z_INDEX }
            val overlay = root.container().also {
                it.zIndex = OVERLAY_Z_INDEX
                it.mouseEnabled = false
            }
            val uiUnderlay = root.container().also {
                it.zIndex = UI_UNDERLAY_Z_INDEX
                it.mouseEnabled = false
                it.name = UI_UNDERLAY_NAME
            }
            val ui = root.container().also { it.zIndex = UI_Z_INDEX }

            return MapViewerSceneLayers(
                background = background,
                map = map,
                overlay = overlay,
                uiUnderlay = uiUnderlay,
                ui = ui,
            )
        }

        private const val BACKGROUND_Z_INDEX = 1.0
        private const val MAP_Z_INDEX = 10.0
        private const val OVERLAY_Z_INDEX = 100.0
        private const val UI_UNDERLAY_Z_INDEX = 500.0
        private const val UI_Z_INDEX = 1_000.0
        const val UI_UNDERLAY_NAME = "map-viewer-ui-underlay"
    }
}
