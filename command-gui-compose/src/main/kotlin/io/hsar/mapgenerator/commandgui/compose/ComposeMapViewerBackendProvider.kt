package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.operation.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.hsar.mapgenerator.commandgui.MapViewerCommand
import io.hsar.mapgenerator.commandgui.MapViewerCommandContext
import io.hsar.mapgenerator.commandgui.MapViewerCommandItem
import io.hsar.mapgenerator.commandgui.MapViewerCommandResolver
import io.hsar.mapgenerator.commandgui.MapViewerChromeLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerDisplaySettings
import io.hsar.mapgenerator.commandgui.MapViewerObjectCommand
import io.hsar.mapgenerator.commandgui.MapViewerPanel
import io.hsar.mapgenerator.commandgui.MapViewerDrawingColour
import io.hsar.mapgenerator.commandgui.MapViewerDrawingMode
import io.hsar.mapgenerator.commandgui.MapViewerGlobalSettings
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayRequest
import io.hsar.mapgenerator.commandgui.MapViewerGridReference
import io.hsar.mapgenerator.commandgui.MapViewerImageLoader
import io.hsar.mapgenerator.commandgui.MapViewerMenuLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerMenuTokens
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
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationDestroyedUnitDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationFreeformDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationFireArcDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationGraphic
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicAuthoring
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationLedgerTarget
import io.hsar.mapgenerator.commandgui.operation.OperationObject
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OPERATION_DISPLAY_NAME_PROPERTY
import io.hsar.mapgenerator.commandgui.operation.OperationNote
import io.hsar.mapgenerator.commandgui.operation.OperationNoteDraft
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
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
import io.hsar.mapgenerator.commandgui.operation.OperationTurnGroup
import io.hsar.mapgenerator.commandgui.operation.contains
import io.hsar.mapgenerator.commandgui.operation.objectAtScreen
import io.hsar.mapgenerator.commandgui.operation.optionalDisplayName
import io.hsar.mapgenerator.commandgui.operation.selectionBounds
import io.hsar.mapgenerator.commandgui.operation.selectionFrame
import io.hsar.mapgenerator.commandgui.operation.selectionScreenBounds
import io.hsar.mapgenerator.commandgui.operation.nextTurnName
import io.hsar.mapgenerator.commandgui.operation.turnGroups
import io.hsar.mapgenerator.commandgui.save.MapThumbnailRenderer
import io.hsar.mapgenerator.commandgui.toWorldPoint
import io.hsar.mapgenerator.commandgui.viewerBackgroundBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
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
    ) {
        val fontDiagnostic = inspectComposeFonts()
        val buildInfo = ComposeBuildInfo.load()
        val presentationResources = ComposePresentationResources.load()
        application {
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
            NordTheme(fontAvailable = fontDiagnostic.available) {
                ComposeMapViewer(
                    renderMap = renderMap,
                    initialSettings = initialSettings,
                    initialSetupInputs = initialSetupInputs,
                    awtWindow = window,
                    buildInfo = buildInfo,
                    presentationResources = presentationResources,
                    fontDiagnostic = fontDiagnostic,
                    onTitleChanged = { windowTitle = it },
                    onExit = ::exitApplication,
                    onCloseRequestHandlerChanged = { closeRequestHandler = it },
                )
            }
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
    buildInfo: ComposeBuildInfo,
    presentationResources: ComposePresentationResources,
    fontDiagnostic: ComposeFontDiagnostic,
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
    val modalHost = remember { ComposeModalHostState() }
    val filePicker = remember(awtWindow) { ComposeFilePicker(awtWindow as? Frame) }

    LaunchedEffect(launchState.activeMap?.settings?.operationName, launchState.screen) {
        onTitleChanged(
            launchState.activeMap?.settings?.windowTitle()
                ?: "Operational Command Tool - ${launchState.screen.windowTitle}",
        )
    }

    CompositionLocalProvider(LocalComposeFilePicker provides filePicker) {
    Box(Modifier.fillMaxSize()) {
    launchState.activeMap?.let { map ->
        key(map) {
            ViewerWorkspace(
                initialMap = map,
                repository = repository,
                initialDisplaySettings = globalSettings.displaySettings(),
                onUnitIconSizeChanged = globalSettings::updateUnitIconSizePx,
                onDestroyedUnitDisplayModeChanged = globalSettings::updateDestroyedUnitDisplayMode,
                onFireArcDisplayModeChanged = globalSettings::updateFireArcDisplayMode,
                onReturnToLaunchpad = launchState::returnToLaunchpad,
                onExitProgram = onExit,
                onCloseRequestHandlerChanged = onCloseRequestHandlerChanged,
                modalHost = modalHost,
                filePicker = filePicker,
            )
        }
    } ?: run {
        LaunchedEffect(launchState.screen) {
            onCloseRequestHandlerChanged(onExit)
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
            onBack = launchState::returnToLaunchpad,
            onLaunch = { settings, renderData ->
                launchState.openOperation(repository.createOperation(settings, renderData ?: error("Operation map was not rendered")))
            },
        )

        ComposeLaunchScreen.EXISTING_OPERATIONS -> ComposeExistingOperationsScreen(
            repository = repository,
            onBack = launchState::returnToLaunchpad,
            onMapLoaded = launchState::openOperation,
        )
        }
    }
    if (launchState.activeMap == null && launchState.screen == ComposeLaunchScreen.MAIN) {
        VersionAboutButton(
            version = buildInfo.version,
            onClick = {
                modalHost.show(ComposeModalRequest("about") { dismiss ->
                    AboutModalContent(buildInfo, presentationResources, fontDiagnostic.label, dismiss)
                })
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).height(32.dp),
        )
    }
    ComposeModalHost(modalHost)
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
    onBack: () -> Unit,
    onLaunch: (MapViewerSettings, MapViewerRenderData?) -> Unit,
) {
    val filePicker = LocalComposeFilePicker.current
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
        loadingMap = true
        scope.launch {
            when (val selected = filePicker.chooseAndDecode(
                title = "Choose map image",
                pattern = "*.png;*.jpg;*.jpeg",
                unsupportedMessage = "The selected file is not a supported image.",
                decode = ImageIO::read,
            )) {
                ComposeFileSelection.Cancelled -> Unit
                is ComposeFileSelection.Selected -> {
                    mapImage = selected.value
                    mapName = selected.file.name
                    errorMessage = null
                }
                is ComposeFileSelection.Unreadable -> errorMessage = "Could not open ${selected.file.name}: ${selected.message}"
                is ComposeFileSelection.Unsupported -> errorMessage = selected.message
            }
            loadingMap = false
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
    onBack: () -> Unit,
    onMapLoaded: (OperationDocument) -> Unit,
) {
    val filePicker = LocalComposeFilePicker.current
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
        if (loadingPath != null) return
        scope.launch {
            when (val selected = filePicker.chooseAndDecode(
                title = "Open operation save",
                pattern = "*.zip",
                unsupportedMessage = "The selected file is not a supported operation archive.",
                decode = { file -> repository.loadOperation(file.toPath()) },
            )) {
                ComposeFileSelection.Cancelled -> Unit
                is ComposeFileSelection.Selected -> onMapLoaded(selected.value)
                is ComposeFileSelection.Unreadable -> errorMessage = "Could not load ${selected.file.name}: ${selected.message}"
                is ComposeFileSelection.Unsupported -> errorMessage = selected.message
            }
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

private data class ComposeViewerEditorActions(
    val displaySettings: MapViewerDisplaySettings,
    val onCommand: (MapViewerCommand) -> Unit,
    val onSave: () -> Unit,
    val onEditDrawingName: (OperationObjectId) -> Unit,
    val onDestroyedUnitDisplayModeChanged: (OperationDestroyedUnitDisplayMode) -> Unit,
    val onUnitIconSizeChanged: (Double) -> Unit,
    val onFireArcDisplayModeChanged: (OperationFireArcDisplayMode) -> Unit,
)

private val LocalViewerEditorActions = staticCompositionLocalOf<ComposeViewerEditorActions> {
    error("Viewer editor actions are unavailable")
}

private val LocalEditableTextFocusChanged = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

private fun Modifier.trackEditableTextFocus() = composed {
    val onChanged = LocalEditableTextFocusChanged.current
    onFocusChanged { onChanged(it.isFocused) }
}

@Composable
private fun ViewerWorkspace(
    initialMap: ComposeActiveMap,
    repository: MapViewerSaveRepository,
    initialDisplaySettings: MapViewerDisplaySettings,
    onUnitIconSizeChanged: (Double) -> Double,
    onDestroyedUnitDisplayModeChanged: (OperationDestroyedUnitDisplayMode) -> OperationDestroyedUnitDisplayMode,
    onFireArcDisplayModeChanged: (OperationFireArcDisplayMode) -> OperationFireArcDisplayMode,
    onReturnToLaunchpad: () -> Unit,
    onExitProgram: () -> Unit,
    onCloseRequestHandlerChanged: (() -> Unit) -> Unit,
    modalHost: ComposeModalHostState,
    filePicker: ComposeFilePicker,
) {
    var operationState by remember(initialMap.document) {
        mutableStateOf(ComposeOperationState(initialMap.document))
    }
    var displaySettings by remember { mutableStateOf(initialDisplaySettings) }
    val unitIconSizePx = displaySettings.unitIconSizePx
    var escapeMenuOpen by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ComposeMapContextMenu?>(null) }
    var restoreMapFocusAfterMenu by remember { mutableStateOf(false) }
    var drawingMode by remember { mutableStateOf(MapViewerDrawingMode.FREEFORM) }
    var drawingColour by remember { mutableStateOf(MapViewerDrawingColour.RED) }
    var drawingNameEdit by remember { mutableStateOf<ComposeDrawingNameEdit?>(null) }
    var toolGestureCancellationEpoch by remember { mutableStateOf(0) }
    var pendingExit by remember { mutableStateOf<ComposePendingExit?>(null) }
    val transientMessages = remember { ComposeTransientMessageHostState() }
    var draggedDockTab by remember { mutableStateOf<OperationEditorId?>(null) }
    var dockDropTarget by remember { mutableStateOf<Pair<OperationDockRegion, Int>?>(null) }
    var dispatchedKeysDown by remember { mutableStateOf(emptySet<Key>()) }
    var editableTextFocused by remember { mutableStateOf(false) }
    val autosaveScheduler = remember { ComposeAutosaveScheduler(debounceMillis = COMPOSE_AUTOSAVE_DEBOUNCE_MS) }
    val scope = rememberCoroutineScope()
    val mapFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialMap.document.operationId) {
        if (operationState.workspace.activeEditorId == null) mapFocusRequester.requestFocus()
    }

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
                transientMessages.show("Saved", ComposeMessageSeverity.SUCCESS)
                if (operationState.revision == targetRevision) onSuccess?.invoke()
            }.onFailure { failure ->
                operationState = operationState.saveFailed(failure.message ?: failure::class.simpleName ?: "Unknown save failure")
                transientMessages.show("Save failed", ComposeMessageSeverity.ERROR)
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

    LaunchedEffect(displaySettings.destroyedUnitDisplayMode, operationState.visibleContent.objects) {
        if (displaySettings.destroyedUnitDisplayMode == OperationDestroyedUnitDisplayMode.Hide) {
            val hiddenIds = operationState.visibleContent.objects.filterIsInstance<OperationUnit>()
                .filterNot { OperationDestroyedUnitDisplayPolicy.visible(it, displaySettings.destroyedUnitDisplayMode) }
                .mapTo(hashSetOf(), OperationUnit::id)
            if (hiddenIds.isNotEmpty()) {
                operationState = operationState.selectBox(operationState.selectedObjectIds - hiddenIds, false)
                operationState.workspace.editors.filter { (it.target as? OperationEditorTarget.Object)?.objectId in hiddenIds }
                    .forEach { operationState = operationState.closeEditor(it.id) }
                contextMenu = contextMenu?.takeUnless { it.objectId in hiddenIds }
            }
        }
    }

    fun dispatch(command: MapViewerCommand) {
        val restoreMapAfterDispatch = contextMenu != null && when (command) {
            is MapViewerCommand.OpenNote -> false
            is MapViewerCommand.ObjectAction -> command.action in setOf(
                MapViewerObjectCommand.DELETE_UNIT,
                MapViewerObjectCommand.DELETE_GRAPHIC,
                MapViewerObjectCommand.DELETE_DRAWING,
            )
            else -> true
        }
        when (command) {
            is MapViewerCommand.TogglePanel -> operationState = operationState.togglePanel(command.panel)
            MapViewerCommand.Save -> saveNow()
            MapViewerCommand.Undo -> operationState = operationState.undo()
            MapViewerCommand.Redo -> operationState = operationState.redo()
            MapViewerCommand.ReturnToLaunchpad -> { escapeMenuOpen = false; requestExit(ComposePendingExit.LAUNCHPAD) }
            MapViewerCommand.Exit -> { escapeMenuOpen = false; requestExit(ComposePendingExit.DESKTOP) }
            MapViewerCommand.CloseEscapeMenu -> escapeMenuOpen = false
            is MapViewerCommand.SetWorkspaceMode -> operationState = operationState.updateWorkspaceMode(command.mode)
            is MapViewerCommand.SetDestroyedUnitDisplayMode -> displaySettings = displaySettings.copy(destroyedUnitDisplayMode = onDestroyedUnitDisplayModeChanged(command.mode))
            is MapViewerCommand.SetUnitIconSize -> displaySettings = displaySettings.copy(unitIconSizePx = onUnitIconSizeChanged(command.sizePx))
            is MapViewerCommand.SetFireArcDisplayMode -> displaySettings = displaySettings.copy(fireArcDisplayMode = onFireArcDisplayModeChanged(command.mode))
            is MapViewerCommand.DeleteObjects -> operationState = operationState.selectBox(command.objectIds, false).deleteSelection()
            is MapViewerCommand.OpenNote -> operationState = operationState.openNoteEditor(command.noteId)
            is MapViewerCommand.ObjectAction -> when (command.action) {
                MapViewerObjectCommand.EDIT_UNIT -> operationState = operationState.openUnitEditor(command.objectId)
                MapViewerObjectCommand.DELETE_UNIT,
                MapViewerObjectCommand.DELETE_GRAPHIC,
                MapViewerObjectCommand.DELETE_DRAWING,
                    -> operationState = operationState.selectObject(command.objectId).deleteSelection()
                MapViewerObjectCommand.SCIREP -> operationState = operationState.openSensorReport(command.objectId, OperationWorkflowReportMode.SCIREP)
                MapViewerObjectCommand.NAVREP -> operationState = operationState.openSensorReport(command.objectId, OperationWorkflowReportMode.NAVREP)
                MapViewerObjectCommand.TACTICAL -> operationState = operationState.openTactical(command.objectId)
                MapViewerObjectCommand.STATUS -> operationState = operationState.openOlympiadDawnStatus(command.objectId)
                MapViewerObjectCommand.EDIT_GRAPHIC -> operationState = operationState.openGraphicEditor(command.objectId)
                MapViewerObjectCommand.EDIT_DRAWING_NAME -> {
                    val drawing = operationState.visibleContent.objectById(command.objectId) as? OperationDrawingObject
                    if (drawing != null && !operationState.isReadOnly) drawingNameEdit = ComposeDrawingNameEdit(drawing.id, drawing.optionalDisplayName.orEmpty())
                }
            }
        }
        restoreMapFocusAfterMenu = restoreMapAfterDispatch
        contextMenu = null
    }

    fun dismissContextMenuAndRestoreMapFocus() {
        restoreMapFocusAfterMenu = contextMenu != null
        contextMenu = null
    }

    LaunchedEffect(contextMenu, restoreMapFocusAfterMenu) {
        if (contextMenu == null && restoreMapFocusAfterMenu) {
            mapFocusRequester.requestFocus()
            restoreMapFocusAfterMenu = false
        }
    }

    val editorActions = ComposeViewerEditorActions(
        displaySettings = displaySettings,
        onCommand = ::dispatch,
        onSave = { dispatch(MapViewerCommand.Save) },
        onEditDrawingName = { id ->
            (operationState.visibleContent.objectById(id) as? OperationDrawingObject)?.let { drawing ->
                drawingNameEdit = ComposeDrawingNameEdit(drawing.id, drawing.optionalDisplayName.orEmpty())
            }
        },
        onDestroyedUnitDisplayModeChanged = { mode ->
            dispatch(MapViewerCommand.SetDestroyedUnitDisplayMode(mode))
        },
        onUnitIconSizeChanged = { size ->
            dispatch(MapViewerCommand.SetUnitIconSize(size))
        },
        onFireArcDisplayModeChanged = { mode ->
            dispatch(MapViewerCommand.SetFireArcDisplayMode(mode))
        },
    )

    CompositionLocalProvider(
        LocalViewerEditorActions provides editorActions,
        LocalEditableTextFocusChanged provides { editableTextFocused = it },
    ) {
    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.fillMaxSize().focusRequester(mapFocusRequester).focusable().onKeyEvent { event ->
            if (filePicker.open || modalHost.isShowing) return@onKeyEvent true
            if (event.type == KeyEventType.KeyUp) {
                dispatchedKeysDown = dispatchedKeysDown - event.key
                return@onKeyEvent false
            }
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            if (event.key in dispatchedKeysDown) return@onKeyEvent true
            dispatchedKeysDown = dispatchedKeysDown + event.key
            val handled = when {
                event.key == Key.Escape && contextMenu != null -> {
                    dismissContextMenuAndRestoreMapFocus()
                    true
                }

                event.key == Key.Escape && operationState.previewContent != null -> {
                    operationState = operationState.cancelPreview().cancelPlacements()
                    toolGestureCancellationEpoch++
                    true
                }

                event.key == Key.Escape && (operationState.unitPlacement != null || operationState.graphicPlacement != null || operationState.noteReferencePick != null) -> {
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
                    dispatch(MapViewerCommand.DeleteObjects(operationState.selectedObjectIds))
                    true
                }

                event.isCtrlPressed && event.key == Key.S -> {
                    dispatch(MapViewerCommand.Save)
                    true
                }

                editableTextFocused && (event.key == Key.Delete || event.isCtrlPressed && event.key in setOf(Key.A, Key.C, Key.X, Key.V, Key.Z, Key.Y)) -> false

                event.isCtrlPressed && event.key == Key.Z && event.isShiftPressed -> {
                    dispatch(MapViewerCommand.Redo)
                    true
                }

                event.isCtrlPressed && event.key == Key.Z -> {
                    dispatch(MapViewerCommand.Undo)
                    true
                }

                event.isCtrlPressed && event.key == Key.Y -> {
                    dispatch(MapViewerCommand.Redo)
                    true
                }

                event.isCtrlPressed && event.key == Key.Tab && operationState.workspaceMode == WorkspaceMode.DOCKED -> {
                    operationState = operationState.cycleDockTabs(if (event.isShiftPressed) -1 else 1)
                    true
                }

                else -> false
            }
            if (!handled) dispatchedKeysDown = dispatchedKeysDown - event.key
            handled
        },
    ) {
        Column {
            Row(Modifier.weight(1f)) {
                if (operationState.workspaceMode == WorkspaceMode.DOCKED) {
                    DockedWorkspace(state = operationState, region = OperationDockRegion.LEFT, draggedEditorId = draggedDockTab, dropTarget = dockDropTarget, onDragStarted = { draggedDockTab = it }, onDropTargetChanged = { dockDropTarget = it }, onDrop = { id -> dockDropTarget?.let { target -> operationState = operationState.dockEditor(id, target.first, target.second) }; draggedDockTab = null; dockDropTarget = null }, onStateChanged = { operationState = it })
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val chromeLayout = MapViewerChromeLayoutBuilder.build(maxWidth.value.toDouble())
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
                                if (mapObject.id !in operationState.selectedObjectIds) operationState = operationState.selectObject(mapObject.id)
                                val selectedIds = if (mapObject.id in operationState.selectedObjectIds) operationState.selectedObjectIds else setOf(mapObject.id)
                                contextMenu = ComposeMapContextMenu(
                                    mapObject.id,
                                    mapObject.displayName(operationState.visibleContent),
                                    position,
                                    MapViewerCommandResolver.contextMenu(
                                        mapObject.id,
                                        MapViewerCommandContext(
                                            operationState.visibleContent,
                                            selectedIds,
                                            operationState.isReadOnly,
                                            operationState.olympiadDawnEnabled,
                                            operationState.activeTool,
                                            operationState.unitPlacement != null || operationState.graphicPlacement != null || operationState.noteReferencePick != null,
                                            operationState.workspace,
                                            operationState.savingRevision == null,
                                        ),
                                    ),
                                )
                            },
                            onContextMenuDismissed = ::dismissContextMenuAndRestoreMapFocus,
                        onPreviewChanged = { operationState = operationState.preview(it) },
                        onPreviewCommitted = { _ -> operationState = operationState.commitPreview() },
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
                        noteReferencePickActive = operationState.noteReferencePick != null,
                        noteReferenceHoveredObjectId = operationState.noteReferencePick?.hoveredObjectId,
                        onNoteReferencePreview = { operationState = operationState.previewNoteReference(it) },
                        onNoteReferencePicked = { operationState = operationState.addPickedNoteReference(it) },
                        onNoteReferenceCancelled = { operationState = operationState.cancelNoteReferencePick() },
                        cancellationEpoch = toolGestureCancellationEpoch,
                            ecmFields = operationState.ecmFields(),
                            unitsPerPixel = operationState.document.settings.unitsPerPixel,
                        tacticalOverlayLines = operationState.workspace.editors.takeIf { operationState.olympiadDawnEnabled }.orEmpty().mapNotNull { editor ->
                                val draft = editor.draft as? OperationEditorDraft.Operational ?: return@mapNotNull null
                                if (editor.kind != OperationEditorKind.TACTICAL) return@mapNotNull null
                                val sourceId = (editor.target as? OperationEditorTarget.Object)?.objectId ?: return@mapNotNull null
                                val targetId = draft.selectedTargetId ?: return@mapNotNull null
                                val source = operationState.visibleContent.objectById(sourceId) as? OperationUnit ?: return@mapNotNull null
                                val target = operationState.visibleContent.objectById(targetId) as? OperationUnit ?: return@mapNotNull null
                                source.coordinate to target.coordinate
                            },
                            unitIconSizePx = unitIconSizePx,
                            destroyedUnitDisplayMode = displaySettings.destroyedUnitDisplayMode,
                            fireArcDisplayMode = displaySettings.fireArcDisplayMode.takeIf { operationState.olympiadDawnEnabled } ?: OperationFireArcDisplayMode.None,
                            onUnitIconSizeChanged = editorActions.onUnitIconSizeChanged,
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
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = chromeLayout.toolRail.top.dp),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ViewerChromeButton("GAME") { dispatch(MapViewerCommand.TogglePanel(MapViewerPanel.GAME)) }
                        ViewerChromeButton("NOTES") { dispatch(MapViewerCommand.TogglePanel(MapViewerPanel.NOTES)) }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ViewerChromeButton("GRAPHICS") { dispatch(MapViewerCommand.TogglePanel(MapViewerPanel.GRAPHICS)) }
                        ViewerChromeButton("UNITS") { dispatch(MapViewerCommand.TogglePanel(MapViewerPanel.UNITS)) }
                    }
                    if (operationState.noteReferencePick != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 92.dp),
                            color = CommandColours.Dark.copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, CommandColours.Backing),
                            shape = RectangleShape,
                        ) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("PICK NOTE REFERENCES — CLICK OBJECTS", color = CommandColours.White)
                                CommandButton(onClick = { operationState = operationState.cancelNoteReferencePick() }) { Text("DONE") }
                            }
                        }
                    }
                    contextMenu?.let { menu ->
                        ComposeMapContextMenu(
                            menu = menu,
                            onActionSelected = { item -> item.command?.let(::dispatch) },
                            onDismiss = ::dismissContextMenuAndRestoreMapFocus,
                        )
                    }

                    if (operationState.workspaceMode == WorkspaceMode.FLOATING) {
                        FloatingWorkspace(
                            state = operationState,
                            onStateChanged = { operationState = it },
                        )
                    }
                    ComposeTransientMessageHost(
                        state = transientMessages,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
                }
                if (operationState.workspaceMode == WorkspaceMode.DOCKED) {
                    DockedWorkspace(state = operationState, region = OperationDockRegion.BOTTOM, draggedEditorId = draggedDockTab, dropTarget = dockDropTarget, onDragStarted = { draggedDockTab = it }, onDropTargetChanged = { dockDropTarget = it }, onDrop = { id -> dockDropTarget?.let { target -> operationState = operationState.dockEditor(id, target.first, target.second) }; draggedDockTab = null; dockDropTarget = null }, onStateChanged = { operationState = it })
                }
                if (operationState.workspaceMode == WorkspaceMode.DOCKED) {
                    DockedWorkspace(state = operationState, region = OperationDockRegion.RIGHT, draggedEditorId = draggedDockTab, dropTarget = dockDropTarget, onDragStarted = { draggedDockTab = it }, onDropTargetChanged = { dockDropTarget = it }, onDrop = { id -> dockDropTarget?.let { target -> operationState = operationState.dockEditor(id, target.first, target.second) }; draggedDockTab = null; dockDropTarget = null }, onStateChanged = { operationState = it })
                }
            }
        }
    }
    LaunchedEffect(escapeMenuOpen) {
        if (escapeMenuOpen) modalHost.show(ComposeModalRequest(
            key = "escape-menu",
            onCancelled = { escapeMenuOpen = false },
        ) { dismiss ->
            EscapeMenuDialog(
                onDismiss = { dispatch(MapViewerCommand.CloseEscapeMenu); dismiss() },
                onReturnToLaunchpad = { dispatch(MapViewerCommand.ReturnToLaunchpad); dismiss() },
                onExitProgram = { dispatch(MapViewerCommand.Exit); dismiss() },
            )
        })
    }
    LaunchedEffect(pendingExit, operationState.saveStatus, operationState.saveFailure) {
        if (pendingExit != null && operationState.saveStatus == ComposeSaveStatus.FAILED) {
            modalHost.show(ComposeModalRequest(
                key = "save-failure",
                priority = ComposeModalPriority.LOSS_PREVENTION,
                onCancelled = { pendingExit = null },
            ) { dismiss ->
                SaveFailureDialog(
                    message = operationState.saveFailure.orEmpty(),
                    onRetry = retry@{
                        val target = pendingExit ?: return@retry
                        dismiss()
                        saveNow {
                            pendingExit = null
                            if (target == ComposePendingExit.LAUNCHPAD) onReturnToLaunchpad() else onExitProgram()
                        }
                    },
                    onCancel = { pendingExit = null; dismiss() },
                )
            })
        }
    }
    LaunchedEffect(drawingNameEdit?.objectId) {
        drawingNameEdit?.let { initial ->
            modalHost.show(ComposeModalRequest(
                key = "drawing-name:${initial.objectId.value}",
                onCancelled = { drawingNameEdit = null },
            ) { dismiss ->
                DrawingNameDialog(
                    value = drawingNameEdit?.value.orEmpty(),
                    onValueChanged = { drawingNameEdit = drawingNameEdit?.copy(value = it) },
                    onSave = {
                        drawingNameEdit?.let { edit -> operationState = operationState.renameDrawing(edit.objectId, edit.value) }
                        drawingNameEdit = null
                        dismiss()
                    },
                    onCancel = { drawingNameEdit = null; dismiss() },
                )
            })
        }
    }
    }
    }
}

@Composable
internal fun ViewerChromeButton(label: String, onClick: () -> Unit) = CommandButton(
    onClick = onClick,
    modifier = Modifier.size(120.dp, 40.dp),
) { Text(label) }

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
    showToggle: Boolean = true,
    collapsedAffiliationGroups: Set<String> = emptySet(),
    onCollapsedAffiliationGroupsChanged: (Set<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) = Surface(
    modifier = if (showToggle) modifier.width(if (expanded) 320.dp else 92.dp) else modifier.fillMaxWidth(),
    color = CommandColours.Dark.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.7f)),
    shape = RectangleShape,
) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showToggle) CommandButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "UNIT TYPES ◀" else "UNITS ▶") }
        if (expanded) {
            CommandButton(onClick = onCreate, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("CREATE UNIT TYPE") }
            if (templates.isEmpty()) Text("No unit types", color = CommandColours.White.copy(alpha = 0.7f))
            LazyColumn(Modifier.height(430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (OperationUnitAffiliation.entries.map { it as OperationUnitAffiliation? } + null).forEach { affiliation ->
                    val group = templates.filter { it.affiliation == affiliation }
                    if (group.isNotEmpty()) {
                        val groupKey = affiliation?.name ?: "UNAFFILIATED"
                        item(key = "affiliation:${affiliation?.name ?: "none"}") {
                            CommandButton(
                                onClick = { onCollapsedAffiliationGroupsChanged(if (groupKey in collapsedAffiliationGroups) collapsedAffiliationGroups - groupKey else collapsedAffiliationGroups + groupKey) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("${if (groupKey in collapsedAffiliationGroups) "▶" else "▼"} $groupKey", color = affiliation.composeColour(), fontWeight = FontWeight.Bold) }
                        }
                        if (groupKey !in collapsedAffiliationGroups) items(group, key = { it.id.value }) { template ->
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
                                        CommandButton(onClick = { onDelete(template.id) }, enabled = editable, destructive = true, modifier = Modifier.weight(1f).height(32.dp)) { Text("DELETE") }
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
    showToggle: Boolean = true,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = if (showToggle) modifier.width(if (expanded) 320.dp else 92.dp) else modifier.fillMaxWidth(),
    color = CommandColours.Dark.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.7f)),
    shape = RectangleShape,
) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showToggle) CommandButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "GRAPHICS ◀" else "GRAPHICS ▶") }
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
                                CommandButton({ onDelete(template.id) }, enabled = editable, destructive = true, modifier = Modifier.weight(1f).height(32.dp)) { Text("DELETE") }
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
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("OPERATION NOTES", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        CommandButton(onClick = { onStateChanged(state.openNewNoteEditor()) }, enabled = !state.isReadOnly) { Text("CREATE") }
    }
    if (state.visibleContent.notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No notes") }
    } else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.visibleContent.notes, key = { it.id.value }) { note ->
            val index = state.visibleContent.notes.indexOf(note)
            Surface(color = CommandColours.Dark, border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.55f)), shape = RectangleShape) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(note.title.ifBlank { "Untitled Note" }, color = CommandColours.White, fontWeight = FontWeight.Bold)
                    Text(note.body.lineSequence().firstOrNull().orEmpty(), color = CommandColours.Backing, maxLines = 1)
                    Text("${note.referencedObjectIds.size} reference${if (note.referencedObjectIds.size == 1) "" else "s"}", color = CommandColours.White.copy(alpha = 0.65f))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CommandButton({ onStateChanged(state.openNoteEditor(note.id)) }, modifier = Modifier.weight(1f).height(32.dp)) { Text("OPEN") }
                        CommandButton({
                            val ids = state.visibleContent.notes.map(OperationNote::id).toMutableList()
                            if (index > 0) java.util.Collections.swap(ids, index, index - 1)
                            onStateChanged(state.reorderNotes(ids))
                        }, enabled = !state.isReadOnly && index > 0, modifier = Modifier.height(32.dp)) { Text("↑") }
                        CommandButton({
                            val ids = state.visibleContent.notes.map(OperationNote::id).toMutableList()
                            if (index < ids.lastIndex) java.util.Collections.swap(ids, index, index + 1)
                            onStateChanged(state.reorderNotes(ids))
                        }, enabled = !state.isReadOnly && index < state.visibleContent.notes.lastIndex, modifier = Modifier.height(32.dp)) { Text("↓") }
                        CommandButton({ onStateChanged(state.deleteNote(note.id)) }, enabled = !state.isReadOnly, destructive = true, modifier = Modifier.height(32.dp)) { Text("DELETE") }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DockedWorkspace(
    state: ComposeOperationState,
    region: OperationDockRegion,
    draggedEditorId: OperationEditorId?,
    dropTarget: Pair<OperationDockRegion, Int>?,
    onDragStarted: (OperationEditorId?) -> Unit,
    onDropTargetChanged: (Pair<OperationDockRegion, Int>) -> Unit,
    onDrop: (OperationEditorId) -> Unit,
    onStateChanged: (ComposeOperationState) -> Unit,
) {
    val dock = state.workspace.dockRegions.firstOrNull { it.region == region } ?: return
    var previewSize by remember(dock.size) { mutableStateOf(dock.size) }
    var resizeStart by remember { mutableStateOf(dock.size) }
    var resizeDistance by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val vertical = region != OperationDockRegion.BOTTOM
    val extent = if (dock.collapsed) 48.dp else previewSize.dp
    val resizeModifier = if (vertical) Modifier.width(8.dp).fillMaxHeight() else Modifier.height(8.dp).fillMaxWidth()
    @Composable fun resizeHandle() = Box(
        resizeModifier.background(CommandColours.Dark).pointerInput(region) {
            detectDragGestures(
                onDragStart = { resizeStart = dock.size; resizeDistance = 0f },
                onDragEnd = { onStateChanged(state.updateDockSize(region, previewSize)) },
            ) { change, amount ->
                change.consume()
                resizeDistance += if (region == OperationDockRegion.RIGHT) -amount.x else if (region == OperationDockRegion.LEFT) amount.x else -amount.y
                val value = resizeStart + with(density) { resizeDistance.toDp().value }
                previewSize = value.coerceIn(if (vertical) 240.0 else 180.0, if (vertical) 720.0 else 600.0)
            }
        },
    )
    @Composable fun panel(modifier: Modifier) = DockedWorkspacePanel(
        dock, state, draggedEditorId, dropTarget, onDragStarted, onDropTargetChanged, onDrop, onStateChanged,
        modifier.onPointerEvent(PointerEventType.Enter) { if (draggedEditorId != null) onDropTargetChanged(region to dock.editorIds.size) },
    )
    if (vertical) {
        Row(Modifier.requiredWidth(extent).fillMaxHeight()) {
            if (region == OperationDockRegion.RIGHT) resizeHandle()
            panel(Modifier.weight(1f).fillMaxHeight())
            if (region == OperationDockRegion.LEFT) resizeHandle()
        }
    } else {
        Column(Modifier.fillMaxWidth().height(extent)) {
            resizeHandle()
            panel(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DockedWorkspacePanel(
    dock: io.hsar.mapgenerator.commandgui.operation.OperationDockRegionState,
    state: ComposeOperationState,
    draggedEditorId: OperationEditorId?,
    dropTarget: Pair<OperationDockRegion, Int>?,
    onDragStarted: (OperationEditorId?) -> Unit,
    onDropTargetChanged: (Pair<OperationDockRegion, Int>) -> Unit,
    onDrop: (OperationEditorId) -> Unit,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier,
) = Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
    CommandButton(
        onClick = { onStateChanged(state.toggleDockCollapsed(dock.region)) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (dock.collapsed) "OPEN ${dock.region.name}" else "COLLAPSE") }
    if (!dock.collapsed) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            dock.editorIds.mapNotNull(state.workspace::editor).filter { it.availableFor(state) }.forEachIndexed { index, editor ->
                DockTab(editor, dock, index, state, draggedEditorId, dropTarget, onDragStarted, onDropTargetChanged, onDrop, onStateChanged)
            }
        }
        dock.activeEditorId?.let(state.workspace::editor)?.takeIf { it.availableFor(state) }?.let { editor ->
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

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DockTab(
    editor: OperationEditorState,
    dock: io.hsar.mapgenerator.commandgui.operation.OperationDockRegionState,
    index: Int,
    state: ComposeOperationState,
    draggedEditorId: OperationEditorId?,
    dropTarget: Pair<OperationDockRegion, Int>?,
    onDragStarted: (OperationEditorId?) -> Unit,
    onDropTargetChanged: (Pair<OperationDockRegion, Int>) -> Unit,
    onDrop: (OperationEditorId) -> Unit,
    onStateChanged: (ComposeOperationState) -> Unit,
) {
    var menuOpen by remember(editor.id) { mutableStateOf(false) }
    var dragged by remember(editor.id) { mutableStateOf(false) }
    val focusRequester = remember(editor.id) { FocusRequester() }
    LaunchedEffect(state.workspace.activeEditorId) {
        if (dock.activeEditorId == editor.id && state.workspace.activeEditorId == editor.id) focusRequester.requestFocus()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommandButton(
            onClick = { onStateChanged(state.activateDockTab(dock.region, editor.id)) },
            active = editor.id == dock.activeEditorId,
            modifier = Modifier
                .focusRequester(focusRequester)
                .then(if (dropTarget == Pair(dock.region, index)) Modifier.border(3.dp, CommandColours.Backing) else Modifier)
                .onPointerEvent(PointerEventType.Enter) { if (draggedEditorId != null) onDropTargetChanged(dock.region to index) }
                .pointerInput(editor.id) {
                    detectDragGestures(
                        onDragStart = { dragged = false; onDragStarted(editor.id) },
                        onDragEnd = { if (dragged) onDrop(editor.id) else onDragStarted(null) },
                    ) { change, amount ->
                        if (amount.getDistance() > 0f) dragged = true
                        change.consume()
                    }
                },
        ) { Text(editor.title(state.visibleContent)) }
        CommandButton(onClick = { menuOpen = true }, modifier = Modifier.width(42.dp)) { Text("⋮") }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            OperationDockRegion.entries.filterNot { it == dock.region }.forEach { destination ->
                DropdownMenuItem(
                    text = { Text("Move to ${destination.name.lowercase().replaceFirstChar(Char::uppercase)}") },
                    onClick = { menuOpen = false; onStateChanged(state.dockEditor(editor.id, destination)) },
                )
            }
            DropdownMenuItem(
                text = { Text("Move earlier") },
                enabled = index > 0,
                onClick = {
                    menuOpen = false
                    val ids = dock.editorIds.toMutableList().also { java.util.Collections.swap(it, index, index - 1) }
                    onStateChanged(state.reorderDockTabs(dock.region, ids))
                },
            )
            DropdownMenuItem(
                text = { Text("Move later") },
                enabled = index < dock.editorIds.lastIndex,
                onClick = {
                    menuOpen = false
                    val ids = dock.editorIds.toMutableList().also { java.util.Collections.swap(it, index, index + 1) }
                    onStateChanged(state.reorderDockTabs(dock.region, ids))
                },
            )
        }
    }
}

@Composable
private fun FloatingWorkspace(
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
) = state.workspace.editors.filter { it.availableFor(state) }.forEach { editor ->
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
    val focusRequester = remember(editor.id) { FocusRequester() }
    LaunchedEffect(state.workspace.activeEditorId) {
        if (state.workspace.activeEditorId == editor.id) focusRequester.requestFocus()
    }
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
            .focusRequester(focusRequester)
            .focusable()
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
    is OperationEditorDraft.Game -> GameEditorContent(editor.id, draft, state, onStateChanged, modifier)

    is OperationEditorDraft.Notes -> NotesPanel(state, onStateChanged, modifier)

    is OperationEditorDraft.UnitTypes -> UnitTypesPalette(
        templates = state.visibleContent.unitTemplates,
        expanded = true,
        editable = !state.isReadOnly,
        activePlacement = state.unitPlacement?.templateId,
        onToggle = {},
        onCreate = { onStateChanged(state.openNewUnitTemplateEditor()) },
        onEdit = { onStateChanged(state.openUnitTemplateEditor(it)) },
        onPlace = { onStateChanged(state.armUnitPlacement(it)) },
        onDelete = { id -> if (OperationUnitAuthoring.canDeleteTemplate(state.document.content, id)) onStateChanged(state.deleteUnitTemplate(id)) },
        showToggle = false,
        collapsedAffiliationGroups = draft.collapsedAffiliationGroups,
        onCollapsedAffiliationGroupsChanged = { onStateChanged(state.updateEditorDraft(editor.id, draft.copy(collapsedAffiliationGroups = it))) },
        modifier = modifier,
    )

    OperationEditorDraft.GraphicTypes -> GraphicTypesPalette(
        templates = state.visibleContent.graphicTemplates,
        expanded = true,
        editable = !state.isReadOnly,
        activePlacement = state.graphicPlacement?.templateId,
        onToggle = {},
        onCreate = { onStateChanged(state.openNewGraphicTemplateEditor()) },
        onEdit = { onStateChanged(state.openGraphicTemplateEditor(it)) },
        onPlace = { onStateChanged(state.armGraphicPlacement(it)) },
        onDelete = { id -> if (OperationGraphicAuthoring.canDeleteTemplate(state.document.content, id)) onStateChanged(state.deleteGraphicTemplate(id)) },
        showToggle = false,
        modifier = modifier,
    )

    is OperationEditorDraft.Note -> {
        val target = editor.target as? OperationEditorTarget.Note
        if (target != null && state.visibleContent.notes.none { it.id == target.noteId }) {
            Box(modifier, contentAlignment = Alignment.Center) {
                Text(if (state.isReadOnly) "This note is unavailable at this history point." else "This note no longer exists.")
            }
        } else NoteEditorContent(editor.id, draft.value, state, onStateChanged, modifier)
    }

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

    is OperationEditorDraft.Operational -> OperationalEditorContent(editor, draft, state, onStateChanged, modifier)

    is OperationEditorDraft.Opaque -> Box(modifier, contentAlignment = Alignment.Center) { Text("This restored editor is not supported yet.") }
}

@Composable
private fun GameEditorContent(
    editorId: OperationEditorId,
    draft: OperationEditorDraft.Game,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier,
) {
    val actions = LocalViewerEditorActions.current
    val thumbnailRenderer = remember { MapThumbnailRenderer() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            io.hsar.mapgenerator.commandgui.operation.OperationGameTab.entries.forEach { tab ->
                CommandButton(
                    onClick = { onStateChanged(state.updateEditorDraft(editorId, draft.copy(activeTab = tab))) },
                    active = draft.activeTab == tab,
                    modifier = Modifier.weight(1f),
                ) { Text(tab.name) }
            }
        }
        when (draft.activeTab) {
            io.hsar.mapgenerator.commandgui.operation.OperationGameTab.TURNS -> OperationLedgerPanel(
                state = state,
                onUndo = { actions.onCommand(MapViewerCommand.Undo) },
                onRedo = { actions.onCommand(MapViewerCommand.Redo) },
                onTurnSelected = { onStateChanged(state.revertToTurn(it)) },
                onCurrent = { onStateChanged(state.returnToCurrent()) },
                onAddTurn = { name ->
                    val document = state.document
                    onStateChanged(state.addTurn(name, thumbnailRenderer.renderTurnThumbnail(document.renderData.baseImage, document.renderData.gridLayer, document.content)))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            io.hsar.mapgenerator.commandgui.operation.OperationGameTab.SETTINGS -> GameSettingsContent(state, actions, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun GameSettingsContent(
    state: ComposeOperationState,
    actions: ComposeViewerEditorActions,
    modifier: Modifier,
) = Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("SAVE", fontWeight = FontWeight.Bold)
    CommandButton(actions.onSave, enabled = state.savingRevision == null, modifier = Modifier.fillMaxWidth()) {
        Text(
            when (state.saveStatus) {
                ComposeSaveStatus.CLEAN -> "SAVED"
                ComposeSaveStatus.DIRTY -> "SAVE"
                ComposeSaveStatus.SAVING -> "SAVING…"
                ComposeSaveStatus.FAILED -> "SAVE FAILED"
            },
        )
    }
    Text("WORKSPACE MODE", fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkspaceMode.entries.forEach { mode ->
            CommandButton({ actions.onCommand(MapViewerCommand.SetWorkspaceMode(mode)) }, active = state.workspaceMode == mode, modifier = Modifier.weight(1f)) { Text(mode.name) }
        }
    }
    Text("DESTROYED UNITS", fontWeight = FontWeight.Bold)
    OperationDestroyedUnitDisplayMode.entries.forEach { mode ->
        CommandButton({ actions.onDestroyedUnitDisplayModeChanged(mode) }, active = actions.displaySettings.destroyedUnitDisplayMode == mode, modifier = Modifier.fillMaxWidth()) { Text(mode.label.uppercase()) }
    }
    Text("UNIT ICON SIZE", fontWeight = FontWeight.Bold)
    var iconSizeText by remember(actions.displaySettings.unitIconSizePx) { mutableStateOf(UnitIconDisplaySize.format(actions.displaySettings.unitIconSizePx)) }
    OutlinedTextField(
        value = iconSizeText,
        onValueChange = { value -> iconSizeText = value; UnitIconDisplaySize.parse(value)?.let(actions.onUnitIconSizeChanged) },
        suffix = { Text("px") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().trackEditableTextFocus(),
    )
    if (state.olympiadDawnEnabled) {
        Text("FIRE ARCS", fontWeight = FontWeight.Bold)
        OperationFireArcDisplayMode.entries.forEach { mode ->
            CommandButton({ actions.onFireArcDisplayModeChanged(mode) }, active = actions.displaySettings.fireArcDisplayMode == mode, modifier = Modifier.fillMaxWidth()) { Text(mode.label.uppercase()) }
        }
    }
}

private fun OperationEditorState.availableFor(state: ComposeOperationState) =
    draft !is OperationEditorDraft.Operational || state.olympiadDawnEnabled

@Composable
private fun OperationalEditorContent(
    editor: OperationEditorState,
    draft: OperationEditorDraft.Operational,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unit = (editor.target as? OperationEditorTarget.Object)?.objectId?.let(state.visibleContent::objectById) as? OperationUnit
    when {
        !state.olympiadDawnEnabled -> Box(modifier, contentAlignment = Alignment.Center) { Text("Olympiad Dawn workflows are disabled for this operation.") }
        unit == null -> Box(modifier, contentAlignment = Alignment.Center) { Text("This unit is unavailable.") }
        editor.kind == OperationEditorKind.OLYMPIAD_DAWN_STATUS -> OlympiadDawnStatusContent(unit, state, onStateChanged, modifier)
        editor.kind == OperationEditorKind.TACTICAL -> TacticalEditorContent(editor.id, unit, draft, state, onStateChanged, modifier)
        else -> ReportEditorContent(draft.reportText, modifier)
    }
}

@Composable
private fun ReportEditorContent(text: String, modifier: Modifier) {
    val clipboard = LocalClipboardManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectionContainer { Text(text.ifBlank { "No report entries." }, Modifier.weight(1f).verticalScroll(rememberScrollState())) }
        CommandButton({ clipboard.setText(AnnotatedString(text)) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("COPY REPORT") }
    }
}

@Composable
private fun OlympiadDawnStatusContent(unit: OperationUnit, state: ComposeOperationState, onStateChanged: (ComposeOperationState) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OperationWorkflowFacade.statusRows(unit, state.visibleContent).forEachIndexed { index, row ->
            Text(row, style = if (index == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = if (row in setOf("SYSTEMS", "WEAPONS") || row.startsWith("CLASS ")) CommandColours.Backing else Color.Unspecified)
        }
        Text("DAMAGE", color = CommandColours.Backing)
        OperationWorkflowFacade.damageOptions(unit, state.visibleContent).forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(option.damaged, { onStateChanged(state.toggleOperationalDamage(unit.id, option.token)) }, enabled = !state.isReadOnly)
                Text(option.label)
            }
        }
    }
}

@Composable
private fun TacticalEditorContent(
    editorId: OperationEditorId,
    unit: OperationUnit,
    draft: OperationEditorDraft.Operational,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier,
) {
    val report = state.tacticalReport(unit.id)
    fun update(value: OperationEditorDraft.Operational) = onStateChanged(state.updateEditorDraft(editorId, value))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("FIRE CONTROL ${report?.weaponsUsed ?: 0}/${report?.fireControlCapacity ?: 0}", color = CommandColours.Backing)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            report?.targets?.forEach { target ->
                CommandButton({ update(draft.copy(selectedTargetId = target.id, selectedWeaponKeys = emptySet())) }, active = draft.selectedTargetId == target.id, modifier = Modifier.fillMaxWidth()) {
                    Text("${target.name} — ${"%.1f".format(target.rangeUnits)}U — FP ${target.firepower}")
                }
                if (draft.selectedTargetId == target.id) target.contributions.forEach { contribution ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(contribution.weaponKey in draft.selectedWeaponKeys, { checked ->
                            update(draft.copy(selectedWeaponKeys = if (checked) draft.selectedWeaponKeys + contribution.weaponKey else draft.selectedWeaponKeys - contribution.weaponKey))
                        }, enabled = !state.isReadOnly)
                        Text("${contribution.weaponName} [${contribution.arc}] — ${contribution.firepower}")
                    }
                }
            }
            if (report?.targets.isNullOrEmpty()) Text("No valid tactical targets.")
        }
        CommandButton({ onStateChanged(state.fireTactical(editorId)) }, enabled = !state.isReadOnly && draft.selectedTargetId != null && draft.selectedWeaponKeys.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("FIRE") }
    }
}

@Composable
private fun NoteEditorContent(
    editorId: OperationEditorId,
    draft: OperationNoteDraft,
    state: ComposeOperationState,
    onStateChanged: (ComposeOperationState) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun update(value: OperationNoteDraft) = onStateChanged(state.updateEditorDraft(editorId, OperationEditorDraft.Note(value)))
    val picking = state.noteReferencePick?.editorId == editorId
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            draft.title,
            { update(draft.copy(title = it)) },
            Modifier.fillMaxWidth().trackEditableTextFocus(),
            enabled = !state.isReadOnly,
            singleLine = true,
            label = { Text("Title") },
        )
        OutlinedTextField(
            draft.body,
            { update(draft.copy(body = it)) },
            Modifier.fillMaxWidth().weight(1f).trackEditableTextFocus(),
            enabled = !state.isReadOnly,
            minLines = 8,
            label = { Text("Body") },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("REFERENCES (${draft.referencedObjectIds.size})", Modifier.weight(1f), color = CommandColours.Backing)
            CommandButton(
                onClick = { onStateChanged(if (picking) state.cancelNoteReferencePick() else state.armNoteReferencePick(editorId)) },
                active = picking,
                enabled = !state.isReadOnly,
            ) { Text(if (picking) "DONE PICKING" else "PICK REFERENCES") }
        }
        Column(Modifier.fillMaxWidth().height(150.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (draft.referencedObjectIds.isEmpty()) Text("No referenced map objects", color = CommandColours.White.copy(alpha = 0.65f))
            draft.referencedObjectIds.forEachIndexed { index, objectId ->
                val value = state.visibleContent.objectById(objectId)
                Surface(color = CommandColours.Dark, border = BorderStroke(1.dp, CommandColours.White.copy(alpha = 0.45f)), shape = RectangleShape) {
                    Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(value?.let { "${it.objectTypeLabel()}: ${it.displayName(state.visibleContent)}" } ?: "Missing object", Modifier.weight(1f))
                        CommandButton({
                            val ids = draft.referencedObjectIds.toMutableList()
                            if (index > 0) java.util.Collections.swap(ids, index, index - 1)
                            update(draft.copy(referencedObjectIds = ids))
                        }, enabled = !state.isReadOnly && index > 0) { Text("↑") }
                        CommandButton({
                            val ids = draft.referencedObjectIds.toMutableList()
                            if (index < ids.lastIndex) java.util.Collections.swap(ids, index, index + 1)
                            update(draft.copy(referencedObjectIds = ids))
                        }, enabled = !state.isReadOnly && index < draft.referencedObjectIds.lastIndex) { Text("↓") }
                        CommandButton({ update(draft.copy(referencedObjectIds = draft.referencedObjectIds.filterNot { it == objectId })) }, enabled = !state.isReadOnly) { Text("×") }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommandButton({ onStateChanged(state.saveNoteEditor(editorId)) }, enabled = !state.isReadOnly, modifier = Modifier.weight(1f)) { Text("SAVE") }
            if (state.document.content.notes.any { it.id == draft.id }) {
                CommandButton({ onStateChanged(state.deleteNote(draft.id)) }, enabled = !state.isReadOnly, destructive = true, modifier = Modifier.weight(1f)) { Text("DELETE") }
            }
            CommandButton({ onStateChanged(state.closeEditor(editorId)) }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
    }
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
    val validation = state.workspace.editor(editorId)?.let(state::validationFor) ?: ComposeEditorValidation(emptyMap())
    val selectedTemplate = draft.unitTemplateId?.let(state.document.content::unitTemplate)
        ?: state.document.content.unitTemplate(unit.unitTemplateId)
    val valid = validation.valid && selectedTemplate != null
    fun update(value: OperationEditorDraft.Unit) = onStateChanged(state.updateEditorDraft(editorId, value))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.visibleContent.unitTemplate(unit.unitTemplateId)?.name ?: "Unit", color = CommandColours.Backing)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth().trackEditableTextFocus(), enabled = !state.isReadOnly, singleLine = true, label = { Text("Name") })
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
                    Modifier.weight(1f).trackEditableTextFocus(),
                    enabled = !state.isReadOnly,
                    singleLine = true,
                    isError = "position" in validation.errors,
                    label = { Text(if (state.document.renderData.gridLayer == null) "Map X" else "Easting") })
                OutlinedTextField(
                    draft.positionY,
                    { update(draft.copy(positionY = it)) },
                    Modifier.weight(1f).trackEditableTextFocus(),
                    enabled = !state.isReadOnly,
                    singleLine = true,
                    isError = "position" in validation.errors,
                    label = { Text(if (state.document.renderData.gridLayer == null) "Map Y" else "Northing") })
            }
            OutlinedTextField(
                draft.facing,
                { update(draft.copy(facing = it)) },
                Modifier.fillMaxWidth().trackEditableTextFocus(),
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
    val filePicker = LocalComposeFilePicker.current
    val scope = rememberCoroutineScope()
    var imageError by remember(editor.id) { mutableStateOf<String?>(null) }
    val existing = (editor.target as? OperationEditorTarget.UnitTemplate)?.templateId?.let(state.document.content::unitTemplate)
    val decoded = remember(draft.replacementImage) {
        draft.replacementImage?.let { runCatching { MapViewerImageLoader.load(it.bytes(), it.originalFilename) }.getOrNull() }
    }
    val validation = state.validationFor(editor)
    fun update(value: OperationUnitTemplateDraft) = onStateChanged(
        state.updateEditorDraft(editor.id, OperationEditorDraft.UnitTemplate(value)),
    )
    fun browse() {
        scope.launch {
            when (val selected = filePicker.chooseAndDecode(
                title = "Choose unit image",
                pattern = "*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp;*.svg",
                unsupportedMessage = "Unsupported image",
                decode = { file ->
                    val imageDraft = OperationImageDraft.fromBytes(file.name, file.readBytes())
                    imageDraft.takeIf { MapViewerImageLoader.load(it.bytes(), it.originalFilename) != null }
                },
            )) {
                ComposeFileSelection.Cancelled -> Unit
                is ComposeFileSelection.Selected -> { imageError = null; update(draft.copy(replacementImage = selected.value)) }
                is ComposeFileSelection.Unreadable -> imageError = selected.message.ifBlank { "Could not load the selected image" }
                is ComposeFileSelection.Unsupported -> imageError = selected.message
            }
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
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth().trackEditableTextFocus(), enabled = !state.isReadOnly, isError = "name" in validation.errors, singleLine = true, label = { Text("Name") })
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
    val validation = state.workspace.editor(editorId)?.let(state::validationFor) ?: ComposeEditorValidation(emptyMap())
    val selectedTemplate = draft.graphicTemplateId?.let(state.document.content::graphicTemplate)
    fun update(value: io.hsar.mapgenerator.commandgui.operation.OperationGraphicDraft) =
        onStateChanged(state.updateEditorDraft(editorId, OperationEditorDraft.Graphic(value)))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            selectedTemplate?.let { template ->
                androidx.compose.foundation.Image(template.image.toComposeImageBitmap(), null, Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Fit)
            }
            OutlinedTextField(draft.displayName, { update(draft.copy(displayName = it)) }, Modifier.fillMaxWidth().trackEditableTextFocus(), enabled = !state.isReadOnly, singleLine = true, label = { Text("Display label (optional)") })
            Text("Graphic type", color = CommandColours.Backing)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.document.content.graphicTemplates.sortedBy(OperationGraphicTemplate::index).forEach { template ->
                    CommandButton({ update(draft.copy(graphicTemplateId = template.id)) }, active = selectedTemplate?.id == template.id, enabled = !state.isReadOnly) { Text(template.name) }
                }
            }
            validation.errors["template"]?.let { Text(it, color = Color(0xFFDF4A4A)) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(draft.positionX, { update(draft.copy(positionX = it)) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = !state.isReadOnly, singleLine = true, isError = "position" in validation.errors, label = { Text(if (state.document.renderData.gridLayer == null) "Map X" else "Easting") })
                OutlinedTextField(draft.positionY, { update(draft.copy(positionY = it)) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = !state.isReadOnly, singleLine = true, isError = "position" in validation.errors, label = { Text(if (state.document.renderData.gridLayer == null) "Map Y" else "Northing") })
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
    val filePicker = LocalComposeFilePicker.current
    val scope = rememberCoroutineScope()
    var imageError by remember(editor.id) { mutableStateOf<String?>(null) }
    val existing = (editor.target as? OperationEditorTarget.GraphicTemplate)?.templateId?.let(state.document.content::graphicTemplate)
    val decoded = remember(draft.replacementImage) { draft.replacementImage?.let { runCatching { MapViewerImageLoader.load(it.bytes(), it.originalFilename) }.getOrNull() } }
    val validation = state.validationFor(editor)
    fun update(value: OperationGraphicTemplateDraft) = onStateChanged(state.updateEditorDraft(editor.id, OperationEditorDraft.GraphicTemplate(value)))
    fun browse() {
        scope.launch {
            when (val selected = filePicker.chooseAndDecode(
                title = "Choose graphic image",
                pattern = "*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp;*.svg",
                unsupportedMessage = "Unsupported image",
                decode = { file ->
                    val imageDraft = OperationImageDraft.fromBytes(file.name, file.readBytes())
                    MapViewerImageLoader.load(imageDraft.bytes(), imageDraft.originalFilename)?.let { image -> imageDraft to image }
                },
            )) {
                ComposeFileSelection.Cancelled -> Unit
                is ComposeFileSelection.Selected -> {
                val (imageDraft, image) = selected.value
                imageError = null
                val height = draft.heightPercent.takeIf(String::isNotBlank)
                    ?: OperationGraphicAuthoring.defaultHeightPercent(
                        image, state.document.renderData.baseImage.width, state.document.renderData.baseImage.height,
                        draft.widthPercent.toDoubleOrNull() ?: io.hsar.mapgenerator.commandgui.operation.DEFAULT_GRAPHIC_WIDTH_PERCENT,
                    ).toString()
                update(draft.copy(
                    name = draft.name.ifBlank { selected.file.nameWithoutExtension },
                    replacementImage = imageDraft,
                    heightPercent = height,
                ))
                }
                is ComposeFileSelection.Unreadable -> imageError = selected.message.ifBlank { "Could not load the selected image" }
                is ComposeFileSelection.Unsupported -> imageError = selected.message
            }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val preview = decoded ?: existing?.image
            preview?.let { androidx.compose.foundation.Image(it.toComposeImageBitmap(), null, Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit) }
            CommandButton(::browse, enabled = !state.isReadOnly, modifier = Modifier.fillMaxWidth()) { Text(if (preview == null) "CHOOSE IMAGE" else "REPLACE IMAGE") }
            (imageError ?: validation.errors["image"])?.let { Text(it, color = Color(0xFFDF4A4A)) }
            OutlinedTextField(draft.name, { update(draft.copy(name = it)) }, Modifier.fillMaxWidth().trackEditableTextFocus(), enabled = !state.isReadOnly, isError = "name" in validation.errors, singleLine = true, label = { Text("Name") })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(draft.widthPercent, { update(draft.copy(widthPercent = it)) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = !state.isReadOnly, isError = "width" in validation.errors, singleLine = true, label = { Text("Map width %") })
                OutlinedTextField(draft.heightPercent, { update(draft.copy(heightPercent = it)) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = !state.isReadOnly, isError = "height" in validation.errors, singleLine = true, label = { Text("Map height %") })
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
            OutlinedTextField(property.key, { key -> onChanged(properties.mapIndexed { row, value -> if (row == index) value.copy(key = key) else value }) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = enabled, singleLine = true, isError = "property:$index" in errors, supportingText = errors["property:$index"]?.let { message -> { Text(message) } }, label = { Text("Key") })
            OutlinedTextField(property.value, { value -> onChanged(properties.mapIndexed { row, current -> if (row == index) current.copy(value = value) else current }) }, Modifier.weight(1f).trackEditableTextFocus(), enabled = enabled, singleLine = true, label = { Text("Value") })
            CommandButton(onClick = { onChanged(properties.filterIndexed { row, _ -> row != index }) }, enabled = enabled) { Text("×") }
        }
    }
    CommandButton(onClick = { onChanged(properties + OperationPropertyDraft()) }, enabled = enabled) { Text("ADD PROPERTY") }
}

private fun OperationEditorState.title(content: OperationContent): String = when (kind) {
    OperationEditorKind.GAME -> "Game"
    OperationEditorKind.OPERATION_NOTES -> "Notes"
    OperationEditorKind.UNIT_TYPES -> "Units"
    OperationEditorKind.GRAPHIC_TYPES -> "Graphics"
    OperationEditorKind.NOTE -> when (val value = target) {
        is OperationEditorTarget.Note -> content.notes.firstOrNull { it.id == value.noteId }?.title ?: "Edit Note"
        else -> "New Note"
    }
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
    OperationEditorKind.OLYMPIAD_DAWN_STATUS -> "Olympiad Dawn Status"
    OperationEditorKind.SENSOR_REPORT -> (draft as? OperationEditorDraft.Operational)?.mode?.uppercase() ?: "Sensor Report"
    OperationEditorKind.TACTICAL -> "TACTICAL"
    OperationEditorKind.GUNREP -> "GUNREP"
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
) = CommandPanel(Modifier.width(420.dp)) {
        Text("MENU", style = MaterialTheme.typography.headlineMedium)
        Text("Return to the launchpad after saving.")
        CommandButton(onClick = onReturnToLaunchpad, modifier = Modifier.fillMaxWidth()) {
            Text("RETURN TO LAUNCHPAD")
        }
        CommandButton(onClick = onExitProgram, modifier = Modifier.fillMaxWidth()) {
            Text("EXIT")
        }
        DefaultCommandButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("CLOSE")
        }
    }

@Composable
private fun SaveFailureDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) = CommandPanel(Modifier.width(460.dp)) {
        Text("SAVE FAILED", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Text(message.ifBlank { "The operation could not be saved." })
        Text("The viewer will remain open so no changes are discarded.")
        DefaultCommandButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("RETRY") }
        CommandButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("CANCEL EXIT") }
}

@Composable
private fun DrawingNameDialog(
    value: String,
    onValueChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) = CommandPanel(Modifier.width(420.dp)) {
        Text("EDIT DRAWING NAME", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth().trackEditableTextFocus(),
            singleLine = true,
            label = { Text("Display name (optional)") },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultCommandButton(onClick = onSave, modifier = Modifier.weight(1f)) { Text("SAVE") }
            CommandButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("CANCEL") }
        }
}

private enum class ComposePendingExit { LAUNCHPAD, DESKTOP }

private data class ComposeMapContextMenu(
    val objectId: OperationObjectId,
    val objectName: String,
    val position: IntOffset,
    val actions: List<MapViewerCommandItem>,
)

private data class ComposeDrawingNameEdit(val objectId: OperationObjectId, val value: String)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeMapContextMenu(
    menu: ComposeMapContextMenu,
    onActionSelected: (MapViewerCommandItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var focusedIndex by remember(menu) { mutableStateOf(0) }
    var submenuIndex by remember(menu) { mutableStateOf<Int?>(null) }
    var submenuFocusedIndex by remember(menu) { mutableStateOf(-1) }
    var submenuCloseRequested by remember(menu) { mutableStateOf(false) }
    val focusRequester = remember(menu) { FocusRequester() }
    LaunchedEffect(menu) { focusRequester.requestFocus() }
    LaunchedEffect(submenuCloseRequested) {
        if (submenuCloseRequested) {
            kotlinx.coroutines.delay(MapViewerMenuTokens.CLOSE_DELAY_MILLIS)
            if (submenuCloseRequested) submenuIndex = null
        }
    }

    fun activate(item: MapViewerCommandItem) = when {
        !item.enabled -> Unit
        item.children.isNotEmpty() -> submenuIndex = menu.actions.indexOf(item)
        item.command != null -> onActionSelected(item)
        else -> Unit
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> { submenuIndex?.let { parent -> submenuFocusedIndex = (submenuFocusedIndex + 1).coerceAtMost(menu.actions[parent].children.lastIndex) } ?: run { focusedIndex = (focusedIndex + 1).coerceAtMost(menu.actions.lastIndex) }; true }
                    Key.DirectionUp -> { if (submenuIndex != null) submenuFocusedIndex = (submenuFocusedIndex - 1).coerceAtLeast(0) else focusedIndex = (focusedIndex - 1).coerceAtLeast(0); true }
                    Key.MoveHome -> { if (submenuIndex != null) submenuFocusedIndex = 0 else focusedIndex = 0; true }
                    Key.MoveEnd -> { submenuIndex?.let { submenuFocusedIndex = menu.actions[it].children.lastIndex } ?: run { focusedIndex = menu.actions.lastIndex.coerceAtLeast(0) }; true }
                    Key.DirectionRight -> { if (menu.actions.getOrNull(focusedIndex)?.children?.isNotEmpty() == true) { submenuIndex = focusedIndex; submenuFocusedIndex = 0 }; true }
                    Key.DirectionLeft -> { submenuIndex = null; submenuFocusedIndex = -1; true }
                    Key.Enter, Key.Spacebar -> { submenuIndex?.let { menu.actions[it].children.getOrNull(submenuFocusedIndex) }?.let(::activate) ?: menu.actions.getOrNull(focusedIndex)?.let(::activate); true }
                    Key.Escape -> { if (submenuIndex != null) { submenuIndex = null; submenuFocusedIndex = -1 } else onDismiss(); true }
                    else -> false
                }
            },
    ) {
        val density = LocalDensity.current
        val viewportWidth = maxWidth.value.toDouble()
        val viewportHeight = maxHeight.value.toDouble()
        val menuWidth = CommandDesignTokens.menuWidth.value.toDouble()
        val rootHeight = ((menu.actions.size + 1) * MapViewerMenuTokens.ROW_HEIGHT) + MapViewerMenuTokens.PADDING * 2
        val rootBounds = MapViewerMenuLayoutBuilder.root(menu.position.x / density.density.toDouble(), menu.position.y / density.density.toDouble(), menuWidth, rootHeight, viewportWidth, viewportHeight)
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        ComposeCommandMenuPanel(
            title = menu.objectName,
            items = menu.actions,
            focusedIndex = focusedIndex,
            onHovered = { index ->
                focusedIndex = index
                if (menu.actions[index].children.isNotEmpty()) { submenuCloseRequested = false; submenuIndex = index }
                else if (submenuIndex != null) submenuCloseRequested = true
            },
            onSelected = ::activate,
            modifier = Modifier.offset { IntOffset((rootBounds.left * density.density).roundToInt(), (rootBounds.top * density.density).roundToInt()) },
        )
        submenuIndex?.let { index ->
            val children = menu.actions.getOrNull(index)?.children.orEmpty()
            if (children.isNotEmpty()) {
                val childHeight = children.size * MapViewerMenuTokens.ROW_HEIGHT + MapViewerMenuTokens.PADDING * 2
                val childBounds = MapViewerMenuLayoutBuilder.child(rootBounds, index + 1, menuWidth, childHeight, viewportWidth, viewportHeight)
                val corridorLeft = if (childBounds.left >= rootBounds.right) rootBounds.right else childBounds.right
                Box(
                    Modifier.offset { IntOffset((corridorLeft * density.density).roundToInt(), (childBounds.top * density.density).roundToInt()) }
                        .width(MapViewerMenuTokens.SUBMENU_CORRIDOR.dp)
                        .height(44.dp)
                        .onPointerEvent(PointerEventType.Enter) { submenuCloseRequested = false }
                        .clickable(onClick = {}),
                )
                ComposeCommandMenuPanel(
                title = null,
                items = children,
                focusedIndex = submenuFocusedIndex,
                onHovered = { childIndex -> submenuCloseRequested = false; submenuFocusedIndex = childIndex },
                onSelected = ::activate,
                modifier = Modifier.offset { IntOffset((childBounds.left * density.density).roundToInt(), (childBounds.top * density.density).roundToInt()) }
                    .onPointerEvent(PointerEventType.Enter) { submenuCloseRequested = false },
            )
            }
        }
}
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeCommandMenuPanel(
    title: String?,
    items: List<MapViewerCommandItem>,
    focusedIndex: Int,
    onHovered: (Int) -> Unit,
    onSelected: (MapViewerCommandItem) -> Unit,
    modifier: Modifier,
) = Surface(modifier.widthIn(min = MapViewerMenuTokens.MIN_WIDTH.dp).width(CommandDesignTokens.menuWidth).clickable(onClick = {}), color = CommandColours.Dark, border = BorderStroke(MapViewerMenuTokens.BORDER_WIDTH.dp, CommandColours.White), shape = RectangleShape) {
    Column(Modifier.padding(4.dp)) {
        title?.let { Text(it, Modifier.height(MapViewerMenuTokens.ROW_HEIGHT.dp).padding(horizontal = CommandDesignTokens.menuRowInset), style = MaterialTheme.typography.titleMedium, color = CommandColours.White) }
        items.forEachIndexed { index, item ->
            Row(
                Modifier.fillMaxWidth().height(MapViewerMenuTokens.ROW_HEIGHT.dp)
                    .background(if (focusedIndex == index) CommandColours.Backing else Color.Transparent)
                    .onPointerEvent(PointerEventType.Enter) { onHovered(index) }
                    .clickable { if (item.enabled) onSelected(item) }
                    .padding(horizontal = CommandDesignTokens.menuRowInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.label.uppercase(),
                        color = when {
                        !item.enabled -> CommandColours.White.copy(alpha = 0.38f)
                        item.destructive -> CommandColours.Red
                        else -> CommandColours.White
                        },
                    )
                    item.disabledReason?.let { Text(it, color = CommandColours.White.copy(alpha = 0.38f), fontSize = 9.sp, maxLines = 1) }
                }
                if (item.children.isNotEmpty()) Text("▶", color = CommandColours.White)
            }
        }
    }
}

private fun OperationObject.displayName(content: OperationContent): String = when (this) {
    is OperationUnit -> instanceName.ifBlank { content.unitTemplate(unitTemplateId)?.name ?: "Unit" }
    is OperationGraphic -> customProperties["displayName"]?.takeIf(String::isNotBlank)
        ?: instanceName.ifBlank { content.graphicTemplate(graphicTemplateId)?.name ?: "Graphic" }

    is OperationDrawingObject -> customProperties[OPERATION_DISPLAY_NAME_PROPERTY]?.trim()?.takeIf(String::isNotEmpty) ?: drawing.displayName
}

private fun OperationObject.objectTypeLabel() = when (this) {
    is OperationUnit -> "Unit"
    is OperationGraphic -> "Graphic"
    is OperationDrawingObject -> "Drawing"
}

@Composable
internal fun OperationLedgerPanel(
    state: ComposeOperationState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTurnSelected: (Int) -> Unit,
    onCurrent: () -> Unit,
    onAddTurn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeline = state.document.timeline
    val groups = timeline.turnGroups()
    var turnName by remember(state.document.operationId) { mutableStateOf(timeline.nextTurnName()) }
    LaunchedEffect(timeline.entries.size) {
        if (turnName.isBlank()) turnName = timeline.nextTurnName()
    }
    Column(
        modifier.fillMaxWidth().background(CommandColours.Dark),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CommandButton(onUndo, enabled = timeline.canUndo && !state.isReadOnly, modifier = Modifier.weight(1f)) { Text("UNDO") }
            CommandButton(onRedo, enabled = timeline.canRedo && !state.isReadOnly, modifier = Modifier.weight(1f)) { Text("REDO") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommandButton(
                onClick = {
                    onAddTurn(turnName)
                    turnName = ""
                },
                enabled = !state.isReadOnly,
            ) { Text("ADD TURN") }
            Box(
                Modifier.weight(1f).height(40.dp)
                    .background(CommandColours.Dark)
                    .border(2.dp, CommandColours.White),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = turnName,
                    onValueChange = { turnName = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).trackEditableTextFocus(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelLarge.copy(color = CommandColours.White),
                    cursorBrush = SolidColor(CommandColours.White),
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (groups.isEmpty()) item {
                Text("NO TURNS", Modifier.fillMaxWidth().padding(12.dp), color = CommandColours.Light)
            }
            items(groups, key = { group -> group.marker?.entryIndex?.let { "turn:$it" } ?: "unmarked" }) { group ->
                OperationTurnButton(
                    state = state,
                    group = group,
                    onSelect = group.marker?.let { marker -> { onTurnSelected(marker.entryIndex) } },
                )
            }
        }
        CurrentTurnButton(
            active = state.historyPreviewCount == null && timeline.appliedCount == timeline.entries.size,
            onClick = onCurrent,
        )
    }
}

@Composable
private fun OperationTurnButton(
    state: ComposeOperationState,
    group: OperationTurnGroup,
    onSelect: (() -> Unit)?,
) {
    val marker = group.marker
    val thumbnail = marker?.thumbnailAssetId
        ?.let { id -> state.document.generatedImageAssets.firstOrNull { it.id == id } }
        ?.image
    val active = marker?.appliedCount == state.document.timeline.appliedCount &&
        (state.historyPreviewCount == null || state.historyPreviewCount == marker.appliedCount)
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(enabled = onSelect != null) { onSelect?.invoke() },
        color = if (active) CommandColours.Dark else CommandColours.Main,
        border = BorderStroke(2.dp, CommandColours.White),
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = remember(thumbnail) { thumbnail.toImageBitmap() },
                    contentDescription = "${group.name} thumbnail",
                    modifier = Modifier.width(80.dp).fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.width(80.dp).fillMaxHeight().background(CommandColours.Dark).border(1.dp, CommandColours.Backing),
                    contentAlignment = Alignment.Center,
                ) { Text("NO IMAGE", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            }
            Text(
                group.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                marker?.appliedCount?.let(::operationTurnIndexLabel) ?: "----",
                modifier = Modifier.width(48.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (active) CommandColours.White else CommandColours.Light,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CurrentTurnButton(active: Boolean, onClick: () -> Unit) = CommandButton(
    onClick = onClick,
    active = active,
    modifier = Modifier.fillMaxWidth().height(52.dp),
) {
    Box(Modifier.fillMaxWidth()) {
        Text("CURRENT", modifier = Modifier.align(Alignment.Center))
        Text("Current", modifier = Modifier.align(Alignment.CenterEnd), color = CommandColours.Light, fontSize = 12.sp)
    }
}

internal fun operationTurnIndexLabel(appliedCount: Int): String = "%04X".format(appliedCount)

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
    noteReferencePickActive: Boolean,
    noteReferenceHoveredObjectId: OperationObjectId?,
    onNoteReferencePreview: (OperationObjectId?) -> Unit,
    onNoteReferencePicked: (OperationObjectId) -> Unit,
    onNoteReferenceCancelled: () -> Unit,
    cancellationEpoch: Int,
    ecmFields: List<OperationWorkflowEcmField>,
    unitsPerPixel: Double,
    tacticalOverlayLines: List<Pair<MapCoordinate, MapCoordinate>>,
    unitIconSizePx: Double,
    destroyedUnitDisplayMode: OperationDestroyedUnitDisplayMode,
    fireArcDisplayMode: OperationFireArcDisplayMode,
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
    var toolSession by remember { mutableStateOf<ComposeDrawMeasureSession?>(null) }
    var activeReferenceCoordinate by remember { mutableStateOf<MapCoordinate?>(null) }
    LaunchedEffect(
        activeTool, drawingMode, editable, renderData.gridLayer, cancellationEpoch,
        unitPlacementActive, graphicPlacementActive, noteReferencePickActive,
    ) {
        transformGesture = null
        marquee = null
        toolSession = null
        activeReferenceCoordinate = null
    }
    val unitIcons = remember(content.unitTemplates) { content.unitTemplates.associate { it.id to it.image.toImageBitmap() } }
    val graphicIcons = remember(content.graphicTemplates) { content.graphicTemplates.associate { it.id to it.image.toImageBitmap() } }
    val gridOverlay = remember(
        renderData.gridLayer, currentViewport, nordFontFamily, toolSession?.measurementOrigin, toolSession?.measurement, activeReferenceCoordinate,
    ) {
        renderData.gridLayer?.let { grid ->
            val referenceCoordinate = toolSession?.measurement?.destination ?: toolSession?.measurementOrigin ?: activeReferenceCoordinate
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
                    measurement = toolSession?.measurement,
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

    val pointerViewport by rememberUpdatedState(currentViewport)

    fun screenToMap(point: Offset, projectionViewport: MapViewport = currentViewport) = MapCoordinate(
        x = (projectionViewport.left + point.x / projectionViewport.zoom - mapOffsetX) / mapWidth,
        y = (projectionViewport.top + point.y / projectionViewport.zoom - mapOffsetY) / mapHeight,
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

    val interactionContent = if (destroyedUnitDisplayMode == OperationDestroyedUnitDisplayMode.Hide)
        content.copy(objects = content.objects.filter { it !is OperationUnit || OperationDestroyedUnitDisplayPolicy.visible(it, destroyedUnitDisplayMode) })
    else content

    fun objectAtScreen(point: Offset): OperationObject? = interactionContent.objectAtScreen(
        ViewerPoint(point.x.toDouble(), point.y.toDouble()), selectionProjection,
        10.0 / (currentViewport.zoom * minOf(mapWidth, mapHeight)),
    )

    fun selectionFrame() = interactionContent.selectionFrame(selectedObjectIds, selectionProjection)

    fun containsNonMovableDestroyed(ids: Set<OperationObjectId>) =
        !OperationDestroyedUnitDisplayPolicy.movable(interactionContent.objects, ids, destroyedUnitDisplayMode)

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
            .pointerInput(
                activeTool, drawingMode, drawingColour, editable, renderData.gridLayer, cancellationEpoch,
                unitPlacementActive, graphicPlacementActive, noteReferencePickActive,
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    if (unitPlacementActive || graphicPlacementActive || noteReferencePickActive) return@awaitEachGesture
                    val started = when {
                        activeTool == MapViewerTool.MEASURE -> ComposeDrawMeasureSession.startMeasurement(
                            screenToMap(down.position, pointerViewport), renderData.gridLayer,
                        )
                        editable && activeTool == MapViewerTool.DRAW -> ComposeDrawMeasureSession.startDrawing(
                            drawingMode, drawingColour.argb, screenToMap(down.position, pointerViewport), viewerBackgroundBounds(mapWidth, mapHeight),
                        )
                        else -> null
                    } ?: return@awaitEachGesture
                    toolSession = started
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed || event.buttons.isSecondaryPressed) {
                                if (!event.buttons.isSecondaryPressed) {
                                    (toolSession?.release() as? ComposeDrawMeasureOutcome.DrawingCommitted)?.drawing?.let(onDrawingCommitted)
                                } else {
                                    toolSession?.cancel()
                                }
                                toolSession = null
                                change.consume()
                                break
                            }
                            toolSession = toolSession?.update(
                                screenToMap(change.position, pointerViewport), event.keyboardModifiers.isShiftPressed, aspectRatio,
                                ComposeDrawingPresentation.minimumFreeformMapDistance(pointerViewport.zoom, mapWidth, mapHeight),
                                renderData.gridLayer, mapWidth, mapHeight,
                            )
                            change.consume()
                        }
                    } finally {
                        toolSession?.cancel()
                        toolSession = null
                    }
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
                    if (toolSession?.active == true) {
                        toolSession?.cancel()
                        toolSession = null
                        change.consume()
                        return@onPointerEvent
                    }
                    if (noteReferencePickActive) {
                        onNoteReferenceCancelled()
                        change.consume()
                        return@onPointerEvent
                    }
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
                if (event.buttons.isPrimaryPressed && noteReferencePickActive) {
                    objectAtScreen(change.position)?.id?.let(onNoteReferencePicked)
                    change.consume()
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
                    rotateHit && selectedObjectIds.isNotEmpty() && !containsNonMovableDestroyed(selectedObjectIds) -> {
                        val bounds = content.selectionBounds(selectedObjectIds, aspectRatio) ?: return@onPointerEvent
                        transformGesture = OperationTransformGesture.Rotate(
                            content, selectedObjectIds, bounds.centre, screenToMap(change.position), aspectRatio,
                        )
                    }

                    resizeHit != null && selectedObjectIds.isNotEmpty() && !containsNonMovableDestroyed(selectedObjectIds) -> {
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
                        if (!containsNonMovableDestroyed(ids)) transformGesture = OperationTransformGesture.Move(
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
                if (noteReferencePickActive && !event.buttons.isPrimaryPressed && !event.buttons.isSecondaryPressed) {
                    onNoteReferencePreview(objectAtScreen(change.position)?.id)
                    return@onPointerEvent
                }
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
                activeReferenceCoordinate = null
                marquee?.let { current ->
                    val box = MapSelectionBox(
                        ViewerPoint(current.start.x.toDouble(), current.start.y.toDouble()),
                        ViewerPoint(current.current.x.toDouble(), current.current.y.toDouble())
                    )
                    if (box.isDrag) {
                        onObjectsSelected(
                            interactionContent.objects.filter { box.overlaps(it.selectionScreenBounds(selectionProjection)) }.mapTo(linkedSetOf(), OperationObject::id),
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
                tacticalOverlayLines.forEach { (source, target) ->
                    drawLine(
                        CommandColours.Backing,
                        Offset((source.x * mapWidth).toFloat(), (source.y * mapHeight).toFloat()),
                        Offset((target.x * mapWidth).toFloat(), (target.y * mapHeight).toFloat()),
                        strokeWidth = ComposeDrawingPresentation.worldStrokeWidth(zoom.toDouble()) * 2f,
                    )
                }
                ecmFields.forEach { field ->
                    val emitter = content.objectById(field.emitterId) as? OperationUnit ?: return@forEach
                    val centre = Offset((emitter.coordinate.x * mapWidth).toFloat(), (emitter.coordinate.y * mapHeight).toFloat())
                    val radius = (field.rangeUnits / unitsPerPixel).toFloat()
                    drawCircle(CommandColours.Backing.copy(alpha = 0.18f), radius, centre)
                    drawCircle(CommandColours.Backing.copy(alpha = 0.8f), radius, centre, style = androidx.compose.ui.graphics.drawscope.Stroke(ComposeDrawingPresentation.worldStrokeWidth(zoom.toDouble())))
                    val radians = Math.toRadians(field.bearingDegrees)
                    drawLine(CommandColours.Backing, centre, centre + Offset((kotlin.math.sin(radians) * radius).toFloat(), (-kotlin.math.cos(radians) * radius).toFloat()), ComposeDrawingPresentation.worldStrokeWidth(zoom.toDouble()))
                }
                content.objects.filterIsInstance<OperationDrawingObject>().forEach { value ->
                    drawOperationDrawing(value.drawing, mapWidth, mapHeight, ComposeDrawingPresentation.worldStrokeWidth(zoom.toDouble()))
                }
                toolSession?.drawing?.let {
                    drawOperationDrawing(it.preview, mapWidth, mapHeight, ComposeDrawingPresentation.worldStrokeWidth(zoom.toDouble()))
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
            gridOverlay?.arrows?.forEach { arrow ->
                drawLine(arrow.colour.toComposeColour(), Offset(arrow.start.x.toFloat(), arrow.start.y.toFloat()), Offset(arrow.end.x.toFloat(), arrow.end.y.toFloat()), 2f)
            }
            val selectedUnits = interactionContent.objects.filterIsInstance<OperationUnit>().filter { it.id in selectedObjectIds }
            val selectedAffiliations = selectedUnits.map { it.affiliation ?: interactionContent.unitTemplate(it.unitTemplateId)?.affiliation }.distinct()
            interactionContent.objects.filterIsInstance<OperationUnit>()
                .filter { unit -> OperationFireArcSelectionService.shouldDraw(
                    fireArcDisplayMode,
                    selectedUnits.mapTo(hashSetOf(), OperationUnit::id),
                    selectedAffiliations,
                    unit.id,
                    unit.affiliation ?: interactionContent.unitTemplate(unit.unitTemplateId)?.affiliation,
                ) }
                .forEach { unit ->
                    val facing = unit.facingDegrees ?: return@forEach
                    val centre = Offset(
                        currentViewport.screenX(mapOffsetX + unit.coordinate.x * mapWidth).toFloat(),
                        currentViewport.screenY(mapOffsetY + unit.coordinate.y * mapHeight).toFloat(),
                    )
                    val colour = (unit.affiliation ?: interactionContent.unitTemplate(unit.unitTemplateId)?.affiliation).facingIndicatorColor()
                    OperationFireArcOverlayBuilder.build(facing, OperationFireArcOverlayService.weaponsFor(unit, interactionContent)).forEach { primitive ->
                        val radius = (primitive.radiusUnits / unitsPerPixel * currentViewport.zoom).toFloat()
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(primitive.strokeWidthPx.toFloat())
                        val primitiveColour = colour.copy(alpha = if (primitive.outerBoundary) 0.9f else 0.45f)
                        when (primitive) {
                            is OperationFireArcPrimitive.Circle -> drawCircle(primitiveColour, radius, centre, style = stroke)
                            is OperationFireArcPrimitive.SectorArc -> drawArc(
                                primitiveColour,
                                startAngle = (primitive.startDegrees - 90.0).toFloat(),
                                sweepAngle = (primitive.endDegrees - primitive.startDegrees).toFloat(),
                                useCenter = false,
                                topLeft = centre - Offset(radius, radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                                style = stroke,
                            )
                            is OperationFireArcPrimitive.Radial -> {
                                val radians = Math.toRadians(primitive.degrees)
                                drawLine(primitiveColour, centre, centre + Offset((sin(radians) * radius).toFloat(), (-cos(radians) * radius).toFloat()), primitive.strokeWidthPx.toFloat())
                            }
                        }
                    }
                }
            interactionContent.objects.filterIsInstance<OperationUnit>().forEach { value ->
                val icon = unitIcons[value.unitTemplateId]
                val centre = Offset(
                    currentViewport.screenX(mapOffsetX + value.coordinate.x * mapWidth).toFloat(),
                    currentViewport.screenY(mapOffsetY + value.coordinate.y * mapHeight).toFloat(),
                )
                val width = unitIconSizePx.roundToInt().coerceAtLeast(1)
                val height = icon?.let { (width.toDouble() * it.height / it.width).roundToInt() } ?: width
                val destroyedAlpha = OperationDestroyedUnitDisplayPolicy.iconAlpha(value, destroyedUnitDisplayMode).toFloat()
                icon?.let {
                    drawImage(
                        image = it,
                        dstOffset = IntOffset((centre.x - width / 2f).roundToInt(), (centre.y - height / 2f).roundToInt()),
                        dstSize = IntSize(width, height),
                        alpha = destroyedAlpha,
                    )
                } ?: drawCircle(Color(0xFF67D5E8).copy(alpha = destroyedAlpha), width / 2f, centre)
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
                    val crossColour = if (destroyedUnitDisplayMode == OperationDestroyedUnitDisplayMode.Partial) Color(0xFFDF4A4A).copy(alpha = 0.5f) else Color.Black
                    drawLine(crossColour, centre - Offset(radius, radius), centre + Offset(radius, radius), 2f)
                    drawLine(crossColour, centre + Offset(radius, -radius), centre + Offset(-radius, radius), 2f)
                }
            }
            content.objects.filterIsInstance<OperationDrawingObject>().forEach { value ->
                value.optionalDisplayName?.let { label ->
                    val bounds = value.selectionScreenBounds(selectionProjection)
                    val style = TextStyle(Color.White, fontFamily = nordFontFamily, fontSize = 18.sp)
                    val measured = textMeasurer.measure(label, style)
                    val position = ComposeDrawingPresentation.labelTopLeft(bounds, measured.size.width, measured.size.height)
                    drawText(
                        textMeasurer,
                        label,
                        Offset(position.x.toFloat(), position.y.toFloat()),
                        style,
                    )
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
            interactionContent.objects.filter { it.id in selectedObjectIds }.map { it.selectionScreenBounds(selectionProjection) }.forEach { bounds ->
                drawRect(
                    Color.White.copy(alpha = 0.82f),
                    Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                )
            }
            noteReferenceHoveredObjectId?.let(content::objectById)?.selectionScreenBounds(selectionProjection)?.let { bounds ->
                drawRect(
                    CommandColours.Backing,
                    Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    androidx.compose.ui.geometry.Size((bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3f),
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
    strokeWidth: Float,
) {
    val colour = drawing.colour.let { Color(it.red, it.green, it.blue, it.alpha) }
    fun io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint.offset() = Offset((x * mapWidth).toFloat(), (y * mapHeight).toFloat())
    when (drawing) {
        is OperationFreeformDrawing -> if (drawing.points.size > 1) drawPath(
            Path().apply { moveTo(drawing.points.first().offset().x, drawing.points.first().offset().y); drawing.points.drop(1).forEach { lineTo(it.offset().x, it.offset().y) } },
            colour, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth),
        )

        is OperationLineDrawing -> drawLine(colour, drawing.start.offset(), drawing.end.offset(), strokeWidth)
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
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
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
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth),
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
