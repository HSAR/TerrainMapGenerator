package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedUnitManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerInteraction
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSidebar
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Camera
import korlibs.korge.view.camera
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size2D
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit
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
            var currentViewport = CameraViewport.centeredOnMap(
                geometry = renderedMap.geometry,
                mapWidth = renderedMap.bitmap.width,
                mapHeight = renderedMap.bitmap.height,
                viewportWidth = screenSize.width,
                viewportHeight = screenSize.height,
            )

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    title = initialSettings.windowTitle(currentViewport.zoom),
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    lateinit var world: Camera
                    var gridLinesOverlay: GridOverlayLinesView? = null
                    lateinit var iconLayer: korlibs.korge.view.Container
                    lateinit var unitManager: MapPlacedUnitManager
                    var gridLabelsOverlay: GridOverlayView? = null

                    solidRect(gameWindow.width, gameWindow.height, Colors.BLACK)
                    clipContainer(Size2D(gameWindow.width, gameWindow.height)) {
                        world = camera {
                            solidRect(renderedMap.geometry.backgroundWidth, renderedMap.geometry.backgroundHeight, Colors.BLACK)

                            image(renderedMap.bitmap) {
                                x = renderedMap.geometry.mapOffsetX.toDouble()
                                y = renderedMap.geometry.mapOffsetY.toDouble()
                            }

                            val gridLineLayer = container()
                            gridLineLayer.x = renderedMap.geometry.mapOffsetX.toDouble()
                            gridLineLayer.y = renderedMap.geometry.mapOffsetY.toDouble()
                            gridLineLayer.zIndex = 1.0
                            renderData.gridLayer?.let { layer ->
                                gridLinesOverlay = GridOverlayLinesView(layer).also {
                                    gridLineLayer.addChild(it)
                                }
                            }

                            iconLayer = container()
                            iconLayer.zIndex = 2.0
                        }
                    }

                    unitManager = MapPlacedUnitManager(
                        root = views.stage,
                        unitLayer = iconLayer,
                        geometry = renderedMap.geometry,
                        mapMetresPerPixel = initialSettings.metresPerPixel,
                        mapWidth = renderedMap.bitmap.width,
                        mapHeight = renderedMap.bitmap.height,
                        viewportProvider = { currentViewport },
                    )
                    unitManager.install()

                    gridLabelsOverlay = renderData.gridLayer?.let { layer ->
                        GridOverlayView(layer).also {
                            it.zIndex = 25_000.0
                            addChild(it)
                        }
                    }
                    val sidebar = MapViewerSidebar(
                        root = views.stage,
                        viewportWidth = gameWindow.width,
                        viewportHeight = gameWindow.height,
                        onUnitDropped = unitManager::place,
                    )

                    MapViewerInteraction(
                        root = views.stage,
                        camera = world,
                        geometry = renderedMap.geometry,
                        viewportWidth = gameWindow.width,
                        viewportHeight = gameWindow.height,
                        mapWidth = renderedMap.bitmap.width,
                        mapHeight = renderedMap.bitmap.height,
                        gridLayer = renderData.gridLayer,
                        isScreenOverlayBlocking = { point ->
                            sidebar.blocksScreenPoint(point) ||
                                    sidebar.isUnitDragging ||
                                    sidebar.isUnitDialogOpen ||
                                    unitManager.isContextMenuOpen ||
                                    unitManager.blocksScreenPoint(point)
                        },
                        isMapPlacementDragging = unitManager::isDragging,
                        onViewportChanged = { currentViewport = it },
                        onTitleChanged = { gameWindow.title = it },
                        onOverlayStateChanged = { state ->
                            gridLinesOverlay?.update(state)
                            gridLabelsOverlay?.update(state)
                        },
                    ).install()
                }
            }
        }
    }
}
