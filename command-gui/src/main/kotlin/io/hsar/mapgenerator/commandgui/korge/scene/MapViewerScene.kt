package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.MapViewerWindowTitle
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingColour
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingManager
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingMode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingView
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerController
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapActionHistory
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapObjectController
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectPatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSelectionKind
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSelectionResolver
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedUnitManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapSelectionController
import io.hsar.mapgenerator.commandgui.korge.interaction.MapSelectionOverlayView
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerInteraction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplatePatch
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchFlow
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchpadView
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerNewOperationView
import io.hsar.mapgenerator.commandgui.korge.launch.NewOperationDraft
import io.hsar.mapgenerator.commandgui.korge.launch.OperationLaunchConfigurationFactory
import io.hsar.mapgenerator.commandgui.korge.launch.OperationMapImage
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSidebar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerToolbar
import io.hsar.mapgenerator.commandgui.save.LoadedOperation
import io.hsar.mapgenerator.commandgui.save.OperationSaveCatalogue
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveRequest
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.commandgui.save.toViewport
import korlibs.event.Key
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Camera
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.camera
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Size2D
import korlibs.render.awt.AwtGameWindow
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.concurrent.thread

internal class MapViewerScene(
    private val renderMap: (MapViewerSettings) -> MapViewerRenderData,
    private val initialSettings: MapViewerSettings,
    private val initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
) {
    private val renderer = MapViewerRenderer()
    private val logger = LogManager.getLogger(MapViewerScene::class.java)

    @OptIn(KorgeExperimental::class)
    fun showWindow() {
        thread(name = "command-gui-korge", isDaemon = false) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    fullscreen = false,
                    title = MapViewerWindowTitle.format("Launchpad"),
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    (gameWindow as? AwtGameWindow)?.frame?.extendedState = JFrame.MAXIMIZED_BOTH
                    val launchFlow = MapViewerLaunchFlow()
                    var launchpadView: MapViewerLaunchpadView? = null
                    var newOperationView: MapViewerNewOperationView? = null
                    lateinit var showLaunchpad: () -> Unit
                    lateinit var showNewOperation: () -> Unit
                    val exitProgram = {
                        gameWindow.exit(0)
                    }

                    fun showViewer(
                        settings: MapViewerSettings,
                        initialLedgerEntries: List<LedgerEntry> = emptyList(),
                        operationMapImage: OperationMapImage? = null,
                        loadedOperation: LoadedOperation? = null,
                    ) {
                        val renderData = renderMap(settings)
                        val renderedMap = renderer.render(renderData)
                        var currentViewport = loadedOperation?.camera?.let { camera ->
                            camera.toViewport(
                                geometry = renderedMap.geometry,
                                mapWidth = renderedMap.bitmap.width,
                                mapHeight = renderedMap.bitmap.height,
                                viewportWidth = gameWindow.width,
                                viewportHeight = gameWindow.height,
                            )
                        } ?: CameraViewport.centeredOnMap(
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            zoom = CameraViewport.fitZoomToViewport(
                                mapWidth = renderedMap.bitmap.width,
                                mapHeight = renderedMap.bitmap.height,
                                viewportWidth = gameWindow.width,
                                viewportHeight = gameWindow.height,
                            ),
                        )
                        gameWindow.title = settings.windowTitle()

                        var currentTool = MapViewerTool.SELECT
                        var currentDrawingMode = MapDrawingMode.FREEFORM
                        var currentDrawingColour = MapDrawingColour.RED
                        lateinit var world: Camera
                        lateinit var sceneLayers: MapViewerSceneLayers
                        var escapeMenu: MapViewerEscapeMenu? = null
                        var gridLinesOverlay: GridOverlayLinesView? = null
                        val drawingStore = MapDrawingStore()
                        lateinit var contextMenuManager: ContextMenuManager
                        lateinit var drawingManager: MapDrawingManager
                        lateinit var drawingView: MapDrawingView
                        lateinit var iconLayer: korlibs.korge.view.Container
                        lateinit var unitManager: MapPlacedUnitManager
                        lateinit var objectController: MapObjectController
                        lateinit var selectionController: MapSelectionController
                        lateinit var saveNoticeLayer: Container
                        val actionHistory = MapActionHistory()
                        val operationId = loadedOperation?.operationId ?: UUID.randomUUID()
                        val operationCreatedAt = loadedOperation?.createdAt ?: Instant.now()
                        val saveWriter = OperationSaveWriter()
                        var gridLabelsOverlay: GridOverlayView? = null
                        val coordinateSpace = MapCoordinateSpace(renderedMap.geometry, renderedMap.bitmap.width, renderedMap.bitmap.height)
                        var objectMovePreview: io.hsar.mapgenerator.commandgui.korge.interaction.MapObjectMovePreview? = null
                        var saveNoticeMessage: String? = null
                        var saveNoticeExpiresAtMs = 0L
                        var previousLeftHeld = false
                        var previousRightHeld = false

                        fun saveOperation(noticePrefix: String = "Saved") {
                            val thumbnailImage = renderedMap.bitmap.toBufferedImage()
                            val target = saveWriter.write(
                                OperationSaveRequest(
                                    operationId = operationId,
                                    operationName = settings.operationName ?: "Operation",
                                    createdAt = operationCreatedAt,
                                    updatedAt = Instant.now(),
                                    camera = currentViewport,
                                    settings = settings,
                                    mapImage = renderData.baseImage,
                                    thumbnailImage = thumbnailImage,
                                    mapAssetSource = operationMapImage?.assetSource,
                                    ledgerEntries = actionHistory.appliedEntries,
                                ),
                            )
                            saveNoticeMessage = "$noticePrefix ${target.fileName}"
                            saveNoticeExpiresAtMs = System.currentTimeMillis() + 10_000L
                            saveNoticeLayer.removeChildren()
                            val noticeBounds = MapViewerTextViews.bounds(saveNoticeMessage!!)
                            saveNoticeLayer.solidRect(
                                width = noticeBounds.width + 24.0,
                                height = noticeBounds.height + 16.0,
                                color = korlibs.image.color.RGBA(0, 0, 0, 180),
                            )
                            MapViewerTextViews.label(saveNoticeLayer, saveNoticeMessage!!, color = io.hsar.mapgenerator.image.Palette.Colours.WHITE) {
                                x = 12.0
                                y = 8.0
                            }
                            saveNoticeLayer.visible = true
                            saveNoticeLayer.xy(16.0, gameWindow.height - (noticeBounds.height + 16) - 16.0)
                        }

                        val autosaveController = MapViewerAutosaveController {
                            saveOperation("Autosaved")
                        }

                        fun clearTransientInteractionState() {
                            contextMenuManager.close()
                            objectMovePreview = null
                            unitManager.cancelTransientState()
                            drawingManager.cancelTransientState()
                            drawingStore.clearPreview()
                            drawingView.update(drawingStore.state, currentViewport.zoom)
                        }

                        fun recordLedgerEntry(entry: LedgerEntry) {
                            actionHistory.record(entry)
                            autosaveController.enqueue(entry)
                        }

                        sceneLayers = MapViewerSceneLayers.create(views.stage, gameWindow.width, gameWindow.height)

                        sceneLayers.background.solidRect(gameWindow.width, gameWindow.height, Colors.BLACK)
                        sceneLayers.map.clipContainer(Size2D(gameWindow.width, gameWindow.height)) {
                            world = camera {
                                image(renderedMap.bitmap) {
                                    x = renderedMap.geometry.mapOffsetX.toDouble()
                                    y = renderedMap.geometry.mapOffsetY.toDouble()
                                }

                                val gridLineLayer = container()
                                gridLineLayer.x = renderedMap.geometry.mapOffsetX.toDouble()
                                gridLineLayer.y = renderedMap.geometry.mapOffsetY.toDouble()
                                renderData.gridLayer?.let { layer ->
                                    gridLinesOverlay = GridOverlayLinesView(layer).also {
                                        gridLineLayer.addChild(it)
                                    }
                                }

                                val drawingLayer = container()
                                drawingView = MapDrawingView(coordinateSpace).also {
                                    drawingLayer.addChild(it)
                                }

                                iconLayer = container()
                            }
                        }

                        contextMenuManager = ContextMenuManager(sceneLayers.ui)

                        unitManager = MapPlacedUnitManager(
                            root = sceneLayers.ui,
                            unitLayer = iconLayer,
                            geometry = renderedMap.geometry,
                            mapMetresPerPixel = settings.metresPerPixel,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            gridLayer = renderData.gridLayer,
                            viewportProvider = { currentViewport },
                            contextMenuManager = contextMenuManager,
                            canInteractWithUnits = { currentTool == MapViewerTool.SELECT },
                            isHigherPriorityDragInProgress = { drawingManager.isDragging },
                            onMovePreviewChanged = { preview ->
                                objectMovePreview = preview
                            },
                            onObjectMoved = { before, after ->
                                MapPlacedObjectPatch.diff(before, after)
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { recordLedgerEntry(EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before))) }
                            },
                            onObjectDeleted = { snapshot ->
                                objectController.deleteObject(snapshot.id)
                                recordLedgerEntry(DeleteObjectAction(snapshot))
                            },
                            onUnitEdited = { before, after ->
                                MapPlacedObjectPatch.diff(before, after)
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { recordLedgerEntry(EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before))) }
                            },
                        )
                        unitManager.install()

                        drawingManager = MapDrawingManager(
                            root = sceneLayers.ui,
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportProvider = { currentViewport },
                            currentTool = { currentTool },
                            drawingStore = drawingStore,
                            contextMenuManager = contextMenuManager,
                            isHigherPriorityDragCandidateUnderPointer = { point ->
                                unitManager.hasDragCandidate(point)
                            },
                            isHigherPriorityDragInProgress = { unitManager.isDragging },
                            canInteractWithDrawings = { currentTool == MapViewerTool.SELECT },
                            onDrawingsChanged = { state ->
                                drawingView.update(state, currentViewport.zoom)
                            },
                            onObjectMoved = { before, after ->
                                MapPlacedObjectPatch.diff(before, after)
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { recordLedgerEntry(EditObjectAction(before.id, it, MapPlacedObjectPatch.diff(after, before))) }
                            },
                            onObjectDeleted = { snapshot ->
                                objectController.deleteObject(snapshot.id)
                                recordLedgerEntry(DeleteObjectAction(snapshot))
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

                        val selectionOverlay = MapSelectionOverlayView(
                            coordinateSpace = coordinateSpace,
                            viewportProvider = { currentViewport },
                        ).also {
                            sceneLayers.ui.addChild(it)
                            it.zIndex = 20_000.0
                            it.mouseEnabled = false
                        }

                        fun objectSelectionAt(pointer: korlibs.math.geom.Vector2D) =
                            MapPlacedObjectSelectionResolver.choose(
                                unitManager.selectionAt(pointer),
                                drawingManager.selectionAt(pointer),
                            )
                        selectionController = MapSelectionController(
                            coordinateSpace = coordinateSpace,
                            viewportProvider = { currentViewport },
                            objectController = objectController,
                            contextMenuManager = contextMenuManager,
                            objectAt = ::objectSelectionAt,
                            onStateChanged = selectionOverlay::update,
                            onMovePreviewChanged = { preview -> objectMovePreview = preview },
                            onLedgerEntry = ::recordLedgerEntry,
                        )

                        gridLabelsOverlay = renderData.gridLayer?.let { layer ->
                            GridOverlayView(layer).also {
                                sceneLayers.overlay.addChild(it)
                            }
                        }
                        val sidebar = MapViewerSidebar(
                            root = sceneLayers.ui,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            onUnitDropped = { template, screenPoint ->
                                unitManager.resolvePlacement(screenPoint)
                                    ?.let { mapCoordinate ->
                                        objectController.createUnit(template, mapCoordinate)
                                            .also { recordLedgerEntry(CreateObjectAction(it)) }
                                    }
                            },
                            contextMenuManager = contextMenuManager,
                            onUnitTemplateCreated = { snapshot ->
                                recordLedgerEntry(CreateUnitTemplateAction(snapshot))
                            },
                            onUnitTemplateDeleted = { snapshot ->
                                recordLedgerEntry(DeleteUnitTemplateAction(snapshot))
                            },
                            onUnitTemplateEdited = { before, after ->
                                val changes = MapViewerUnitTemplatePatch.diff(before, after)
                                recordLedgerEntry(
                                    EditUnitTemplateAction(
                                        id = before.id,
                                        forward = changes.forward,
                                        backward = changes.backward,
                                        beforeSnapshot = before,
                                        afterSnapshot = after,
                                    ),
                                )
                            },
                            canStartUnitDrag = { currentTool == MapViewerTool.SELECT },
                            initialUnitTemplates = emptyList(),
                        )
                        val ledgerController = object : LedgerController {
                            override fun createObject(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot) {
                                objectController.createObject(snapshot)
                            }

                            override fun applyObjectEdit(
                                id: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ) {
                                objectController.applyObjectEdit(id, changes)
                            }

                            override fun deleteObject(id: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId): io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot? =
                                objectController.deleteObject(id)

                            override fun createUnitTemplate(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot) {
                                sidebar.addUnitTemplate(snapshot, notify = false)
                            }

                            override fun replaceUnitTemplate(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot) {
                                sidebar.replaceUnitTemplate(snapshot, notify = false)
                            }

                            override fun applyUnitTemplateEdit(
                                id: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ): io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot? {
                                val current = sidebar.snapshotUnitTemplate(id) ?: return null
                                return sidebar.replaceUnitTemplate(
                                    MapViewerUnitTemplatePatch.apply(current, changes),
                                    notify = false,
                                )
                            }

                            override fun deleteUnitTemplate(id: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId): io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot? =
                                sidebar.removeUnitTemplate(id, notify = false)
                        }
                        val toolbar = MapViewerToolbar(
                            root = sceneLayers.ui,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            selectedTool = { currentTool },
                            selectedDrawingMode = { currentDrawingMode },
                            selectedDrawingColour = { currentDrawingColour },
                            onToolSelected = {
                                currentTool = it
                                if (it != MapViewerTool.DRAW) {
                                    currentDrawingMode = MapDrawingMode.FREEFORM
                                }
                                if (it != MapViewerTool.SELECT) {
                                    selectionController.clear()
                                }
                            },
                            onDrawingModeSelected = { currentDrawingMode = it },
                            onDrawingColourSelected = { currentDrawingColour = it },
                        )

                        saveNoticeLayer = sceneLayers.ui.container().also {
                            it.mouseEnabled = false
                            it.visible = false
                        }
                        escapeMenu = MapViewerEscapeMenu(
                            root = sceneLayers.ui,
                            viewportWidth = { gameWindow.width },
                            viewportHeight = { gameWindow.height },
                            onReturnToLaunchpad = {
                                clearTransientInteractionState()
                                escapeMenu?.close()
                                saveOperation()
                                sceneLayers.destroy()
                                showLaunchpad()
                            },
                            onExitProgram = exitProgram,
                        )

                        val startupLedgerEntries = loadedOperation?.ledgerEntries ?: initialLedgerEntries
                        startupLedgerEntries.forEach { entry ->
                            entry.redo(ledgerController)
                            actionHistory.record(entry)
                        }

                        sceneLayers.ui.addUpdater {
                            val now = System.currentTimeMillis()
                            val input = views.input
                            val pointer = input.mousePos
                            val leftHeld = input.mouseButtonPressed(korlibs.event.MouseButton.LEFT)
                            val rightHeld = input.mouseButtonPressed(korlibs.event.MouseButton.RIGHT)
                            val uiBlocksPointer = toolbar.blocksScreenPoint(pointer) ||
                                    sidebar.blocksScreenPoint(pointer) ||
                                    sidebar.isUnitDragging ||
                                    sidebar.isUnitDialogOpen ||
                                    unitManager.isEditDialogOpen ||
                                    unitManager.isContextMenuOpen ||
                                    escapeMenu?.isOpen == true ||
                                    contextMenuManager.blocksScreenPoint(pointer)

                            if (currentTool == MapViewerTool.SELECT && !uiBlocksPointer) {
                                if (leftHeld && !previousLeftHeld) {
                                    selectionController.beginLeftDrag(pointer)
                                }
                                if (rightHeld && !previousRightHeld) {
                                    if (!selectionController.openContextMenu(pointer)) {
                                        when (objectSelectionAt(pointer)?.kind) {
                                            MapPlacedObjectSelectionKind.UNIT -> unitManager.openContextMenuAt(pointer)
                                            MapPlacedObjectSelectionKind.DRAWING -> drawingManager.openContextMenuAt(pointer)
                                            null -> Unit
                                        }
                                    }
                                }
                            }
                            selectionController.update(pointer, leftHeld)

                            if (saveNoticeMessage != null) {
                                if (now <= saveNoticeExpiresAtMs) {
                                    saveNoticeLayer.visible = true
                                } else {
                                    saveNoticeMessage = null
                                    saveNoticeLayer.removeChildren()
                                    saveNoticeLayer.visible = false
                                }
                            }
                            autosaveController.update(now)
                            if (input.keys.justPressed(Key.S) &&
                                input.keys[Key.CONTROL] &&
                                !sidebar.isUnitDialogOpen &&
                                !sidebar.isUnitDragging &&
                                !unitManager.isDragging &&
                                !unitManager.isEditDialogOpen &&
                                !drawingManager.isDragging &&
                                !selectionController.isDragging
                            ) {
                                saveOperation()
                            }
                            if (input.keys.justPressed(Key.ESCAPE) &&
                                !sidebar.isUnitDialogOpen &&
                                !sidebar.isUnitDragging &&
                                !unitManager.isDragging &&
                                !unitManager.isEditDialogOpen &&
                                !drawingManager.isDragging &&
                                !selectionController.isDragging
                            ) {
                                escapeMenu?.toggle()
                            }

                            previousLeftHeld = leftHeld
                            previousRightHeld = rightHeld
                        }

                        MapViewerInteraction(
                            root = sceneLayers.ui,
                            camera = world,
                            geometry = renderedMap.geometry,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            gridLayer = renderData.gridLayer,
                            initialViewport = currentViewport,
                            currentTool = { currentTool },
                            currentDrawingMode = { currentDrawingMode },
                            currentDrawingColour = { currentDrawingColour.color },
                            drawingStore = drawingStore,
                            canUseUndoRedo = {
                                !sidebar.isUnitDialogOpen &&
                                        !sidebar.isUnitDragging &&
                                        !unitManager.isDragging &&
                                        !unitManager.isEditDialogOpen &&
                                        !drawingManager.isDragging &&
                                        !selectionController.isDragging
                            },
                            objectMovePreviewProvider = { objectMovePreview },
                            onDrawingCommitted = { drawing ->
                                objectController.createDrawing(drawing)
                                    .also { recordLedgerEntry(CreateObjectAction(it)) }
                            },
                            onUndoRequested = {
                                actionHistory.undoEntry(ledgerController)?.let(autosaveController::enqueue)
                            },
                            onRedoRequested = {
                                actionHistory.redoEntry(ledgerController)?.let(autosaveController::enqueue)
                            },
                            isMovableObjectUnderPointer = { point ->
                                currentTool == MapViewerTool.SELECT &&
                                        MapPlacedObjectSelectionResolver.choose(
                                            unitManager.selectionAt(point),
                                            drawingManager.selectionAt(point),
                                        ) != null
                            },
                            isScreenOverlayBlocking = { point ->
                                toolbar.blocksScreenPoint(point) ||
                                        sidebar.blocksScreenPoint(point) ||
                                        sidebar.isUnitDragging ||
                                        sidebar.isUnitDialogOpen ||
                                        unitManager.isEditDialogOpen ||
                                        unitManager.isContextMenuOpen ||
                                        drawingManager.blocksScreenPoint(point) ||
                                        escapeMenu?.isOpen == true ||
                                        contextMenuManager.blocksScreenPoint(point) ||
                                        (currentTool == MapViewerTool.SELECT &&
                                                MapPlacedObjectSelectionResolver.choose(
                                                    unitManager.selectionAt(point),
                                                    drawingManager.selectionAt(point),
                                                ) != null)
                            },
                            isMapPlacementDragging = { unitManager.isDragging || selectionController.isDragging },
                            onViewportChanged = {
                                currentViewport = it
                                drawingView.update(drawingStore.state, it.zoom)
                                selectionController.refresh()
                            },
                            onTitleChanged = {},
                            onOverlayStateChanged = { state ->
                                gridLinesOverlay?.update(state)
                                gridLabelsOverlay?.update(state)
                            },
                            onDrawingStateChanged = { state ->
                                drawingView.update(state, currentViewport.zoom)
                            },
                        ).install()
                    }

                    fun loadExistingOperation(path: Path) {
                        val loaded = runCatching { OperationSaveLoader().load(path) }
                            .onFailure { throwable ->
                                logger.error("Failed to load operation from {}", path, throwable)
                            }
                            .getOrNull()
                            ?: return
                        launchpadView?.close()
                        launchpadView = null
                        showViewer(
                            settings = loaded.settings,
                            initialLedgerEntries = emptyList(),
                            loadedOperation = loaded,
                        )
                    }

                    fun browseForExistingOperation() {
                        val defaultSaveDirectory = OperationSaveWriter().defaultSaveDirectory()
                        Files.createDirectories(defaultSaveDirectory)
                        val chooser = JFileChooser().apply {
                            isAcceptAllFileFilterUsed = true
                            currentDirectory = defaultSaveDirectory.toFile()
                            fileFilter = FileNameExtensionFilter("Operational Command Tool saves", "zip")
                        }
                        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                            return
                        }

                        loadExistingOperation(chooser.selectedFile.toPath())
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
                            initialLedgerEntries = launchConfiguration.initialLedgerEntries,
                            operationMapImage = launchConfiguration.mapImage,
                        )
                    }

                    showNewOperation = {
                        launchFlow.createNewOperation()
                        launchpadView?.close()
                        launchpadView = null
                        gameWindow.title = MapViewerWindowTitle.format("New Operation")
                        newOperationView = MapViewerNewOperationView(
                            root = views.stage,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            initialSetupInputs = initialSetupInputs,
                            initialPixelsPerMetre = initialSettings.metresPerPixel,
                            initialSeed = initialSettings.seed,
                            onLaunch = ::launchDraft,
                            onBack = { showLaunchpad() },
                            onTitleChanged = { title -> gameWindow.title = MapViewerWindowTitle.format(title) },
                        )
                    }

                    showLaunchpad = {
                        launchFlow.returnToLaunchpad()
                        newOperationView?.close()
                        newOperationView = null
                        gameWindow.title = MapViewerWindowTitle.format("Launchpad")
                        val saveDirectory = OperationSaveWriter().defaultSaveDirectory()
                        Files.createDirectories(saveDirectory)
                        val savedOperations = OperationSaveCatalogue().list(saveDirectory)
                        launchpadView = MapViewerLaunchpadView(
                            root = views.stage,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            savedOperations = savedOperations,
                            onCreateNewOperation = { showNewOperation() },
                            onLoadSavedOperation = { summary -> loadExistingOperation(summary.path) },
                            onOpenSavePicker = { browseForExistingOperation() },
                            onExitProgram = exitProgram,
                            onTitleChanged = { title -> gameWindow.title = MapViewerWindowTitle.format(title) },
                        )
                    }

                    if (initialSetupInputs.mapImagePath != null) {
                        showNewOperation()
                    } else {
                        showLaunchpad()
                    }
                }
            }
        }
    }
}

private fun Bitmap32.toBufferedImage(): java.awt.image.BufferedImage =
    java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB).also { image ->
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgba = getRgba(x, y)
                image.setRGB(x, y, (rgba.a shl 24) or (rgba.r shl 16) or (rgba.g shl 8) or rgba.b)
            }
        }
    }

private fun MapViewerSettings.operationTitle(viewerTitle: String): String =
    operationName
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it - $viewerTitle" }
        ?: viewerTitle
