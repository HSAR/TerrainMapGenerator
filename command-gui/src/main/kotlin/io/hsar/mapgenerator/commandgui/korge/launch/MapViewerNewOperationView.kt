package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLinesView
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayState
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayView
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconFit
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextInputs
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayerBuilder
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

@OptIn(KorgeExperimental::class)
internal class MapViewerNewOperationView(
    private val root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
    private val initialPixelsPerMetre: Double = 20.0,
    private val initialSeed: Long = System.currentTimeMillis(),
    private val onLaunch: (NewOperationDraft) -> Unit,
    private val onBack: () -> Unit,
    private val onTitleChanged: (String) -> Unit = {},
) {
    private val layer = root.container()
    private val unitRows = mutableListOf<UnitRow>()
    private var operationName = ""
    private var pixelsPerMetreText = initialPixelsPerMetre.toString()
    private var mapIdText = ""
    private var gridStartXText = ""
    private var gridStartYText = ""
    private var operationNameInput: UITextInput? = null
    private var pixelsPerMetreInput: UITextInput? = null
    private var mapIdInput: UITextInput? = null
    private var gridStartXInput: UITextInput? = null
    private var gridStartYInput: UITextInput? = null
    private var mapPreviewContainer: Container? = null
    private var mapPreviewBounds: ScreenBounds? = null
    private var mapPreviewSignature: String? = null
    private var mapPreviewDirty = true
    private var mapImage: MapImageRow? = initialSetupInputs.mapImagePath?.let { path ->
        val image = initialSetupInputs.mapImage
        if (image == null) {
            null
        } else {
            MapImageRow(
                name = File(path).nameWithoutExtension.ifBlank { File(path).name },
                imagePath = path,
                image = image,
                previewBitmap = image.toBMP32().premultipliedIfRequired(),
                assetSource = AssetSource.fromFile(File(path)),
            )
        }
    }
    private var statusContainer: Container? = null
    private var layout = MapViewerNewOperationLayoutBuilder.build(viewportWidth, viewportHeight)
    private var dragState: ReorderDragState? = null
    private var nextRowId = 1L
    private var reseedGridDefaults = mapImage != null

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            captureInputs()
            render(width, height)
        }
        layer.addUpdater {
            updateDrag(layer.stage?.views?.input?.mousePos, layer.stage?.views?.input?.mouseButtonPressed(MouseButton.LEFT) == true)
            updatePreviewIfNeeded()
        }
        mapImage?.let { applyGridDefaults(it) }
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        captureInputs()
        if (reseedGridDefaults) {
            mapImage?.let(::applyGridDefaults)
            reseedGridDefaults = false
        }
        layout = MapViewerNewOperationLayoutBuilder.build(viewportWidth, viewportHeight)
        layer.removeChildren()
        layer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 255))

        renderHeader()
        renderMapSection()
        renderUnitTypes()
        renderButtons()
        onTitleChanged(screenTitle())
    }

    private fun renderHeader() {
        MapViewerLaunchpadTemplate.renderHeader(layer, layout.headerBounds)
        MapViewerLaunchpadTemplate.renderScreenShell(layer, layout.screenHeaderBounds, screenTitle(), onBack)
    }

    private fun renderMapSection() {
        val panel = MapViewerLaunchUi.panel(layer, layout.mapPanelBounds)
        val panelWidth = layout.mapPanelBounds.width
        val fieldWidth = (panelWidth - (CONTENT_PADDING * 2.0) - (FIELD_GAP * 3.0)) / 4.0
        val imageButtonTop = 128.0
        val fieldsTop = 186.0
        val inputsTop = 210.0
        val previewTop = 292.0

        MapViewerLaunchUi.label(panel, "Operational Details", strong = true).xy(CONTENT_PADDING, CONTENT_PADDING)
        MapViewerLaunchUi.label(panel, "Operation Name").xy(CONTENT_PADDING, 60.0)
        operationNameInput = textInput(
            parent = panel,
            text = operationName,
            bounds = ScreenBounds(
                left = CONTENT_PADDING,
                top = 84.0,
                width = panelWidth - CONTENT_PADDING * 2.0,
                height = INPUT_HEIGHT,
            ),
        )
        wirePreviewRefresh(operationNameInput)

        MapViewerLaunchUi.button(panel, 160.0, 36.0, "Choose Map Image") {
            chooseMapImage()
        }.xy(CONTENT_PADDING, imageButtonTop)

        mapImage?.let { image ->
            MapViewerLaunchUi.label(panel, image.name, color = Palette.Colours.LIGHT).xy(208.0, 132.0)
        }

        MapViewerLaunchUi.label(panel, "Map ID").xy(CONTENT_PADDING, fieldsTop)
        mapIdInput = textInput(
            parent = panel,
            text = mapIdText,
            bounds = ScreenBounds(
                left = CONTENT_PADDING,
                top = inputsTop,
                width = fieldWidth,
                height = INPUT_HEIGHT,
            ),
        )
        wirePreviewRefresh(mapIdInput)

        MapViewerLaunchUi.label(panel, "Starting X").xy(CONTENT_PADDING + fieldWidth + FIELD_GAP, fieldsTop)
        MapViewerLaunchUi.label(panel, "Starting Y").xy(CONTENT_PADDING + (fieldWidth + FIELD_GAP) * 2, fieldsTop)
        MapViewerLaunchUi.label(panel, "Pixels Per Metre").xy(CONTENT_PADDING + (fieldWidth + FIELD_GAP) * 3, fieldsTop)
        gridStartXInput = textInput(
            parent = panel,
            text = gridStartXText,
            bounds = ScreenBounds(
                left = CONTENT_PADDING + fieldWidth + FIELD_GAP,
                top = inputsTop,
                width = fieldWidth,
                height = INPUT_HEIGHT,
            ),
        )
        wirePreviewRefresh(gridStartXInput)
        gridStartYInput = textInput(
            parent = panel,
            text = gridStartYText,
            bounds = ScreenBounds(
                left = CONTENT_PADDING + (fieldWidth + FIELD_GAP) * 2,
                top = inputsTop,
                width = fieldWidth,
                height = INPUT_HEIGHT,
            ),
        )
        wirePreviewRefresh(gridStartYInput)

        pixelsPerMetreInput = textInput(
            parent = panel,
            text = pixelsPerMetreText,
            bounds = ScreenBounds(
                left = CONTENT_PADDING + (fieldWidth + FIELD_GAP) * 3,
                top = inputsTop,
                width = fieldWidth,
                height = INPUT_HEIGHT,
            ),
        )
        wirePreviewRefresh(pixelsPerMetreInput)

        mapPreviewBounds = ScreenBounds(
            left = CONTENT_PADDING,
            top = previewTop,
            width = panelWidth - CONTENT_PADDING * 2.0,
            height = layout.mapPanelBounds.height - previewTop - CONTENT_PADDING,
        )
        mapPreviewContainer = panel.container().also {
            it.xy(mapPreviewBounds!!.left, mapPreviewBounds!!.top)
        }
        renderMapPreview(mapPreviewContainer!!, mapPreviewBounds!!, mapImage)
        mapPreviewSignature = previewSignature()
        mapPreviewDirty = false
    }

    private fun renderUnitTypes() {
        val panel = MapViewerLaunchUi.panel(layer, layout.unitsPanelBounds)
        MapViewerLaunchUi.label(panel, "Unit Types", strong = true).xy(CONTENT_PADDING, CONTENT_PADDING)
        MapViewerLaunchUi.button(panel, 150.0, 36.0, "Add Unit Type") {
            addUnitType()
        }.xy(layout.unitsPanelBounds.width - CONTENT_PADDING - 150.0, CONTENT_PADDING - 4.0)

        unitRows.forEachIndexed { index, row ->
            renderUnitRow(panel, row, index)
        }
    }

    private fun renderButtons() {
        MapViewerLaunchUi.button(layer, 190.0, 42.0, "Launch Operation") {
            launch()
        }.xy(layout.launchButtonBounds.left, layout.launchButtonBounds.top)
    }

    private fun renderUnitRow(parent: Container, row: UnitRow, index: Int) {
        val top = LIST_TOP + index * ROW_HEIGHT
        val rowWidth = layout.unitsPanelBounds.width - CONTENT_PADDING * 2
        val rowContainer = parent.container()
        rowContainer.xy(CONTENT_PADDING, top)
        rowContainer.solidRect(rowWidth, ROW_HEIGHT, RGBA(255, 255, 255, 255))
        rowContainer.solidRect(rowWidth - 4.0, ROW_HEIGHT - 4.0, MapViewerLaunchUi.toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }

        renderPreviewFrame(
            parent = rowContainer,
            bitmap = row.bitmap,
            left = PREVIEW_LEFT,
            top = (ROW_HEIGHT - PREVIEW_HEIGHT) * 0.5,
        )

        row.input = textInput(
            parent = rowContainer,
            text = row.name,
            bounds = ScreenBounds(
                left = PREVIEW_LEFT + PREVIEW_WIDTH + 10.0,
                top = (ROW_HEIGHT - INPUT_HEIGHT) * 0.5,
                width = rowWidth - PREVIEW_LEFT - PREVIEW_WIDTH - HANDLE_WIDTH - 34.0,
                height = INPUT_HEIGHT,
            ),
        )

        renderHandle(
            parent = rowContainer,
            x = rowWidth - HANDLE_WIDTH - 10.0,
            y = (ROW_HEIGHT - HANDLE_HEIGHT) * 0.5,
            rowId = row.id,
            index = index,
        )
    }

    private fun renderPreviewFrame(parent: Container, bitmap: Bitmap32, left: Double, top: Double) {
        val frame = parent.container()
        frame.xy(left, top)
        frame.solidRect(PREVIEW_WIDTH, PREVIEW_HEIGHT, RGBA(255, 255, 255, 255))
        frame.solidRect(PREVIEW_WIDTH - 4.0, PREVIEW_HEIGHT - 4.0, MapViewerLaunchUi.toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }
        val fittedIcon = MapViewerIconFit.inside(bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
        frame.image(bitmap) {
            scale = fittedIcon.scale
            this.x = 6.0 + fittedIcon.x
            this.y = 6.0 + fittedIcon.y
        }
    }

    private fun renderHandle(parent: Container, x: Double, y: Double, rowId: Long, index: Int) {
        val handle = parent.container()
        handle.xy(x, y)
        handle.solidRect(HANDLE_WIDTH, HANDLE_HEIGHT, RGBA(255, 255, 255, 255))
        handle.solidRect(HANDLE_WIDTH - 4.0, HANDLE_HEIGHT - 4.0, MapViewerLaunchUi.toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }

        val grip = handle.container()
        grip.xy(7.0, 7.0)
        repeat(3) { barIndex ->
            grip.solidRect(12.0, 2.0, RGBA(255, 255, 255, 255)) {
                this.y = barIndex * 4.0
            }
        }

        handle.mouse {
            onDown {
                if (button == MouseButton.LEFT) {
                    dragState = ReorderDragState(rowId = rowId, startIndex = index)
                }
            }
        }
    }

    private fun chooseMapImage() {
        val file = chooseRasterImageFile() ?: return
        val image = ImageIO.read(file) ?: return showStatus("Could not load map image")
        mapImage = MapImageRow(
            name = file.nameWithoutExtension,
            imagePath = file.absolutePath,
            image = image,
            previewBitmap = image.toBMP32().premultipliedIfRequired(),
            assetSource = AssetSource.fromFile(file),
        )
        reseedGridDefaults = true
        mapPreviewDirty = true
        render(layout.viewportWidth, layout.viewportHeight)
    }

    private fun addUnitType() {
        val file = chooseUnitImageFile() ?: return
        val bitmap = MapViewerIconLoader.load(file) ?: return showStatus("Could not load unit image")
        unitRows += UnitRow(
            id = nextRowId++,
            name = file.nameWithoutExtension,
            bitmap = bitmap,
            assetSource = AssetSource.fromFile(file),
        )
        render(layout.viewportWidth, layout.viewportHeight)
    }

    private fun launch() {
        captureInputs()
        val gridConfiguration = parseGridConfiguration()
        val draft = NewOperationDraft(
            operationName = operationName,
            mapImage = mapImage?.let { image ->
                OperationMapImage(
                    name = image.name,
                    imagePath = image.imagePath,
                    image = image.image,
                    assetSource = image.assetSource,
                )
            },
            pixelsPerMetre = pixelsPerMetreText.toDoubleOrNull() ?: -1.0,
            gridConfiguration = gridConfiguration,
            unitTemplates = unitRows.map { row ->
                MapViewerUnitTemplate(
                    name = row.name,
                    bitmap = row.bitmap,
                    assetSource = row.assetSource,
                )
            },
        )
        when {
            draft.operationName.isBlank() -> showStatus("Enter an operation name")
            draft.mapImage == null -> showStatus("Choose a map image")
            draft.pixelsPerMetre <= 0.0 -> showStatus("Enter a pixels-per-metre value")
            draft.gridConfiguration == null -> showStatus("Enter a map ID and starting coordinates")
            else -> onLaunch(draft)
        }
    }

    private fun captureInputs() {
        operationNameInput?.text?.trim()?.let { operationName = it }
        pixelsPerMetreInput?.text?.trim()?.let { pixelsPerMetreText = it }
        mapIdInput?.text?.trim()?.let { mapIdText = it }
        gridStartXInput?.text?.trim()?.let { gridStartXText = it }
        gridStartYInput?.text?.trim()?.let { gridStartYText = it }
        unitRows.forEach { row ->
            row.name = row.input?.text?.trim().orEmpty().ifBlank { row.name }
        }
    }

    private fun applyGridDefaults(image: MapImageRow) {
        val metresPerPixel = pixelsPerMetreText.toDoubleOrNull() ?: initialPixelsPerMetre
        val defaults = defaultGridConfiguration(image.image, metresPerPixel, initialSeed)
        mapIdText = defaults.mapId
        gridStartXText = defaults.startX.toString()
        gridStartYText = defaults.startY.toString()
        mapPreviewDirty = true
    }

    private fun parseGridConfiguration(): GridConfiguration? {
        val mapId = mapIdText.trim()
        val startX = gridStartXText.trim().toIntOrNull()
        val startY = gridStartYText.trim().toIntOrNull()
        return if (mapId.isNotBlank() && startX != null && startY != null) {
            GridConfiguration(
                mapId = mapId,
                startX = startX,
                startY = startY,
            )
        } else {
            null
        }
    }

    private fun renderMapPreview(parent: Container, bounds: ScreenBounds, mapImage: MapImageRow?) {
        parent.removeChildren()
        parent.solidRect(bounds.width, bounds.height, RGBA(255, 255, 255, 255))
        parent.solidRect(bounds.width - 4.0, bounds.height - 4.0, MapViewerLaunchUi.toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }

        val bitmap = mapImage?.previewBitmap
        if (bitmap == null) {
            MapViewerLaunchUi.label(parent, "Map preview appears here", color = Palette.Colours.LIGHT)
                .xy(16.0, (bounds.height - 18.0) * 0.5)
            return
        }

        val fittedIcon = MapViewerIconFit.inside(bitmap, bounds.width - 16.0, bounds.height - 16.0)
        val previewWidth = fittedIcon.width.roundToInt().coerceAtLeast(1)
        val previewHeight = fittedIcon.height.roundToInt().coerceAtLeast(1)
        val imageContainer = parent.container().also {
            it.xy(8.0 + fittedIcon.x, 8.0 + fittedIcon.y)
        }
        imageContainer.image(bitmap) {
            scale = fittedIcon.scale
        }

        val gridConfiguration = resolvedGridConfiguration() ?: return
        val gridLayer = GridLayerBuilder.build(
            width = previewWidth,
            height = previewHeight,
            gridScale = 1_000,
            metresPerPixel = pixelsPerMetreText.toDoubleOrNull() ?: initialPixelsPerMetre,
            mapId = gridConfiguration.mapId,
            verticalLineNumberStart = gridConfiguration.startX,
            horizontalLineNumberStart = gridConfiguration.startY,
        )
        val lineOverlay = GridOverlayLinesView(gridLayer)
        val labelOverlay = GridOverlayView(gridLayer)
        imageContainer.addChild(lineOverlay)
        imageContainer.addChild(labelOverlay)
        val state = GridOverlayState(
            viewport = Rectangle(Point.ORIGIN, Point(previewWidth.toDouble(), previewHeight.toDouble())),
            mapWorldX = 0.0,
            mapWorldY = 0.0,
            mapOffsetX = 0.0,
            mapOffsetY = 0.0,
            scale = 1.0,
            pixelWidth = previewWidth,
            pixelHeight = previewHeight,
            backgroundWidth = previewWidth,
            backgroundHeight = previewHeight,
            visibleCanvasLeft = 0.0,
            visibleCanvasTop = 0.0,
            visibleCanvasRight = previewWidth.toDouble(),
            visibleCanvasBottom = previewHeight.toDouble(),
            activeReference = null,
            measurement = null,
            gridLayer = gridLayer,
        )
        lineOverlay.update(state)
        labelOverlay.update(state)
    }

    private fun resolvedGridConfiguration(): GridConfiguration? {
        parseGridConfiguration()?.let { return it }
        val image = mapImage ?: return null
        return defaultGridConfiguration(
            mapImage = image.image,
            metresPerPixel = pixelsPerMetreText.toDoubleOrNull() ?: initialPixelsPerMetre,
            seed = initialSeed,
        )
    }

    private fun updatePreviewIfNeeded() {
        captureInputs()
        val preview = mapPreviewContainer ?: return
        val bounds = mapPreviewBounds ?: return
        val signature = previewSignature()
        if (!mapPreviewDirty && signature == mapPreviewSignature) {
            return
        }
        mapPreviewSignature = signature
        mapPreviewDirty = false
        renderMapPreview(preview, bounds, mapImage)
    }

    private fun previewSignature(): String =
        buildString {
            append(mapImage?.imagePath.orEmpty()).append('|')
            append(pixelsPerMetreInput?.text?.trim().orEmpty()).append('|')
            append(mapIdInput?.text?.trim().orEmpty()).append('|')
            append(gridStartXInput?.text?.trim().orEmpty()).append('|')
            append(gridStartYInput?.text?.trim().orEmpty())
        }

    private fun updateDrag(mousePos: Vector2D?, leftHeld: Boolean) {
        val drag = dragState ?: return
        if (!leftHeld || mousePos == null) {
            dragState = null
            return
        }

        val currentIndex = unitRows.indexOfFirst { it.id == drag.rowId }
        if (currentIndex < 0) {
            dragState = null
            return
        }

        val listTop = layout.unitsPanelBounds.top + LIST_TOP
        val relativeY = mousePos.y - listTop
        val targetIndex = (relativeY / ROW_HEIGHT).toInt().coerceIn(0, unitRows.lastIndex)
        if (targetIndex != currentIndex) {
            unitRows.move(currentIndex, targetIndex)
            dragState = drag.copy(startIndex = targetIndex)
            render(layout.viewportWidth, layout.viewportHeight)
        }
    }

    private fun showStatus(text: String) {
        statusContainer?.removeChildren()
        statusContainer?.let { MapViewerLaunchUi.label(it, text, color = Palette.Colours.RED) }
    }

    private fun screenTitle(): String =
        "New Operation"

    private fun textInput(parent: Container, text: String, bounds: ScreenBounds): UITextInput =
        MapViewerTextInputs.input(parent, text, bounds.width, bounds.height, bounds.left, bounds.top)

    private fun wirePreviewRefresh(input: UITextInput?) {
        input ?: return
        input.controller.onTextUpdated.add {
            captureInputs()
            mapPreviewDirty = true
        }
    }

    private fun chooseRasterImageFile(): File? =
        chooseImageFile("Map image files", "png", "jpg", "jpeg", "gif", "bmp", "webp")

    private fun chooseUnitImageFile(): File? =
        chooseImageFile("Unit image files", "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")

    private fun chooseImageFile(description: String, vararg extensions: String): File? =
        JFileChooser().apply {
            isAcceptAllFileFilterUsed = true
            fileFilter = FileNameExtensionFilter(description, *extensions)
        }.let { chooser ->
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        }

    private data class MapImageRow(
        val name: String,
        val imagePath: String,
        val image: BufferedImage,
        val previewBitmap: Bitmap32,
        val assetSource: AssetSource?,
    )

    private data class UnitRow(
        override val id: Long,
        override var name: String,
        val bitmap: Bitmap32,
        val assetSource: AssetSource?,
        var input: UITextInput? = null,
    ) : ReorderRow

    private data class ReorderDragState(
        val rowId: Long,
        val startIndex: Int,
    )

    private interface ReorderRow {
        val id: Long
        var name: String
    }

    private companion object {
        const val Z_INDEX = 60_000.0
        const val CONTENT_PADDING = 24.0
        const val LIST_TOP = 72.0
        const val ROW_HEIGHT = 44.0
        const val PREVIEW_WIDTH = 64.0
        const val PREVIEW_HEIGHT = 36.0
        const val PREVIEW_LEFT = 0.0
        const val INPUT_HEIGHT = 40.0
        const val HANDLE_WIDTH = 28.0
        const val HANDLE_HEIGHT = 28.0
        const val FIELD_GAP = 12.0
        private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
            if (fromIndex == toIndex) return
            val item = removeAt(fromIndex)
            add(if (toIndex > fromIndex) toIndex - 1 else toIndex, item)
        }
    }
}

internal data class MapViewerNewOperationLayout(
    val viewportWidth: Int,
    val viewportHeight: Int,
    val headerBounds: ScreenBounds,
    val screenHeaderBounds: ScreenBounds,
    val titleBounds: ScreenBounds,
    val operationNameBounds: ScreenBounds,
    val mapPanelBounds: ScreenBounds,
    val unitsPanelBounds: ScreenBounds,
    val backButtonBounds: ScreenBounds,
    val launchButtonBounds: ScreenBounds,
)

internal object MapViewerNewOperationLayoutBuilder {
    fun build(viewportWidth: Int, viewportHeight: Int): MapViewerNewOperationLayout {
        val margin = 36.0
        val top = 20.0
        val shellTop = 108.0
        val shellHeight = (viewportHeight - shellTop - margin).coerceAtLeast(560.0)
        val shellWidth = minOf(viewportWidth * 0.75, viewportWidth - margin * 2.0)
        val shellLeft = (viewportWidth - shellWidth) * 0.5
        val shellPadding = 24.0
        val shellContentTop = shellTop + 86.0
        val shellContentHeight = shellHeight - 86.0 - shellPadding
        val sidebarWidth = 300.0
        val columnGap = 24.0
        val detailsWidth = shellWidth - (shellPadding * 2.0) - sidebarWidth - columnGap
        val detailsLeft = shellLeft + shellPadding
        val sidebarLeft = detailsLeft + detailsWidth + columnGap
        val headerWidth = 540.0

        return MapViewerNewOperationLayout(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            headerBounds = ScreenBounds((viewportWidth - headerWidth) * 0.5, top, headerWidth, 78.0),
            screenHeaderBounds = ScreenBounds(shellLeft, shellTop, shellWidth, shellHeight),
            titleBounds = ScreenBounds(shellLeft, shellTop, shellWidth, 56.0),
            operationNameBounds = ScreenBounds(20.0, 62.0, detailsWidth - 40.0, 38.0),
            mapPanelBounds = ScreenBounds(detailsLeft, shellContentTop, detailsWidth, shellContentHeight),
            unitsPanelBounds = ScreenBounds(sidebarLeft, shellContentTop, sidebarWidth, shellContentHeight),
            backButtonBounds = ScreenBounds(shellLeft + shellPadding, shellTop + shellPadding, 120.0, 42.0),
            launchButtonBounds = ScreenBounds(shellLeft + shellWidth - shellPadding - 190.0, shellTop + shellPadding, 190.0, 42.0),
        )
    }
}
