package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSetupInputs
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconFit
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerKorgeFonts
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D
import korlibs.math.geom.Vector2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(KorgeExperimental::class)
internal class MapViewerNewOperationView(
    private val root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    initialSetupInputs: MapViewerSetupInputs = MapViewerSetupInputs(),
    initialPixelsPerMetre: Double = 20.0,
    private val onLaunch: (NewOperationDraft) -> Unit,
    private val onBack: () -> Unit,
) {
    private val layer = root.container()
    private val unitRows = mutableListOf<UnitRow>()
    private var operationName = ""
    private var pixelsPerMetreText = initialPixelsPerMetre.toString()
    private var operationNameInput: UITextInput? = null
    private var pixelsPerMetreInput: UITextInput? = null
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

    init {
        layer.zIndex = Z_INDEX
        layer.onStageResized { width, height ->
            captureInputs()
            render(width, height)
        }
        layer.addUpdater {
            updateDrag(layer.stage?.views?.input?.mousePos, layer.stage?.views?.input?.mouseButtonPressed(MouseButton.LEFT) == true)
        }
        render(viewportWidth, viewportHeight)
    }

    fun close() {
        layer.removeFromParent()
    }

    private fun render(viewportWidth: Int, viewportHeight: Int) {
        captureInputs()
        layout = MapViewerNewOperationLayoutBuilder.build(viewportWidth, viewportHeight)
        layer.removeChildren()
        layer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 255))

        renderHeader()
        renderMapSection()
        renderUnitTypes()
        renderButtons()
    }

    private fun renderHeader() {
        MapViewerLaunchpadTemplate.renderHeader(layer, layout.headerBounds)
        MapViewerLaunchpadTemplate.renderScreenHeader(layer, layout.screenHeaderBounds, "New Operation", onBack)
    }

    private fun renderMapSection() {
        val panel = MapViewerLaunchUi.panel(layer, layout.mapPanelBounds)
        MapViewerLaunchUi.label(panel, "Operational Details", strong = true).xy(CONTENT_PADDING, CONTENT_PADDING)

        MapViewerLaunchUi.label(panel, "Operation Name").xy(CONTENT_PADDING, 34.0)
        operationNameInput = textInput(
            parent = panel,
            text = operationName,
            bounds = layout.operationNameBounds,
        )

        statusContainer = panel.container().also {
            it.xy(layout.operationNameBounds.left, layout.operationNameBounds.bottom + 10.0)
        }

        MapViewerLaunchUi.button(panel, 160.0, 36.0, if (mapImage == null) "Choose Image" else "Change Image") {
            chooseMapImage()
        }.xy(layout.mapPanelBounds.width - CONTENT_PADDING - 160.0, CONTENT_PADDING - 4.0)

        val previewBounds = ScreenBounds(
            left = CONTENT_PADDING,
            top = 104.0,
            width = layout.mapPanelBounds.width - CONTENT_PADDING * 2.0,
            height = 164.0,
        )
        renderMapPreview(panel, mapImage, previewBounds)

        MapViewerLaunchUi.label(panel, "Pixels per metre").xy(CONTENT_PADDING, 292.0)
        pixelsPerMetreInput = textInput(
            parent = panel,
            text = pixelsPerMetreText,
            bounds = ScreenBounds(
                left = CONTENT_PADDING,
                top = 314.0,
                width = 200.0,
                height = INPUT_HEIGHT,
            ),
        )

        mapImage?.let { image ->
            MapViewerLaunchUi.label(panel, image.name).xy(CONTENT_PADDING, 356.0)
        } ?: MapViewerLaunchUi.label(panel, "No map image selected", color = Palette.Colours.LIGHT).xy(CONTENT_PADDING, 356.0)
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

    private fun renderMapPreview(parent: Container, mapImage: MapImageRow?, bounds: ScreenBounds) {
        val preview = parent.container()
        preview.xy(bounds.left, bounds.top)
        preview.solidRect(bounds.width, bounds.height, RGBA(255, 255, 255, 255))
        preview.solidRect(bounds.width - 4.0, bounds.height - 4.0, MapViewerLaunchUi.toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }

        val bitmap = mapImage?.previewBitmap
        if (bitmap == null) {
            MapViewerLaunchUi.label(preview, "Map preview appears here", color = Palette.Colours.LIGHT)
                .xy(16.0, (bounds.height - 18.0) * 0.5)
            return
        }

        val fittedIcon = MapViewerIconFit.inside(bitmap, bounds.width - 16.0, bounds.height - 16.0)
        preview.image(bitmap) {
            scale = fittedIcon.scale
            this.x = 8.0 + fittedIcon.x
            this.y = 8.0 + fittedIcon.y
        }
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
            else -> onLaunch(draft)
        }
    }

    private fun captureInputs() {
        operationName = operationNameInput?.text?.trim().orEmpty()
        pixelsPerMetreText = pixelsPerMetreInput?.text?.trim().orEmpty()
        unitRows.forEach { row ->
            row.name = row.input?.text?.trim().orEmpty().ifBlank { row.name }
        }
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

    private fun textInput(parent: Container, text: String, bounds: ScreenBounds): UITextInput =
        parent.uiTextInput(text, Size2D(bounds.width, bounds.height)).apply {
            xy(bounds.left, bounds.top)
            skin = ViewRenderer { }
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = MapViewerKorgeFonts.font(MapViewerFontRole.MAIN)
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
        const val CONTENT_PADDING = 20.0
        const val LIST_TOP = 72.0
        const val ROW_HEIGHT = 44.0
        const val PREVIEW_WIDTH = 64.0
        const val PREVIEW_HEIGHT = 36.0
        const val PREVIEW_LEFT = 0.0
        const val INPUT_HEIGHT = 32.0
        const val HANDLE_WIDTH = 28.0
        const val HANDLE_HEIGHT = 28.0
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
        val columnGap = 28.0
        val screenHeaderTop = 108.0
        val panelTop = 210.0
        val panelWidth = (viewportWidth - margin * 2.0 - columnGap) * 0.5
        val panelHeight = viewportHeight - panelTop - margin
        val operationWidth = 440.0
        val bodyWidth = viewportWidth - margin * 2.0
        val headerWidth = 540.0

        return MapViewerNewOperationLayout(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            headerBounds = ScreenBounds((viewportWidth - headerWidth) * 0.5, top, headerWidth, 78.0),
            screenHeaderBounds = ScreenBounds((viewportWidth - bodyWidth) * 0.5, screenHeaderTop, bodyWidth, 86.0),
            titleBounds = ScreenBounds((viewportWidth - operationWidth) * 0.5, screenHeaderTop + 16.0, operationWidth, 34.0),
            operationNameBounds = ScreenBounds(20.0, 62.0, panelWidth - 40.0, 38.0),
            mapPanelBounds = ScreenBounds(margin, panelTop, panelWidth, panelHeight),
            unitsPanelBounds = ScreenBounds(margin + panelWidth + columnGap, panelTop, panelWidth, panelHeight),
            backButtonBounds = ScreenBounds(margin, screenHeaderTop + 22.0, 120.0, 42.0),
            launchButtonBounds = ScreenBounds(viewportWidth - margin - 190.0, screenHeaderTop + 22.0, 190.0, 42.0),
        )
    }
}
