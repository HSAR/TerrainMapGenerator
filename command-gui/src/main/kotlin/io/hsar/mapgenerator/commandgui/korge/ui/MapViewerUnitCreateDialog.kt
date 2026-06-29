package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchUi
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitCreateDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogTitle: String = "Create Unit Type",
    private val confirmLabel: String = "Create",
    private val dialogKey: Any = Any(),
    private val initialTemplate: MapViewerUnitTemplate? = null,
    private val onCreated: (MapViewerUnitTemplate) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private var dialogWindow: MapViewerDialogWindow? = null
    private var selectedBitmap: Bitmap32? = initialTemplate?.bitmap
    private var selectedAssetSource: AssetSource? = initialTemplate?.assetSource
    private var selectedAffiliation: UnitAffiliation? = initialTemplate?.affiliation
    private var nameValue: String = initialTemplate?.name ?: "Unit Type Name"
    private var statusText: String = selectedBitmap?.let { "Image selected" } ?: "No image selected"
    private var nameInput: MapViewerInputField? = null
    private var statusLabel: Container? = null
    private var previewFrame: Container? = null
    private var isOpen = false
    private val affiliationDropdown = MapViewerDropdownMenu(
        root = root,
        options = UnitAffiliationUi.options,
        onSelected = { affiliation -> selectedAffiliation = affiliation },
        labelForValue = UnitAffiliationUi::label,
        swatchForValue = UnitAffiliationUi::swatchColor,
    )

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        dialogWindow = MapViewerDialogWindow(
            root = root,
            title = dialogTitle,
            titleProvider = { MapViewerDialogTitles.unitType(dialogTitle, nameValue) },
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = ::minimumContentWidth,
            contentHeight = { width -> contentHeightFor(width) },
            actions = listOf(
                MapViewerDialogAction(confirmLabel) { confirm() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = { close() },
            renderContent = { content, contentWidth ->
                renderContent(content, contentWidth)
            },
            mode = MapViewerDialogMode.FLOATING,
        ).also {
            it.show(viewportWidth, viewportHeight)
            dialogManager.register(
                owner = this,
                targetKey = dialogKey,
                blocksScreenPoint = { point -> it.blocksScreenPoint(point) },
                isTextInputActive = { isTextInputActive },
                close = { close() },
            )
        }
    }

    val isTextInputActive: Boolean
        get() = nameInput?.isFocused == true

    val isModalBlocking: Boolean
        get() = dialogWindow?.isModal == true

    fun blocksScreenPoint(point: Vector2D): Boolean =
        dialogWindow?.blocksScreenPoint(point) == true

    fun close() {
        if (!isOpen) {
            return
        }

        isOpen = false
        dialogManager.unregister(this)
        affiliationDropdown.close()
        dialogWindow?.close()
        dialogWindow = null
        onDismiss()
    }

    private fun renderContent(content: Container, contentWidth: Double) {
        content.removeChildren()

        val layout = layoutForWidth(contentWidth)

        MapViewerTextViews.label(content, "Name", color = Palette.Colours.WHITE)
        nameInput = MapViewerTextInputs.input(
            content,
            nameValue,
            layout.nameFieldWidth,
            MapViewerDialogSpacing.INPUT_HEIGHT,
            0.0,
            MapViewerDialogSpacing.LABEL_TO_FIELD_GAP,
        )
        nameInput?.controller?.onTextUpdated?.add {
            nameValue = nameInput?.text.orEmpty()
        }

        affiliationDropdown.renderField(
            parent = content,
            label = "Affiliation",
            value = selectedAffiliation,
            x = layout.affiliationLeft,
            y = layout.affiliationTop,
            width = layout.affiliationFieldWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
        )

        MapViewerTextViews.label(content, "Image", color = Palette.Colours.WHITE) {
            x = 0.0
            y = layout.imageLabelTop
        }

        previewFrame = content.container().also {
            it.xy(0.0, layout.previewTop)
        }
        previewFrame?.let { frame ->
            selectedBitmap?.let { bitmap ->  
                renderPreview(frame, bitmap, layout.previewWidth)
            } ?: run {
                MapViewerUiBoxes.box(frame, layout.previewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
                MapViewerTextViews.box(
                    frame,
                    "No image selected",
                    layout.previewWidth,
                    MapViewerDialogSpacing.PREVIEW_HEIGHT,
                    color = Palette.Colours.WHITE,
                )
            }
        }

        statusLabel = content.container().also {
            it.xy(0.0, layout.statusTop)
        }
        showStatus(statusLabel, statusText, statusWidthFor(statusText))

        val browseButton = content.container()
        browseButton.xy(0.0, layout.browseTop)
        MapViewerLaunchUi.button(browseButton, 140.0, MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT, "Browse Image") {
            val file = chooseImageFile() ?: return@button
            val bitmap = loadBitmap(file) ?: run {
                showStatus(statusLabel, "Could not load image", statusWidthFor("Could not load image"))
                return@button
            }

            selectedBitmap = bitmap
            selectedAssetSource = AssetSource.fromFile(file)
            statusText = file.name
            previewFrame?.let { frame ->
                renderPreview(frame, bitmap, layout.previewWidth)
            }
            showStatus(statusLabel, statusText, statusWidthFor(statusText))
        }

        nameInput?.focus()
    }

    private fun confirm() {
        val name = nameInput?.text?.trim().orEmpty()
        val bitmap = selectedBitmap
        when {
            name.isBlank() -> showStatus(statusLabel, "Enter a type name", statusWidthFor("Enter a type name"))
            bitmap == null -> showStatus(statusLabel, "Choose an image", statusWidthFor("Choose an image"))
            else -> {
                onCreated(
                    MapViewerUnitTemplate(
                        id = initialTemplate?.id ?: UnitTemplateId(UUID.randomUUID()),
                        name = name,
                        bitmap = bitmap,
                        assetSource = selectedAssetSource,
                        affiliation = selectedAffiliation,
                    ),
                )
                close()
            }
        }
    }

    private fun renderPreview(previewFrame: Container, bitmap: Bitmap32, previewWidth: Double) {
        previewFrame.removeChildren()
        MapViewerUiBoxes.imageBox(previewFrame, bitmap, previewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
    }

    private fun showStatus(parent: Container?, text: String, width: Double) {
        parent ?: return
        parent.removeChildren()
        statusText = text
        MapViewerTextViews.leftBox(parent, text, width, 28.0, color = Palette.Colours.WHITE)
    }

    private fun layoutForWidth(contentWidth: Double): UnitTypeDialogLayout {
        val affiliationFieldWidth = affiliationMinimumWidth()
        val twoColumnWidth = NAME_FIELD_WIDTH + FIELD_GAP + affiliationFieldWidth
        val twoColumn = contentWidth >= twoColumnWidth
        val nameFieldWidth = if (twoColumn) NAME_FIELD_WIDTH else contentWidth.coerceAtLeast(MIN_FIELD_WIDTH)
        val renderedAffiliationWidth = if (twoColumn) affiliationFieldWidth else contentWidth.coerceAtLeast(MIN_FIELD_WIDTH)
        val affiliationLeft = if (twoColumn) NAME_FIELD_WIDTH + FIELD_GAP else 0.0
        val nameLabelTop = 0.0
        val nameInputTop = MapViewerDialogSpacing.inputTop(nameLabelTop)
        val firstRowBottom = nameInputTop + MapViewerDialogSpacing.INPUT_HEIGHT
        val affiliationTop = if (twoColumn) {
            nameLabelTop
        } else {
            MapViewerDialogSpacing.sectionTop(firstRowBottom)
        }
        val affiliationBottom = if (twoColumn) {
            nameInputTop + MapViewerDialogSpacing.INPUT_HEIGHT
        } else {
            affiliationTop + MapViewerDialogSpacing.LABEL_TO_FIELD_GAP + MapViewerDialogSpacing.INPUT_HEIGHT
        }
        val imageLabelTop = MapViewerDialogSpacing.sectionTop(max(firstRowBottom, affiliationBottom))
        val previewTop = MapViewerDialogSpacing.inputTop(imageLabelTop)
        val statusTop = previewTop + MapViewerDialogSpacing.PREVIEW_HEIGHT + MapViewerDialogSpacing.PREVIEW_BOTTOM_GAP
        val browseTop = statusTop + 30.0
        return UnitTypeDialogLayout(
            nameFieldWidth = nameFieldWidth,
            affiliationFieldWidth = renderedAffiliationWidth,
            affiliationLeft = affiliationLeft,
            affiliationTop = affiliationTop,
            imageLabelTop = imageLabelTop,
            previewTop = previewTop,
            previewWidth = contentWidth.coerceAtLeast(MIN_FIELD_WIDTH),
            statusTop = statusTop,
            browseTop = browseTop,
        )
    }

    private fun contentHeightFor(contentWidth: Double): Double {
        val layout = layoutForWidth(contentWidth)
        return layout.browseTop + MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT + MapViewerDialogSpacing.FORM_BOTTOM_PADDING
    }

    private fun minimumContentWidth(): Double =
        maxOf(
            nameMinimumWidth(nameValue),
            affiliationMinimumWidth(),
            MIN_FIELD_WIDTH,
            statusWidthFor(statusText),
            BROWSE_BUTTON_WIDTH,
            MapViewerDialogActions.minimumWidth(2),
        )

    private fun chooseImageFile(): File? {
        val chooser = JFileChooser().apply {
            isAcceptAllFileFilterUsed = true
            fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")
        }

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    private fun loadBitmap(file: File): Bitmap32? =
        MapViewerIconLoader.load(file)

    private fun nameMinimumWidth(text: String): Double =
        maxOf(MIN_FIELD_WIDTH, MapViewerTextViews.measure(text.ifBlank { "Unit Type Name" }).width + 24.0)

    private fun statusWidthFor(text: String): Double =
        maxOf(180.0, MapViewerTextViews.measure(text).width + 24.0)

    private fun affiliationMinimumWidth(): Double =
        maxOf(
            MIN_FIELD_WIDTH,
            MapViewerDropdownLayoutBuilder.minimumMenuWidth(
                labels = UnitAffiliationUi.options.map { it.label },
            ) + 18.0,
        )

    private companion object {
        const val PREFERRED_WIDTH = 560.0
        const val PREFERRED_HEIGHT = 420.0
        const val NAME_FIELD_WIDTH = 240.0
        const val BROWSE_BUTTON_WIDTH = 140.0
        const val FIELD_GAP = MapViewerDialogSpacing.FIELD_GAP
        const val MIN_FIELD_WIDTH = MapViewerDialogSpacing.MIN_FIELD_WIDTH
    }
}

private data class UnitTypeDialogLayout(
    val nameFieldWidth: Double,
    val affiliationFieldWidth: Double,
    val affiliationLeft: Double,
    val affiliationTop: Double,
    val imageLabelTop: Double,
    val previewTop: Double,
    val previewWidth: Double,
    val statusTop: Double,
    val browseTop: Double,
)
