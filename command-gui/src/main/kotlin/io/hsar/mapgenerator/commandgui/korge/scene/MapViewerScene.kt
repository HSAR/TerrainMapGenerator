package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingManager
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingMode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingView
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapActionHistory
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapObjectController
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedUnitManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerInteraction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.MoveObjectAction
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchFlow
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchpadView
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerNewOperationView
import io.hsar.mapgenerator.commandgui.korge.launch.NewOperationDraft
import io.hsar.mapgenerator.commandgui.korge.launch.OperationLaunchConfigurationFactory
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSidebar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerToolbar
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
    private val initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
) {
    private val renderer = MapViewerRenderer()

    @OptIn(KorgeExperimental::class)
    fun showWindow() {
        thread(name = "command-gui-korge", isDaemon = false) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    title = "Operational Command Tool - Launchpad",
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    val launchFlow = MapViewerLaunchFlow()
                    var launchpadView: MapViewerLaunchpadView? = null
                    var newOperationView: MapViewerNewOperationView? = null
                    lateinit var showLaunchpad: () -> Unit
                    lateinit var showNewOperation: () -> Unit

                    fun showViewer(settings: MapViewerSettings, initialUnitTemplates: List<MapViewerUnitTemplate>) {
                        val renderData = renderMap(settings)
                        val renderedMap = renderer.render(renderData)
                        var currentViewport = CameraViewport.centeredOnMap(
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                        )
                        gameWindow.title = settings.windowTitle(currentViewport.zoom)

                        var currentTool = MapViewerTool.SELECT
                        var currentDrawingMode = MapDrawingMode.FREEFORM
                        lateinit var world: Camera
                        var gridLinesOverlay: GridOverlayLinesView? = null
                        val drawingStore = MapDrawingStore()
                        lateinit var contextMenuManager: MapPlacedObjectContextMenuManager
                        lateinit var drawingManager: MapDrawingManager
                        lateinit var drawingView: MapDrawingView
                        lateinit var iconLayer: korlibs.korge.view.Container
                        lateinit var unitManager: MapPlacedUnitManager
                        lateinit var objectController: MapObjectController
                        val actionHistory = MapActionHistory()
                        var gridLabelsOverlay: GridOverlayView? = null
                        val coordinateSpace = MapCoordinateSpace(renderedMap.geometry, renderedMap.bitmap.width, renderedMap.bitmap.height)

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

                                val drawingLayer = container()
                                drawingLayer.zIndex = 1.5
                                drawingView = MapDrawingView(coordinateSpace).also {
                                    drawingLayer.addChild(it)
                                }

                                iconLayer = container()
                                iconLayer.zIndex = 2.0
                            }
                        }

                        contextMenuManager = MapPlacedObjectContextMenuManager(views.stage)

                        unitManager = MapPlacedUnitManager(
                            root = views.stage,
                            unitLayer = iconLayer,
                            geometry = renderedMap.geometry,
                            mapMetresPerPixel = settings.metresPerPixel,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            gridLayer = renderData.gridLayer,
                            viewportProvider = { currentViewport },
                            contextMenuManager = contextMenuManager,
                            canInteractWithUnits = { currentTool == MapViewerTool.SELECT },
                            onObjectMoved = { before, after ->
                                actionHistory.record(MoveObjectAction(before, after))
                            },
                            onObjectDeleted = { snapshot ->
                                objectController.deleteObject(snapshot.id)
                                actionHistory.record(DeleteObjectAction(snapshot))
                            },
                            onUnitEdited = { before, after ->
                                actionHistory.record(EditUnitAction(before, after))
                            },
                        )
                        unitManager.install()

                        drawingManager = MapDrawingManager(
                            root = views.stage,
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportProvider = { currentViewport },
                            currentTool = { currentTool },
                            drawingStore = drawingStore,
                            contextMenuManager = contextMenuManager,
                            canInteractWithDrawings = { currentTool == MapViewerTool.SELECT },
                            onDrawingsChanged = { state ->
                                drawingView.update(state, currentViewport.zoom)
                            },
                            onObjectMoved = { before, after ->
                                actionHistory.record(MoveObjectAction(before, after))
                            },
                            onObjectDeleted = { snapshot ->
                                objectController.deleteObject(snapshot.id)
                                actionHistory.record(DeleteObjectAction(snapshot))
                            },
                        )
                        drawingManager.install()

                        objectController = MapObjectController(
                            unitManager = unitManager,
                            drawingStore = drawingStore,
                            onDrawingsChanged = { state ->
                                drawingView.update(state, currentViewport.zoom)
                            },
                            clearTransientState = {
                                contextMenuManager.close()
                                unitManager.cancelTransientState()
                                drawingManager.cancelTransientState()
                                drawingStore.clearPreview()
                                drawingView.update(drawingStore.state, currentViewport.zoom)
                            },
                        )

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
                            onUnitDropped = { template, screenPoint ->
                                unitManager.resolvePlacement(screenPoint)
                                    ?.let { mapCoordinate ->
                                        objectController.createUnit(template, mapCoordinate)
                                            .also { actionHistory.record(CreateObjectAction(it)) }
                                    }
                            },
                            canStartUnitDrag = { currentTool == MapViewerTool.SELECT },
                            initialUnitTemplates = initialUnitTemplates,
                        )
                        val toolbar = MapViewerToolbar(
                            root = views.stage,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            selectedTool = { currentTool },
                            selectedDrawingMode = { currentDrawingMode },
                            onToolSelected = {
                                currentTool = it
                                if (it != MapViewerTool.DRAW) {
                                    currentDrawingMode = MapDrawingMode.FREEFORM
                                }
                            },
                            onDrawingModeSelected = { currentDrawingMode = it },
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
                            currentTool = { currentTool },
                            currentDrawingMode = { currentDrawingMode },
                            drawingStore = drawingStore,
                            canUseUndoRedo = {
                                !sidebar.isUnitDialogOpen &&
                                        !sidebar.isUnitDragging &&
                                        !unitManager.isDragging &&
                                        !unitManager.isEditDialogOpen &&
                                        !drawingManager.isDragging
                            },
                            onDrawingCommitted = { drawing ->
                                objectController.createDrawing(drawing)
                                    .also { actionHistory.record(CreateObjectAction(it)) }
                            },
                            onUndoRequested = {
                                actionHistory.undo(objectController)
                            },
                            onRedoRequested = {
                                actionHistory.redo(objectController)
                            },
                            isMovableObjectUnderPointer = { point ->
                                currentTool == MapViewerTool.SELECT &&
                                        (unitManager.blocksScreenPoint(point) || drawingManager.blocksScreenPoint(point))
                            },
                            isScreenOverlayBlocking = { point ->
                                toolbar.blocksScreenPoint(point) ||
                                        sidebar.blocksScreenPoint(point) ||
                                        sidebar.isUnitDragging ||
                                        sidebar.isUnitDialogOpen ||
                                        unitManager.isContextMenuOpen ||
                                        drawingManager.blocksScreenPoint(point) ||
                                        contextMenuManager.blocksScreenPoint(point) ||
                                        (currentTool == MapViewerTool.SELECT && unitManager.blocksScreenPoint(point))
                            },
                            isMapPlacementDragging = unitManager::isDragging,
                            onViewportChanged = {
                                currentViewport = it
                                drawingView.update(drawingStore.state, it.zoom)
                            },
                            onTitleChanged = { gameWindow.title = settings.operationTitle(it) },
                            onOverlayStateChanged = { state ->
                                gridLinesOverlay?.update(state)
                                gridLabelsOverlay?.update(state)
                            },
                            onDrawingStateChanged = { state ->
                                drawingView.update(state, currentViewport.zoom)
                            },
                        ).install()
                    }

                    fun launchDraft(draft: NewOperationDraft) {
                        if (!launchFlow.launchNewOperation(draft)) {
                            return
                        }

                        val launchConfiguration = OperationLaunchConfigurationFactory.create(draft, initialSettings)
                        newOperationView?.close()
                        newOperationView = null
                        showViewer(
                            settings = launchConfiguration.settings,
                            initialUnitTemplates = launchConfiguration.unitTemplates,
                        )
                    }

                    showNewOperation = {
                        launchFlow.createNewOperation()
                        launchpadView?.close()
                        launchpadView = null
                        newOperationView = MapViewerNewOperationView(
                            root = views.stage,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            initialSetupInputs = initialSetupInputs,
                            initialPixelsPerMetre = initialSettings.metresPerPixel,
                            onLaunch = ::launchDraft,
                            onBack = { showLaunchpad() },
                        )
                    }

                    showLaunchpad = {
                        launchFlow.returnToLaunchpad()
                        newOperationView?.close()
                        newOperationView = null
                        launchpadView = MapViewerLaunchpadView(
                            root = views.stage,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            onCreateNewOperation = { showNewOperation() },
                            onLaunchExistingOperation = launchFlow::launchExistingOperation,
                        )
                    }

                    showLaunchpad()
                }
            }
        }
    }
}

private fun MapViewerSettings.operationTitle(viewerTitle: String): String =
    operationName
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it - $viewerTitle" }
        ?: viewerTitle
