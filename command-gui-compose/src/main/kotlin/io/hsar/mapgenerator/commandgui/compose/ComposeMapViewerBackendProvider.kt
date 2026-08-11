package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.hsar.mapgenerator.commandgui.GridReferenceFormatter
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionBox
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.MapViewerBackend
import io.hsar.mapgenerator.commandgui.MapViewerBackendProvider
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuCommand
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuItem
import io.hsar.mapgenerator.commandgui.MapViewerDrawingColour
import io.hsar.mapgenerator.commandgui.MapViewerDrawingGesture
import io.hsar.mapgenerator.commandgui.MapViewerDrawingMode
import io.hsar.mapgenerator.commandgui.MapViewerGlobalSettings
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayRequest
import io.hsar.mapgenerator.commandgui.MapViewerGridReference
import io.hsar.mapgenerator.commandgui.MapViewerImageLoader
import io.hsar.mapgenerator.commandgui.MapViewerMeasurement
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerSavedOperation
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.UnitIconDisplaySize
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.measureMapDrag
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationFreeformDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationObject
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationImageDraft
import io.hsar.mapgenerator.commandgui.operation.OperationPropertyDraft
import io.hsar.mapgenerator.commandgui.operation.OperationOvalDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationSelectionProjection
import io.hsar.mapgenerator.commandgui.operation.OperationTransformGesture
import io.hsar.mapgenerator.commandgui.operation.OperationTransformModifiers
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.contains
import io.hsar.mapgenerator.commandgui.operation.objectAtScreen
import io.hsar.mapgenerator.commandgui.operation.selectionBounds
import io.hsar.mapgenerator.commandgui.operation.selectionFrame
import io.hsar.mapgenerator.commandgui.operation.selectionScreenBounds
import io.hsar.mapgenerator.commandgui.startedAt
import io.hsar.mapgenerator.commandgui.toWorldPoint
import io.hsar.mapgenerator.commandgui.viewerBackgroundBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList
import java.util.Collections.emptySet
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class ComposeMapViewerBackendProvider : MapViewerBackendProvider {
    override val backend = MapViewerBackend.COMPOSE

    override fun showWindow(
        renderMap: (MapViewerSettings) -> MapViewerRenderData,
        initialSettings: MapViewerSettings,
        initialSetupInputs: MapViewerSetupInputs,
    ) = application {
        var workspaceModeRequestHandler by remember { mutableStateOf<(WorkspaceMode) -> Unit>({}) }
        var windowTitle by remember { mutableStateOf(MapViewerSettings().windowTitle()) }
        var closeRequestHandler by remember { mutableStateOf<() -> Unit>({ exitApplication() }) }
        val windowState = rememberWindowState(placement = COMPOSE_INITIAL_WINDOW_PLACEMENT)
        val applicationIcon = remember {
            requireNotNull(ComposeMapViewerBackendProvider::class.java.classLoader.getResourceAsStream("logo512.png")) {
                "Application icon resource logo512.png is missing."
            }.use { stream -> Image.makeFromEncoded(stream.readBytes()).toComposeImageBitmap() }
        }

        Window(
            title = windowTitle,
            state = windowState,
            onCloseRequest = { closeRequestHandler() },
            icon = BitmapPainter(applicationIcon),
        ) {
            MenuBar {
                Menu("Workspace") {
                    Item(
                        text = "Docked",
                        onClick = { workspaceModeRequestHandler(WorkspaceMode.DOCKED) },
                        shortcut = null,
                    )
                    Item(
                        text = "Floating",
                        onClick = { workspaceModeRequestHandler(WorkspaceMode.FLOATING) },
                        shortcut = null,
                    )
                }
            }

            NordTheme {
                ComposeMapViewer(
                    renderMap = renderMap,
                    initialSettings = initialSettings,
                    initialSetupInputs = initialSetupInputs,
                    awtWindow = window,
                    onWorkspaceModeRequestHandlerChanged = { workspaceModeRequestHandler = it },
                    onTitleChanged = { windowTitle = it },
                    onExit = ::exitApplication,
                    onCloseRequestHandlerChanged = { closeRequestHandler = it },
                )
            }
        }
    }
}

internal typealias WorkspaceMode = MapViewerWorkspaceMode

@Composable
private fun ComposeMapViewer(
    renderMap: (MapViewerSettings) -> MapViewerRenderData,
    initialSettings: MapViewerSettings,
    initialSetupInputs: MapViewerSetupInputs,
    awtWindow: java.awt.Window,
    onWorkspaceModeRequestHandlerChanged: ((WorkspaceMode) -> Unit) -> Unit,
    onTitleChanged: (String) -> Unit,
    onExit: () -> Unit,
    onCloseRequestHandlerChanged: (() -> Unit) -> Unit,
) {
    val suppliedMap = remember {
        initialSetupInputs.mapImage
            ?: initialSetupInputs.mapImagePath?.let { path -> runCatching { ImageIO.read(File(path)) }.getOrNull() }
    }
    val launchState = remember {
        ComposeLaunchState(
            initialScreen = if (suppliedMap == null) ComposeLaunchScreen.MAIN else ComposeLaunchScreen.NEW_OPERATION,
        )
    }
    val repository = remember { MapViewerSaveRepository() }
    val globalSettings = remember { MapViewerGlobalSettings() }

    LaunchedEffect(launchState.activeMap?.settings?.operationName, launchState.screen) {
        onTitleChanged(
            launchState.activeMap?.settings?.windowTitle()
                ?: "Operational Command Tool - ${launchState.screen.windowTitle}",
        )
    }

    launchState.activeMap?.let { map ->
        key(map) {
            ViewerWorkspace(
                initialMap = map,
                repository = repository,
                initialUnitIconSizePx = globalSettings.unitIconSizePx(),
                onUnitIconSizeChanged = globalSettings::updateUnitIconSizePx,
                onWorkspaceModeRequestHandlerChanged = onWorkspaceModeRequestHandlerChanged,
                onReturnToLaunchpad = launchState::returnToLaunchpad,
                onExitProgram = onExit,
                onCloseRequestHandlerChanged = onCloseRequestHandlerChanged,
            )
        }
    } ?: run {
        LaunchedEffect(launchState.screen) {
            onCloseRequestHandlerChanged(onExit)
            onWorkspaceModeRequestHandlerChanged {}
        }
        when (launchState.screen) {
        ComposeLaunchScreen.MAIN -> ComposeMainLaunchpad(
            onNewOperation = launchState::showNewOperation,
            onExistingOperations = launchState::showExistingOperations,
            onExit = onExit,
        )

        ComposeLaunchScreen.NEW_OPERATION -> ComposeNewOperationScreen(
            renderMap = renderMap,
            initialSettings = initialSettings,
            initialMap = suppliedMap,
            initialMapPath = initialSetupInputs.mapImagePath,
            awtWindow = awtWindow,
            onBack = launchState::returnToLaunchpad,
            onLaunch = { settings, renderData ->
                launchState.openOperation(repository.createOperation(settings, renderData ?: error("Operation map was not rendered")))
            },
        )

        ComposeLaunchScreen.EXISTING_OPERATIONS -> ComposeExistingOperationsScreen(
            repository = repository,
            awtWindow = awtWindow,
            onBack = launchState::returnToLaunchpad,
            onMapLoaded = launchState::openOperation,
        )
        }
    }
}

internal val COMPOSE_INITIAL_WINDOW_PLACEMENT = WindowPlacement.Maximized

internal data class ComposeActiveMap(val document: OperationDocument) {
    val settings get() = document.settings
    val renderData get() = document.renderData
    val camera get() = document.camera
    val sourcePath get() = document.sourcePath?.toString()
}

internal enum class ComposeLaunchScreen(val windowTitle: String) {
    MAIN("Launchpad"),
    NEW_OPERATION("New Operation"),
    EXISTING_OPERATIONS("Ongoing Operations"),
}

internal class ComposeLaunchState(initialScreen: ComposeLaunchScreen = ComposeLaunchScreen.MAIN) {
    var screen by mutableStateOf(initialScreen)
        private set

    var activeMap by mutableStateOf<ComposeActiveMap?>(null)
        private set

    fun showNewOperation() {
        screen = ComposeLaunchScreen.NEW_OPERATION
    }

    fun showExistingOperations() {
        screen = ComposeLaunchScreen.EXISTING_OPERATIONS
    }

    fun openOperation(document: OperationDocument) {
        activeMap = ComposeActiveMap(document)
    }

    fun returnToLaunchpad() {
        activeMap = null
        screen = ComposeLaunchScreen.MAIN
    }
}

@Composable
private fun ComposeMainLaunchpad(
    onNewOperation: () -> Unit,
    onExistingOperations: () -> Unit,
    onExit: () -> Unit,
) = CommandLaunchScreen(title = "Launchpad") {
    CommandPanel(Modifier.width(460.dp)) {
        Text(
            "WELCOME, COMMANDER.",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineSmall,
        )
        CommandButton(
            onClick = onNewOperation,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("NEW OPERATION")
        }
        CommandButton(
            onClick = onExistingOperations,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("ONGOING OPERATIONS")
        }
        CommandButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("EXIT")
        }
    }
}

@Composable
private fun ComposeNewOperationScreen(
    renderMap: (MapViewerSettings) -> MapViewerRenderData,
    initialSettings: MapViewerSettings,
    initialMap: java.awt.image.BufferedImage?,
    initialMapPath: String?,
    awtWindow: java.awt.Window,
    onBack: () -> Unit,
    onLaunch: (MapViewerSettings, MapViewerRenderData?) -> Unit,
) {
    var operationName by remember { mutableStateOf(initialSettings.operationName.orEmpty()) }
    var mapImage by remember { mutableStateOf(initialMap) }
    var mapName by remember { mutableStateOf(initialMapPath?.let(::File)?.name ?: if (initialMap == null) "" else "Provided map") }
    var referenceSystem by remember { mutableStateOf(initialSettings.referenceSystem) }
    var mapId by remember { mutableStateOf(initialSettings.mapId.orEmpty()) }
    var startX by remember { mutableStateOf(initialSettings.gridStartX?.format().orEmpty()) }
    var startY by remember { mutableStateOf(initialSettings.gridStartY?.format().orEmpty()) }
    var unitsPerPixel by remember { mutableStateOf(initialSettings.unitsPerPixel.toString()) }
    var loadingMap by remember { mutableStateOf(false) }
    var launching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun chooseMap() {
        FileDialog(awtWindow as? Frame, "Choose map image", FileDialog.LOAD).also { dialog ->
            dialog.file = "*.png;*.jpg;*.jpeg"
            dialog.isVisible = true
            val fileName = dialog.file ?: return
            val file = File(dialog.directory, fileName)
            loadingMap = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        ImageIO.read(file) ?: error("The selected file is not a supported image.")
                    }
                }.onSuccess {
                    mapImage = it
                    mapName = file.name
                    errorMessage = null
                }.onFailure {
                    errorMessage = "Could not open ${file.name}: ${it.message ?: it::class.simpleName}"
                }
                loadingMap = false
            }
        }
    }

    fun launch() {
        val image = mapImage ?: return
        val scale = unitsPerPixel.toDoubleOrNull()?.takeIf { it > 0.0 }
        if (operationName.isBlank() || scale == null) {
            errorMessage = "Enter an operation name and a positive map scale."
            return
        }
        val parsedStartX = startX.takeIf(String::isNotBlank)?.let {
            runCatching { referenceSystem.parseCoord(it, io.hsar.mapgenerator.grid.CoordAxis.X, referenceSystem.gridScale) }.getOrNull()
        }
        val parsedStartY = startY.takeIf(String::isNotBlank)?.let {
            runCatching { referenceSystem.parseCoord(it, io.hsar.mapgenerator.grid.CoordAxis.Y, referenceSystem.gridScale) }.getOrNull()
        }
        if ((startX.isNotBlank() && parsedStartX == null) || (startY.isNotBlank() && parsedStartY == null)) {
            errorMessage = "Starting coordinates are not valid for ${referenceSystem.name}."
            return
        }
        val mapLayer = io.hsar.mapgenerator.commandgui.StaticImageLayer(COMPOSE_OPERATION_MAP_ID, "Map")
        val settings = initialSettings.copy(
            operationName = operationName.trim(),
            availableLayers = listOf(mapLayer, io.hsar.mapgenerator.commandgui.MapLayer.GRID),
            enabledLayerIds = setOf(mapLayer.id, io.hsar.mapgenerator.commandgui.MapLayer.GRID.id),
            staticLayerImages = mapOf(mapLayer.id to image),
            mapId = mapId.trim().takeIf(String::isNotBlank),
            referenceSystem = referenceSystem,
            gridStartX = parsedStartX,
            gridStartY = parsedStartY,
            unitsPerPixel = scale,
            width = image.width,
            height = image.height,
        )
        launching = true
        errorMessage = null
        scope.launch {
            runCatching { withContext(Dispatchers.Default) { renderMap(settings) } }
                .onSuccess { onLaunch(settings, it) }
                .onFailure { errorMessage = "Could not prepare operation: ${it.message ?: it::class.simpleName}" }
            launching = false
        }
    }

    CommandLaunchScreen(title = "New Operation", onBack = onBack) {
        CommandPanel(Modifier.fillMaxSize().commandPanelWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OPERATIONAL DETAILS", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                if (loadingMap || launching) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(
                    modifier = Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = operationName,
                        onValueChange = { operationName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Operation Name") },
                        singleLine = true,
                    )
                    Text("MAP IMAGE", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CommandButton(onClick = ::chooseMap, enabled = !loadingMap && !launching) {
                            Text("CHOOSE MAP IMAGE")
                        }
                        Text(
                            mapName.ifBlank { "No map selected" },
                            color = if (mapImage == null) MaterialTheme.colorScheme.error else CommandColours.Backing,
                        )
                    }
                    Text("COORDINATE SYSTEM", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        io.hsar.mapgenerator.grid.ReferenceSystem.entries.forEach { system ->
                            CommandButton(
                                onClick = { referenceSystem = system },
                                enabled = !launching,
                            ) {
                                Text(if (system == referenceSystem) "[ ${system.name} ]" else system.name)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = mapId,
                        onValueChange = { mapId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Map ID") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = startX,
                            onValueChange = { startX = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Starting X") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = startY,
                            onValueChange = { startY = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Starting Y") },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = unitsPerPixel,
                        onValueChange = { unitsPerPixel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Metres Per Pixel") },
                        singleLine = true,
                    )
                }
                Box(
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                        .background(CommandColours.Main)
                        .border(1.dp, CommandColours.Light),
                    contentAlignment = Alignment.Center,
                ) {
                    mapImage?.let {
                        Image(
                            bitmap = remember(it) { it.toImageBitmap() },
                            contentDescription = "Selected operation map",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } ?: Text("MAP PREVIEW", color = CommandColours.Backing)
                }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                CommandButton(
                    onClick = ::launch,
                    enabled = operationName.isNotBlank() && mapImage != null &&
                            unitsPerPixel.toDoubleOrNull()?.let { it > 0.0 } == true &&
                            !loadingMap && !launching,
                ) {
                    Text(if (launching) "PREPARING OPERATION…" else "LAUNCH OPERATION")
                }
            }
        }
    }
}

@Composable
private fun ComposeExistingOperationsScreen(
    repository: MapViewerSaveRepository,
    awtWindow: java.awt.Window,
    onBack: () -> Unit,
    onMapLoaded: (OperationDocument) -> Unit,
) {
    var saves by remember { mutableStateOf<List<MapViewerSavedOperation>>(emptyList()) }
    var refreshing by remember { mutableStateOf(true) }
    var loadingPath by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        if (refreshing && saves.isNotEmpty()) return
        refreshing = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.list() } }
                .onSuccess {
                    saves = it
                    errorMessage = null
                }
                .onFailure { errorMessage = "Could not list saves: ${it.message ?: it::class.simpleName}" }
            refreshing = false
        }
    }

    fun load(path: java.nio.file.Path) {
        if (loadingPath != null) return
        loadingPath = path.toString()
        errorMessage = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.loadOperation(path) } }
                .onSuccess(onMapLoaded)
                .onFailure { errorMessage = "Could not load ${path.fileName}: ${it.message ?: it::class.simpleName}" }
            loadingPath = null
        }
    }

    fun browse() {
        FileDialog(awtWindow as? Frame, "Open operation save", FileDialog.LOAD).also { dialog ->
            dialog.directory = repository.savesDirectory.toString()
            dialog.file = "*.zip"
            dialog.isVisible = true
            val fileName = dialog.file ?: return
            load(File(dialog.directory, fileName).toPath())
        }
    }

    LaunchedEffect(Unit) { refresh() }

    CommandLaunchScreen(title = "Ongoing Operations", onBack = onBack) {
        CommandPanel(
            modifier = Modifier.fillMaxSize().commandPanelWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SAVED OPERATIONS", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                if (refreshing || loadingPath != null) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                CommandButton(onClick = ::browse, enabled = loadingPath == null) {
                    Text("BROWSE FOR SAVE")
                }
                CommandButton(onClick = ::refresh, enabled = !refreshing) {
                    Text(if (refreshing) "REFRESHING…" else "REFRESH")
                }
            }
            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (!refreshing && saves.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No valid saves found in\n${repository.savesDirectory}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(saves, key = { it.path.toString() }) { save ->
                        SavedOperationCard(
                            operation = save,
                            loading = loadingPath == save.path.toString(),
                            enabled = loadingPath == null,
                            onClick = { load(save.path) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedOperationCard(
    operation: MapViewerSavedOperation,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val thumbnail = remember(operation.thumbnail) { operation.thumbnail?.toImageBitmap() }
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RectangleShape,
        border = BorderStroke(1.dp, CommandColours.Light),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(116.dp).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 160.dp, height = 96.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                thumbnail?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "${operation.operationName} map preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } ?: Text("No preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(operation.operationName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Updated ${SAVE_DATE_FORMAT.format(operation.updatedAt.atZone(ZoneId.systemDefault()))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    operation.path.fileName.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (loading) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
            else Text("Open", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private val SAVE_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm")
private const val COMPOSE_OPERATION_MAP_ID = "compose-operation-map"
private const val COMPOSE_AUTOSAVE_DEBOUNCE_MS = 2_000L

@Composable
private fun ViewerWorkspace(
    initialMap: ComposeActiveMap,
    repository: MapViewerSaveRepository,
    initialUnitIconSizePx: Double,
    onUnitIconSizeChanged: (Double) -> Double,
    onWorkspaceModeRequestHandlerChanged: ((WorkspaceMode) -> Unit) -> Unit,
    onReturnToLaunchpad: () -> Unit,
    onExitProgram: () -> Unit,
    onCloseRequestHandlerChanged: (() -> Unit) -> Unit,
) {
    var operationState by remember(initialMap.document) {
        mutableStateOf(ComposeOperationState(initialMap.document))
    }
    var unitIconSizePx by remember { mutableStateOf(UnitIconDisplaySize.coerce(initialUnitIconSizePx)) }
    var escapeMenuOpen by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ComposeMapContextMenu?>(null) }
    var drawingMode by remember { mutableStateOf(MapViewerDrawingMode.FREEFORM) }
    var drawingColour by remember { mutableStateOf(MapViewerDrawingColour.RED) }
    var toolGestureCancellationEpoch by remember { mutableStateOf(0) }
    var pendingExit by remember { mutableStateOf<ComposePendingExit?>(null) }
    var unitLibraryExpanded by remember { mutableStateOf(true) }
    var graphicLibraryExpanded by remember { mutableStateOf(false) }
    var transientNotice by remember { mutableStateOf<String?>(null) }
    val autosaveScheduler = remember { ComposeAutosaveScheduler(debounceMillis = COMPOSE_AUTOSAVE_DEBOUNCE_MS) }
    val scope = rememberCoroutineScope()

    fun autosaveObservation() = ComposeAutosaveObservation(
        revision = operationState.revision,
        savedRevision = operationState.savedRevision,
        savingRevision = operationState.savingRevision,
        failed = operationState.saveStatus == ComposeSaveStatus.FAILED,
        exiting = pendingExit != null,
    )

    fun saveNow(onSuccess: (() -> Unit)? = null) {
        if (operationState.savingRevision != null) return
        val snapshot = operationState
        val targetRevision = snapshot.revision
        operationState = operationState.saveStarted(targetRevision)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.saveOperation(snapshot.document, snapshot.savedCamera(), snapshot.operationUiState())
                }
            }.onSuccess { saved ->
                operationState = operationState.saveSucceeded(saved, targetRevision)
                if (operationState.revision == targetRevision) onSuccess?.invoke()
            }.onFailure { failure ->
                operationState = operationState.saveFailed(failure.message ?: failure::class.simpleName ?: "Unknown save failure")
            }
        }
    }

    fun requestExit(target: ComposePendingExit) {
        autosaveScheduler.cancel()
        pendingExit = target
        saveNow {
            pendingExit = null
            when (target) {
                ComposePendingExit.LAUNCHPAD -> onReturnToLaunchpad()
                ComposePendingExit.DESKTOP -> onExitProgram()
            }
        }
    }

    val currentExitRequester by rememberUpdatedState<(ComposePendingExit) -> Unit>(::requestExit)

    DisposableEffect(Unit) {
        onCloseRequestHandlerChanged { currentExitRequester(ComposePendingExit.DESKTOP) }
        onDispose { onCloseRequestHandlerChanged(onExitProgram) }
    }

    LaunchedEffect(operationState.revision, operationState.savedRevision, operationState.savingRevision) {
        val waitMillis = autosaveScheduler.observe(autosaveObservation()) ?: return@LaunchedEffect
        kotlinx.coroutines.delay(waitMillis)
        if (autosaveScheduler.consumeIfDue(autosaveObservation()) != null) saveNow()
    }

    LaunchedEffect(pendingExit, operationState.savingRevision, operationState.saveStatus) {
        val target = pendingExit ?: return@LaunchedEffect
        if (operationState.savingRevision == null && operationState.saveStatus != ComposeSaveStatus.FAILED) {
            saveNow {
                pendingExit = null
                if (target == ComposePendingExit.LAUNCHPAD) onReturnToLaunchpad() else onExitProgram()
            }
        }
    }

    val currentWorkspaceModeRequester by rememberUpdatedState<(WorkspaceMode) -> Unit> { mode ->
        operationState = operationState.updateWorkspaceMode(mode)
    }
    DisposableEffect(Unit) {
        onWorkspaceModeRequestHandlerChanged { currentWorkspaceModeRequester(it) }
        onDispose { onWorkspaceModeRequestHandlerChanged {} }
    }

    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            when {
                event.key == Key.Escape && operationState.previewContent != null -> {
                    operationState = operationState.cancelPreview().cancelPlacements()
                    toolGestureCancellationEpoch++
                    true
                }

                event.key == Key.Escape && (operationState.unitPlacement != null || operationState.graphicPlacement != null) -> {
                    operationState = operationState.cancelPlacements()
                    true
                }

                event.key == Key.Escape && operationState.activeTool in setOf(MapViewerTool.DRAW, MapViewerTool.MEASURE) -> {
                    toolGestureCancellationEpoch++
                    true
                }

                event.key == Key.Escape -> {
                    escapeMenuOpen = !escapeMenuOpen
                    true
                }

                event.key == Key.Delete && !operationState.isReadOnly -> {
                    operationState = operationState.deleteSelection()
                    true
                }

                event.isCtrlPressed && event.key == Key.S -> {
                    saveNow()
                    true
                }

                event.isCtrlPressed && event.key == Key.Z && event.isShiftPressed -> {
                    operationState = operationState.redo()
                    true
                }

                event.isCtrlPressed && event.key == Key.Z -> {
                    operationState = operationState.undo()
                    true
                }

                event.isCtrlPressed && event.key == Key.Y -> {
                    operationState = operationState.redo()
                    true
                }

                else -> false
            }
        },
    ) {
        Column {
            Row(Modifier.weight(1f)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    MapCanvas(
                        renderData = operationState.document.renderData,
                        savedCamera = operationState.document.camera,
                        content = operationState.visibleContent,
                        viewport = operationState.viewport,
                        onViewportChanged = { operationState = operationState.updateViewport(it) },
                            activeTool = operationState.activeTool,
                        editable = !operationState.isReadOnly,
                            selectedObjectIds = operationState.selectedObjectIds,
                            onObjectsSelected = { ids, additive -> operationState = operationState.selectBox(ids, additive) },
                            onContextMenuRequested = { mapObject, position ->
                                operationState = operationState.selectObject(mapObject.id)
                                contextMenu = ComposeMapContextMenu(mapObject.id, mapObject.displayName(operationState.visibleContent), position, mapObject.contextMenuItems())
                            },
                            onContextMenuDismissed = { contextMenu = null },
                        onPreviewChanged = { operationState = operationState.preview(it) },
                        onPreviewCommitted = { description -> operationState = operationState.commitPreview(description = description) },
                        drawingMode = drawingMode,
                        drawingColour = drawingColour,
                        onDrawingCommitted = { operationState = operationState.addDrawing(it) },
                        unitPlacementActive = operationState.unitPlacement != null,
                        onUnitPlacementPreview = { coordinate -> operationState = operationState.previewUnitPlacement(coordinate, unitIconSizePx) },
                        onUnitPlacementCommitted = { operationState = operationState.commitUnitPlacement() },
                        onUnitPlacementCancelled = { operationState = operationState.cancelUnitPlacement() },
                        graphicPlacementActive = operationState.graphicPlacement != null,
                        onGraphicPlacementPreview = { coordinate -> operationState = operationState.previewGraphicPlacement(coordinate) },
                        onGraphicPlacementCommitted = { operationState = operationState.commitGraphicPlacement() },
                        onGraphicPlacementCancelled = { operationState = operationState.cancelGraphicPlacement() },
                        cancellationEpoch = toolGestureCancellationEpoch,
                            unitIconSizePx = unitIconSizePx,
                            onUnitIconSizeChanged = { size -> unitIconSizePx = onUnitIconSizeChanged(size) },
                            modifier = Modifier.fillMaxSize(),
                        )

                    ToolRail(
                        activeTool = operationState.activeTool,
                        drawingMode = drawingMode,
                        drawingColour = drawingColour,
                        editable = !operationState.isReadOnly,
                        onToolSelected = {
                            operationState = operationState.selectTool(it)
                            if (it != MapViewerTool.DRAW) drawingMode = MapViewerDrawingMode.FREEFORM
                        },
                        onDrawingModeSelected = { drawingMode = it },
                        onDrawingColourSelected = { drawingColour = it },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    )
                    Column(
                        Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UnitTypesPalette(
                            templates = operationState.visibleContent.unitTemplates,
                            expanded = unitLibraryExpanded,
                            editable = !operationState.isReadOnly,
                            activePlacement = operationState.unitPlacement?.templateId,
                            onToggle = {
                                unitLibraryExpanded = !unitLibraryExpanded
                                if (unitLibraryExpanded) graphicLibraryExpanded = false
                            },
                            onCreate = { operationState = operationState.openNewUnitTemplateEditor() },
                            onEdit = { operationState = operationState.openUnitTemplateEditor(it) },
                            onPlace = { operationState = operationState.armUnitPlacement(it) },
                            onDelete = { id ->
                                if (!OperationUnitAuthoring.canDeleteTemplate(operationState.document.content, id)) {
                                    transientNotice = "Delete or retarget placed units before deleting this unit type."
                                } else operationState = operationState.deleteUnitTemplate(id)
                            },
                        )
                        GraphicTypesPalette(
                            templates = operationState.visibleContent.graphicTemplates,
                            expanded = graphicLibraryExpanded,
                            editable = !operationState.isReadOnly,
                            activePlacement = operationState.graphicPlacement?.templateId,
                            onToggle = {
                                graphicLibraryExpanded = !graphicLibraryExpanded
                                if (graphicLibraryExpanded) unitLibraryExpanded = false
                            },
                            onCreate = { operationState = operationState.openNewGraphicTemplateEditor() },
                            onEdit = { operationState = operationState.openGraphicTemplateEditor(it) },
                            onPlace = { operationState = operationState.armGraphicPlacement(it) },
                            onDelete = { id ->
                                if (!OperationGraphicAuthoring.canDeleteTemplate(operationState.document.content, id)) {
                                    transientNotice = "Delete or retarget placed graphics before deleting this graphic type."
                                } else operationState = operationState.deleteGraphicTemplate(id)
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CommandButton(onClick = { escapeMenuOpen = true }) { Text("MENU") }
                        CommandButton(onClick = { operationState = operationState.openNotes() }) { Text("NOTES") }
                    }
                    contextMenu?.let { menu ->
                        ComposeMapContextMenu(
                            menu = menu,
                            onActionSelected = { action ->
                                when (action.action) {
                                    MapViewerContextMenuCommand.EDIT_UNIT -> operationState = operationState.openUnitEditor(menu.objectId)
                                    MapViewerContextMenuCommand.EDIT_GRAPHIC -> operationState = operationState.openGraphicEditor(menu.objectId)
                                    else -> if (action.action.name.startsWith("DELETE")) {
                                        operationState = operationState.selectObject(menu.objectId).deleteSelection()
                                    }
                                }
                                contextMenu = null
                            },
                            onDismiss = { contextMenu = null },
                        )
                    }

                    if (operationState.workspaceMode == WorkspaceMode.FLOATING) {
                        FloatingWorkspace(
                            state = operationState,
                            onStateChanged = { operationState = it },
                        )
                    }
                    transientNotice?.let { notice ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clickable { transientNotice = null },
                            color = CommandColours.Backing,
                            border = BorderStroke(1.dp, CommandColours.White),
                            shape = RectangleShape,
                        ) { Text(notice, Modifier.padding(12.dp), color = CommandColours.White) }
                    }
                }
                if (operationState.workspaceMode == WorkspaceMode.DOCKED) {
                    DockedWorkspace(
                        state = operationState,
                        onStateChanged = { operationState = it },
                    )
                }
            }
            OperationLedgerPanel(
                state = operationState,
                onUndo = { operationState = operationState.undo() },
                onRedo = { operationState = operationState.redo() },
                onPreviewIndex = { operationState = operationState.previewHistory(it) },
                onCurrent = { operationState = operationState.returnToCurrent() },
                onSave = { saveNow() },
            )
        }
    }
    if (escapeMenuOpen) {
        EscapeMenuDialog(
            onDismiss = { escapeMenuOpen = false },
            onReturnToLaunchpad = {
                escapeMenuOpen = false
                requestExit(ComposePendingExit.LAUNCHPAD)
            },
            onExitProgram = { requestExit(ComposePendingExit.DESKTOP) },
        )
    }
    if (pendingExit != null && operationState.saveStatus == ComposeSaveStatus.FAILED) {
        SaveFailureDialog(
            message = operationState.saveFailure.orEmpty(),
            onRetry = {
                val target = pendingExit ?: return@SaveFailureDialog
                saveNow {
                    pendingExit = null
                    if (target == ComposePendingExit.LAUNCHPAD) onReturnToLaunchpad() else onExitProgram()
                }
            },
            onCancel = { pendingExit = null },
        )
    }
    }
}

@Composable
private fun ToolRail(
    activeTool: MapViewerTool,
    drawingMode: MapViewerDrawingMode,
    drawingColour: MapViewerDrawingColour,
    editable: Boolean,
    onToolSelected: (MapViewerTool) -> Unit,
    onDrawingModeSelected: (MapViewerDrawingMode) -> Unit,
    onDrawingColourSelected: (MapViewerDrawingColour) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MapViewerTool.entries.forEach { tool ->
            CommandButton(onClick = { onToolSelected(tool) }, active = tool == activeTool, enabled = editable || tool == MapViewerTool.MEASURE) { Text(tool.name) }
        }
    }
    if (editable && activeTool == MapViewerTool.DRAW) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MapViewerDrawingMode.entries.forEach { mode ->
                CommandButton(
                    onClick = { onDrawingModeSelected(mode) },
                    active = drawingMode == mode,
                    modifier = Modifier.height(34.dp)
                ) { Text(mode.label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapViewerDrawingColour.entries.forEach { colour ->
                Box(Modifier.size(28.dp).border(2.dp, CommandColours.White).background(colour.argb.toComposeColour()).clickable { onDrawingColourSelected(colour) }
                    .then(if (colour == drawingColour) Modifier.border(3.dp, CommandColours.Dark) else Modifier))
            }
        }
    }
}

@Composable
private fun UnitTypesPalette(
    templates: List<OperationUnitTemplate>,
    expanded: Boolean,
    editable: Boolean,
    activePlacement: OperationUnitTemplateId?,
    onToggle: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (OperationUnitTemplateId) -> Unit,
    onPlace: (OperationUnitTemplateId) -> Unit,
    onDelete: (OperationUnitTemplateId) -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.width(if (expanded) 320.dp else 92.dp),
    color = CommandColours.Dark.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.7f)),
    shape = RectangleShape,
) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "UNIT TYPES ◀" else "UNITS ▶") }
        if (expanded) {
            CommandButton(onClick = onCreate, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("CREATE UNIT TYPE") }
            if (templates.isEmpty()) Text("No unit types", color = CommandColours.White.copy(alpha = 0.7f))
            LazyColumn(Modifier.height(430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (OperationUnitAffiliation.entries.map { it as OperationUnitAffiliation? } + null).forEach { affiliation ->
                    val group = templates.filter { it.affiliation == affiliation }
                    if (group.isNotEmpty()) {
                        item(key = "affiliation:${affiliation?.name ?: "none"}") {
                            Text(affiliation?.name ?: "UNAFFILIATED", color = affiliation.composeColour(), fontWeight = FontWeight.Bold)
                        }
                        items(group, key = { it.id.value }) { template ->
                            Surface(
                                color = if (activePlacement == template.id) CommandColours.Backing else CommandColours.Dark,
                                border = BorderStroke(1.dp, template.affiliation.composeColour()),
                                shape = RectangleShape,
                            ) {
                                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.foundation.Image(
                                            bitmap = template.image.toComposeImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(42.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Text(template.name, Modifier.weight(1f), color = CommandColours.White)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CommandButton(onClick = { onPlace(template.id) }, enabled = editable, active = activePlacement == template.id, modifier = Modifier.weight(1f).height(32.dp)) { Text("PLACE") }
                                        CommandButton(onClick = { onEdit(template.id) }, enabled = editable, modifier = Modifier.weight(1f).height(32.dp)) { Text("EDIT") }
                                        CommandButton(onClick = { onDelete(template.id) }, enabled = editable, modifier = Modifier.weight(1f).height(32.dp)) { Text("DELETE") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphicTypesPalette(
    templates: List<OperationGraphicTemplate>,
    expanded: Boolean,
    editable: Boolean,
    activePlacement: OperationGraphicTemplateId?,
    onToggle: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (OperationGraphicTemplateId) -> Unit,
    onPlace: (OperationGraphicTemplateId) -> Unit,
    onDelete: (OperationGraphicTemplateId) -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.width(if (expanded) 320.dp else 92.dp),
    color = CommandColours.Dark.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.7f)),
    shape = RectangleShape,
) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "GRAPHICS ◀" else "GRAPHICS ▶") }
        if (expanded) {
            CommandButton(onClick = onCreate, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("CREATE GRAPHIC TYPE") }
            if (templates.isEmpty()) Text("No graphic types", color = CommandColours.White.copy(alpha = 0.7f))
            LazyColumn(Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates.sortedBy(OperationGraphicTemplate::index), key = { it.id.value }) { template ->
                    Surface(
                        color = if (activePlacement == template.id) CommandColours.Backing else CommandColours.Dark,
                        border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.7f)),
                        shape = RectangleShape,
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.foundation.Image(template.image.toComposeImageBitmap(), null, Modifier.size(42.dp), contentScale = ContentScale.Fit)
                                Text(template.name, Modifier.weight(1f), color = CommandColours.White)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CommandButton({ onPlace(template.id) }, enabled = editable, active = activePlacement == template.id, modifier = Modifier.weight(1f).height(32.dp)) { Text("PLACE") }
                                CommandButton({ onEdit(template.id) }, enabled = editable, modifier = Modifier.weight(1f).height(32.dp)) { Text("EDIT") }
                                CommandButton({ onDelete(template.id) }, enabled = editable, modifier = Modifier.weight(1f).height(32.dp)) { Text("DELETE") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun OperationUnitAffiliation?.composeColour() = when (this) {
    OperationUnitAffiliation.FRIEND -> Color(0xFF4EA1FF)
    OperationUnitAffiliation.HOSTILE -> Color(0xFFDF4A4A)
    OperationUnitAffiliation.NEUTRAL -> Color(0xFF66C27C)
    OperationUnitAffiliation.UNKNOWN -> Color(0xFFE6C45A)
    null -> CommandColours.White.copy(alpha = 0.75f)
}

@Composable
private fun NotesPanel(
    note: String,
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text("Inspector & notes", style = MaterialTheme.typography.titleMedium)
    Text("Compose provides native cursor, selection, wrapping, clipboard, IME, and newline handling.")
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChanged,
        modifier = Modifier.fillMaxWidth().weight(1f),
        label = { Text("Operation notes") },
        minLines = 8,
    )
}

@Composable
private fun DockedWorkspace(
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
) {
    val dock = state.workspace.dockRegions.firstOrNull { it.region == OperationDockRegion.RIGHT }
        ?: return
    var previewWidth by remember(dock.size) { mutableStateOf(dock.size) }
    var resizeStart by remember { mutableStateOf(dock.size) }
    var resizeDistance by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    Row(Modifier.requiredWidth(if (dock.collapsed) 48.dp else previewWidth.dp).fillMaxHeight()) {
        Box(
            Modifier.width(8.dp).fillMaxHeight().background(CommandColours.Dark).pointerInput(dock.region) {
                detectDragGestures(
                    onDragStart = { resizeStart = dock.size; resizeDistance = 0f },
                    onDragEnd = { onStateChanged(state.updateDockSize(OperationDockRegion.RIGHT, previewWidth)) },
                ) { change, amount ->
                    change.consume()
                    resizeDistance -= amount.x
                    previewWidth = (resizeStart + with(density) { resizeDistance.toDp().value }).coerceIn(240.0, 720.0)
                }
            },
        )
        Column(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)) {
            CommandButton(
                onClick = { onStateChanged(state.toggleDockCollapsed(OperationDockRegion.RIGHT)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (dock.collapsed) "OPEN" else "COLLAPSE") }
            if (!dock.collapsed) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    dock.editorIds.mapNotNull(state.workspace::editor).forEach { editor ->
                        CommandButton(
                            onClick = { onStateChanged(state.activateDockTab(OperationDockRegion.RIGHT, editor.id)) },
                            active = editor.id == dock.activeEditorId,
                        ) { Text(editor.title(state.visibleContent)) }
                    }
                }
                dock.activeEditorId?.let(state.workspace::editor)?.let { editor ->
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(editor.title(state.visibleContent), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            CommandButton(onClick = { onStateChanged(state.closeEditor(editor.id)) }) { Text("×") }
                        }
                        EditorContent(editor, state, onStateChanged, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingWorkspace(
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
) = state.workspace.editors.forEach { editor ->
    key(editor.id.value) {
        FloatingEditorWindow(editor, state, onStateChanged)
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun FloatingEditorWindow(
    editor: OperationEditorState,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
) {
    val density = LocalDensity.current
    val densityScale = density.density.toDouble()
    val viewport = state.viewport
    val projected = editor.displayBounds(state, densityScale).clampedTo(
        (viewport?.viewportWidth ?: Int.MAX_VALUE) / densityScale,
        (viewport?.viewportHeight ?: Int.MAX_VALUE) / densityScale,
    )
    var bounds by remember(editor.id, projected) { mutableStateOf(projected) }
    var dragStart by remember(editor.id) { mutableStateOf(projected) }
    var dragDelta by remember(editor.id) { mutableStateOf(Offset.Zero) }
    fun clamped(value: OperationEditorBounds) = value.clampedTo(
        (viewport?.viewportWidth ?: Int.MAX_VALUE) / densityScale,
        (viewport?.viewportHeight ?: Int.MAX_VALUE) / densityScale,
    )

    fun commit(value: OperationEditorBounds) {
        var next = state.updateFloatingEditor(editor.id, value)
        if (editor.floating.pinned) {
            next = next.pinEditor(editor.id, ViewerPoint((value.left + value.width) * densityScale, value.top * densityScale))
        }
        onStateChanged(next)
    }

    Box(
        Modifier.offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .width(bounds.width.dp)
            .height(if (editor.floating.minimized) 42.dp else bounds.height.dp)
            .onPointerEvent(PointerEventType.Press) { onStateChanged(state.activateEditor(editor.id)) },
    ) {
        CommandFloatingPanel(
            title = editor.title(state.visibleContent),
            onClose = { onStateChanged(state.closeEditor(editor.id)) },
            modifier = Modifier.fillMaxSize(),
            titleBarModifier = Modifier.pointerInput(editor.id) {
                detectDragGestures(
                    onDragStart = { dragStart = bounds; dragDelta = Offset.Zero },
                    onDragEnd = { commit(bounds) },
                ) { change, amount ->
                    change.consume()
                    dragDelta += amount
                    bounds = clamped(
                        dragStart.copy(
                            left = dragStart.left + (dragDelta.x / density.density).toDouble(),
                            top = dragStart.top + (dragDelta.y / density.density).toDouble(),
                        )
                    )
                }
            },
            titleActions = {
                CommandButton(onClick = { onStateChanged(state.toggleEditorMinimized(editor.id)) }) {
                    Text(if (editor.floating.minimized) "+" else "−")
                }
                CommandButton(onClick = {
                    if (editor.floating.pinned) onStateChanged(state.unpinEditor(editor.id, bounds))
                    else onStateChanged(state.pinEditor(editor.id, ViewerPoint((bounds.left + bounds.width) * densityScale, bounds.top * densityScale)))
                }, active = editor.floating.pinned) { Text("PIN") }
            },
        ) {
            if (!editor.floating.minimized) EditorContent(editor, state, onStateChanged, Modifier.weight(1f).fillMaxWidth())
        }
        if (!editor.floating.minimized) Canvas(
            Modifier.align(Alignment.BottomEnd).size(30.dp).pointerInput(editor.id) {
                detectDragGestures(
                    onDragStart = { dragStart = bounds; dragDelta = Offset.Zero },
                    onDragEnd = { commit(bounds) },
                ) { change, amount ->
                    change.consume()
                    dragDelta += amount
                    bounds = clamped(
                        dragStart.copy(
                            width = clampedDialogDimensionAtPointer(dragStart.width.toFloat(), dragDelta.x / density.density, 320f).toDouble(),
                            height = clampedDialogDimensionAtPointer(dragStart.height.toFloat(), dragDelta.y / density.density, 240f).toDouble(),
                        )
                    )
                }
            },
        ) {
            val inset = size.minDimension * 0.15f
            drawLine(CommandColours.White, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), strokeWidth = 2f)
            drawLine(CommandColours.White, Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), strokeWidth = 2f)
        }
    }
}

@Composable
private fun EditorContent(
    editor: OperationEditorState,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) = when (val draft = editor.draft) {
    is OperationEditorDraft.Notes -> NotesPanel(
        note = draft.body,
        onNoteChanged = { onStateChanged(state.updateEditorDraft(editor.id, draft.copy(body = it))) },
        modifier = modifier,
    )

    is OperationEditorDraft.Unit -> {
        val target = editor.target as? OperationEditorTarget.Object
        val unit = target?.objectId?.let(state.visibleContent::objectById) as? OperationUnit
        if (unit == null) Box(modifier, contentAlignment = Alignment.Center) {
            Text(if (state.isReadOnly) "This unit is unavailable at this history point." else "This unit no longer exists.")
        } else UnitEditorContent(editor.id, draft, unit, state, onStateChanged, modifier)
    }

    is OperationEditorDraft.UnitTemplate -> {
        val target = editor.target as? OperationEditorTarget.UnitTemplate
        if (target != null && state.visibleContent.unitTemplate(target.templateId) == null) {
            Box(modifier, contentAlignment = Alignment.Center) { Text("This unit type is unavailable at this history point.") }
        } else UnitTemplateEditorContent(editor, draft.value, state, onStateChanged, modifier)
    }

    is OperationEditorDraft.Graphic -> {
        val target = editor.target as? OperationEditorTarget.Object
        val graphic = target?.objectId?.let(state.visibleContent::objectById) as? OperationGraphic
        if (graphic == null) Box(modifier, contentAlignment = Alignment.Center) {
            Text(if (state.isReadOnly) "This graphic is unavailable at this history point." else "This graphic no longer exists.")
        } else GraphicEditorContent(editor.id, draft.value, graphic, state, onStateChanged, modifier)
    }

    is OperationEditorDraft.GraphicTemplate -> {
        val target = editor.target as? OperationEditorTarget.GraphicTemplate
        if (target != null && state.visibleContent.graphicTemplate(target.templateId) == null) {
            Box(modifier, contentAlignment = Alignment.Center) { Text("This graphic type is unavailable at this history point.") }
        } else GraphicTemplateEditorContent(editor, draft.value, state, onStateChanged, modifier)
    }

    is OperationEditorDraft.Opaque -> Box(modifier, contentAlignment = Alignment.Center) { Text("This restored editor is not supported yet.") }
}

@Composable
private fun UnitEditorContent(
    editorId: OperationEditorId,
    draft: OperationEditorDraft.Unit,
    unit: OperationUnit,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coordinate = OperationUnitAuthoring.parsePosition(state.document.renderData, draft.positionX, draft.positionY)
    val facingDegrees = draft.facing.trim().takeIf(String::isNotEmpty)?.toIntOrNull()?.mod(360)
    val validation = OperationUnitAuthoring.validateUnitDraft(
        draft.name, draft.positionX, draft.positionY, draft.facing, draft.properties, state.document.renderData,
    )
    val selectedTemplate = draft.unitTemplateId?.let(state.document.content::unitTemplate)
        ?: state.document.content.unitTemplate(unit.unitTemplateId)
    val valid = validation.valid && selectedTemplate != null
    fun update(value: OperationEditorDraft.Unit) = onStateChanged(state.updateEditorDraft(editorId, value))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.visibleContent.unitTemplate(unit.unitTemplateId)?.name ?: "Unit", color = CommandColours.Backing)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth(), enabled = !state.isReadOnly, singleLine = true, label = { Text("Name") })
            Text("Unit type", color = CommandColours.Backing)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.document.content.unitTemplates.forEach { template ->
                    CommandButton(onClick = { update(draft.copy(unitTemplateId = template.id, affiliation = template.affiliation)) }, active = selectedTemplate?.id == template.id, enabled = !state.isReadOnly) { Text(template.name) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    draft.positionX,
                    { update(draft.copy(positionX = it)) },
                    Modifier.weight(1f),
                    enabled = !state.isReadOnly,
                    singleLine = true,
                    isError = "position" in validation.errors,
                    label = { Text(if (state.document.renderData.gridLayer == null) "Map X" else "Easting") })
                OutlinedTextField(
                    draft.positionY,
                    { update(draft.copy(positionY = it)) },
                    Modifier.weight(1f),
                    enabled = !state.isReadOnly,
                    singleLine = true,
                    isError = "position" in validation.errors,
                    label = { Text(if (state.document.renderData.gridLayer == null) "Map Y" else "Northing") })
            }
            OutlinedTextField(
                draft.facing,
                { update(draft.copy(facing = it)) },
                Modifier.fillMaxWidth(),
                enabled = !state.isReadOnly,
                singleLine = true,
                isError = draft.facing.isNotBlank() && facingDegrees == null,
                label = { Text("Facing (0–359, blank for none)") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(draft.destroyed, { update(draft.copy(destroyed = it)) }, enabled = !state.isReadOnly)
                Text("Destroyed")
            }
            Text("Affiliation", color = CommandColours.Backing)
            AffiliationButtons(draft.affiliation, !state.isReadOnly) { update(draft.copy(affiliation = it)) }
            if (selectedTemplate?.customProperties?.isNotEmpty() == true) {
                Text("Inherited from unit type", color = CommandColours.Backing)
                selectedTemplate.customProperties.toSortedMap().forEach { (key, value) ->
                    Text("$key: $value", color = CommandColours.Backing.copy(alpha = 0.75f))
                }
            }
            PropertyRows(draft.properties, !state.isReadOnly, validation.errors) { update(draft.copy(properties = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CommandButton(onClick = {
                val updated = unit.copy(
                    instanceName = draft.name.trim(),
                    unitTemplateId = selectedTemplate?.id ?: unit.unitTemplateId,
                    coordinate = coordinate ?: unit.coordinate,
                    facingDegrees = facingDegrees,
                    destroyed = draft.destroyed,
                    affiliation = draft.affiliation,
                    customProperties = OperationUnitAuthoring.properties(draft.properties),
                    normalizedHeight = selectedTemplate?.let { template ->
                        unit.normalizedWidth * state.document.renderData.baseImage.width / state.document.renderData.baseImage.height.toDouble() *
                            template.image.height / template.image.width.toDouble()
                    } ?: unit.normalizedHeight,
                )
                onStateChanged(state.editUnit(updated).closeEditor(editorId))
            }, enabled = !state.isReadOnly && valid, modifier = Modifier.weight(1f)) { Text("SAVE") }
            CommandButton(onClick = { onStateChanged(state.closeEditor(editorId)) }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
    }
}

@Composable
private fun UnitTemplateEditorContent(
    editor: OperationEditorState,
    draft: OperationUnitTemplateDraft,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var imageError by remember(editor.id) { mutableStateOf<String?>(null) }
    val existing = (editor.target as? OperationEditorTarget.UnitTemplate)?.templateId?.let(state.document.content::unitTemplate)
    val decoded = remember(draft.replacementImage) {
        draft.replacementImage?.let { runCatching { MapViewerImageLoader.load(it.bytes(), it.originalFilename) }.getOrNull() }
    }
    val validation = OperationUnitAuthoring.validateTemplateDraft(draft, existing != null)
    fun update(value: OperationUnitTemplateDraft) = onStateChanged(
        state.updateEditorDraft(editor.id, OperationEditorDraft.UnitTemplate(value)),
    )
    fun browse() {
        FileDialog(null as Frame?, "Choose unit image", FileDialog.LOAD).also { dialog ->
            dialog.file = "*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp;*.svg"
            dialog.isVisible = true
            val name = dialog.file ?: return
            val file = File(dialog.directory, name)
            runCatching {
                val imageDraft = OperationImageDraft.fromBytes(file.name, file.readBytes())
                require(MapViewerImageLoader.load(imageDraft.bytes(), imageDraft.originalFilename) != null) { "Unsupported image" }
                imageDraft
            }.onSuccess {
                imageError = null
                update(draft.copy(replacementImage = it))
            }.onFailure { imageError = it.message ?: "Could not load the selected image" }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val preview = decoded ?: existing?.image
            preview?.let { androidx.compose.foundation.Image(it.toComposeImageBitmap(), null, Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit) }
            CommandButton(onClick = ::browse, enabled = !state.isReadOnly, modifier = Modifier.fillMaxWidth()) {
                Text(if (preview == null) "CHOOSE IMAGE" else "REPLACE IMAGE")
            }
            (imageError ?: validation.errors["image"])?.let { Text(it, color = Color(0xFFDF4A4A)) }
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth(), enabled = !state.isReadOnly, isError = "name" in validation.errors, singleLine = true, label = { Text("Name") })
            Text("Affiliation", color = CommandColours.Backing)
            AffiliationButtons(draft.affiliation, !state.isReadOnly) { update(draft.copy(affiliation = it)) }
            PropertyRows(draft.properties, !state.isReadOnly, validation.errors) { update(draft.copy(properties = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CommandButton(onClick = { onStateChanged(state.saveUnitTemplateEditor(editor.id, decoded)) }, enabled = !state.isReadOnly && validation.valid, modifier = Modifier.weight(1f)) { Text("SAVE") }
            CommandButton(onClick = { onStateChanged(state.closeEditor(editor.id)) }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
    }
}

@Composable
private fun GraphicEditorContent(
    editorId: OperationEditorId,
    draft: io.hsar.mapgenerator.commandgui.operation.OperationGraphicDraft,
    graphic: OperationGraphic,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validation = OperationGraphicAuthoring.validateGraphicDraft(draft, state.document.content, state.document.renderData)
    val selectedTemplate = draft.graphicTemplateId?.let(state.document.content::graphicTemplate)
    fun update(value: io.hsar.mapgenerator.commandgui.operation.OperationGraphicDraft) =
        onStateChanged(state.updateEditorDraft(editorId, OperationEditorDraft.Graphic(value)))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            selectedTemplate?.let { template ->
                androidx.compose.foundation.Image(template.image.toComposeImageBitmap(), null, Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Fit)
            }
            OutlinedTextField(draft.displayName, { update(draft.copy(displayName = it)) }, Modifier.fillMaxWidth(), enabled = !state.isReadOnly, singleLine = true, label = { Text("Display label (optional)") })
            Text("Graphic type", color = CommandColours.Backing)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.document.content.graphicTemplates.sortedBy(OperationGraphicTemplate::index).forEach { template ->
                    CommandButton({ update(draft.copy(graphicTemplateId = template.id)) }, active = selectedTemplate?.id == template.id, enabled = !state.isReadOnly) { Text(template.name) }
                }
            }
            validation.errors["template"]?.let { Text(it, color = Color(0xFFDF4A4A)) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(draft.positionX, { update(draft.copy(positionX = it)) }, Modifier.weight(1f), enabled = !state.isReadOnly, singleLine = true, isError = "position" in validation.errors, label = { Text(if (state.document.renderData.gridLayer == null) "Map X" else "Easting") })
                OutlinedTextField(draft.positionY, { update(draft.copy(positionY = it)) }, Modifier.weight(1f), enabled = !state.isReadOnly, singleLine = true, isError = "position" in validation.errors, label = { Text(if (state.document.renderData.gridLayer == null) "Map Y" else "Northing") })
            }
            validation.errors["position"]?.let { Text(it, color = Color(0xFFDF4A4A)) }
            if (selectedTemplate?.customProperties?.isNotEmpty() == true) {
                Text("Inherited from graphic type", color = CommandColours.Backing)
                selectedTemplate.customProperties.toSortedMap().forEach { (key, value) -> Text("$key: $value", color = CommandColours.Backing.copy(alpha = 0.75f)) }
            }
            PropertyRows(draft.properties, !state.isReadOnly, validation.errors) { update(draft.copy(properties = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CommandButton({ onStateChanged(state.saveGraphicEditor(editorId)) }, enabled = !state.isReadOnly && validation.valid, modifier = Modifier.weight(1f)) { Text("SAVE") }
            CommandButton({ onStateChanged(state.closeEditor(editorId)) }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
    }
}

@Composable
private fun GraphicTemplateEditorContent(
    editor: OperationEditorState,
    draft: OperationGraphicTemplateDraft,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var imageError by remember(editor.id) { mutableStateOf<String?>(null) }
    val existing = (editor.target as? OperationEditorTarget.GraphicTemplate)?.templateId?.let(state.document.content::graphicTemplate)
    val decoded = remember(draft.replacementImage) { draft.replacementImage?.let { runCatching { MapViewerImageLoader.load(it.bytes(), it.originalFilename) }.getOrNull() } }
    val validation = OperationGraphicAuthoring.validateTemplateDraft(draft, existing != null)
    fun update(value: OperationGraphicTemplateDraft) = onStateChanged(state.updateEditorDraft(editor.id, OperationEditorDraft.GraphicTemplate(value)))
    fun browse() {
        FileDialog(null as Frame?, "Choose graphic image", FileDialog.LOAD).also { dialog ->
            dialog.file = "*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp;*.svg"
            dialog.isVisible = true
            val name = dialog.file ?: return
            val file = File(dialog.directory, name)
            runCatching {
                val imageDraft = OperationImageDraft.fromBytes(file.name, file.readBytes())
                val image = requireNotNull(MapViewerImageLoader.load(imageDraft.bytes(), imageDraft.originalFilename)) { "Unsupported image" }
                imageDraft to image
            }.onSuccess { (imageDraft, image) ->
                imageError = null
                val height = draft.heightPercent.takeIf(String::isNotBlank)
                    ?: OperationGraphicAuthoring.defaultHeightPercent(
                        image, state.document.renderData.baseImage.width, state.document.renderData.baseImage.height,
                        draft.widthPercent.toDoubleOrNull() ?: io.hsar.mapgenerator.commandgui.operation.DEFAULT_GRAPHIC_WIDTH_PERCENT,
                    ).toString()
                update(draft.copy(
                    name = draft.name.ifBlank { file.nameWithoutExtension },
                    replacementImage = imageDraft,
                    heightPercent = height,
                ))
            }.onFailure { imageError = it.message ?: "Could not load the selected image" }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val preview = decoded ?: existing?.image
            preview?.let { androidx.compose.foundation.Image(it.toComposeImageBitmap(), null, Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit) }
            CommandButton(::browse, enabled = !state.isReadOnly, modifier = Modifier.fillMaxWidth()) { Text(if (preview == null) "CHOOSE IMAGE" else "REPLACE IMAGE") }
            (imageError ?: validation.errors["image"])?.let { Text(it, color = Color(0xFFDF4A4A)) }
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth(), enabled = !state.isReadOnly, isError = "name" in validation.errors, singleLine = true, label = { Text("Name") })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(draft.widthPercent, { update(draft.copy(widthPercent = it)) }, Modifier.weight(1f), enabled = !state.isReadOnly, isError = "width" in validation.errors, singleLine = true, label = { Text("Map width %") })
                OutlinedTextField(draft.heightPercent, { update(draft.copy(heightPercent = it)) }, Modifier.weight(1f), enabled = !state.isReadOnly, isError = "height" in validation.errors, singleLine = true, label = { Text("Map height %") })
            }
            PropertyRows(draft.properties, !state.isReadOnly, validation.errors) { update(draft.copy(properties = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CommandButton({ onStateChanged(state.saveGraphicTemplateEditor(editor.id, decoded)) }, enabled = !state.isReadOnly && validation.valid, modifier = Modifier.weight(1f)) { Text("SAVE") }
            CommandButton({ onStateChanged(state.closeEditor(editor.id)) }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
    }
}

@Composable
private fun AffiliationButtons(
    selected: OperationUnitAffiliation?,
    enabled: Boolean,
    onSelected: (OperationUnitAffiliation?) -> Unit,
) = Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    (listOf(null) + OperationUnitAffiliation.entries).forEach { value ->
        CommandButton(onClick = { onSelected(value) }, active = selected == value, enabled = enabled) { Text(value?.name ?: "NONE") }
    }
}

@Composable
private fun PropertyRows(
    properties: List<OperationPropertyDraft>,
    enabled: Boolean,
    errors: Map<String, String>,
    onChanged: (List<OperationPropertyDraft>) -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Custom properties", color = CommandColours.Backing)
    properties.forEachIndexed { index, property ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(property.key, { key -> onChanged(properties.mapIndexed { row, value -> if (row == index) value.copy(key = key) else value }) }, Modifier.weight(1f), enabled = enabled, singleLine = true, isError = "property:$index" in errors, supportingText = errors["property:$index"]?.let { message -> { Text(message) } }, label = { Text("Key") })
            OutlinedTextField(property.value, { value -> onChanged(properties.mapIndexed { row, current -> if (row == index) current.copy(value = value) else current }) }, Modifier.weight(1f), enabled = enabled, singleLine = true, label = { Text("Value") })
            CommandButton(onClick = { onChanged(properties.filterIndexed { row, _ -> row != index }) }, enabled = enabled) { Text("×") }
        }
    }
    CommandButton(onClick = { onChanged(properties + OperationPropertyDraft()) }, enabled = enabled) { Text("ADD PROPERTY") }
}

private fun OperationEditorState.title(content: OperationContent): String = when (kind) {
    OperationEditorKind.OPERATION_NOTES -> "Notes"
    OperationEditorKind.UNIT -> (target as? OperationEditorTarget.Object)?.objectId?.let(content::objectById)?.name?.takeIf(String::isNotBlank) ?: "Edit Unit"
    OperationEditorKind.UNIT_TEMPLATE -> when (val value = target) {
        is OperationEditorTarget.UnitTemplate -> content.unitTemplate(value.templateId)?.name ?: "Edit Unit Type"
        else -> "Create Unit Type"
    }
    OperationEditorKind.GRAPHIC -> (target as? OperationEditorTarget.Object)?.objectId?.let(content::objectById)?.name?.takeIf(String::isNotBlank) ?: "Edit Graphic"
    OperationEditorKind.GRAPHIC_TEMPLATE -> when (val value = target) {
        is OperationEditorTarget.GraphicTemplate -> content.graphicTemplate(value.templateId)?.name ?: "Edit Graphic Type"
        else -> "Create Graphic Type"
    }
    else -> kind.value
}

private fun OperationEditorState.displayBounds(state: ComposeOperationState, density: Double): OperationEditorBounds {
    val anchor = floating.mapAnchor?.takeIf { floating.pinned } ?: return floating.bounds
    val viewport = state.viewport ?: return floating.bounds
    val right = viewport.screenX(anchor.x) / density
    val top = viewport.screenY(anchor.y) / density
    return floating.bounds.copy(left = right - floating.bounds.width, top = top)
}

private fun OperationEditorBounds.clampedTo(width: Double, height: Double) = copy(
    left = left.coerceIn(0.0, (width - this.width).coerceAtLeast(0.0)),
    top = top.coerceIn(0.0, (height - this.height.coerceAtMost(42.0)).coerceAtLeast(0.0)),
)

@Composable
private fun EmptyMap() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No terrain map is available for this operation.")
        }
    }

@Composable
private fun EscapeMenuDialog(
    onDismiss: () -> Unit,
    onReturnToLaunchpad: () -> Unit,
    onExitProgram: () -> Unit,
) = androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    CommandPanel(Modifier.width(420.dp)) {
        Text("MENU", style = MaterialTheme.typography.headlineMedium)
        Text("Return to the launchpad after saving.")
        CommandButton(onClick = onReturnToLaunchpad, modifier = Modifier.fillMaxWidth()) {
            Text("RETURN TO LAUNCHPAD")
        }
        CommandButton(onClick = onExitProgram, modifier = Modifier.fillMaxWidth()) {
            Text("EXIT")
        }
        CommandButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("CLOSE")
        }
    }
}

@Composable
private fun SaveFailureDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) = androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
    CommandPanel(Modifier.width(460.dp)) {
        Text("SAVE FAILED", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Text(message.ifBlank { "The operation could not be saved." })
        Text("The viewer will remain open so no changes are discarded.")
        CommandButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("RETRY") }
        CommandButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("CANCEL EXIT") }
    }
}

private enum class ComposePendingExit { LAUNCHPAD, DESKTOP }

private data class ComposeMapContextMenu(
    val objectId: OperationObjectId,
    val objectName: String,
    val position: IntOffset,
    val actions: List<MapViewerContextMenuItem>,
)

@Composable
private fun ComposeMapContextMenu(
    menu: ComposeMapContextMenu,
    onActionSelected: (MapViewerContextMenuItem) -> Unit,
    onDismiss: () -> Unit,
) = CommandPanel(
    modifier = Modifier.offset { menu.position }.width(220.dp),
) {
    Text(menu.objectName, style = MaterialTheme.typography.titleMedium)
    menu.actions.forEach { action ->
        CommandButton(onClick = { onActionSelected(action) }, modifier = Modifier.fillMaxWidth()) {
            Text(action.label.uppercase())
        }
    }
    CommandButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("CLOSE")
    }
}

private fun OperationObject.displayName(content: OperationContent): String = when (this) {
    is OperationUnit -> instanceName.ifBlank { content.unitTemplate(unitTemplateId)?.name ?: "Unit" }
    is OperationGraphic -> customProperties["displayName"]?.takeIf(String::isNotBlank)
        ?: instanceName.ifBlank { content.graphicTemplate(graphicTemplateId)?.name ?: "Graphic" }

    is OperationDrawingObject -> drawing.displayName
}

private fun OperationObject.contextMenuItems(): List<MapViewerContextMenuItem> = when (this) {
    is OperationUnit -> listOf(
        MapViewerContextMenuItem(MapViewerContextMenuCommand.EDIT_UNIT, "Edit Unit"),
        MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_UNIT, "Delete Unit"),
        MapViewerContextMenuItem(MapViewerContextMenuCommand.SITREP, "SITREP"),
    )

    is OperationGraphic -> listOf(
        MapViewerContextMenuItem(MapViewerContextMenuCommand.EDIT_GRAPHIC, "Edit Graphic"),
        MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_GRAPHIC, "Delete Graphic"),
    )

    is OperationDrawingObject -> listOf(
        MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_DRAWING, "Delete Drawing"),
    )
}

@Composable
private fun OperationLedgerPanel(
    state: ComposeOperationState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPreviewIndex: (Int) -> Unit,
    onCurrent: () -> Unit,
    onSave: () -> Unit,
) {
    val entries = state.document.timeline.entries
    val currentSelectedIndex = state.historyPreviewCount?.minus(1)
    Column(
        Modifier.fillMaxWidth().height(230.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("LEDGER", style = MaterialTheme.typography.titleMedium)
            CommandButton(onClick = onUndo, enabled = state.document.timeline.canUndo && !state.isReadOnly) { Text("UNDO") }
            CommandButton(onClick = onRedo, enabled = state.document.timeline.canRedo && !state.isReadOnly) { Text("REDO") }
            CommandButton(onClick = onCurrent, active = !state.isReadOnly) { Text("CURRENT") }
            CommandButton(onClick = onSave, enabled = state.savingRevision == null) { Text("SAVE") }
            Spacer(Modifier.weight(1f))
            Text(
                when (state.saveStatus) {
                    ComposeSaveStatus.CLEAN -> "SAVED"
                    ComposeSaveStatus.DIRTY -> "UNSAVED CHANGES"
                    ComposeSaveStatus.SAVING -> "SAVING…"
                    ComposeSaveStatus.FAILED -> "SAVE FAILED"
                },
                color = if (state.saveStatus == ComposeSaveStatus.FAILED) MaterialTheme.colorScheme.error else CommandColours.Backing,
            )
        }
        HorizontalDivider()
        Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1.15f).fillMaxHeight()) {
                if (entries.isEmpty()) {
                    item {
                        Text(
                            "No persisted ledger entries",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(entries.indices.toList(), key = { it }) { index ->
                    val entry = entries[index]
                    val selectedEntry = index == currentSelectedIndex
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onPreviewIndex(index) },
                        color = if (selectedEntry) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                            Text(entry.kind.name.lowercase().replace('_', ' '), modifier = Modifier.width(110.dp))
                            Text(entry.description, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            VerticalDivider()
            Column(
                modifier = Modifier.weight(0.85f).fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(if (state.isReadOnly) "READ-ONLY HISTORY" else "CURRENT STATE", style = MaterialTheme.typography.titleSmall)
                currentSelectedIndex?.let { index ->
                    entries.getOrNull(index)?.let { Text("Action ${index + 1}: ${it.description}") }
                    HorizontalDivider()
                }
                state.visibleContent.objects.take(PROTOTYPE_STATE_ROW_LIMIT).forEach { mapObject ->
                    Text("${mapObject::class.simpleName?.removePrefix("Operation")?.lowercase()}: ${mapObject.displayName(state.visibleContent)}")
                }
                state.visibleContent.notes.take(PROTOTYPE_STATE_ROW_LIMIT).forEach { note ->
                    Text("note: ${note.title}")
                }
                if (state.visibleContent.objects.isEmpty() && state.visibleContent.notes.isEmpty()) {
                    Text(
                        "No live objects or notes at the ledger head.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val PROTOTYPE_STATE_ROW_LIMIT = 12

private data class ComposeRasterTile(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val image: ImageBitmap,
)

private data class ComposeSelectionMarquee(
    val start: Offset,
    val current: Offset,
    val additive: Boolean,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MapCanvas(
    renderData: MapViewerRenderData,
    savedCamera: MapViewerSavedCamera?,
    content: OperationContent,
    viewport: MapViewport?,
    onViewportChanged: (MapViewport) -> Unit,
    activeTool: MapViewerTool,
    editable: Boolean,
    selectedObjectIds: Set<OperationObjectId>,
    onObjectsSelected: (Set<OperationObjectId>, Boolean) -> Unit,
    onContextMenuRequested: (OperationObject, IntOffset) -> Unit,
    onContextMenuDismissed: () -> Unit,
    onPreviewChanged: (OperationContent) -> Unit,
    onPreviewCommitted: (String) -> Unit,
    drawingMode: MapViewerDrawingMode,
    drawingColour: MapViewerDrawingColour,
    onDrawingCommitted: (io.hsar.mapgenerator.commandgui.operation.OperationDrawing) -> Unit,
    unitPlacementActive: Boolean,
    onUnitPlacementPreview: (MapCoordinate?) -> Unit,
    onUnitPlacementCommitted: () -> Unit,
    onUnitPlacementCancelled: () -> Unit,
    graphicPlacementActive: Boolean,
    onGraphicPlacementPreview: (MapCoordinate?) -> Unit,
    onGraphicPlacementCommitted: () -> Unit,
    onGraphicPlacementCancelled: () -> Unit,
    cancellationEpoch: Int,
    unitIconSizePx: Double,
    onUnitIconSizeChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nordFontFamily = NordFontFamily()
    val textMeasurer = rememberTextMeasurer()
    val tiles = remember(renderData.baseImage) {
        RasterTileSet.from(renderData.baseImage).tiles.map { tile ->
            ComposeRasterTile(tile.left, tile.top, tile.image.width, tile.image.height, tile.image.toImageBitmap())
        }
    }
    val mapWidth = renderData.baseImage.width
    val mapHeight = renderData.baseImage.height
    val aspectRatio = mapWidth.toDouble() / mapHeight
    val worldWidth = mapWidth * 1.66
    val worldHeight = mapHeight * 1.66
    val mapOffsetX = (worldWidth - mapWidth) * 0.5
    val mapOffsetY = (worldHeight - mapHeight) * 0.5
    val debugMapObjects = System.getProperty("commandgui.compose.debugMapObjects") == "true"
    val initialViewport = remember(renderData.baseImage, savedCamera) {
        savedCamera?.let { camera ->
            MapViewport(
                center = ViewerPoint(mapOffsetX + camera.x * mapWidth, mapOffsetY + camera.y * mapHeight),
                zoom = camera.zoom.coerceIn(MapViewerSettings.MIN_ZOOM, MapViewerSettings.MAX_ZOOM),
                viewportWidth = 1,
                viewportHeight = 1,
                worldWidth = worldWidth,
                worldHeight = worldHeight,
            )
        } ?: MapViewport.centered(worldWidth, worldHeight)
    }
    var panningViewport by remember(renderData.baseImage) { mutableStateOf<MapViewport?>(null) }
    val currentViewport = panningViewport ?: viewport ?: initialViewport
    var rightClickTargetId by remember { mutableStateOf<OperationObjectId?>(null) }
    var marquee by remember { mutableStateOf<ComposeSelectionMarquee?>(null) }
    var transformGesture by remember { mutableStateOf<OperationTransformGesture?>(null) }
    var drawingGesture by remember { mutableStateOf<MapViewerDrawingGesture?>(null) }
    var measurementOrigin by remember { mutableStateOf<MapCoordinate?>(null) }
    var measurement by remember { mutableStateOf<MapViewerMeasurement?>(null) }
    var activeReferenceCoordinate by remember { mutableStateOf<MapCoordinate?>(null) }
    LaunchedEffect(activeTool, renderData.gridLayer, cancellationEpoch) {
        transformGesture = null
        marquee = null
        drawingGesture = null
        measurementOrigin = null
        measurement = null
        activeReferenceCoordinate = null
    }
    val unitIcons = remember(content.unitTemplates) { content.unitTemplates.associate { it.id to it.image.toImageBitmap() } }
    val graphicIcons = remember(content.graphicTemplates) { content.graphicTemplates.associate { it.id to it.image.toImageBitmap() } }
    val gridOverlay = remember(renderData.gridLayer, currentViewport, nordFontFamily, measurement, activeReferenceCoordinate) {
        renderData.gridLayer?.let { grid ->
            val referenceCoordinate = measurement?.destination ?: activeReferenceCoordinate
            MapViewerGridOverlayLayoutBuilder.build(
                MapViewerGridOverlayRequest(
                    grid, currentViewport, mapOffsetX, mapOffsetY, mapWidth, mapHeight,
                    activeReference = referenceCoordinate?.let { coordinate ->
                        MapViewerGridReference(
                            coordinate,
                            GridReferenceFormatter.format(grid, mapWidth, mapHeight, coordinate.x * mapWidth, coordinate.y * mapHeight),
                            grid.colour,
                        )
                    },
                    measurement = measurement,
                ),
            ) { text, fontSize, role ->
                textMeasurer.measure(
                    text,
                    TextStyle(
                        fontFamily = nordFontFamily,
                        fontWeight = if (role == io.hsar.mapgenerator.commandgui.MapViewerGridFontRole.STRONG) FontWeight.Bold else FontWeight.Normal,
                        fontSize = fontSize.sp
                    ),
                ).size.let { ViewerPoint(it.width.toDouble(), it.height.toDouble()) }
            }
        }
    }

    fun screenToMap(point: Offset) = MapCoordinate(
        x = (currentViewport.left + point.x / currentViewport.zoom - mapOffsetX) / mapWidth,
        y = (currentViewport.top + point.y / currentViewport.zoom - mapOffsetY) / mapHeight,
    )

    val selectionProjection = object : OperationSelectionProjection {
        override val aspectRatio = aspectRatio

        override fun toScreen(point: MapCoordinate): ViewerPoint {
            val world = point.toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight)
            return ViewerPoint(currentViewport.screenX(world.x), currentViewport.screenY(world.y))
        }

        override fun toMap(point: ViewerPoint) = screenToMap(Offset(point.x.toFloat(), point.y.toFloat()))

        override fun unitScreenBounds(unit: OperationUnit): MapSelectionScreenBounds {
            val value = unit
            val centre = value.coordinate.toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight)
            val x = currentViewport.screenX(centre.x)
            val y = currentViewport.screenY(centre.y)
            return MapSelectionScreenBounds(x - unitIconSizePx * 0.5, y - unitIconSizePx * 0.5, x + unitIconSizePx * 0.5, y + unitIconSizePx * 0.5)
        }
    }

    fun objectAtScreen(point: Offset): OperationObject? = content.objectAtScreen(
        ViewerPoint(point.x.toDouble(), point.y.toDouble()), selectionProjection,
        10.0 / (currentViewport.zoom * minOf(mapWidth, mapHeight)),
    )

    fun selectionFrame() = content.selectionFrame(selectedObjectIds, selectionProjection)

    if (debugMapObjects) {
        LaunchedEffect(content, currentViewport) {
            println(
                "Compose map-object diagnostic: objects=${content.objects.size}, units=${content.objects.count { it is OperationUnit }}, " +
                        "graphics=${content.objects.count { it is OperationGraphic }}, viewport=$currentViewport",
            )
        }
    }

    Canvas(
        modifier = modifier.background(Color(0xFF15181B))
            .onSizeChanged {
                val resized = currentViewport.resized(it.width, it.height)
                if (resized != currentViewport) {
                    panningViewport = null
                    onViewportChanged(resized)
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                if (event.keyboardModifiers.isAltPressed) {
                    onUnitIconSizeChanged(UnitIconDisplaySize.scroll(unitIconSizePx, change.scrollDelta.y))
                } else {
                    val factor = if (change.scrollDelta.y < 0f) 1.1 else 1.0 / 1.1
                    panningViewport = null
                    onViewportChanged(currentViewport.zoomAt(ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble()), factor))
                }
                change.consume()
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                if (event.buttons.isSecondaryPressed) {
                    if (unitPlacementActive || graphicPlacementActive) {
                        if (unitPlacementActive) onUnitPlacementCancelled() else onGraphicPlacementCancelled()
                        change.consume()
                        return@onPointerEvent
                    }
                    objectAtScreen(change.position)?.let { value ->
                        rightClickTargetId = value.id
                        onContextMenuRequested(value, IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt()))
                    } ?: run {
                        rightClickTargetId = null
                        onContextMenuDismissed()
                    }
                    return@onPointerEvent
                }
                if (event.buttons.isPrimaryPressed && unitPlacementActive) {
                    val coordinate = screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
                    onUnitPlacementPreview(coordinate)
                    if (coordinate != null) onUnitPlacementCommitted()
                    change.consume()
                    return@onPointerEvent
                }
                if (event.buttons.isPrimaryPressed && graphicPlacementActive) {
                    val coordinate = screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
                    onGraphicPlacementPreview(coordinate)
                    if (coordinate != null) onGraphicPlacementCommitted()
                    change.consume()
                    return@onPointerEvent
                }
                if (event.buttons.isPrimaryPressed && activeTool == MapViewerTool.MEASURE) {
                    if (renderData.gridLayer != null) screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }?.let { measurementOrigin = it }
                    return@onPointerEvent
                }
                if (event.buttons.isPrimaryPressed && editable && activeTool == MapViewerTool.DRAW) {
                    screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }?.let { drawingGesture = drawingMode.startedAt(it, drawingColour.argb) }
                    return@onPointerEvent
                }
                if (!event.buttons.isPrimaryPressed || activeTool != MapViewerTool.SELECT) return@onPointerEvent
                activeReferenceCoordinate = screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
                if (!editable) {
                    objectAtScreen(change.position)?.let { onObjectsSelected(setOf(it.id), false) }
                    return@onPointerEvent
                }
                val point = ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble())
                val frame = selectionFrame()
                val rotateHit = frame?.rotationHandle()?.let { hypot(it.x - point.x, it.y - point.y) <= 12.0 } == true
                val resizeHit = frame?.cornerBrackets()?.firstOrNull { it.contains(point) }
                when {
                    rotateHit && selectedObjectIds.isNotEmpty() -> {
                        val bounds = content.selectionBounds(selectedObjectIds, aspectRatio) ?: return@onPointerEvent
                        transformGesture = OperationTransformGesture.Rotate(
                            content, selectedObjectIds, bounds.centre, screenToMap(change.position), aspectRatio,
                        )
                    }

                    resizeHit != null && selectedObjectIds.isNotEmpty() -> {
                        transformGesture = OperationTransformGesture.Resize(
                            original = content,
                            selectedIds = selectedObjectIds,
                            grabbedCorner = resizeHit.corner,
                            aspectRatio = aspectRatio,
                            originalFixedCorner = frame.corner(resizeHit.corner.opposite()).let(selectionProjection::toMap),
                        )
                    }

                    else -> objectAtScreen(change.position)?.let { selected ->
                        val ids = when {
                            selected.id in selectedObjectIds -> selectedObjectIds
                            event.keyboardModifiers.isShiftPressed -> selectedObjectIds + selected.id
                            else -> setOf(selected.id)
                        }
                        if (selected.id !in selectedObjectIds) onObjectsSelected(setOf(selected.id), event.keyboardModifiers.isShiftPressed)
                        transformGesture = OperationTransformGesture.Move(
                            content, ids, screenToMap(change.position), viewerBackgroundBounds(mapWidth, mapHeight), aspectRatio,
                        )
                    } ?: run {
                        marquee = ComposeSelectionMarquee(change.position, change.position, event.keyboardModifiers.isShiftPressed)
                    }
                }
                onContextMenuDismissed()
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                if (unitPlacementActive && !event.buttons.isPrimaryPressed && !event.buttons.isSecondaryPressed) {
                    onUnitPlacementPreview(screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 })
                    return@onPointerEvent
                }
                if (graphicPlacementActive && !event.buttons.isPrimaryPressed && !event.buttons.isSecondaryPressed) {
                    onGraphicPlacementPreview(screenToMap(change.position).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 })
                    return@onPointerEvent
                }
                if (event.buttons.isSecondaryPressed && rightClickTargetId == null) {
                    val delta = change.position - change.previousPosition
                    if (delta != Offset.Zero) {
                        panningViewport = (panningViewport ?: currentViewport).pan(ViewerPoint(delta.x.toDouble(), delta.y.toDouble()))
                    }
                    return@onPointerEvent
                }
                drawingGesture?.let { gesture ->
                    val point = screenToMap(change.position).let { MapCoordinate(it.x.coerceIn(0.0, 1.0), it.y.coerceIn(0.0, 1.0)) }
                    drawingGesture = gesture.update(point, event.keyboardModifiers.isShiftPressed, aspectRatio, 2.0 / minOf(mapWidth.toDouble(), mapHeight.toDouble()))
                    return@onPointerEvent
                }
                measurementOrigin?.let { origin ->
                    renderData.gridLayer?.let { layer ->
                        val destination = screenToMap(change.position).let { MapCoordinate(it.x.coerceIn(0.0, 1.0), it.y.coerceIn(0.0, 1.0)) }
                        measurement = measureMapDrag(layer, origin, destination, mapWidth, mapHeight)
                    }
                    return@onPointerEvent
                }
                transformGesture?.let { gesture ->
                    val point = screenToMap(change.position)
                    activeReferenceCoordinate = MapCoordinate(point.x.coerceIn(0.0, 1.0), point.y.coerceIn(0.0, 1.0))
                    val preview = gesture.preview(
                        point,
                        OperationTransformModifiers(event.keyboardModifiers.isAltPressed, event.keyboardModifiers.isShiftPressed),
                    )
                    onPreviewChanged(preview)
                    change.consume()
                } ?: marquee?.let { current ->
                    marquee = current.copy(current = change.position)
                    change.consume()
                }
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                if (!event.buttons.isSecondaryPressed) {
                    rightClickTargetId = null
                    panningViewport?.let(onViewportChanged)
                    panningViewport = null
                }
                transformGesture?.let { gesture ->
                    onPreviewCommitted(
                        when (gesture) {
                            is OperationTransformGesture.Move -> "moved ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                            is OperationTransformGesture.Resize -> "resized ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                            is OperationTransformGesture.Rotate -> "rotated ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                        },
                    )
                    transformGesture = null
                }
                drawingGesture?.let { gesture ->
                    gesture.committedOrNull()?.let(onDrawingCommitted)
                    drawingGesture = null
                }
                measurementOrigin = null
                measurement = null
                activeReferenceCoordinate = null
                marquee?.let { current ->
                    val box = MapSelectionBox(
                        ViewerPoint(current.start.x.toDouble(), current.start.y.toDouble()),
                        ViewerPoint(current.current.x.toDouble(), current.current.y.toDouble())
                    )
                    if (box.isDrag) {
                        onObjectsSelected(
                            content.objects.filter { box.overlaps(it.selectionScreenBounds(selectionProjection)) }.mapTo(linkedSetOf(), OperationObject::id),
                            current.additive
                        )
                    } else if (!current.additive) onObjectsSelected(emptySet(), false)
                    marquee = null
                }
            },
    ) {
        val zoom = currentViewport.zoom.toFloat()
        val mapLeft = currentViewport.screenX(mapOffsetX).toFloat()
        val mapTop = currentViewport.screenY(mapOffsetY).toFloat()
        clipRect {
            withTransform({ translate(mapLeft, mapTop); scale(zoom, zoom, Offset.Zero) }) {
                tiles.forEach { tile ->
                    drawImage(
                        image = tile.image,
                        dstOffset = IntOffset(tile.left, tile.top),
                        dstSize = IntSize(tile.width, tile.height),
                    )
                }
                content.objects.filterIsInstance<OperationDrawingObject>().forEach { value ->
                    drawOperationDrawing(value.drawing, mapWidth, mapHeight)
                }
                drawingGesture?.let { drawOperationDrawing(it.preview, mapWidth, mapHeight) }
                content.objects.filterIsInstance<OperationGraphic>().forEach { value ->
                    val icon = graphicIcons[value.graphicTemplateId]
                    val centre = Offset((value.coordinate.x * mapWidth).toFloat(), (value.coordinate.y * mapHeight).toFloat())
                    val width = (value.normalizedWidth * mapWidth).roundToInt().coerceAtLeast(1)
                    val height = (value.normalizedHeight * mapHeight).roundToInt().coerceAtLeast(1)
                    icon?.let {
                        drawImage(
                            image = it,
                            dstOffset = IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt()),
                            dstSize = IntSize(width, height),
                        )
                    }
                        ?: drawRect(Color(0xFFFFC857), centre - Offset(width / 2f, height / 2f), androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()))
                }
            }
            gridOverlay?.lineSegments?.forEach { segment ->
                drawLine(
                    segment.colour.toComposeColour(),
                    Offset(segment.start.x.toFloat(), segment.start.y.toFloat()),
                    Offset(segment.end.x.toFloat(), segment.end.y.toFloat()),
                    segment.thickness.toFloat(),
                    StrokeCap.Square
                )
            }
            gridOverlay?.arrows?.forEach { arrow ->
                drawLine(arrow.colour.toComposeColour(), Offset(arrow.start.x.toFloat(), arrow.start.y.toFloat()), Offset(arrow.end.x.toFloat(), arrow.end.y.toFloat()), 2f)
            }
            content.objects.filterIsInstance<OperationUnit>().forEach { value ->
                val icon = unitIcons[value.unitTemplateId]
                val centre = Offset(
                    currentViewport.screenX(mapOffsetX + value.coordinate.x * mapWidth).toFloat(),
                    currentViewport.screenY(mapOffsetY + value.coordinate.y * mapHeight).toFloat(),
                )
                val width = unitIconSizePx.roundToInt().coerceAtLeast(1)
                val height = icon?.let { (width.toDouble() * it.height / it.width).roundToInt() } ?: width
                icon?.let {
                    drawImage(
                        image = it,
                        dstOffset = IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt()),
                        dstSize = IntSize(width, height),
                    )
                } ?: drawCircle(Color(0xFF67D5E8), width / 2f, centre)
                value.facingDegrees?.let { degrees ->
                    drawUnitFacingIndicator(
                        centre = centre,
                        iconWidth = width.toFloat(),
                        iconHeight = height.toFloat(),
                        facingDegrees = degrees,
                        color = (value.affiliation ?: content.unitTemplate(value.unitTemplateId)?.affiliation).facingIndicatorColor(),
                    )
                }
                if (value.destroyed) {
                    val radius = maxOf(width, height) / 2f
                    drawLine(Color.Black, centre - Offset(radius, radius), centre + Offset(radius, radius), 2f)
                    drawLine(Color.Black, centre + Offset(radius, -radius), centre + Offset(-radius, radius), 2f)
                }
            }
            content.objects.filterIsInstance<OperationGraphic>().forEach { value ->
                value.customProperties["displayName"]?.takeIf(String::isNotBlank)?.let { label ->
                    val centre = Offset(
                        currentViewport.screenX(mapOffsetX + value.coordinate.x * mapWidth).toFloat(),
                        currentViewport.screenY(mapOffsetY + value.coordinate.y * mapHeight).toFloat(),
                    )
                    val height = value.normalizedHeight * mapHeight * currentViewport.zoom
                    val style = TextStyle(Color.White, fontFamily = nordFontFamily, fontSize = 16.sp)
                    val measured = textMeasurer.measure(label, style)
                    drawText(textMeasurer, label, Offset(centre.x - measured.size.width / 2f, centre.y - height.toFloat() / 2f - measured.size.height - 4f), style)
                }
            }
            content.objects.filter { it.id in selectedObjectIds }.map { it.selectionScreenBounds(selectionProjection) }.forEach { bounds ->
                drawRect(
                    Color.White.copy(alpha = 0.82f),
                    Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                )
            }
            if (debugMapObjects) {
                content.objects.filter { it is OperationUnit || it is OperationGraphic }.forEach { value ->
                    val bounds = value.selectionScreenBounds(selectionProjection)
                    val centre = Offset(((bounds.left + bounds.right) * 0.5).toFloat(), ((bounds.top + bounds.bottom) * 0.5).toFloat())
                    drawLine(Color.Magenta, centre - Offset(8f, 0f), centre + Offset(8f, 0f), 2f)
                    drawLine(Color.Magenta, centre - Offset(0f, 8f), centre + Offset(0f, 8f), 2f)
                    drawRect(
                        Color.Magenta,
                        Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                        androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
                    )
                }
            }
            selectionFrame()?.let { frame ->
                frame.cornerBrackets().forEach { bracket ->
                    drawPath(Path().apply {
                        moveTo(bracket.points.first().x.toFloat(), bracket.points.first().y.toFloat()); bracket.points.drop(1)
                        .forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }; close()
                    }, Color.White)
                }
                frame.rotationHandle().let { handle ->
                    drawCircle(Color.White, 7f, Offset(handle.x.toFloat(), handle.y.toFloat()))
                    drawCircle(Color(0xFF120909), 3.5f, Offset(handle.x.toFloat(), handle.y.toFloat()))
                }
            }
            marquee?.let { selection ->
                val box = MapSelectionBox(
                    ViewerPoint(selection.start.x.toDouble(), selection.start.y.toDouble()),
                    ViewerPoint(selection.current.x.toDouble(), selection.current.y.toDouble())
                )
                if (box.isDrag) {
                    val topLeft = Offset(box.left.toFloat(), box.top.toFloat())
                    val size = androidx.compose.ui.geometry.Size((box.right - box.left).toFloat(), (box.bottom - box.top).toFloat())
                    drawRect(Color(0xFFDF4A4A).copy(alpha = 0.15f), topLeft, size)
                    drawRect(Color(0xFFDF4A4A), topLeft, size, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
            gridOverlay?.labels?.forEach { label ->
                val style = TextStyle(
                    label.colour.toComposeColour(),
                    fontFamily = nordFontFamily,
                    fontWeight = if (label.strong) FontWeight.Bold else FontWeight.Normal,
                    fontSize = label.fontSize.sp
                )
                val measured = textMeasurer.measure(label.text, style)
                label.shadows.forEach { shadow ->
                    drawText(
                        textMeasurer,
                        label.text,
                        Offset(
                            (label.centre.x - measured.size.width / 2.0 + shadow.offset.x).toFloat(),
                            (label.centre.y - measured.size.height / 2.0 + shadow.offset.y).toFloat(),
                        ),
                        style.copy(color = shadow.colour.toComposeColour()),
                    )
                }
                drawText(textMeasurer, label.text, Offset((label.centre.x - measured.size.width / 2.0).toFloat(), (label.centre.y - measured.size.height / 2.0).toFloat()), style)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOperationDrawing(
    drawing: io.hsar.mapgenerator.commandgui.operation.OperationDrawing,
    mapWidth: Int,
    mapHeight: Int,
) {
    val colour = drawing.colour.let { Color(it.red, it.green, it.blue, it.alpha) }
    fun io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint.offset() = Offset((x * mapWidth).toFloat(), (y * mapHeight).toFloat())
    when (drawing) {
        is OperationFreeformDrawing -> if (drawing.points.size > 1) drawPath(
            Path().apply { moveTo(drawing.points.first().offset().x, drawing.points.first().offset().y); drawing.points.drop(1).forEach { lineTo(it.offset().x, it.offset().y) } },
            colour, style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
        )

        is OperationLineDrawing -> drawLine(colour, drawing.start.offset(), drawing.end.offset(), 2f)
        is OperationRectangleDrawing -> {
            val left = minOf(drawing.start.x, drawing.end.x) * mapWidth
            val top = minOf(drawing.start.y, drawing.end.y) * mapHeight
            val width = abs(drawing.end.x - drawing.start.x) * mapWidth
            val height = abs(drawing.end.y - drawing.start.y) * mapHeight
            val centre = Offset((left + width * 0.5).toFloat(), (top + height * 0.5).toFloat())
            withTransform({ rotate(drawing.rotationDegrees.toFloat(), centre) }) {
                drawRect(
                    colour,
                    Offset(left.toFloat(), top.toFloat()),
                    androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                )
            }
        }

        is OperationOvalDrawing -> {
            val centre = drawing.centre.offset()
            withTransform({ rotate(drawing.rotationDegrees.toFloat(), centre) }) {
                drawOval(
                    colour,
                    topLeft = Offset((centre.x - drawing.xRadius * mapWidth).toFloat(), (centre.y - drawing.yRadius * mapHeight).toFloat()),
                    size = androidx.compose.ui.geometry.Size((drawing.xRadius * mapWidth * 2).toFloat(), (drawing.yRadius * mapHeight * 2).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUnitFacingIndicator(
    centre: Offset,
    iconWidth: Float,
    iconHeight: Float,
    facingDegrees: Int,
    color: Color,
) {
    val outerRadius = minOf(iconWidth, iconHeight) * 0.72f
    val innerRadius = outerRadius * 0.86f
    val arcRadius = (outerRadius + innerRadius) * 0.5f
    val mainStroke = outerRadius - innerRadius
    val thinStroke = mainStroke * 0.5f
    val ringBounds = androidx.compose.ui.geometry.Rect(
        centre.x - arcRadius,
        centre.y - arcRadius,
        centre.x + arcRadius,
        centre.y + arcRadius,
    )
    drawArc(
        color = color,
        startAngle = facingDegrees - 150f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = ringBounds.topLeft,
        size = ringBounds.size,
        style = androidx.compose.ui.graphics.drawscope.Stroke(mainStroke),
    )
    drawArc(color, facingDegrees - 210f, 60f, false, ringBounds.topLeft, ringBounds.size, style = androidx.compose.ui.graphics.drawscope.Stroke(thinStroke))
    drawArc(color, facingDegrees - 30f, 60f, false, ringBounds.topLeft, ringBounds.size, style = androidx.compose.ui.graphics.drawscope.Stroke(thinStroke))

    val radians = Math.toRadians(facingDegrees.toDouble())
    val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
    val perpendicular = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val tip = centre + direction * (outerRadius * 1.22f)
    val base = centre + direction * arcRadius
    val halfWidth = minOf(iconWidth, iconHeight) * 0.16f
    drawPath(
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x + perpendicular.x * halfWidth, base.y + perpendicular.y * halfWidth)
            lineTo(base.x - perpendicular.x * halfWidth, base.y - perpendicular.y * halfWidth)
            close()
        },
        color,
    )
}

private fun OperationUnitAffiliation?.facingIndicatorColor() = when (this) {
    OperationUnitAffiliation.FRIEND -> Color(0xFF276A88)
    OperationUnitAffiliation.HOSTILE -> Color(0xFFC23E3C)
    OperationUnitAffiliation.NEUTRAL -> Color(0xFF3D7652)
    OperationUnitAffiliation.UNKNOWN -> Color(0xFF806E29)
    null -> Color(0xFF403A3A)
}

private fun java.awt.Color.toComposeColour() =
    Color(red = red, green = green, blue = blue, alpha = alpha)

private fun io.hsar.mapgenerator.commandgui.operation.OperationArgb.toComposeColour() =
    Color(red = red, green = green, blue = blue, alpha = alpha)

internal fun java.awt.image.BufferedImage.toImageBitmap(): ImageBitmap =
    ByteArrayOutputStream().use { output ->
        ImageIO.write(this, "png", output)
        Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    }
