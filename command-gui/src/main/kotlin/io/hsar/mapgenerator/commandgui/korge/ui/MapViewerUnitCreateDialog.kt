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
    private var statusText: String? = null
    private var nameInput: MapViewerInputField? = null
    private var statusLabel: Container? = null
    private var previewFrame: Container? = null
    private var isOpen = false
    private var initialFocusApplied = false
    private var nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
    private var pendingNameFocus = false
    private val affiliationDropdown = MapViewerDropdownMenu(
        root = root,
        options = UnitAffiliationUi.options,
        onSelected = { affiliation -> selectedAffiliation = affiliation },
        labelForValue = UnitAffiliationUi::label,
        swatchForValue = UnitAffiliationUi::swatchColor,
    )
    private val customPropertiesEditor = MapViewerCustomPropertiesEditor(
        initialProperties = initialTemplate?.customProperties.orEmpty(),
        reservedKeys = UNIT_TYPE_FIELD_KEYS,
        onRowsChanged = { dialogWindow?.refresh() },
        onValidationChanged = { dialogWindow?.refresh() },
        tooltipRoot = root,
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
                MapViewerDialogAction(confirmLabel, enabled = { canConfirm() }) { confirm() },
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
        get() = nameInput?.isFocused == true || customPropertiesEditor.isTextInputActive

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
        customPropertiesEditor.hideTooltip()
        dialogWindow?.close()
        dialogWindow = null
        onDismiss()
    }

    private fun renderContent(content: Container, contentWidth: Double) {
        content.removeChildren()

        val layout = layoutForWidth(contentWidth)
        val inlineBlock = MapViewerInlineInputs.block(listOf("Name"))

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
                    "No Image Selected",
                    layout.previewWidth,
                    MapViewerDialogSpacing.PREVIEW_HEIGHT,
                    color = Palette.Colours.WHITE,
                )
            }
        }

        val browseButton = content.container()
        browseButton.xy(layout.browseLeft, layout.browseTop)
        MapViewerLaunchUi.button(browseButton, layout.browseButtonWidth, MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT, "Browse Image") {
            val file = chooseImageFile() ?: return@button
            val bitmap = loadBitmap(file) ?: run {
                showStatus(statusLabel, "Could not load image", statusWidthFor("Could not load image"))
                return@button
            }

            selectedBitmap = bitmap
            selectedAssetSource = AssetSource.fromFile(file)
            statusText = null
            statusLabel?.removeChildren()
            dialogWindow?.refresh()
        }

        statusLabel = content.container().also {
            it.xy(layout.browseLeft, layout.statusTop)
        }
        statusText?.let {
            showStatus(statusLabel, it, statusWidthFor(it))
        } ?: statusLabel?.removeChildren()

        nameInput = inlineBlock.text(
            parent = content,
            label = "Name",
            text = nameValue,
            x = 0.0,
            y = layout.nameTop,
            width = layout.nameFieldWidth,
            inputMinimumWidth = nameMinimumWidth(nameValue),
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(MapViewerTextInputValidation.isPresent(nameValue)),
        )
        nameInput?.controller?.onTextUpdated?.add {
            val wasValid = nameWasValid
            nameValue = nameInput?.text.orEmpty()
            nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
            if (nameWasValid != wasValid) {
                pendingNameFocus = true
                dialogWindow?.refresh()
            }
        }

        MapViewerInlineInputs.block(emptyList()).controlWithoutLabel(
            parent = content,
            x = layout.affiliationLeft,
            y = layout.affiliationTop,
            width = layout.affiliationFieldWidth,
            inputMinimumWidth = affiliationMinimumWidth(),
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
        ) { input, inputWidth ->
            affiliationDropdown.renderTrigger(
                parent = input,
                value = selectedAffiliation,
                width = inputWidth,
                height = MapViewerDialogSpacing.INPUT_HEIGHT,
            )
        }

        val customPropertiesContainer = content.container()
        customPropertiesContainer.xy(0.0, layout.customPropertiesTop)
        customPropertiesEditor.render(customPropertiesContainer, contentWidth.coerceAtLeast(MIN_FIELD_WIDTH))

        if (!initialFocusApplied) {
            nameInput?.focus()
            initialFocusApplied = true
        }
        if (pendingNameFocus) {
            nameInput?.focus()
            pendingNameFocus = false
        }
    }

    fun renderEmbedded(parent: Container, width: Double, confirmLabel: String = "Save") {
        renderContent(parent, width)
        parent.container().also { actions ->
            actions.xy(0.0, contentHeightFor(width) + MapViewerDialogSpacing.FIELD_GAP)
            MapViewerLaunchUi.button(actions, 120.0, MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT, confirmLabel) {
                createTemplate()?.let(onCreated)
            }
        }
    }

    fun embeddedHeight(width: Double): Double =
        contentHeightFor(width) + MapViewerDialogSpacing.FIELD_GAP + MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT

    fun dispose() {
        affiliationDropdown.close()
        customPropertiesEditor.hideTooltip()
    }

    private fun confirm() {
        createTemplate()?.let { template ->
            onCreated(template)
            close()
        }
    }

    private fun createTemplate(): MapViewerUnitTemplate? {
        val name = nameInput?.text?.trim().orEmpty()
        val bitmap = selectedBitmap
        when {
            name.isBlank() -> {
                showStatus(statusLabel, "Enter a type name", statusWidthFor("Enter a type name"))
                return null
            }

            !customPropertiesEditor.isValid -> return null
            bitmap == null -> {
                showStatus(statusLabel, "Choose an image", statusWidthFor("Choose an image"))
                return null
            }
        }
        return MapViewerUnitTemplate(
            id = initialTemplate?.id ?: UnitTemplateId(UUID.randomUUID()),
            name = name,
            bitmap = bitmap,
            assetSource = selectedAssetSource,
            affiliation = selectedAffiliation,
            customProperties = customPropertiesEditor.properties(),
        )
    }

    private fun canConfirm(): Boolean =
        MapViewerTextInputValidation.isPresent(nameValue) && selectedBitmap != null && customPropertiesEditor.isValid

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
        val nameBlockMinimumWidth = MapViewerInlineInputs.minimumWidth("Name", nameMinimumWidth(nameValue))
        val affiliationFieldWidth = affiliationMinimumWidth()
        val twoColumnWidth = nameBlockMinimumWidth + FIELD_GAP + affiliationFieldWidth
        val twoColumn = contentWidth >= twoColumnWidth
        val splitWidth = if (twoColumn) (contentWidth - FIELD_GAP) / 2.0 else contentWidth.coerceAtLeast(MIN_FIELD_WIDTH)
        val nameFieldWidth = if (twoColumn) splitWidth else contentWidth.coerceAtLeast(MIN_FIELD_WIDTH)
        val renderedAffiliationWidth = if (twoColumn) splitWidth else contentWidth.coerceAtLeast(MIN_FIELD_WIDTH)
        val affiliationLeft = if (twoColumn) splitWidth + FIELD_GAP else 0.0
        val previewTop = 0.0
        val browseTop = previewTop + (MapViewerDialogSpacing.PREVIEW_HEIGHT - MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT) * 0.5
        val browseBottom = browseTop + MapViewerDialogSpacing.ACTION_BUTTON_HEIGHT
        val statusTop = browseBottom + 8.0
        val nameTop = statusTop + 36.0
        val affiliationTop = if (twoColumn) {
            nameTop
        } else {
            MapViewerDialogSpacing.sectionTop(nameTop + MapViewerDialogSpacing.INPUT_HEIGHT)
        }
        val customPropertiesTop = MapViewerDialogSpacing.sectionTop(max(nameTop + MapViewerDialogSpacing.INPUT_HEIGHT, affiliationTop + MapViewerDialogSpacing.INPUT_HEIGHT))
        return UnitTypeDialogLayout(
            nameFieldWidth = nameFieldWidth,
            affiliationFieldWidth = renderedAffiliationWidth,
            affiliationLeft = affiliationLeft,
            nameTop = nameTop,
            affiliationTop = affiliationTop,
            previewTop = previewTop,
            previewWidth = contentWidth.coerceAtLeast(MIN_FIELD_WIDTH),
            browseTop = browseTop,
            browseLeft = (contentWidth - BROWSE_BUTTON_WIDTH - BROWSE_RIGHT_GUTTER).coerceAtLeast(0.0),
            browseButtonWidth = BROWSE_BUTTON_WIDTH,
            statusTop = statusTop,
            customPropertiesTop = customPropertiesTop,
        )
    }

    private fun contentHeightFor(contentWidth: Double): Double {
        val layout = layoutForWidth(contentWidth)
        return layout.customPropertiesTop + customPropertiesEditor.height() + MapViewerDialogSpacing.FORM_BOTTOM_PADDING
    }

    private fun minimumContentWidth(): Double =
        maxOf(
            MapViewerInlineInputs.minimumWidth("Name", nameMinimumWidth(nameValue)),
            affiliationMinimumWidth(),
            MIN_FIELD_WIDTH,
            BROWSE_BUTTON_WIDTH,
            customPropertiesEditor.minimumWidth(),
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
        const val BROWSE_BUTTON_WIDTH = 140.0
        const val BROWSE_RIGHT_GUTTER = 12.0
        const val FIELD_GAP = MapViewerDialogSpacing.FIELD_GAP
        const val MIN_FIELD_WIDTH = MapViewerDialogSpacing.MIN_FIELD_WIDTH
        val UNIT_TYPE_FIELD_KEYS = setOf("name", "assetId", "affiliation")
    }
}

private data class UnitTypeDialogLayout(
    val nameFieldWidth: Double,
    val affiliationFieldWidth: Double,
    val affiliationLeft: Double,
    val nameTop: Double,
    val affiliationTop: Double,
    val previewTop: Double,
    val previewWidth: Double,
    val browseTop: Double,
    val browseLeft: Double,
    val browseButtonWidth: Double,
    val statusTop: Double,
    val customPropertiesTop: Double,
)
