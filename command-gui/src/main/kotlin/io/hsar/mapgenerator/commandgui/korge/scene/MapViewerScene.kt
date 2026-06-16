package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerInteraction
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import java.awt.Toolkit
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Camera
import korlibs.korge.view.camera
import korlibs.korge.view.clipContainer
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.math.geom.Size2D
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

internal class MapViewerScene(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings,
) {
    private val renderer = MapViewerRenderer()

    @OptIn(KorgeExperimental::class)
    fun showWindow() {
        thread(name = "command-gui-korge", isDaemon = false) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val renderData = renderMap(initialSettings)
            val renderedMap = renderer.render(renderData)

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    title = initialSettings.windowTitle(),
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    lateinit var world: Camera
                    var gridOverlay: GridOverlayView? = null

                    solidRect(gameWindow.width, gameWindow.height, Colors.BLACK)
                    clipContainer(Size2D(gameWindow.width, gameWindow.height)) {
                        world = camera {
                            solidRect(renderedMap.geometry.backgroundWidth, renderedMap.geometry.backgroundHeight, Colors.BLACK)

                            image(renderedMap.bitmap) {
                                x = renderedMap.geometry.mapOffsetX.toDouble()
                                y = renderedMap.geometry.mapOffsetY.toDouble()
                            }
                        }
                    }

                    gridOverlay = renderData.gridLayer?.let { layer ->
                        GridOverlayView(layer).also { addChild(it) }
                    }

                    MapViewerInteraction(
                        root = views.stage,
                        camera = world,
                        geometry = renderedMap.geometry,
                        viewportWidth = gameWindow.width,
                        viewportHeight = gameWindow.height,
                        mapWidth = renderedMap.bitmap.width,
                        mapHeight = renderedMap.bitmap.height,
                        gridLayer = renderData.gridLayer,
                        onOverlayStateChanged = { state ->
                            gridOverlay?.update(state)
                        },
                    ).install()
                }
            }
        }
    }
}
