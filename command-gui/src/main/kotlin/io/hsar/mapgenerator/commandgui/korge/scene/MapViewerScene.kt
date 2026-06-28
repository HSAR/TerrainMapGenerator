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
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuDeferredActionRunner
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
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnPanelMode
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchFlow
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchpadView
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerNewOperationView
import io.hsar.mapgenerator.commandgui.korge.launch.NewOperationDraft
import io.hsar.mapgenerator.commandgui.korge.launch.OperationLaunchConfigurationFactory
import io.hsar.mapgenerator.commandgui.korge.launch.OperationMapImage
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerLedgerPanel
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSidebar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerToolbar
import io.hsar.mapgenerator.commandgui.save.GeneratedImageAsset
import io.hsar.mapgenerator.commandgui.save.LoadedOperation
import io.hsar.mapgenerator.commandgui.save.MapThumbnailRenderer
import io.hsar.mapgenerator.commandgui.save.OperationSaveCatalogue
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveRequest
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.commandgui.save.toViewport
import korlibs.event.Key
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
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
import korlibs.korge.view.onStageResized
import korlibs.korge.view.size
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
                        var toolbar: MapViewerToolbar? = null
                        var ledgerPanel: MapViewerLedgerPanel? = null
                        val actionHistory = MapActionHistory()
                        val operationId = loadedOperation?.operationId ?: UUID.randomUUID()
                        val operationCreatedAt = loadedOperation?.createdAt ?: Instant.now()
                        val saveWriter = OperationSaveWriter()
                        val thumbnailRenderer = MapThumbnailRenderer()
                        val generatedImageAssets = loadedOperation
                            ?.generatedImageAssets
                            ?.associateBy { it.id }
                            ?.toMutableMap()
                            ?: mutableMapOf()
                        val generatedThumbnailBitmaps = mutableMapOf<UUID, Bitmap32>()
                        var gridLabelsOverlay: GridOverlayView? = null
                        val coordinateSpace = MapCoordinateSpace(renderedMap.geometry, renderedMap.bitmap.width, renderedMap.bitmap.height)
                        var objectMovePreview: io.hsar.mapgenerator.commandgui.korge.interaction.MapObjectMovePreview? = null
                        var saveNoticeMessage: String? = null
                        var saveNoticeExpiresAtMs = 0L
                        var readOnlyBannerLayer: Container? = null
                        var turnMode: TurnPanelMode = TurnPanelMode.Edit
                        var previousLeftHeld = false
                        var previousRightHeld = false
                        var selectionControllerReady = false

                        fun isReadOnly(): Boolean =
                            turnMode is TurnPanelMode.ReadOnly

                        fun effectiveTool(): MapViewerTool =
                            if (isReadOnly()) {
                                MapViewerTool.MEASURE
                            } else {
                                currentTool
                            }

                        fun renderOperationThumbnail() =
                            thumbnailRenderer.renderOperationThumbnail(
                                mapImage = renderData.baseImage,
                                geometry = renderedMap.geometry,
                                viewport = currentViewport,
                                gridLayer = renderData.gridLayer,
                                snapshots = objectController.snapshots(),
                            )

                        fun renderTurnThumbnailAsset(): GeneratedImageAsset {
                            val assetId = UUID.randomUUID()
                            return GeneratedImageAsset(
                                id = assetId,
                                role = "turn-thumbnail",
                                image = thumbnailRenderer.renderTurnThumbnail(
                                    mapImage = renderData.baseImage,
                                    geometry = renderedMap.geometry,
                                    gridLayer = renderData.gridLayer,
                                    snapshots = objectController.snapshots(),
                                ),
                            )
                        }

                        fun AddTurnLedgerEntry.withGeneratedThumbnailIfRequired(): AddTurnLedgerEntry =
                            if (thumbnailAssetId != null) {
                                this
                            } else {
                                renderTurnThumbnailAsset().also { generatedAsset ->
                                    generatedImageAssets[generatedAsset.id] = generatedAsset
                                    generatedThumbnailBitmaps.remove(generatedAsset.id)
                                }.let { generatedAsset ->
                                    copy(thumbnailAssetId = generatedAsset.id)
                                }
                            }

                        fun turnThumbnailBitmap(id: UUID): Bitmap32? {
                            val asset = generatedImageAssets[id] ?: return null
                            return generatedThumbnailBitmaps.getOrPut(id) { asset.image.toBitmap32() }
                        }

                        fun saveOperation(noticePrefix: String = "Saved"): Boolean {
                            val thumbnailImage = renderOperationThumbnail()
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
                                    mapAssetSource = operationMapImage?.assetSource ?: loadedOperation?.mapAssetSource,
                                    ledgerEntries = actionHistory.fullTimelineEntries,
                                    generatedImageAssets = generatedImageAssets.values.toList(),
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
                            return true
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

                        fun refreshReadOnlyBanner() {
                            val banner = readOnlyBannerLayer ?: return
                            banner.removeChildren()
                            if (!isReadOnly()) {
                                banner.visible = false
                                return
                            }

                            val label = "READ ONLY"
                            val bounds = MapViewerTextViews.bounds(label, role = MapViewerFontRole.STRONG, size = 32.0)
                            val width = bounds.width + 48.0
                            val height = bounds.height + 20.0
                            banner.visible = true
                            banner.xy((gameWindow.width - width) * 0.5, 76.0)
                            banner.solidRect(width, height, korlibs.image.color.RGBA(140, 20, 20, 240))
                            banner.solidRect(width - 4.0, height - 4.0, korlibs.image.color.RGBA(60, 0, 0, 240)) {
                                x = 2.0
                                y = 2.0
                            }
                            MapViewerTextViews.box(
                                banner,
                                label,
                                width,
                                height,
                                role = MapViewerFontRole.STRONG,
                                size = 32.0,
                                color = io.hsar.mapgenerator.image.Palette.Colours.WHITE,
                            )
                        }

                        fun refreshHistoryUi() {
                            ledgerPanel?.refresh()
                            toolbar?.refresh()
                            refreshReadOnlyBanner()
                            selectionController.refresh()
                        }

                        fun recordLedgerEntry(entry: LedgerEntry) {
                            if (isReadOnly()) {
                                return
                            }
                            actionHistory.record(entry)
                            autosaveController.enqueue(entry)
                            refreshHistoryUi()
                        }

                        sceneLayers = MapViewerSceneLayers.create(views.stage, gameWindow.width, gameWindow.height)

                        val mapViewport = sceneLayers.map.clipContainer(Size2D(gameWindow.width, gameWindow.height)) {
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

                        fun resizeSceneViewport(width: Int, height: Int) {
                            currentViewport = currentViewport.copy(
                                viewportWidth = width,
                                viewportHeight = height,
                            )
                            sceneLayers.background.removeChildren()
                            sceneLayers.background.solidRect(width, height, Colors.BLACK)
                            mapViewport.size(width, height)
                            world.setTo(currentViewport.toCameraRectangle())
                            if (selectionControllerReady) {
                                selectionController.refresh()
                            }
                            toolbar?.refresh()
                            ledgerPanel?.refresh()
                        }
                        resizeSceneViewport(gameWindow.width, gameWindow.height)

                        contextMenuManager = ContextMenuManager(sceneLayers.ui)
                        val deferredContextMenuActions = ContextMenuDeferredActionRunner(sceneLayers.ui)

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
                            deferredActionRunner = deferredContextMenuActions,
                            canInteractWithUnits = { effectiveTool() == MapViewerTool.SELECT },
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
                            canInteractWithDrawings = { effectiveTool() == MapViewerTool.SELECT },
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
                            sceneLayers.overlay.addChild(it)
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
                            onStateChanged = { state ->
                                selectionOverlay.update(state)
                                unitManager.updateSelectedUnits(selectionController.selectedIds())
                            },
                            onMovePreviewChanged = { preview -> objectMovePreview = preview },
                            onLedgerEntry = ::recordLedgerEntry,
                        )
                        selectionControllerReady = true

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
                            deferredActionRunner = deferredContextMenuActions,
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
                                unitManager.replaceUnitTemplate(after)
                            },
                            canStartUnitDrag = { effectiveTool() == MapViewerTool.SELECT },
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
                                unitManager.replaceUnitTemplate(snapshot)
                            }

                            override fun applyUnitTemplateEdit(
                                id: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ): io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot? {
                                val current = sidebar.snapshotUnitTemplate(id) ?: return null
                                return sidebar.replaceUnitTemplate(
                                    MapViewerUnitTemplatePatch.apply(current, changes),
                                    notify = false,
                                )?.also(unitManager::replaceUnitTemplate)
                            }

                            override fun deleteUnitTemplate(id: io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId): io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot? =
                                sidebar.removeUnitTemplate(id, notify = false)
                        }
                        fun performUndo() {
                            val wasReadOnly = turnMode is TurnPanelMode.ReadOnly
                            actionHistory.undoEntry(ledgerController)?.let {
                                autosaveController.enqueue(it)
                                if (wasReadOnly && !actionHistory.canRedo) {
                                    turnMode = TurnPanelMode.Edit
                                }
                                refreshHistoryUi()
                            }
                        }

                        fun performRedo() {
                            val wasReadOnly = turnMode is TurnPanelMode.ReadOnly
                            actionHistory.redoEntry(ledgerController)?.let {
                                autosaveController.enqueue(it)
                                if (wasReadOnly && !actionHistory.canRedo) {
                                    turnMode = TurnPanelMode.Edit
                                }
                                refreshHistoryUi()
                            }
                        }

                        ledgerPanel = MapViewerLedgerPanel(
                            root = sceneLayers.ui,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            canUndo = { actionHistory.canUndo },
                            canRedo = { actionHistory.canRedo },
                            turnPanelState = { actionHistory.turnPanelState(turnMode) },
                            isReadOnly = { isReadOnly() },
                            onUndo = ::performUndo,
                            onRedo = ::performRedo,
                            onAddTurn = { name -> recordLedgerEntry(AddTurnLedgerEntry(name).withGeneratedThumbnailIfRequired()) },
                            onRevertToTurn = { marker ->
                                clearTransientInteractionState()
                                selectionController.clear()
                                actionHistory
                                    .revertToLedgerIndex(marker.appliedIndex, ledgerController)
                                    .forEach(autosaveController::enqueue)
                                turnMode = if (marker.appliedIndex == actionHistory.liveHeadIndex) {
                                    TurnPanelMode.Edit
                                } else {
                                    TurnPanelMode.ReadOnly(marker.appliedIndex)
                                }
                                refreshHistoryUi()
                            },
                            onSelectCurrent = {
                                clearTransientInteractionState()
                                actionHistory
                                    .revertToCurrent(ledgerController)
                                    .forEach(autosaveController::enqueue)
                                turnMode = TurnPanelMode.Edit
                                refreshHistoryUi()
                            },
                            turnThumbnail = ::turnThumbnailBitmap,
                        )
                        toolbar = MapViewerToolbar(
                            root = sceneLayers.ui,
                            viewportWidth = gameWindow.width,
                            viewportHeight = gameWindow.height,
                            selectedTool = { effectiveTool() },
                            selectedDrawingMode = { currentDrawingMode },
                            selectedDrawingColour = { currentDrawingColour },
                            isReadOnly = { isReadOnly() },
                            onToolSelected = {
                                if (!(isReadOnly() && it != MapViewerTool.MEASURE)) {
                                    currentTool = it
                                    if (it != MapViewerTool.DRAW) {
                                        currentDrawingMode = MapDrawingMode.FREEFORM
                                    }
                                    if (it != MapViewerTool.SELECT) {
                                        selectionController.clear()
                                    }
                                }
                            },
                            onDrawingModeSelected = { currentDrawingMode = it },
                            onDrawingColourSelected = { currentDrawingColour = it },
                        )

                        saveNoticeLayer = sceneLayers.ui.container().also {
                            it.mouseEnabled = false
                            it.visible = false
                        }
                        readOnlyBannerLayer = sceneLayers.ui.container().also {
                            it.mouseEnabled = false
                            it.visible = false
                            it.zIndex = 42_000.0
                        }
                        sceneLayers.ui.onStageResized { width, height ->
                            resizeSceneViewport(width, height)
                            if (isReadOnly()) {
                                refreshReadOnlyBanner()
                            }
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
                            val startupEntry = (entry as? AddTurnLedgerEntry)?.withGeneratedThumbnailIfRequired() ?: entry
                            startupEntry.redo(ledgerController)
                            actionHistory.record(startupEntry)
                        }
                        refreshHistoryUi()

                        sceneLayers.ui.addUpdater {
                            val now = System.currentTimeMillis()
                            val input = views.input
                            val pointer = input.mousePos
                            val leftHeld = input.mouseButtonPressed(korlibs.event.MouseButton.LEFT)
                            val rightHeld = input.mouseButtonPressed(korlibs.event.MouseButton.RIGHT)
                            val uiBlocksPointer = toolbar?.blocksScreenPoint(pointer) == true ||
                                    ledgerPanel?.blocksScreenPoint(pointer) == true ||
                                    sidebar.blocksScreenPoint(pointer) ||
                                    sidebar.isUnitDragging ||
                                    sidebar.isUnitDialogOpen ||
                                    unitManager.isEditDialogOpen ||
                                    unitManager.isContextMenuOpen ||
                                    escapeMenu?.isOpen == true ||
                                    contextMenuManager.blocksScreenPoint(pointer)

                            if (effectiveTool() == MapViewerTool.SELECT && !uiBlocksPointer) {
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
                                ledgerPanel?.isTextInputActive != true &&
                                !sidebar.isUnitDragging &&
                                !unitManager.isDragging &&
                                !unitManager.isEditDialogOpen &&
                                !drawingManager.isDragging &&
                                !selectionController.isDragging &&
                                !isReadOnly()
                            ) {
                                saveOperation()
                            }
                            if (input.keys.justPressed(Key.ESCAPE) &&
                                !sidebar.isUnitDialogOpen &&
                                ledgerPanel?.isTextInputActive != true &&
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
                            currentTool = { effectiveTool() },
                            currentDrawingMode = { currentDrawingMode },
                            currentDrawingColour = { currentDrawingColour.color },
                            drawingStore = drawingStore,
                            canUseUndoRedo = {
                                !sidebar.isUnitDialogOpen &&
                                        ledgerPanel?.isTextInputActive != true &&
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
                                performUndo()
                            },
                            onRedoRequested = {
                                performRedo()
                            },
                            isMovableObjectUnderPointer = { point ->
                                effectiveTool() == MapViewerTool.SELECT &&
                                        (selectionController.hasPivotHandleAt(point) ||
                                                MapPlacedObjectSelectionResolver.choose(
                                                    unitManager.selectionAt(point),
                                                    drawingManager.selectionAt(point),
                                                ) != null)
                            },
                            isScreenOverlayBlocking = { point ->
                                toolbar.blocksScreenPoint(point) ||
                                        ledgerPanel?.blocksScreenPoint(point) == true ||
                                        sidebar.blocksScreenPoint(point) ||
                                        sidebar.isUnitDragging ||
                                        sidebar.isUnitDialogOpen ||
                                        unitManager.isEditDialogOpen ||
                                        unitManager.isContextMenuOpen ||
                                        drawingManager.blocksScreenPoint(point) ||
                                        escapeMenu?.isOpen == true ||
                                        contextMenuManager.blocksScreenPoint(point) ||
                                        (effectiveTool() == MapViewerTool.SELECT && selectionController.hasPivotHandleAt(point)) ||
                                        (effectiveTool() == MapViewerTool.SELECT &&
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

private fun java.awt.image.BufferedImage.toBitmap32(): Bitmap32 =
    Bitmap32(width, height) { x, y ->
        val argb = getRGB(x, y)
        RGBA(
            (argb ushr 16) and 0xFF,
            (argb ushr 8) and 0xFF,
            argb and 0xFF,
            (argb ushr 24) and 0xFF,
        )
    }.premultipliedIfRequired()

private fun MapViewerSettings.operationTitle(viewerTitle: String): String =
    operationName
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it - $viewerTitle" }
        ?: viewerTitle
