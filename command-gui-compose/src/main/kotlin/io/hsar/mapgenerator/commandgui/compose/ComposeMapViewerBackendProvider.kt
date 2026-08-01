package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionBox
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.MapViewerBackend
import io.hsar.mapgenerator.commandgui.MapViewerBackendProvider
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuCommand
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuItem
import io.hsar.mapgenerator.commandgui.MapViewerGlobalSettings
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayRequest
import io.hsar.mapgenerator.commandgui.MapViewerLedgerReplay
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerReplayObject
import io.hsar.mapgenerator.commandgui.MapViewerReplayObjectKind
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
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationFreeformDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationObject
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationOvalDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.contains
import io.hsar.mapgenerator.commandgui.operation.move
import io.hsar.mapgenerator.commandgui.operation.resize
import io.hsar.mapgenerator.commandgui.operation.rotate
import io.hsar.mapgenerator.commandgui.operation.selectionBounds
import io.hsar.mapgenerator.commandgui.toWorldPoint
import jdk.internal.net.http.common.Utils.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import sun.security.krb5.internal.KDCOptions.with
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
        var workspaceMode by remember { mutableStateOf(WorkspaceMode.DOCKED) }
        var windowTitle by remember { mutableStateOf(MapViewerSettings().windowTitle()) }
        var closeRequestHandler by remember { mutableStateOf<() -> Unit>({ exitApplication() }) }
        val windowState = rememberWindowState(placement = COMPOSE_INITIAL_WINDOW_PLACEMENT)

        Window(
            title = windowTitle,
            state = windowState,
            onCloseRequest = { closeRequestHandler() },
        ) {
            MenuBar {
                Menu("Workspace") {
                    Item(
                        text = "Docked",
                        onClick = { workspaceMode = WorkspaceMode.DOCKED },
                        shortcut = null,
                    )
                    Item(
                        text = "Floating",
                        onClick = { workspaceMode = WorkspaceMode.FLOATING },
                        shortcut = null,
                    )
                }
            }

            NordTheme {
                ComposeMapViewer(
                    renderMap = renderMap,
                    initialSettings = initialSettings,
                    initialSetupInputs = initialSetupInputs,
                    workspaceMode = workspaceMode,
                    awtWindow = window,
                    onWorkspaceModeChanged = { workspaceMode = it },
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
    workspaceMode: WorkspaceMode,
    awtWindow: java.awt.Window,
    onWorkspaceModeChanged: (WorkspaceMode) -> Unit,
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
                workspaceMode = workspaceMode,
                initialUnitIconSizePx = globalSettings.unitIconSizePx(),
                onUnitIconSizeChanged = globalSettings::updateUnitIconSizePx,
                onWorkspaceModeChanged = onWorkspaceModeChanged,
                onReturnToLaunchpad = launchState::returnToLaunchpad,
                onExitProgram = onExit,
                onCloseRequestHandlerChanged = onCloseRequestHandlerChanged,
            )
        }
    } ?: run {
        LaunchedEffect(launchState.screen) { onCloseRequestHandlerChanged(onExit) }
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
private const val OPERATION_NOTES_DRAFT_ID = "operation-notes"
private const val COMPOSE_AUTOSAVE_DEBOUNCE_MS = 2_000L
private const val UNIT_EDITOR_TYPE = "unit"

@Composable
private fun ViewerWorkspace(
    initialMap: ComposeActiveMap,
    repository: MapViewerSaveRepository,
    workspaceMode: WorkspaceMode,
    initialUnitIconSizePx: Double,
    onUnitIconSizeChanged: (Double) -> Double,
    onWorkspaceModeChanged: (WorkspaceMode) -> Unit,
    onReturnToLaunchpad: () -> Unit,
    onExitProgram: () -> Unit,
    onCloseRequestHandlerChanged: (() -> Unit) -> Unit,
) {
    var operationState by remember(initialMap.document) {
        mutableStateOf(ComposeOperationState(initialMap.document))
    }
    var workspaceInitialized by remember(initialMap.document) { mutableStateOf(false) }
    var unitIconSizePx by remember { mutableStateOf(UnitIconDisplaySize.coerce(initialUnitIconSizePx)) }
    var escapeMenuOpen by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ComposeMapContextMenu?>(null) }
    var pendingExit by remember { mutableStateOf<ComposePendingExit?>(null) }
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

    LaunchedEffect(workspaceMode) {
        if (workspaceInitialized) {
            operationState = operationState.updateWorkspaceMode(workspaceMode)
        } else {
            workspaceInitialized = true
            onWorkspaceModeChanged(operationState.workspaceMode)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            when {
                event.key == Key.Escape && operationState.previewContent != null -> {
                    operationState = operationState.cancelPreview()
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
                            unitIconSizePx = unitIconSizePx,
                            onUnitIconSizeChanged = { size -> unitIconSizePx = onUnitIconSizeChanged(size) },
                            modifier = Modifier.fillMaxSize(),
                        )

                    ToolRail(
                        activeTool = operationState.activeTool,
                        onToolSelected = { operationState = operationState.selectTool(it) },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    )
                    CommandButton(
                        onClick = { escapeMenuOpen = true },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    )
                    {
                        Text("MENU")
                    }
                    contextMenu?.let { menu ->
                        ComposeMapContextMenu(
                            menu = menu,
                            onActionSelected = { action ->
                                when (action.action) {
                                    MapViewerContextMenuCommand.EDIT_UNIT -> operationState = operationState.openEditor(
                                        ComposeEditorInstance(
                                            id = menu.objectId.unitEditorId(),
                                            type = UNIT_EDITOR_TYPE,
                                            objectId = menu.objectId,
                                        ),
                                    )
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
                        FloatingNotesPanel(
                            note = operationState.drafts[OPERATION_NOTES_DRAFT_ID].orEmpty(),
                            onNoteChanged = { operationState = operationState.updateDraft(OPERATION_NOTES_DRAFT_ID, it) },
                        )
                    }
                }
                if (operationState.workspaceMode == WorkspaceMode.DOCKED) {
                    NotesPanel(
                        note = operationState.drafts[OPERATION_NOTES_DRAFT_ID].orEmpty(),
                        onNoteChanged = { operationState = operationState.updateDraft(OPERATION_NOTES_DRAFT_ID, it) },
                        modifier = Modifier.requiredWidth(320.dp).fillMaxHeight(),
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
        operationState.openEditors.filter { it.type == UNIT_EDITOR_TYPE }.forEachIndexed { index, editor ->
            key(editor.id) {
                (editor.objectId?.let(operationState.visibleContent::objectById) as? OperationUnit)?.let { unit ->
                    ComposeUnitEditDialog(
                        unit = unit,
                        templateName = operationState.visibleContent.unitTemplate(unit.unitTemplateId)?.name.orEmpty(),
                        initialPosition = IntOffset(96 + index * 32, 96 + index * 32),
                        editable = !operationState.isReadOnly,
                        onSave = { updated ->
                            operationState = operationState.editUnit(updated)
                            operationState = operationState.closeEditor(editor.id)
                        },
                        onDismiss = { operationState = operationState.closeEditor(editor.id) },
                    )
                } ?: run { operationState = operationState.closeEditor(editor.id) }
            }
        }
    }
}

@Composable
private fun ToolRail(
    activeTool: MapViewerTool,
    onToolSelected: (MapViewerTool) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    MapViewerTool.entries.forEach { tool ->
        CommandButton(
            onClick = { onToolSelected(tool) },
            active = tool == activeTool,
        ) {
            Text(tool.name)
        }
    }
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
private fun FloatingNotesPanel(
    note: String,
    onNoteChanged: (String) -> Unit,
) {
    var position by remember { mutableStateOf(IntOffset(32, 32)) }
    var minimized by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .offset { position }
            .size(width = 340.dp, height = if (minimized) 48.dp else 360.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    position += IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notes", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            CommandButton(onClick = { minimized = !minimized }) {
                Text(if (minimized) "Restore" else "Minimize")
            }
        }
        if (!minimized) {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                minLines = 8,
            )
        }
    }
}

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
private fun ComposeUnitEditDialog(
    unit: OperationUnit,
    templateName: String,
    initialPosition: IntOffset,
    editable: Boolean,
    onSave: (OperationUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    var position by remember(unit.id) { mutableStateOf(initialPosition) }
    var width by remember(unit.id) { mutableStateOf(520.dp) }
    var height by remember(unit.id) { mutableStateOf(560.dp) }
    var resizeStartWidth by remember(unit.id) { mutableStateOf(width) }
    var resizeStartHeight by remember(unit.id) { mutableStateOf(height) }
    var resizeDragDistance by remember(unit.id) { mutableStateOf(Offset.Zero) }
    var name by remember(unit.id) { mutableStateOf(unit.instanceName) }
    var positionX by remember(unit.id) { mutableStateOf(unit.coordinate.x.toString()) }
    var positionY by remember(unit.id) { mutableStateOf(unit.coordinate.y.toString()) }
    var facing by remember(unit.id) { mutableStateOf(unit.facingDegrees?.toString().orEmpty()) }
    var destroyed by remember(unit.id) { mutableStateOf(unit.destroyed) }
    val x = positionX.toDoubleOrNull()
    val y = positionY.toDoubleOrNull()
    val facingDegrees = facing.trim().takeIf(String::isNotEmpty)?.toIntOrNull()?.mod(360)
    val valid = name.isNotBlank() && x != null && y != null && (facing.isBlank() || facingDegrees != null)
    val density = LocalDensity.current

    Box(Modifier.offset { position }.width(width).height(height)) {
        CommandFloatingPanel(
            title = listOf("Edit Unit", unit.instanceName).filter(String::isNotBlank).joinToString(" — "),
            onClose = onDismiss,
            modifier = Modifier.fillMaxSize(),
            titleBarModifier = Modifier.pointerInput(unit.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    position += IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            },
        ) {
            Text(templateName.ifBlank { "Unit" }, color = CommandColours.Backing)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editable,
                    singleLine = true,
                    label = { Text("Name") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = positionX,
                        onValueChange = { positionX = it },
                        modifier = Modifier.weight(1f),
                        enabled = editable,
                        singleLine = true,
                        isError = positionX.isNotBlank() && x == null,
                        label = { Text("Map X") },
                    )
                    OutlinedTextField(
                        value = positionY,
                        onValueChange = { positionY = it },
                        modifier = Modifier.weight(1f),
                        enabled = editable,
                        singleLine = true,
                        isError = positionY.isNotBlank() && y == null,
                        label = { Text("Map Y") },
                    )
                }
                OutlinedTextField(
                    value = facing,
                    onValueChange = { facing = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editable,
                    singleLine = true,
                    isError = facing.isNotBlank() && facingDegrees == null,
                    label = { Text("Facing (0–359, blank for none)") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = destroyed, onCheckedChange = { destroyed = it }, enabled = editable)
                    Text("Destroyed")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CommandButton(
                    onClick = {
                        onSave(
                            unit.copy(
                                instanceName = name.trim(),
                                coordinate = MapCoordinate(x ?: unit.coordinate.x, y ?: unit.coordinate.y),
                                facingDegrees = facingDegrees,
                                destroyed = destroyed,
                            ),
                        )
                    },
                    enabled = editable && valid,
                    modifier = Modifier.weight(1f),
                ) { Text("SAVE") }
                CommandButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("CANCEL") }
            }
        }
        Canvas(
            Modifier.align(Alignment.BottomEnd).size(30.dp).pointerInput(unit.id) {
                detectDragGestures(
                    onDragStart = {
                        resizeStartWidth = width
                        resizeStartHeight = height
                        resizeDragDistance = Offset.Zero
                    },
                ) { change, dragAmount ->
                    change.consume()
                    resizeDragDistance += dragAmount
                    with(density) {
                        width = clampedDialogDimensionAtPointer(resizeStartWidth.value, resizeDragDistance.x.toDp().value, 420.dp.value).dp
                        height = clampedDialogDimensionAtPointer(resizeStartHeight.value, resizeDragDistance.y.toDp().value, 360.dp.value).dp
                    }
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

private fun OperationObjectId.unitEditorId() = "unit:$value"

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

private sealed interface ComposeTransformGesture {
    val original: OperationContent
    val selectedIds: Set<OperationObjectId>

    data class Move(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val start: MapCoordinate,
    ) : ComposeTransformGesture

    data class Resize(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val corner: io.hsar.mapgenerator.commandgui.MapSelectionCorner,
    ) : ComposeTransformGesture

    data class Rotate(
        override val original: OperationContent,
        override val selectedIds: Set<OperationObjectId>,
        val pivot: MapCoordinate,
        val start: MapCoordinate,
    ) : ComposeTransformGesture
}

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
    var transformGesture by remember { mutableStateOf<ComposeTransformGesture?>(null) }
    val unitIcons = remember(content.unitTemplates) { content.unitTemplates.associate { it.id to it.image.toImageBitmap() } }
    val graphicIcons = remember(content.graphicTemplates) { content.graphicTemplates.associate { it.id to it.image.toImageBitmap() } }
    val gridOverlay = remember(renderData.gridLayer, currentViewport, nordFontFamily) {
        renderData.gridLayer?.let { grid ->
            MapViewerGridOverlayLayoutBuilder.build(
                MapViewerGridOverlayRequest(grid, currentViewport, mapOffsetX, mapOffsetY, mapWidth, mapHeight),
            ) { text, fontSize, strong ->
                textMeasurer.measure(
                    text,
                    TextStyle(fontFamily = nordFontFamily, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, fontSize = fontSize.sp),
                ).size.let { ViewerPoint(it.width.toDouble(), it.height.toDouble()) }
            }
        }
    }

    fun screenToMap(point: Offset) = MapCoordinate(
        x = (currentViewport.left + point.x / currentViewport.zoom - mapOffsetX) / mapWidth,
        y = (currentViewport.top + point.y / currentViewport.zoom - mapOffsetY) / mapHeight,
    )

    fun screenBounds(value: OperationObject): MapSelectionScreenBounds = value.selectionBounds(aspectRatio).let { bounds ->
        if (value is OperationUnit) {
            val centre = value.coordinate.toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight)
            val x = currentViewport.screenX(centre.x)
            val y = currentViewport.screenY(centre.y)
            MapSelectionScreenBounds(x - unitIconSizePx * 0.5, y - unitIconSizePx * 0.5, x + unitIconSizePx * 0.5, y + unitIconSizePx * 0.5)
        } else {
            MapSelectionScreenBounds(
                currentViewport.screenX(mapOffsetX + bounds.left * mapWidth),
                currentViewport.screenY(mapOffsetY + bounds.top * mapHeight),
                currentViewport.screenX(mapOffsetX + bounds.right * mapWidth),
                currentViewport.screenY(mapOffsetY + bounds.bottom * mapHeight),
            )
        }
    }

    fun objectAtScreen(point: Offset): OperationObject? {
        val screenPoint = ViewerPoint(point.x.toDouble(), point.y.toDouble())
        return content.objects.asReversed().firstOrNull { value ->
            val bounds = screenBounds(value)
            if (value is OperationDrawingObject) {
                value.contains(screenToMap(point), 10.0 / (currentViewport.zoom * minOf(mapWidth, mapHeight)), aspectRatio)
            } else screenPoint.x in bounds.left..bounds.right && screenPoint.y in bounds.top..bounds.bottom
        }
    }

    fun selectionFrame(): io.hsar.mapgenerator.commandgui.MapSelectionFrame? = content.objects
        .filter { it.id in selectedObjectIds }
        .map(::screenBounds)
        .takeIf(List<*>::isNotEmpty)
        ?.let { bounds ->
            MapSelectionScreenBounds(
                bounds.minOf(MapSelectionScreenBounds::left), bounds.minOf(MapSelectionScreenBounds::top),
                bounds.maxOf(MapSelectionScreenBounds::right), bounds.maxOf(MapSelectionScreenBounds::bottom),
            ).frame()
        }

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
                    objectAtScreen(change.position)?.let { value ->
                        rightClickTargetId = value.id
                        onContextMenuRequested(value, IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt()))
                    } ?: run {
                        rightClickTargetId = null
                        onContextMenuDismissed()
                    }
                    return@onPointerEvent
                }
                if (!event.buttons.isPrimaryPressed || activeTool != MapViewerTool.SELECT) return@onPointerEvent
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
                        transformGesture = ComposeTransformGesture.Rotate(content, selectedObjectIds, bounds.centre, screenToMap(change.position))
                    }

                    resizeHit != null && selectedObjectIds.isNotEmpty() -> {
                        transformGesture = ComposeTransformGesture.Resize(content, selectedObjectIds, resizeHit.corner)
                    }

                    else -> objectAtScreen(change.position)?.let { selected ->
                        val ids = when {
                            selected.id in selectedObjectIds -> selectedObjectIds
                            event.keyboardModifiers.isShiftPressed -> selectedObjectIds + selected.id
                            else -> setOf(selected.id)
                        }
                        if (selected.id !in selectedObjectIds) onObjectsSelected(setOf(selected.id), event.keyboardModifiers.isShiftPressed)
                        transformGesture = ComposeTransformGesture.Move(content, ids, screenToMap(change.position))
                    } ?: run {
                        marquee = ComposeSelectionMarquee(change.position, change.position, event.keyboardModifiers.isShiftPressed)
                    }
                }
                onContextMenuDismissed()
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                if (event.buttons.isSecondaryPressed && rightClickTargetId == null) {
                    val delta = change.position - change.previousPosition
                    if (delta != Offset.Zero) {
                        panningViewport = (panningViewport ?: currentViewport).pan(ViewerPoint(delta.x.toDouble(), delta.y.toDouble()))
                    }
                    return@onPointerEvent
                }
                transformGesture?.let { gesture ->
                    val point = screenToMap(change.position)
                    val preview = when (gesture) {
                        is ComposeTransformGesture.Move -> gesture.original.move(
                            gesture.selectedIds,
                            io.hsar.mapgenerator.commandgui.MapCoordinateDelta(point.x - gesture.start.x, point.y - gesture.start.y),
                            io.hsar.mapgenerator.commandgui.MapCoordinateBounds(-0.33, -0.33, 1.33, 1.33),
                            aspectRatio,
                        )

                        is ComposeTransformGesture.Resize -> gesture.original.resize(
                            gesture.selectedIds, gesture.corner, point, aspectRatio,
                            fromCentre = event.keyboardModifiers.isAltPressed,
                            preserveAspect = event.keyboardModifiers.isShiftPressed,
                        )

                        is ComposeTransformGesture.Rotate -> gesture.original.rotate(
                            gesture.selectedIds,
                            io.hsar.mapgenerator.commandgui.operation.rotationDelta(gesture.pivot, gesture.start, point, aspectRatio),
                            aspectRatio,
                            snap = event.keyboardModifiers.isShiftPressed,
                        )
                    }
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
                            is ComposeTransformGesture.Move -> "moved ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                            is ComposeTransformGesture.Resize -> "resized ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                            is ComposeTransformGesture.Rotate -> "rotated ${gesture.selectedIds.size} item${if (gesture.selectedIds.size == 1) "" else "s"}"
                        },
                    )
                    transformGesture = null
                }
                marquee?.let { current ->
                    val box = MapSelectionBox(
                        ViewerPoint(current.start.x.toDouble(), current.start.y.toDouble()),
                        ViewerPoint(current.current.x.toDouble(), current.current.y.toDouble())
                    )
                    if (box.isDrag) {
                        onObjectsSelected(content.objects.filter { box.overlaps(screenBounds(it)) }.mapTo(linkedSetOf(), OperationObject::id), current.additive)
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
                    drawOperationDrawing(value, mapWidth, mapHeight)
                }
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
                        color = content.unitTemplate(value.unitTemplateId)?.affiliation.facingIndicatorColor(),
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
            content.objects.filter { it.id in selectedObjectIds }.map(::screenBounds).forEach { bounds ->
                drawRect(
                    Color.White.copy(alpha = 0.82f),
                    Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                )
            }
            if (debugMapObjects) {
                content.objects.filter { it is OperationUnit || it is OperationGraphic }.forEach { value ->
                    val bounds = screenBounds(value)
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
                drawText(textMeasurer, label.text, Offset((label.centre.x - measured.size.width / 2.0).toFloat(), (label.centre.y - measured.size.height / 2.0).toFloat()), style)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOperationDrawing(
    value: OperationDrawingObject,
    mapWidth: Int,
    mapHeight: Int,
) {
    val colour = value.drawing.colour.let { Color(it.red, it.green, it.blue, it.alpha) }
    fun io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint.offset() = Offset((x * mapWidth).toFloat(), (y * mapHeight).toFloat())
    when (val drawing = value.drawing) {
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

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun LegacyReplayMapCanvas(
    renderData: MapViewerRenderData,
    savedCamera: MapViewerSavedCamera? = null,
    replay: MapViewerLedgerReplay = MapViewerLedgerReplay.EMPTY,
    activeTool: MapViewerTool = MapViewerTool.SELECT,
    selectedObjectIds: Set<java.util.UUID> = emptySet(),
    onObjectsSelected: (Set<java.util.UUID>, Boolean) -> Unit = { _, _ -> },
    onContextMenuRequested: (MapViewerReplayObject, IntOffset) -> Unit = { _, _ -> },
    onContextMenuDismissed: () -> Unit = {},
    unitIconSizePx: Double = UnitIconDisplaySize.DEFAULT_PX,
    onUnitIconSizeChanged: (Double) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val nordFontFamily = NordFontFamily()
    val textMeasurer = rememberTextMeasurer()
    val tiles = remember(renderData.baseImage) {
        RasterTileSet.from(renderData.baseImage).tiles.map { tile ->
            ComposeRasterTile(
                left = tile.left,
                top = tile.top,
                width = tile.image.width,
                height = tile.image.height,
                image = tile.image.toImageBitmap(),
            )
        }
    }
    val worldWidth = remember(renderData.baseImage) { renderData.baseImage.width * 1.66 }
    val worldHeight = remember(renderData.baseImage) { renderData.baseImage.height * 1.66 }
    val mapOffsetX = remember(renderData.baseImage) { (worldWidth - renderData.baseImage.width) / 2.0 }
    val mapOffsetY = remember(renderData.baseImage) { (worldHeight - renderData.baseImage.height) / 2.0 }
    var viewport by remember(renderData.baseImage, savedCamera) {
        mutableStateOf(
            savedCamera?.let { camera ->
                MapViewport(
                    center = ViewerPoint(
                        mapOffsetX + camera.x * renderData.baseImage.width,
                        mapOffsetY + camera.y * renderData.baseImage.height,
                    ),
                    zoom = camera.zoom.coerceIn(MapViewerSettings.MIN_ZOOM, MapViewerSettings.MAX_ZOOM),
                    viewportWidth = 1,
                    viewportHeight = 1,
                    worldWidth = worldWidth,
                    worldHeight = worldHeight,
                )
            } ?: MapViewport.centered(worldWidth, worldHeight),
        )
    }
    var rightClickTargetId by remember { mutableStateOf<java.util.UUID?>(null) }
    var marquee by remember { mutableStateOf<ComposeSelectionMarquee?>(null) }
    val gridOverlay = remember(renderData.gridLayer, viewport, nordFontFamily) {
        renderData.gridLayer?.let { grid ->
            MapViewerGridOverlayLayoutBuilder.build(
                MapViewerGridOverlayRequest(
                    gridLayer = grid,
                    viewport = viewport,
                    mapOffsetX = mapOffsetX,
                    mapOffsetY = mapOffsetY,
                    mapWidth = renderData.baseImage.width,
                    mapHeight = renderData.baseImage.height,
                ),
            ) { text, fontSize, strong ->
                textMeasurer.measure(
                    text = text,
                    style = TextStyle(
                        fontFamily = nordFontFamily,
                        fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal,
                        fontSize = fontSize.sp,
                    ),
                ).size.let { ViewerPoint(it.width.toDouble(), it.height.toDouble()) }
            }
        }
    }
    val unitIcons = remember(replay) {
        replay.objects
            .filter { it.kind == MapViewerReplayObjectKind.UNIT }
            .mapNotNull { mapObject -> mapObject.icon?.let { mapObject.id to it.toImageBitmap() } }
            .toMap()
    }
    val graphicIcons = remember(replay) {
        replay.objects
            .filter { it.kind == MapViewerReplayObjectKind.GRAPHIC }
            .mapNotNull { mapObject -> mapObject.icon?.let { mapObject.id to it.toImageBitmap() } }
            .toMap()
    }

    Canvas(
        modifier = modifier
            .background(Color(0xFF15181B))
            .onSizeChanged { viewport = viewport.resized(it.width, it.height) }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                if (event.keyboardModifiers.isAltPressed) {
                    onUnitIconSizeChanged(UnitIconDisplaySize.scroll(unitIconSizePx, change.scrollDelta.y))
                } else {
                    val factor = if (change.scrollDelta.y < 0f) 1.1f else 1f / 1.1f
                    viewport = viewport.zoomAt(
                        ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble()),
                        factor.toDouble(),
                    )
                }
                change.consume()
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (event.buttons.isSecondaryPressed && rightClickTargetId == null) {
                    event.changes.firstOrNull()?.let { change ->
                        val delta = change.position - change.previousPosition
                        if (delta != Offset.Zero) {
                            viewport = viewport.pan(ViewerPoint(delta.x.toDouble(), delta.y.toDouble()))
                            change.consume()
                        }
                    }
                }
                marquee?.let { current ->
                    event.changes.firstOrNull()?.let { change ->
                        marquee = current.copy(current = change.position)
                        change.consume()
                    }
                }
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.buttons.isSecondaryPressed) {
                    event.changes.firstOrNull()?.let { change ->
                        replay.objectAtScreen(
                            screenPoint = ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble()),
                            viewport = viewport,
                            mapOffsetX = mapOffsetX,
                            mapOffsetY = mapOffsetY,
                            mapWidth = renderData.baseImage.width,
                            mapHeight = renderData.baseImage.height,
                            hitRadius = unitIconSizePx / 2.0,
                        )?.let { mapObject ->
                            rightClickTargetId = mapObject.id
                            onContextMenuRequested(
                                mapObject,
                                IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt()),
                            )
                        } ?: run {
                            rightClickTargetId = null
                            onContextMenuDismissed()
                        }
                    }
                } else if (event.buttons.isPrimaryPressed && activeTool == MapViewerTool.SELECT) {
                    event.changes.firstOrNull()?.let { change ->
                        val selected = replay.objectAtScreen(
                            screenPoint = ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble()),
                            viewport = viewport,
                            mapOffsetX = mapOffsetX,
                            mapOffsetY = mapOffsetY,
                            mapWidth = renderData.baseImage.width,
                            mapHeight = renderData.baseImage.height,
                            hitRadius = unitIconSizePx / 2.0,
                        )
                        if (selected != null) {
                            onObjectsSelected(setOf(selected.id), event.keyboardModifiers.isShiftPressed)
                        } else {
                            marquee = ComposeSelectionMarquee(change.position, change.position, event.keyboardModifiers.isShiftPressed)
                        }
                    }
                    onContextMenuDismissed()
                }
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                if (!event.buttons.isSecondaryPressed) rightClickTargetId = null
                marquee?.let { current ->
                    val box = MapSelectionBox(
                        start = ViewerPoint(current.start.x.toDouble(), current.start.y.toDouble()),
                        current = ViewerPoint(current.current.x.toDouble(), current.current.y.toDouble()),
                    )
                    if (box.isDrag) {
                        onObjectsSelected(
                            replay.objects.filter {
                                it.selectionBounds(viewport, mapOffsetX, mapOffsetY, renderData.baseImage.width, renderData.baseImage.height, unitIconSizePx)
                                    ?.let(box::overlaps) == true
                            }
                                .mapTo(linkedSetOf(), MapViewerReplayObject::id),
                            current.additive,
                        )
                    } else if (!current.additive) {
                        onObjectsSelected(emptySet(), false)
                    }
                    marquee = null
                }
            }
    ) {
        val zoom = viewport.zoom.toFloat()
        val mapLeft = viewport.screenX(mapOffsetX).toFloat()
        val mapTop = viewport.screenY(mapOffsetY).toFloat()
        val visibleMapLeft = (viewport.left - mapOffsetX).coerceAtLeast(0.0)
        val visibleMapTop = (viewport.top - mapOffsetY).coerceAtLeast(0.0)
        val visibleMapRight = (viewport.left + viewport.visibleWidth - mapOffsetX)
            .coerceAtMost(renderData.baseImage.width.toDouble())
        val visibleMapBottom = (viewport.top + viewport.visibleHeight - mapOffsetY)
            .coerceAtMost(renderData.baseImage.height.toDouble())
        clipRect {
            withTransform({
                translate(mapLeft, mapTop)
                scale(zoom, zoom, Offset.Zero)
            }) {
                tiles.filter { tile ->
                    tile.left < visibleMapRight &&
                            tile.top < visibleMapBottom &&
                            tile.left + tile.width > visibleMapLeft &&
                            tile.top + tile.height > visibleMapTop
                }.forEach { tile ->
                    drawImage(
                        image = tile.image,
                        dstOffset = IntOffset(tile.left, tile.top),
                        dstSize = IntSize(tile.width, tile.height),
                    )
                }
            }
            gridOverlay?.lineSegments?.forEach { segment ->
                drawLine(
                    color = segment.colour.toComposeColour(),
                    start = Offset(segment.start.x.toFloat(), segment.start.y.toFloat()),
                    end = Offset(segment.end.x.toFloat(), segment.end.y.toFloat()),
                    strokeWidth = segment.thickness.toFloat(),
                    cap = StrokeCap.Square,
                )
            }
            withTransform({
                translate(mapLeft, mapTop)
                scale(zoom, zoom, Offset.Zero)
            }) {
                replay.objects
                    .filter { it.x != null && it.y != null }
                    .forEach { mapObject ->
                        val centre = Offset(
                            (mapObject.x!! * renderData.baseImage.width).toFloat(),
                            (mapObject.y!! * renderData.baseImage.height).toFloat(),
                        )
                        val radius = 7f / zoom
                        val colour = when {
                            mapObject.destroyed == true -> Color(0xFFE05A5A)
                            mapObject.kind == MapViewerReplayObjectKind.GRAPHIC -> Color(0xFFFFC857)
                            else -> Color(0xFF67D5E8)
                        }
                        when (mapObject.kind) {
                            MapViewerReplayObjectKind.GRAPHIC ->
                                graphicIcons[mapObject.id]?.let { icon ->
                                    val width = ((mapObject.normalizedWidth ?: 0.032) * renderData.baseImage.width).roundToInt().coerceAtLeast(1)
                                    val height = ((mapObject.normalizedHeight ?: 0.032) * renderData.baseImage.height).roundToInt().coerceAtLeast(1)
                                    drawImage(
                                        image = icon,
                                        dstOffset = IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt()),
                                        dstSize = IntSize(width, height),
                                    )
                                } ?: drawRect(
                                    color = colour,
                                    topLeft = centre - Offset(radius, radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                                )

                            MapViewerReplayObjectKind.UNIT ->
                                if (mapObject.icon == null) drawCircle(colour, radius, centre)

                            else -> drawCircle(colour, radius, centre)
                        }
                        if (mapObject.destroyed == true && mapObject.icon == null) {
                            drawLine(
                                color = Color.Black,
                                start = centre - Offset(radius, radius),
                                end = centre + Offset(radius, radius),
                                strokeWidth = (2f / zoom).coerceAtLeast(0.5f),
                            )
                            drawLine(
                                color = Color.Black,
                                start = centre + Offset(radius, -radius),
                                end = centre + Offset(-radius, radius),
                                strokeWidth = (2f / zoom).coerceAtLeast(0.5f),
                            )
                        }
                        if (mapObject.id in selectedObjectIds && mapObject.icon == null) {
                            drawCircle(
                                color = Color.White,
                                radius = radius + 4f / zoom,
                                center = centre,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2f / zoom),
                            )
                        }
                    }
            }
            replay.objects
                .filter { it.kind == MapViewerReplayObjectKind.UNIT && it.x != null && it.y != null }
                .forEach { mapObject ->
                    unitIcons[mapObject.id]?.let { icon ->
                        val width = unitIconSizePx.roundToInt()
                        val height = (width.toDouble() * icon.height / icon.width).roundToInt()
                        val centre = Offset(
                            viewport.screenX(mapOffsetX + mapObject.x!! * renderData.baseImage.width).toFloat(),
                            viewport.screenY(mapOffsetY + mapObject.y!! * renderData.baseImage.height).toFloat(),
                        )
                        drawImage(
                            image = icon,
                            dstOffset = IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt()),
                            dstSize = IntSize(width, height),
                        )
                        if (mapObject.destroyed == true) {
                            val radius = maxOf(width, height) / 2f
                            drawLine(Color.Black, centre - Offset(radius, radius), centre + Offset(radius, radius), 2f)
                            drawLine(Color.Black, centre + Offset(radius, -radius), centre + Offset(-radius, radius), 2f)
                        }
                        if (mapObject.id in selectedObjectIds) {
                            drawCircle(
                                color = Color.White,
                                radius = maxOf(width, height) / 2f + 4f,
                                center = centre,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
                            )
                        }
                    }
                }
            replay.objects
                .filter { it.kind == MapViewerReplayObjectKind.GRAPHIC && it.x != null && it.y != null && it.displayName != null }
                .forEach { mapObject ->
                    val centre = Offset(
                        viewport.screenX(mapOffsetX + mapObject.x!! * renderData.baseImage.width).toFloat(),
                        viewport.screenY(mapOffsetY + mapObject.y!! * renderData.baseImage.height).toFloat(),
                    )
                    val graphicHeight = (mapObject.normalizedHeight ?: 0.032) * renderData.baseImage.height * viewport.zoom
                    val style = TextStyle(color = Color.White, fontFamily = nordFontFamily, fontSize = 16.sp)
                    val measured = textMeasurer.measure(mapObject.displayName!!, style)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = mapObject.displayName!!,
                        topLeft = Offset(centre.x - measured.size.width / 2f, centre.y - graphicHeight.toFloat() / 2f - measured.size.height - 4f),
                        style = style,
                    )
                }
            val selectedBounds = replay.objects
                .filter { it.id in selectedObjectIds }
                .mapNotNull { it.selectionBounds(viewport, mapOffsetX, mapOffsetY, renderData.baseImage.width, renderData.baseImage.height, unitIconSizePx) }
            selectedBounds.forEach { bounds ->
                drawRect(
                    color = Color.White.copy(alpha = 0.82f),
                    topLeft = Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    size = androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
                )
            }
            selectedBounds.takeIf { it.isNotEmpty() }?.let { bounds ->
                MapSelectionScreenBounds(
                    left = bounds.minOf(MapSelectionScreenBounds::left),
                    top = bounds.minOf(MapSelectionScreenBounds::top),
                    right = bounds.maxOf(MapSelectionScreenBounds::right),
                    bottom = bounds.maxOf(MapSelectionScreenBounds::bottom),
                ).frame().let { frame ->
                    if (bounds.size > 1) {
                        drawPath(
                            path = Path().apply {
                                moveTo(frame.topLeft.x.toFloat(), frame.topLeft.y.toFloat())
                                lineTo(frame.topRight.x.toFloat(), frame.topRight.y.toFloat())
                                lineTo(frame.bottomRight.x.toFloat(), frame.bottomRight.y.toFloat())
                                lineTo(frame.bottomLeft.x.toFloat(), frame.bottomLeft.y.toFloat())
                                close()
                            },
                            color = Color.White.copy(alpha = 0.11f),
                        )
                    }
                    frame.cornerBrackets().forEach { bracket ->
                        drawPath(
                            path = Path().apply {
                                moveTo(bracket.points.first().x.toFloat(), bracket.points.first().y.toFloat())
                                bracket.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
                                close()
                            },
                            color = Color.White,
                        )
                    }
                    val rotateHandle = frame.rotationHandle()
                    drawCircle(Color.White, 7f, Offset(rotateHandle.x.toFloat(), rotateHandle.y.toFloat()))
                    drawCircle(Color(0xFF120909), 3.5f, Offset(rotateHandle.x.toFloat(), rotateHandle.y.toFloat()))
                }
            }
            marquee?.let { selection ->
                MapSelectionBox(
                    start = ViewerPoint(selection.start.x.toDouble(), selection.start.y.toDouble()),
                    current = ViewerPoint(selection.current.x.toDouble(), selection.current.y.toDouble()),
                ).takeIf(MapSelectionBox::isDrag)?.let { box ->
                    drawRect(
                        color = Color(0xFFDF4A4A).copy(alpha = 0.15f),
                        topLeft = Offset(box.left.toFloat(), box.top.toFloat()),
                        size = androidx.compose.ui.geometry.Size((box.right - box.left).toFloat(), (box.bottom - box.top).toFloat()),
                    )
                    drawRect(
                        color = Color(0xFFDF4A4A),
                        topLeft = Offset(box.left.toFloat(), box.top.toFloat()),
                        size = androidx.compose.ui.geometry.Size((box.right - box.left).toFloat(), (box.bottom - box.top).toFloat()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
                    )
                }
            }
            gridOverlay?.labels?.forEach { label ->
                val style = TextStyle(
                    color = label.colour.toComposeColour(),
                    fontFamily = nordFontFamily,
                    fontWeight = if (label.strong) FontWeight.Bold else FontWeight.Normal,
                    fontSize = label.fontSize.sp,
                )
                val measured = textMeasurer.measure(label.text, style)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label.text,
                    topLeft = Offset(
                        (label.centre.x - measured.size.width / 2.0).toFloat(),
                        (label.centre.y - measured.size.height / 2.0).toFloat(),
                    ),
                    style = style,
                )
            }
        }
    }
}

private data class ComposeSelectionMarquee(
    val start: Offset,
    val current: Offset,
    val additive: Boolean,
)

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

private fun MapViewerReplayObject.selectionBounds(
    viewport: MapViewport,
    mapOffsetX: Double,
    mapOffsetY: Double,
    mapWidth: Int,
    mapHeight: Int,
    unitIconSizePx: Double,
): MapSelectionScreenBounds? = x?.let { mapX ->
    y?.let { mapY ->
        val centre = MapCoordinate(mapX, mapY).toWorldPoint(mapOffsetX, mapOffsetY, mapWidth, mapHeight).let { point ->
            ViewerPoint(viewport.screenX(point.x), viewport.screenY(point.y))
        }
        val halfWidth = when (kind) {
            MapViewerReplayObjectKind.GRAPHIC -> (normalizedWidth ?: 0.032) * mapWidth * viewport.zoom * 0.5
            else -> unitIconSizePx * 0.5
        }
        val halfHeight = when (kind) {
            MapViewerReplayObjectKind.GRAPHIC -> (normalizedHeight ?: 0.032) * mapHeight * viewport.zoom * 0.5
            else -> unitIconSizePx * 0.5
        }
        MapSelectionScreenBounds(centre.x - halfWidth, centre.y - halfHeight, centre.x + halfWidth, centre.y + halfHeight)
    }
}

private fun java.awt.Color.toComposeColour() =
    Color(red = red, green = green, blue = blue, alpha = alpha)

internal fun java.awt.image.BufferedImage.toImageBitmap(): ImageBitmap =
    ByteArrayOutputStream().use { output ->
        ImageIO.write(this, "png", output)
        Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    }
