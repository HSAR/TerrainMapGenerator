package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
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
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.FireArcDisplayMode
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerController
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerReplay
import io.hsar.mapgenerator.commandgui.korge.interaction.MapActionHistory
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteStore
import io.hsar.mapgenerator.commandgui.korge.interaction.MapObjectController
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedGraphicManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectPatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSelectionResolver
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedUnitManager
import io.hsar.mapgenerator.commandgui.korge.interaction.MapSelectionController
import io.hsar.mapgenerator.commandgui.korge.interaction.MapSelectionOverlayView
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerGraphicTemplatePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerInteraction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplatePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.OPERATION_START_TURN_NAME
import io.hsar.mapgenerator.commandgui.korge.interaction.ReplayedLedger
import io.hsar.mapgenerator.commandgui.korge.interaction.TurnPanelMode
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitIconDisplaySize
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.turnOriginFor
import io.hsar.mapgenerator.commandgui.korge.launch.LaunchBackgroundTileGenerator
import io.hsar.mapgenerator.commandgui.korge.launch.LaunchTopographyBackground
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchFlow
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchShell
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchpadView
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerNewOperationView
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerViewportResolver
import io.hsar.mapgenerator.commandgui.korge.launch.NewOperationDraft
import io.hsar.mapgenerator.commandgui.korge.launch.OperationLaunchConfigurationFactory
import io.hsar.mapgenerator.commandgui.korge.launch.OperationMapImage
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.render.MapViewerRenderer
import io.hsar.mapgenerator.commandgui.korge.render.RenderedMap
import io.hsar.mapgenerator.commandgui.korge.ui.DestroyedUnitsDisplay
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerGraphicSidebar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerKorgeFonts
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerLedgerPanel
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerNotesPanel
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSidebar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerToolbar
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTransientNotice
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import io.hsar.mapgenerator.commandgui.korge.ui.noteReferenceContextMenuActions
import io.hsar.mapgenerator.commandgui.save.GeneratedImageAsset
import io.hsar.mapgenerator.commandgui.save.LoadedOperation
import io.hsar.mapgenerator.commandgui.save.MapThumbnailRenderer
import io.hsar.mapgenerator.commandgui.save.OperationAdditionalData
import io.hsar.mapgenerator.commandgui.save.OperationSaveCatalogue
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveRequest
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.commandgui.save.OperationToolStorage
import io.hsar.mapgenerator.commandgui.save.SitrepContactKey
import io.hsar.mapgenerator.commandgui.save.TacticalTurnMarker
import io.hsar.mapgenerator.commandgui.save.UiSessionState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import java.awt.Component
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
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
        MapViewerFeatureFlags.initialize(initialSettings.featureFlags)
        thread(name = "command-gui-korge", isDaemon = false) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val fontPreloadStartedAt = System.nanoTime()
            MapViewerKorgeFonts.preloadAll()
            logger.info("Viewer font preload took {}ms", (System.nanoTime() - fontPreloadStartedAt) / 1_000_000)

            runBlocking {
                Korge(
                    windowWidth = screenSize.width,
                    windowHeight = screenSize.height,
                    fullscreen = false,
                    title = MapViewerWindowTitle.format("Launchpad"),
                    displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP,
                ) {
                    val pendingUiActions = ConcurrentLinkedQueue<() -> Unit>()
                    views.stage.addUpdater {
                        generateSequence { pendingUiActions.poll() }
                            .forEach { action -> action() }
                    }
                    fun runOnUiThread(action: () -> Unit) {
                        pendingUiActions.add(action)
                    }

                    val initialViewportGate = InitialLaunchViewportGate()
                    lateinit var showInitialLaunch: () -> Unit
                    views.stage.addUpdater {
                        if (initialViewportGate.releaseInitialLaunch()) {
                            showInitialLaunch()
                        }
                    }
                    (gameWindow as? AwtGameWindow)?.frame?.also { frame ->
                        frame.addComponentListener(object : ComponentAdapter() {
                            override fun componentResized(event: ComponentEvent) {
                                initialViewportGate.onViewportResized()
                            }
                        })
                        frame.extendedState = JFrame.MAXIMIZED_BOTH
                        frame.disableDropTargets()
                    }

                    val launchFlow = MapViewerLaunchFlow()
                    val launchBackgroundSeed = Random().nextLong()
                    var launchBackground: LaunchTopographyBackground? = null
                    val launchBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    var launchBackgroundJob: Job? = null
                    var launchBackgroundGenerationCompleted = false
                    var launchpadView: MapViewerLaunchpadView? = null
                    var newOperationView: MapViewerNewOperationView? = null
                    var launchShell: MapViewerLaunchShell? = null
                    var saveCatalogueLoadGeneration = 0
                    lateinit var showLaunchpad: () -> Unit
                    lateinit var showNewOperation: () -> Unit
                    lateinit var showPreparedViewer: (
                        MapViewerSettings,
                        List<LedgerEntry>,
                        OperationMapImage?,
                        LoadedOperation?,
                        ReplayedLedger,
                        MapViewerRenderData,
                        RenderedMap,
                        Container,
                    ) -> Unit
                    val exitProgram = {
                        launchBackgroundScope.cancel()
                        gameWindow.exit(0)
                    }
                    fun displayViewport() = MapViewerViewportResolver.resolve(
                        actualWidth = views.actualVirtualWidth,
                        actualHeight = views.actualVirtualHeight,
                        fallbackWidth = gameWindow.width,
                        fallbackHeight = gameWindow.height,
                    )

                    fun currentLaunchBackground(): LaunchTopographyBackground =
                        launchBackground ?: displayViewport().let { viewport ->
                            LaunchTopographyBackground.create(
                                viewportWidth = viewport.width,
                                viewportHeight = viewport.height,
                                seed = launchBackgroundSeed,
                            )
                        }.also { generated -> launchBackground = generated }

                    fun loadLaunchBackground() {
                        if (launchBackgroundJob != null || launchBackgroundGenerationCompleted) return
                        val background = currentLaunchBackground()
                        val startedAt = System.nanoTime()
                        launchBackgroundJob = launchBackgroundScope.launch {
                            runCatching {
                                LaunchBackgroundTileGenerator().generate(
                                    background = background,
                                    onTile = { tile ->
                                        logger.info(
                                            "Launch topography {} tile ({},{}) rendered at {}x{} in {}ms; revealing after {}ms",
                                            tile.quality.name.lowercase(),
                                            tile.coordinate.column,
                                            tile.coordinate.row,
                                            tile.bitmap.width,
                                            tile.bitmap.height,
                                            tile.renderMilliseconds,
                                            tile.arrivalJitterMilliseconds,
                                        )
                                        runOnUiThread {
                                            background.accept(tile)
                                            launchShell?.updateTile(tile)
                                        }
                                    },
                                    onFailure = { job, error, elapsedMilliseconds ->
                                        logger.warn(
                                            "Unable to generate launch topography {} tile ({},{}) after {}ms; continuing.",
                                            job.quality.name.lowercase(),
                                            job.coordinate.column,
                                            job.coordinate.row,
                                            elapsedMilliseconds,
                                            error,
                                        )
                                    },
                                )
                            }.onFailure { error ->
                                logger.error("Launch topography tile generation stopped unexpectedly.", error)
                            }
                            logger.info(
                                "Launch topography tile generation completed in {}ms",
                                (System.nanoTime() - startedAt) / 1_000_000,
                            )
                            runOnUiThread {
                                launchBackgroundJob = null
                                launchBackgroundGenerationCompleted = true
                            }
                        }
                    }

                    fun showViewer(
                        settings: MapViewerSettings,
                        initialLedgerEntries: List<LedgerEntry> = emptyList(),
                        operationMapImage: OperationMapImage? = null,
                        loadedOperation: LoadedOperation? = null,
                    ) {
                        val viewport = displayViewport()
                        val loadingLayer = views.stage.container().also { layer ->
                            layer.zIndex = 50_000.0
                            layer.solidRect(viewport.width, viewport.height, Colors.BLACK)
                            MapViewerTextViews.box(
                                layer,
                                "RENDERING MAP...",
                                viewport.width.toDouble(),
                                viewport.height.toDouble(),
                                color = io.hsar.mapgenerator.image.Palette.Colours.WHITE,
                            )
                        }

                        thread(name = "command-gui-map-render", isDaemon = true) {
                            val startedAt = System.nanoTime()
                            runCatching {
                                val renderData = renderMap(settings)
                                val renderedMap = renderer.render(renderData)
                                val entries = loadedOperation?.ledgerEntries ?: initialLedgerEntries
                                val replayStartedAt = System.nanoTime()
                                val replayedLedger = LedgerReplay.replay(entries)
                                logger.info("Viewer ledger replay took {}ms for {} entries", (System.nanoTime() - replayStartedAt) / 1_000_000, entries.size)
                                Triple(renderData, renderedMap, replayedLedger)
                            }.onSuccess { (renderData, renderedMap, replayedLedger) ->
                                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                                logger.info("Viewer map render took {}ms for {}x{}", elapsedMs, renderedMap.bitmap.width, renderedMap.bitmap.height)
                                runOnUiThread {
                                    showPreparedViewer(settings, initialLedgerEntries, operationMapImage, loadedOperation, replayedLedger, renderData, renderedMap, loadingLayer)
                                }
                            }.onFailure { throwable ->
                                logger.error("Failed to render viewer map", throwable)
                                runOnUiThread {
                                    loadingLayer.parent?.removeChild(loadingLayer)
                                    showLaunchpad()
                                }
                            }
                        }
                    }

                    showPreparedViewer = { settings, initialLedgerEntries, operationMapImage, loadedOperation, replayedLedger, renderData, renderedMap, loadingLayer ->
                        val viewport = displayViewport()
                        var currentViewport = loadedOperation?.camera?.let { camera ->
                            camera.toViewport(
                                geometry = renderedMap.geometry,
                                mapWidth = renderedMap.bitmap.width,
                                mapHeight = renderedMap.bitmap.height,
                                viewportWidth = viewport.width,
                                viewportHeight = viewport.height,
                            )
                        } ?: CameraViewport.centeredOnMap(
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            zoom = CameraViewport.fitZoomToViewport(
                                mapWidth = renderedMap.bitmap.width,
                                mapHeight = renderedMap.bitmap.height,
                                viewportWidth = viewport.width,
                                viewportHeight = viewport.height,
                            ),
                        )
                        gameWindow.title = settings.windowTitle()

                        val additionalData = OperationAdditionalData.fromJson(loadedOperation?.additionalDataJson)
                        val restoredUiState = additionalData.uiState()
                        var currentTool = restoredUiState.selectedTool
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
                        lateinit var graphicManager: MapPlacedGraphicManager
                        lateinit var graphicSidebar: MapViewerGraphicSidebar
                        lateinit var objectController: MapObjectController
                        lateinit var selectionController: MapSelectionController
                        lateinit var dialogManager: MapViewerDialogManager
                        lateinit var sidebar: MapViewerSidebar
                        var transientNotice: MapViewerTransientNotice? = null
                        var toolbar: MapViewerToolbar? = null
                        var ledgerPanel: MapViewerLedgerPanel? = null
                        var notesPanel: MapViewerNotesPanel? = null
                        val actionHistory = MapActionHistory()
                        val sitrepContactPersistence = additionalData.sitrepContacts
                        val tacticalFirePersistence = additionalData.tacticalFire
                        val ecmPersistence = additionalData.ecm
                        val noteStore = MapNoteStore()
                        val operationId = loadedOperation?.operationId ?: UUID.randomUUID()
                        val operationCreatedAt = loadedOperation?.createdAt ?: Instant.now()
                        val toolStorage = OperationToolStorage()
                        val saveWriter = OperationSaveWriter(storage = toolStorage)
                        val toolSettings = toolStorage.readSettings()
                        var destroyedUnitsDisplay = DestroyedUnitsDisplay.fromSettingsValue(toolSettings.displayDestroyedUnits)
                        var unitIconSizePx = UnitIconDisplaySize.coerce(toolSettings.unitIconSizePx)
                        var fireArcDisplayMode = FireArcDisplayMode.fromSettingsValue(toolSettings.fireArcDisplayMode)
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
                        var escapeMenuThumbnailImage: BufferedImage? = null
                        val referencedNotesForObject: (MapPlacedObjectId) -> List<MapNoteSnapshot> = { objectId ->
                            noteStore.snapshotsReferencing(objectId)
                        }
                        val openNoteFromContextMenu: (MapNoteId) -> Boolean = { noteId ->
                            notesPanel?.openNote(noteId) == true
                        }
                        var readOnlyBannerLayer: Container? = null
                        var turnMode: TurnPanelMode = TurnPanelMode.Edit
                        var previousLeftHeld = false
                        var previousRightHeld = false
                        var selectionControllerReady = false
                        var unitManagerReady = false
                        var startupReplayInProgress = true

                        fun isReadOnly(): Boolean =
                            turnMode is TurnPanelMode.ReadOnly

                        fun currentTurnNumber(): Int =
                            actionHistory.turnMarkers.count { !it.isFuture && it.name != OPERATION_START_TURN_NAME } + 1

                        fun effectiveTool(): MapViewerTool =
                            if (isReadOnly()) {
                                MapViewerTool.MEASURE
                            } else {
                                currentTool
                            }

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

                        var saveInProgress = false

                        fun showSaveNotice(message: String) {
                            transientNotice?.show(message)
                        }

                        fun captureWindowThumbnail(): BufferedImage? =
                            (gameWindow as? AwtGameWindow)?.frame
                                ?.takeIf { it.isShowing }
                                ?.let { frame ->
                                    runCatching {
                                        val contentPane = frame.contentPane
                                        val location = contentPane.locationOnScreen
                                        val size = contentPane.size
                                        if (size.width <= 0 || size.height <= 0) {
                                            return@runCatching null
                                        }
                                        Robot().createScreenCapture(
                                            Rectangle(location.x, location.y, size.width, size.height),
                                        )
                                    }.getOrNull()
                                }

                        fun saveOperation(
                            noticePrefix: String = "Saved",
                            showNotice: Boolean = true,
                            thumbnailImage: BufferedImage? = null,
                            onComplete: (Boolean) -> Unit = {},
                        ): Boolean {
                            if (saveInProgress) {
                                onComplete(false)
                                return false
                            }

                            saveInProgress = true
                            val saveViewport = currentViewport
                            val snapshots = objectController.snapshots()
                            val request = OperationSaveRequest(
                                operationId = operationId,
                                operationName = settings.operationName ?: "Operation",
                                createdAt = operationCreatedAt,
                                updatedAt = Instant.now(),
                                camera = saveViewport,
                                settings = settings,
                                mapImage = renderData.baseImage,
                                thumbnailImage = thumbnailImage,
                                mapAssetSource = operationMapImage?.assetSource ?: loadedOperation?.mapAssetSource,
                                ledgerEntries = actionHistory.fullTimelineEntries,
                                generatedImageAssets = generatedImageAssets.values.toList(),
                                additionalDataJson = additionalData.apply {
                                    updateUiState(
                                        UiSessionState(
                                            selectedTool = currentTool,
                                            selectedObjectIds = selectionController.selectedIds().toList(),
                                            dialogs = dialogManager.persistedDialogs() + listOfNotNull(
                                                sidebar.persistedDialog(),
                                                graphicSidebar.persistedDialog(),
                                                notesPanel?.persistedDialog(),
                                            ),
                                        ),
                                    )
                                }.toJsonOrNull(),
                            )

                            thread(name = "command-gui-save", isDaemon = true) {
                                val startedAt = System.nanoTime()
                                runCatching {
                                    saveWriter.write(
                                        request.copy(
                                            thumbnailImage = request.thumbnailImage
                                                ?: escapeMenuThumbnailImage
                                                ?: captureWindowThumbnail()
                                                ?: thumbnailRenderer.renderOperationThumbnail(
                                                    mapImage = renderData.baseImage,
                                                    geometry = renderedMap.geometry,
                                                    viewport = saveViewport,
                                                    gridLayer = renderData.gridLayer,
                                                    snapshots = snapshots,
                                                ),
                                        ),
                                    )
                                }.onSuccess { target ->
                                    val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                                    logger.info("Operation save took {}ms for {}", elapsedMs, target.fileName)
                                    runOnUiThread {
                                        saveInProgress = false
                                        if (showNotice) {
                                            showSaveNotice("$noticePrefix ${target.fileName}")
                                        }
                                        onComplete(true)
                                    }
                                }.onFailure { throwable ->
                                    logger.error("Failed to save operation", throwable)
                                    runOnUiThread {
                                        saveInProgress = false
                                        onComplete(false)
                                    }
                                }
                            }
                            return true
                        }

                        val autosaveController = MapViewerAutosaveController { complete ->
                            saveOperation("Autosaved") { success -> complete(success) }
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
                            banner.xy((displayViewport().width - width) * 0.5, 76.0)
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
                            notesPanel?.refresh()
                            toolbar?.refresh()
                            refreshReadOnlyBanner()
                            selectionController.refresh()
                        }

                        fun reconcileMarkedTacticalState() {
                            val markedUnits = unitManager.snapshots().filter { TacticalTurnMarker.hasMarker(it.customProperties) }
                            tacticalFirePersistence.reconcileMarked(
                                markedUnits.flatMap { snapshot -> TacticalTurnMarker.usages(snapshot.customProperties, snapshot.id).entries }
                                    .associate { it.toPair() },
                            )
                            ecmPersistence.reconcileMarked(
                                markedUnits.flatMap { snapshot -> TacticalTurnMarker.activations(snapshot.customProperties, snapshot.id) },
                            )
                            unitManager.refreshEcmFields()
                        }

                        fun recordLedgerEntry(entry: LedgerEntry) {
                            if (isReadOnly()) {
                                return
                            }
                            actionHistory.record(entry)
                            if (entry is AddTurnLedgerEntry) {
                                val newTurnNumber = currentTurnNumber()
                                ecmPersistence.clearBefore(newTurnNumber)
                                if (unitManagerReady) unitManager.refreshEcmFields()
                                if (newTurnNumber > 1) {
                                    sitrepContactPersistence.copyTurnState(newTurnNumber - 1, newTurnNumber)
                                }
                            }
                            reconcileMarkedTacticalState()
                            autosaveController.enqueue(entry)
                            refreshHistoryUi()
                        }

                        sceneLayers = MapViewerSceneLayers.create(views.stage, viewport.width, viewport.height)

                        val mapViewport = sceneLayers.map.clipContainer(Size2D(viewport.width, viewport.height)) {
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
                            if (unitManagerReady) {
                                unitManager.refreshIconDisplay(force = true)
                            }
                            if (selectionControllerReady) {
                                selectionController.refresh()
                            }
                            toolbar?.refresh()
                            ledgerPanel?.refresh()
                            notesPanel?.refresh()
                        }
                        resizeSceneViewport(viewport.width, viewport.height)

                        contextMenuManager = ContextMenuManager(sceneLayers.ui)
                        val deferredContextMenuActions = ContextMenuDeferredActionRunner(sceneLayers.ui)
                        dialogManager = MapViewerDialogManager()

                        unitManager = MapPlacedUnitManager(
                            root = sceneLayers.ui,
                            unitLayer = iconLayer,
                            geometry = renderedMap.geometry,
                            mapUnitsPerPixel = settings.unitsPerPixel,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            gridLayer = renderData.gridLayer,
                            viewportProvider = { currentViewport },
                            contextMenuManager = contextMenuManager,
                            deferredActionRunner = deferredContextMenuActions,
                            dialogManager = dialogManager,
                            onTransientNotice = { message -> transientNotice?.show(message) },
                            initialDestroyedUnitsDisplay = destroyedUnitsDisplay,
                            initialUnitIconSizePx = unitIconSizePx,
                            initialFireArcDisplayMode = fireArcDisplayMode,
                            canInteractWithUnits = { effectiveTool() == MapViewerTool.SELECT },
                            isHigherPriorityDragInProgress = { drawingManager.isDragging },
                            noteReferenceMenuActionsProvider = { snapshot ->
                                noteReferenceContextMenuActions(
                                    referencedNotes = referencedNotesForObject(snapshot.id),
                                    openNote = { noteId -> deferredContextMenuActions.launch { openNoteFromContextMenu(noteId) } },
                                )
                            },
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
                            onUnitsChanged = {
                                if (selectionControllerReady && !startupReplayInProgress) {
                                    selectionController.refresh()
                                }
                            },
                            turnOriginProvider = actionHistory::turnOriginFor,
                            sitrepTurnNumberProvider = ::currentTurnNumber,
                            tacticalTurnNumberProvider = ::currentTurnNumber,
                            tacticalFireUsageProvider = { key -> tacticalFirePersistence.usage(key) },
                            onTacticalFireUsageChanged = { key, usage ->
                                tacticalFirePersistence.replace(key, usage)
                            },
                            ecmActivationsProvider = ecmPersistence::forTurn,
                            onEcmActivation = ecmPersistence::add,
                            sitrepContactStateProvider = { turnNumber, observerId ->
                                sitrepContactPersistence.stateFor(SitrepContactKey(turnNumber, observerId))
                            },
                            sitrepPreviousContactStateProvider = { turnNumber, observerId ->
                                if (turnNumber <= 1) {
                                    io.hsar.mapgenerator.commandgui.korge.interaction.SitrepContactState()
                                } else {
                                    sitrepContactPersistence.stateFor(SitrepContactKey(turnNumber - 1, observerId))
                                }
                            },
                            onSitrepContactStateChanged = { turnNumber, observerId, state ->
                                sitrepContactPersistence.update(SitrepContactKey(turnNumber, observerId), state)
                            },
                        )
                        unitManagerReady = true
                        unitManager.install()

                        graphicManager = MapPlacedGraphicManager(
                            root = sceneLayers.ui,
                            graphicLayer = iconLayer,
                            geometry = renderedMap.geometry,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            viewportProvider = { currentViewport },
                            contextMenuManager = contextMenuManager,
                            canInteract = { effectiveTool() == MapViewerTool.SELECT },
                            isHigherPriorityDragInProgress = { unitManager.isDragging || drawingManager.isDragging },
                            noteReferenceMenuActionsProvider = { snapshot ->
                                noteReferenceContextMenuActions(
                                    referencedNotes = referencedNotesForObject(snapshot.id),
                                    openNote = { noteId -> deferredContextMenuActions.launch { openNoteFromContextMenu(noteId) } },
                                )
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
                        graphicManager.install()

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
                            noteReferenceMenuActionsProvider = { snapshot ->
                                noteReferenceContextMenuActions(
                                    referencedNotes = referencedNotesForObject(snapshot.id),
                                    openNote = { noteId -> deferredContextMenuActions.launch { openNoteFromContextMenu(noteId) } },
                                )
                            },
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
                            graphicManager = graphicManager,
                            drawingStore = drawingStore,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            onDrawingsChanged = { state ->
                                if (!startupReplayInProgress) {
                                    drawingView.update(state, currentViewport.zoom)
                                }
                            },
                            clearTransientState = {
                                if (!startupReplayInProgress) {
                                    contextMenuManager.close()
                                    unitManager.cancelTransientState()
                                    drawingManager.cancelTransientState()
                                    drawingStore.clearPreview()
                                    drawingView.update(drawingStore.state, currentViewport.zoom)
                                }
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
                                graphicManager.selectionAt(pointer),
                                drawingManager.selectionAt(pointer),
                            )
                        selectionController = MapSelectionController(
                            coordinateSpace = coordinateSpace,
                            viewportProvider = { currentViewport },
                            objectController = objectController,
                            contextMenuManager = contextMenuManager,
                            objectAt = ::objectSelectionAt,
                            isSelectableObject = { snapshot ->
                                snapshot !is UnitSnapshot || !snapshot.destroyed || destroyedUnitsDisplay == DestroyedUnitsDisplay.Full
                            },
                            onStateChanged = { state ->
                                selectionOverlay.update(state.copy(gridColor = renderData.gridLayer?.colour ?: UnitAffiliationUi.unaffiliatedColor()))
                                unitManager.updateSelectedUnits(selectionController.selectedIds())
                            },
                            onMovePreviewChanged = { preview -> objectMovePreview = preview },
                            onLedgerEntry = ::recordLedgerEntry,
                            constrainUnitMove = unitManager::constrainMove,
                        )
                        selectionControllerReady = true

                        gridLabelsOverlay = renderData.gridLayer?.let { layer ->
                            GridOverlayView(layer).also {
                                sceneLayers.overlay.addChild(it)
                            }
                        }
                        sidebar = MapViewerSidebar(
                            root = sceneLayers.ui,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            onUnitDropped = { template, screenPoint ->
                                unitManager.resolvePlacement(screenPoint)
                                    ?.let { mapCoordinate ->
                                        objectController.createUnit(template, mapCoordinate)
                                            .also { recordLedgerEntry(CreateObjectAction(it)) }
                                    }
                            },
                            contextMenuManager = contextMenuManager,
                            deferredActionRunner = deferredContextMenuActions,
                            dialogManager = dialogManager,
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
                            onTransientNotice = { message -> transientNotice?.show(message) },
                            canStartUnitDrag = { effectiveTool() == MapViewerTool.SELECT },
                            initialUnitTemplates = emptyList(),
                        )
                        graphicSidebar = MapViewerGraphicSidebar(
                            root = sceneLayers.ui,
                            contextMenuManager = contextMenuManager,
                            onGraphicDropped = { template, screenPoint ->
                                unitManager.resolvePlacement(screenPoint)?.let { coordinate ->
                                    objectController.createGraphic(template, coordinate)
                                        .also { recordLedgerEntry(CreateObjectAction(it)) }
                                }
                            },
                            onTemplateCreated = { snapshot -> recordLedgerEntry(CreateGraphicTemplateAction(snapshot)) },
                            onTemplateDeleted = { snapshot -> recordLedgerEntry(DeleteGraphicTemplateAction(snapshot)) },
                            onTemplateEdited = { before, after ->
                                val patch = MapViewerGraphicTemplatePatch.diff(before, after)
                                recordLedgerEntry(EditGraphicTemplateAction(before.id, patch.forward, patch.backward, before, after))
                                graphicManager.replaceTemplate(after)
                            },
                            onTransientNotice = { message -> transientNotice?.show(message) },
                        )
                        val ledgerController = object : LedgerController {
                            override fun createObject(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot) {
                                objectController.createObject(snapshot)
                                reconcileMarkedTacticalState()
                            }

                            override fun applyObjectEdit(
                                id: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ) {
                                objectController.applyObjectEdit(id, changes)
                                reconcileMarkedTacticalState()
                            }

                            override fun deleteObject(id: io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId): io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot? =
                                objectController.deleteObject(id).also { reconcileMarkedTacticalState() }

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

                            override fun createGraphicTemplate(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot) {
                                graphicSidebar.addTemplate(snapshot, notify = false)
                            }

                            override fun replaceGraphicTemplate(snapshot: io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot) {
                                graphicSidebar.replaceTemplate(snapshot, notify = false)
                                graphicManager.replaceTemplate(snapshot)
                            }

                            override fun applyGraphicTemplateEdit(
                                id: io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ): io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot? {
                                val current = graphicSidebar.template(id) ?: return null
                                return graphicSidebar.replaceTemplate(
                                    MapViewerGraphicTemplatePatch.apply(current, changes),
                                    notify = false,
                                )?.also(graphicManager::replaceTemplate)
                            }

                            override fun deleteGraphicTemplate(id: io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateId): io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot? =
                                graphicSidebar.removeTemplate(id, notify = false)

                            override fun createNote(snapshot: MapNoteSnapshot) {
                                noteStore.upsert(snapshot)
                            }

                            override fun applyNoteEdit(
                                id: MapNoteId,
                                changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
                            ): MapNoteSnapshot? =
                                noteStore.applyEdit(id, changes)

                            override fun deleteNote(id: MapNoteId): MapNoteSnapshot? =
                                noteStore.delete(id)
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
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
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
                            initialDestroyedUnitsDisplay = destroyedUnitsDisplay,
                            onDestroyedUnitsDisplayChanged = { displayMode ->
                                destroyedUnitsDisplay = displayMode
                                clearTransientInteractionState()
                                selectionController.clear()
                                unitManager.updateDestroyedUnitsDisplay(displayMode)
                                runCatching {
                                    toolStorage.writeSettings(
                                        toolStorage.readSettings().copy(displayDestroyedUnits = displayMode.label),
                                    )
                                }.onFailure { throwable ->
                                    logger.error("Failed to save operation tool settings", throwable)
                                }
                            },
                            initialUnitIconSizePx = unitIconSizePx,
                            onUnitIconSizeChanged = { size ->
                                unitIconSizePx = UnitIconDisplaySize.coerce(size)
                                unitManager.updateUnitIconSize(unitIconSizePx)
                                selectionController.refresh()
                                runCatching {
                                    toolStorage.writeSettings(
                                        toolStorage.readSettings().copy(unitIconSizePx = unitIconSizePx),
                                    )
                                }.onFailure { throwable ->
                                    logger.error("Failed to save operation tool settings", throwable)
                                }
                            },
                            initialFireArcDisplayMode = fireArcDisplayMode,
                            onFireArcDisplayModeChanged = { displayMode ->
                                fireArcDisplayMode = displayMode
                                unitManager.updateFireArcDisplayMode(displayMode)
                                runCatching {
                                    toolStorage.writeSettings(
                                        toolStorage.readSettings().copy(fireArcDisplayMode = displayMode.label),
                                    )
                                }.onFailure { throwable ->
                                    logger.error("Failed to save operation tool settings", throwable)
                                }
                            },
                            turnThumbnail = ::turnThumbnailBitmap,
                        )
                        notesPanel = MapViewerNotesPanel(
                            root = sceneLayers.ui,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            noteStore = noteStore,
                            dialogManager = dialogManager,
                            viewportProvider = { currentViewport },
                            coordinateSpace = coordinateSpace,
                            isReadOnly = { isReadOnly() },
                            objectSnapshot = objectController::snapshot,
                            onLedgerEntry = ::recordLedgerEntry,
                        )
                        toolbar = MapViewerToolbar(
                            root = sceneLayers.ui,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
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
                        transientNotice = MapViewerTransientNotice(
                            root = sceneLayers.ui,
                            viewportHeightProvider = { displayViewport().height },
                        )
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
                            viewportWidth = { displayViewport().width },
                            viewportHeight = { displayViewport().height },
                            onOpen = { escapeMenuThumbnailImage = captureWindowThumbnail() },
                            onClosed = { escapeMenuThumbnailImage = null },
                            onReturnToLaunchpad = {
                                saveOperation(
                                    showNotice = false,
                                    thumbnailImage = escapeMenuThumbnailImage,
                                ) { success ->
                                    if (success) {
                                        clearTransientInteractionState()
                                        escapeMenu?.close()
                                        sceneLayers.destroy()
                                        showLaunchpad()
                                    }
                                }
                            },
                            onExitProgram = {
                                saveOperation(
                                    showNotice = false,
                                    thumbnailImage = escapeMenuThumbnailImage,
                                ) { success ->
                                    if (success) {
                                        clearTransientInteractionState()
                                        escapeMenu?.close()
                                        exitProgram()
                                    }
                                }
                            },
                        )

                        fun completeStartupReplay() {
                            startupReplayInProgress = false
                            drawingView.update(drawingStore.state, currentViewport.zoom)
                            refreshHistoryUi()
                            selectionController.restoreSelection(restoredUiState.selectedObjectIds)
                            restoredUiState.dialogs
                                .filter { it.type == "note" }
                                .forEach { dialog -> notesPanel?.restoreNote(dialog) }
                            restoredUiState.dialogs.firstOrNull { it.type == "unit-types" }?.let(sidebar::restoreDialog)
                            restoredUiState.dialogs.firstOrNull { it.type == "graphics" }?.let(graphicSidebar::restoreDialog)
                            restoredUiState.dialogs.firstOrNull { it.type == "notes" }?.let { dialog -> notesPanel?.restoreDialog(dialog) }
                            restoredUiState.dialogs
                                .filter { it.type == "sitrep" }
                                .forEach(unitManager::restoreSitrepDialog)
                            restoredUiState.dialogs
                                .filter { it.type == "unit-edit" }
                                .forEach(unitManager::restoreUnitEditDialog)
                            restoredUiState.dialogs
                                .filter { it.type == "tactical" }
                                .forEach(unitManager::restoreTacticalDialog)
                            loadingLayer.parent?.removeChild(loadingLayer)
                        }
                        sidebar.restoreUnitTemplates(replayedLedger.unitTemplates)
                        graphicSidebar.restoreTemplates(replayedLedger.graphicTemplates)
                        objectController.restoreAll(replayedLedger.objects)
                        replayedLedger.unitTemplates.forEach(unitManager::replaceUnitTemplate)
                        replayedLedger.notes.forEach(noteStore::upsert)
                        reconcileMarkedTacticalState()
                        (loadedOperation?.ledgerEntries ?: initialLedgerEntries).forEach(actionHistory::record)
                        completeStartupReplay()

                        sceneLayers.ui.addUpdater {
                            if (startupReplayInProgress) {
                                return@addUpdater
                            }
                            val now = System.currentTimeMillis()
                            val input = views.input
                            val pointer = input.mousePos
                            val leftHeld = input.mouseButtonPressed(korlibs.event.MouseButton.LEFT)
                            val rightHeld = input.mouseButtonPressed(korlibs.event.MouseButton.RIGHT)
                            graphicSidebar.update(pointer, leftHeld)
                            val escapeMenuOpen = escapeMenu?.isOpen == true
                            val graphicUnderPointer = graphicManager.selectionAt(pointer) != null
                            val uiBlocksPointer = !graphicUnderPointer && (toolbar?.blocksScreenPoint(pointer) == true ||
                                    ledgerPanel?.blocksScreenPoint(pointer) == true ||
                                    notesPanel?.blocksScreenPoint(pointer) == true ||
                                    sidebar.blocksScreenPoint(pointer) ||
                                    graphicSidebar.blocksScreenPoint(pointer) ||
                                    sidebar.isUnitDragging ||
                                    dialogManager.blocksScreenPoint(pointer) ||
                                    unitManager.blocksScreenPoint(pointer) ||
                                    unitManager.isContextMenuOpen ||
                                    escapeMenu?.isOpen == true ||
                                    contextMenuManager.blocksScreenPoint(pointer))

                            if (effectiveTool() == MapViewerTool.SELECT && !uiBlocksPointer) {
                                if (leftHeld && !previousLeftHeld) {
                                    if (!unitManager.beginEcmHandleDragAt(pointer)) {
                                        selectionController.beginLeftDrag(
                                            pointer,
                                            altHeld = input.keys[Key.ALT],
                                            shiftHeld = input.keys[Key.SHIFT],
                                        )
                                    }
                                }
                                if (rightHeld && !previousRightHeld) {
                                    if (!selectionController.openContextMenu(pointer) &&
                                        !unitManager.openContextMenuAt(pointer)
                                    ) {
                                        if (!graphicManager.openContextMenuAt(pointer)) drawingManager.openContextMenuAt(pointer)
                                    }
                                }
                            }
                            if (!escapeMenuOpen) {
                                if (previousLeftHeld &&
                                    !leftHeld &&
                                    selectionController.isMovingSelection &&
                                    notesPanel?.addDroppedReferences(pointer, selectionController.selectedIds()) == true
                                ) {
                                    selectionController.cancelTransientState()
                                } else {
                                    selectionController.update(pointer, leftHeld, shiftHeld = input.keys[Key.SHIFT])
                                }
                            }

                            transientNotice?.update(now)
                            autosaveController.update(now)
                            if (input.keys.justPressed(Key.S) &&
                                input.keys[Key.CONTROL] &&
                                !escapeMenuOpen &&
                                !dialogManager.isTextInputActive() &&
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
                                MapViewerEscapeMenuHotkeyGate.canToggle(
                                    sidebarIsUnitDragging = sidebar.isUnitDragging,
                                    unitIsDragging = unitManager.isDragging,
                                    drawingIsDragging = drawingManager.isDragging,
                                    selectionIsDragging = selectionController.isDragging,
                                    anyDialogOpen = unitManager.isEditDialogOpen,
                                )
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
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            mapWidth = renderedMap.bitmap.width,
                            mapHeight = renderedMap.bitmap.height,
                            gridLayer = renderData.gridLayer,
                            initialViewport = currentViewport,
                            currentTool = { effectiveTool() },
                            currentDrawingMode = { currentDrawingMode },
                            currentDrawingColour = { currentDrawingColour.color },
                            drawingStore = drawingStore,
                            canUseUndoRedo = {
                                !dialogManager.isTextInputActive() &&
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
                            onUnitIconSizeScroll = { factor ->
                                val nextSize = UnitIconDisplaySize.coerce(unitIconSizePx * factor)
                                if (nextSize != unitIconSizePx) {
                                    unitIconSizePx = nextSize
                                    unitManager.updateUnitIconSize(unitIconSizePx)
                                    selectionController.refresh()
                                    ledgerPanel?.updateUnitIconSize(unitIconSizePx)
                                    runCatching {
                                        toolStorage.writeSettings(
                                            toolStorage.readSettings().copy(unitIconSizePx = unitIconSizePx),
                                        )
                                    }.onFailure { throwable ->
                                        logger.error("Failed to save operation tool settings", throwable)
                                    }
                                }
                            },
                            isMovableObjectUnderPointer = { point ->
                                effectiveTool() == MapViewerTool.SELECT &&
                                        (unitManager.hasEcmHandleAt(point) ||
                                                selectionController.hasResizeHandleAt(point) ||
                                                selectionController.hasPivotHandleAt(point) ||
                                                MapPlacedObjectSelectionResolver.choose(
                                                    unitManager.selectionAt(point),
                                                    graphicManager.selectionAt(point),
                                                    drawingManager.selectionAt(point),
                                                ) != null)
                            },
                            isScreenOverlayBlocking = { point ->
                                toolbar.blocksScreenPoint(point) ||
                                        ledgerPanel?.blocksScreenPoint(point) == true ||
                                        notesPanel?.blocksScreenPoint(point) == true ||
                                        sidebar.blocksScreenPoint(point) ||
                                        sidebar.isUnitDragging ||
                                        dialogManager.blocksScreenPoint(point) ||
                                        unitManager.blocksScreenPoint(point) ||
                                        unitManager.isContextMenuOpen ||
                                        drawingManager.blocksScreenPoint(point) ||
                                        (effectiveTool() != MapViewerTool.SELECT && graphicManager.blocksScreenPoint(point)) ||
                                        escapeMenu?.isOpen == true ||
                                        contextMenuManager.blocksScreenPoint(point) ||
                                        (effectiveTool() == MapViewerTool.SELECT && unitManager.hasEcmHandleAt(point)) ||
                                        (effectiveTool() == MapViewerTool.SELECT && selectionController.hasResizeHandleAt(point)) ||
                                        (effectiveTool() == MapViewerTool.SELECT && selectionController.hasPivotHandleAt(point)) ||
                                        (effectiveTool() == MapViewerTool.SELECT &&
                                                MapPlacedObjectSelectionResolver.choose(
                                                    unitManager.selectionAt(point),
                                                    graphicManager.selectionAt(point),
                                                    drawingManager.selectionAt(point),
                                                ) != null)
                            },
                            isMapPlacementDragging = { unitManager.isDragging || graphicManager.isDragging || selectionController.isDragging },
                            onViewportChanged = {
                                currentViewport = it
                                unitManager.refreshIconDisplay()
                                graphicManager.refresh()
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
                        launchShell?.close()
                        launchShell = null
                        showViewer(
                            settings = loaded.settings,
                            initialLedgerEntries = emptyList(),
                            loadedOperation = loaded,
                        )
                    }

                    fun browseForExistingOperation() {
                        val saveWriter = OperationSaveWriter()
                        saveWriter.ensureDefaultStorage()
                        val defaultSaveDirectory = saveWriter.defaultSaveDirectory()
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

                    fun loadSavedOperationsForLaunchpad() {
                        val targetLaunchpad = launchpadView ?: return
                        val loadGeneration = ++saveCatalogueLoadGeneration
                        thread(name = "command-gui-save-catalogue", isDaemon = true) {
                            val savedOperations = runCatching {
                                val saveWriter = OperationSaveWriter()
                                saveWriter.ensureDefaultStorage()
                                OperationSaveCatalogue().list(saveWriter.defaultSaveDirectory())
                            }.onFailure { throwable ->
                                logger.error("Failed to list saved operations", throwable)
                            }.getOrElse { emptyList() }

                            runOnUiThread {
                                if (loadGeneration == saveCatalogueLoadGeneration && launchpadView === targetLaunchpad) {
                                    targetLaunchpad.updateSavedOperations(savedOperations)
                                }
                            }
                        }
                    }

                    fun launchDraft(draft: NewOperationDraft) {
                        if (!launchFlow.launchNewOperation(draft)) {
                            return
                        }

                        val launchConfiguration = OperationLaunchConfigurationFactory.create(draft, initialSettings)
                        newOperationView?.close()
                        newOperationView = null
                        launchShell?.close()
                        launchShell = null
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
                        val viewport = displayViewport()
                        launchShell ?: run {
                            launchShell = MapViewerLaunchShell(
                                root = views.stage,
                                viewportWidth = viewport.width,
                                viewportHeight = viewport.height,
                                background = currentLaunchBackground(),
                            )
                            loadLaunchBackground()
                        }
                        gameWindow.title = MapViewerWindowTitle.format("New Operation")
                        newOperationView = MapViewerNewOperationView(
                            root = views.stage,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            initialSetupInputs = initialSetupInputs,
                            initialUnitsPerPixel = initialSettings.unitsPerPixel,
                            initialSeed = initialSettings.seed,
                            onLaunch = ::launchDraft,
                            onBack = { showLaunchpad() },
                            onTitleChanged = { title -> gameWindow.title = MapViewerWindowTitle.format(title) },
                        ).also { view ->
                            view.reconcileViewport(viewport.width, viewport.height)
                        }
                    }

                    showLaunchpad = {
                        launchFlow.returnToLaunchpad()
                        newOperationView?.close()
                        newOperationView = null
                        val viewport = displayViewport()
                        launchShell ?: run {
                            launchShell = MapViewerLaunchShell(
                                root = views.stage,
                                viewportWidth = viewport.width,
                                viewportHeight = viewport.height,
                                background = currentLaunchBackground(),
                            )
                            loadLaunchBackground()
                        }
                        saveCatalogueLoadGeneration++
                        gameWindow.title = MapViewerWindowTitle.format("Launchpad")
                        launchpadView = MapViewerLaunchpadView(
                            root = views.stage,
                            viewportWidth = viewport.width,
                            viewportHeight = viewport.height,
                            onCreateNewOperation = { showNewOperation() },
                            onLoadSavedOperation = { summary -> loadExistingOperation(summary.path) },
                            onOpenSavePicker = { browseForExistingOperation() },
                            onExitProgram = exitProgram,
                            onOngoingOperationsEntered = { loadSavedOperationsForLaunchpad() },
                            onTitleChanged = { title -> gameWindow.title = MapViewerWindowTitle.format(title) },
                        ).also { view ->
                            view.reconcileViewport(viewport.width, viewport.height)
                        }
                    }

                    showInitialLaunch = {
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

internal class InitialLaunchViewportGate(
    private val fallbackFrameCount: Int = 2,
) {
    private var resized = false
    private var elapsedFrames = 0
    private var released = false

    fun onViewportResized() {
        resized = true
    }

    fun releaseInitialLaunch(): Boolean {
        if (released) return false
        elapsedFrames++
        if (!resized && elapsedFrames < fallbackFrameCount) return false
        released = true
        return true
    }
}

private fun Component.disableDropTargets() {
    setDropTarget(null)
    if (this is java.awt.Container) {
        components.forEach { it.disableDropTargets() }
    }
}

private fun MapViewerSettings.operationTitle(viewerTitle: String): String =
    operationName
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it - $viewerTitle" }
        ?: viewerTitle
