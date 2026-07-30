package io.hsar.mapgenerator.commandgui.compose

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.hsar.mapgenerator.commandgui.MapViewerBackend
import io.hsar.mapgenerator.commandgui.MapViewerBackendProvider
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerSavedMap
import io.hsar.mapgenerator.commandgui.MapViewerSavedOperation
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
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
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class ComposeMapViewerBackendProvider : MapViewerBackendProvider {
    override val backend = MapViewerBackend.COMPOSE

    override fun showWindow(
        renderMap: (MapViewerSettings) -> MapViewerRenderData,
        initialSettings: MapViewerSettings,
        initialSetupInputs: MapViewerSetupInputs,
    ) = application {
        var workspaceMode by remember { mutableStateOf(WorkspaceMode.DOCKED) }
        var windowTitle by remember { mutableStateOf(MapViewerSettings().windowTitle()) }

        Window(
            title = windowTitle,
            onCloseRequest = ::exitApplication,
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

            MaterialTheme(colorScheme = darkColorScheme()) {
                ComposeMapViewer(
                    renderMap = renderMap,
                    initialSettings = initialSettings,
                    initialSetupInputs = initialSetupInputs,
                    workspaceMode = workspaceMode,
                    awtWindow = window,
                    onWorkspaceModeChanged = { workspaceMode = it },
                    onTitleChanged = { windowTitle = it },
                )
            }
        }
    }
}

internal enum class WorkspaceMode {
    DOCKED,
    FLOATING,
}

@Composable
private fun ComposeMapViewer(
    renderMap: (MapViewerSettings) -> MapViewerRenderData,
    initialSettings: MapViewerSettings,
    initialSetupInputs: MapViewerSetupInputs,
    workspaceMode: WorkspaceMode,
    awtWindow: java.awt.Window,
    onWorkspaceModeChanged: (WorkspaceMode) -> Unit,
    onTitleChanged: (String) -> Unit,
) {
    val suppliedMap = remember {
        initialSetupInputs.mapImage
            ?: initialSetupInputs.mapImagePath?.let { path -> runCatching { ImageIO.read(File(path)) }.getOrNull() }
    }
    val launchState = remember {
        ComposeLaunchState(
            suppliedMap?.let {
                ComposeActiveMap(
                    settings = initialSettings,
                    renderData = MapViewerRenderData(it),
                )
            },
        )
    }
    val repository = remember { MapViewerSaveRepository() }

    LaunchedEffect(launchState.activeMap?.settings?.operationName) {
        onTitleChanged(launchState.activeMap?.settings?.windowTitle() ?: MapViewerSettings().windowTitle())
    }

    launchState.activeMap?.let { map ->
        key(map) {
            ViewerWorkspace(
                renderMap = renderMap,
                initialMap = map,
                workspaceMode = workspaceMode,
                awtWindow = awtWindow,
                onWorkspaceModeChanged = onWorkspaceModeChanged,
                onReturnToLaunchpad = launchState::returnToLaunchpad,
            )
        }
    } ?: ComposeLaunchpad(
        repository = repository,
        awtWindow = awtWindow,
        onCreateOperation = {
            launchState.createOperation(
                initialSettings.copy(operationName = initialSettings.operationName ?: "New Operation"),
            )
        },
        onMapLoaded = launchState::openSavedMap,
    )
}

internal data class ComposeActiveMap(
    val settings: MapViewerSettings,
    val renderData: MapViewerRenderData? = null,
    val camera: MapViewerSavedCamera? = null,
    val sourcePath: String? = null,
)

internal class ComposeLaunchState(initialMap: ComposeActiveMap? = null) {
    var activeMap by mutableStateOf(initialMap)
        private set

    fun createOperation(settings: MapViewerSettings) {
        activeMap = ComposeActiveMap(settings = settings)
    }

    fun openSavedMap(loaded: MapViewerSavedMap) {
        activeMap = ComposeActiveMap(
            settings = loaded.settings,
            renderData = loaded.renderData,
            camera = loaded.camera,
            sourcePath = loaded.operation.path.toString(),
        )
    }

    fun returnToLaunchpad() {
        activeMap = null
    }
}

@Composable
private fun ComposeLaunchpad(
    repository: MapViewerSaveRepository,
    awtWindow: java.awt.Window,
    onCreateOperation: () -> Unit,
    onMapLoaded: (MapViewerSavedMap) -> Unit,
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
            runCatching { withContext(Dispatchers.IO) { repository.loadMap(path) } }
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

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Operational Command Tool", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Compose launchpad",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCreateOperation) {
                    Text("Create new operation")
                }
                OutlinedButton(onClick = ::browse, enabled = loadingPath == null) {
                    Text("Browse for save")
                }
                OutlinedButton(onClick = ::refresh, enabled = !refreshing) {
                    Text(if (refreshing) "Refreshing…" else "Refresh")
                }
            }
            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ongoing operations", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                if (refreshing || loadingPath != null) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            if (!refreshing && saves.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No saved operations were found in\n${repository.savesDirectory}",
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

@Composable
private fun ViewerWorkspace(
    renderMap: (MapViewerSettings) -> MapViewerRenderData,
    initialMap: ComposeActiveMap,
    workspaceMode: WorkspaceMode,
    awtWindow: java.awt.Window,
    onWorkspaceModeChanged: (WorkspaceMode) -> Unit,
    onReturnToLaunchpad: () -> Unit,
) {
    var settings by remember { mutableStateOf(initialMap.settings) }
    var renderData by remember { mutableStateOf(initialMap.renderData) }
    var rendering by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val eventLog = remember {
        mutableStateListOf(
            initialMap.sourcePath?.let { "Loaded ${File(it).name} at map level" }
                ?: "Compose viewer started",
        )
    }
    val scope = rememberCoroutineScope()
    fun openMap() {
        FileDialog(awtWindow as? Frame, "Open map image", FileDialog.LOAD).also { dialog ->
            dialog.file = "*.png"
            dialog.isVisible = true
            val fileName = dialog.file ?: return
            runCatching {
                ImageIO.read(File(dialog.directory, fileName))
                    ?: error("The selected file is not a supported image.")
            }.onSuccess {
                renderData = MapViewerRenderData(it)
                eventLog += "Opened $fileName"
            }.onFailure {
                eventLog += "Open failed: ${it.message ?: it::class.simpleName}"
            }
        }
    }

    fun render() {
        if (rendering) return
        rendering = true
        eventLog += "Rendering map"
        scope.launch {
            runCatching {
                withContext(Dispatchers.Default) { renderMap(settings) }
            }.onSuccess {
                renderData = it
                eventLog += "Map ready"
            }.onFailure {
                eventLog += "Render failed: ${it.message ?: it::class.simpleName}"
            }
            rendering = false
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column {
            ViewerToolbar(
                settings = settings,
                rendering = rendering,
                workspaceMode = workspaceMode,
                onSettingsChanged = { settings = it },
                onRender = ::render,
                onOpenMap = ::openMap,
                onWorkspaceModeChanged = onWorkspaceModeChanged,
                onReturnToLaunchpad = onReturnToLaunchpad,
            )
            HorizontalDivider()
            Row(Modifier.weight(1f)) {
                ToolRail()
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    renderData?.let {
                        MapCanvas(
                            renderData = it,
                            savedCamera = initialMap.camera,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } ?: EmptyMap(onRender = ::render, rendering = rendering)

                    if (workspaceMode == WorkspaceMode.FLOATING) {
                        FloatingNotesPanel(
                            note = note,
                            onNoteChanged = { note = it },
                        )
                    }
                }
                if (workspaceMode == WorkspaceMode.DOCKED) {
                    NotesPanel(
                        note = note,
                        onNoteChanged = { note = it },
                        modifier = Modifier.requiredWidth(320.dp).fillMaxHeight(),
                    )
                }
            }
            EventLog(eventLog)
        }
    }
}

@Composable
private fun ViewerToolbar(
    settings: MapViewerSettings,
    rendering: Boolean,
    workspaceMode: WorkspaceMode,
    onSettingsChanged: (MapViewerSettings) -> Unit,
    onRender: () -> Unit,
    onOpenMap: () -> Unit,
    onWorkspaceModeChanged: (WorkspaceMode) -> Unit,
    onReturnToLaunchpad: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    OutlinedButton(onClick = onReturnToLaunchpad) {
        Text("Launchpad")
    }
    Button(onClick = onRender, enabled = !rendering) {
        Text(if (rendering) "Rendering…" else "Generate map")
    }
    OutlinedButton(onClick = onOpenMap) {
        Text("Open map")
    }
    Text("Layers:")
    settings.availableLayers.forEach { layer ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = layer.id in settings.enabledLayerIds,
                onCheckedChange = { enabled ->
                    onSettingsChanged(
                        settings.copy(
                            enabledLayerIds = settings.enabledLayerIds
                                .let { if (enabled) it + layer.id else it - layer.id },
                        ),
                    )
                },
            )
            Text(layer.displayName)
        }
    }
    Spacer(Modifier.weight(1f))
    OutlinedButton(
        onClick = {
            onWorkspaceModeChanged(
                if (workspaceMode == WorkspaceMode.DOCKED) WorkspaceMode.FLOATING else WorkspaceMode.DOCKED,
            )
        },
    ) {
        Text(if (workspaceMode == WorkspaceMode.DOCKED) "Floating workspace" else "Dock workspace")
    }
}

@Composable
private fun ToolRail() = Column(
    modifier = Modifier.width(104.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text("Tools", style = MaterialTheme.typography.titleMedium)
    listOf("Select", "Pan", "Draw", "Measure").forEach { tool ->
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(tool)
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
            OutlinedButton(onClick = { minimized = !minimized }) {
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
private fun EmptyMap(onRender: () -> Unit, rendering: Boolean) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (rendering) CircularProgressIndicator()
            Text("Create a terrain map to open the Compose Canvas viewer.")
            Button(onClick = onRender, enabled = !rendering) {
                Text("Generate map")
            }
        }
    }

@Composable
private fun EventLog(events: List<String>) = Column(
    Modifier.fillMaxWidth().height(92.dp).background(MaterialTheme.colorScheme.surfaceVariant)
        .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
) {
    Text("Activity", style = MaterialTheme.typography.labelLarge)
    events.takeLast(6).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
}

private data class ComposeRasterTile(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val image: ImageBitmap,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MapCanvas(
    renderData: MapViewerRenderData,
    savedCamera: MapViewerSavedCamera? = null,
    modifier: Modifier = Modifier,
) {
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

    Canvas(
        modifier = modifier
            .background(Color(0xFF15181B))
            .onSizeChanged { viewport = viewport.resized(it.width, it.height) }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                val factor = if (change.scrollDelta.y < 0f) 1.1f else 1f / 1.1f
                viewport = viewport.zoomAt(
                    ViewerPoint(change.position.x.toDouble(), change.position.y.toDouble()),
                    factor.toDouble(),
                )
                change.consume()
            }
            .pointerInput(renderData.baseImage) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    viewport = viewport.pan(ViewerPoint(dragAmount.x.toDouble(), dragAmount.y.toDouble()))
                }
            },
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
                renderData.gridLayer?.let { grid ->
                    val lineColor = Color(
                        red = grid.colour.red,
                        green = grid.colour.green,
                        blue = grid.colour.blue,
                        alpha = grid.colour.alpha,
                    )
                    val strokeWidth = (1.25f / zoom).coerceAtLeast(0.35f)
                    repeat(grid.verticalLineCount + 1) { index ->
                        val x = (index * grid.pixelsPerLine).toFloat()
                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, renderData.baseImage.height.toFloat()),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Square,
                        )
                    }
                    repeat(grid.horizontalLineCount + 1) { index ->
                        val y = (index * grid.pixelsPerLine).toFloat()
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(renderData.baseImage.width.toFloat(), y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Square,
                        )
                    }
                }
            }
        }
    }
}

private fun java.awt.image.BufferedImage.toImageBitmap(): ImageBitmap =
    ByteArrayOutputStream().use { output ->
        ImageIO.write(this, "png", output)
        Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    }
