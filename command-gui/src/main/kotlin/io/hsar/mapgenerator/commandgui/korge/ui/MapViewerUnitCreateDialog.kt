package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.MoveLimit
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDefinition
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.launch.MapViewerLaunchUi
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.concurrent.thread
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
    private val onChanged: (MapViewerUnitTemplate) -> Unit = {},
    private val onDismiss: () -> Unit,
    private val onTransientNotice: (String) -> Unit = {},
) {
    private var dialogWindow: MapViewerDialogWindow? = null
    private var selectedBitmap: Bitmap32? = initialTemplate?.bitmap
    private var selectedAssetSource: AssetSource? = initialTemplate?.assetSource
    private var selectedAffiliation: UnitAffiliation? = initialTemplate?.affiliation
    private var nameValue: String = initialTemplate?.name ?: "Unit Type Name"
    private var shipImportError: String? = null
    private var nameInput: MapViewerInputField? = null
    private var previewFrame: Container? = null
    private var isOpen = false
    private var initialFocusApplied = false
    private var nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
    private var pendingNameFocus = false
    private var shipDefinition = initialTemplate?.customProperties?.let(OlympiadDawnShip::definition)
    private var shipPanelCollapsed = true
    private var shipComponentsCollapsed = true
    private var shipWeaponsCollapsed = true
    private var shipFightersCollapsed = true
    private var customPropertiesExpanded = false
    private var shipImportInProgress = false
    private var shipImportTooltip: Container? = null
    private val pendingShipImports = ConcurrentLinkedQueue<Result<OlympiadDawnShipDefinition>>()
    private var embeddedParent: Container? = null
    private var embeddedWidth = 0.0
    private val initialShipProperties = initialTemplate?.customProperties.orEmpty().filterKeys { it in SHIP_PROPERTY_KEYS }
    private val affiliationDropdown = MapViewerDropdownMenu(
        root = root,
        options = UnitAffiliationUi.options,
        onSelected = { affiliation ->
            selectedAffiliation = affiliation
            notifyChanged()
        },
        labelForValue = UnitAffiliationUi::label,
        swatchForValue = UnitAffiliationUi::swatchColor,
    )
    private val customPropertiesEditor = MapViewerCustomPropertiesEditor(
        initialProperties = initialTemplate?.customProperties.orEmpty(),
        reservedKeys = UNIT_TYPE_FIELD_KEYS,
        onRowsChanged = { notifyChanged(); dialogWindow?.refresh() },
        onValidationChanged = { notifyChanged(); dialogWindow?.refresh() },
        tooltipRoot = root,
    )

    init {
        root.addUpdater { consumePendingShipImport() }
    }

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        dialogWindow = MapViewerDialogWindow(
            root = root,
            title = dialogTitle,
            persistence = MapViewerDialogPersistence("unit-create"),
            titleProvider = { MapViewerDialogTitles.unitType(dialogTitle, nameValue) },
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = ::minimumContentWidth,
            contentHeight = { width -> contentHeightFor(width) },
            actions = listOf(
                MapViewerDialogAction(confirmLabel, enabled = { canConfirm() }) { confirm() },
            ) + if (initialTemplate != null) listOf(MapViewerDialogAction("Cancel") { close() }) else emptyList(),
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
                onTransientNotice("Could not load image")
                return@button
            }

            selectedBitmap = bitmap
            selectedAssetSource = AssetSource.fromFile(file)
            notifyChanged()
            dialogWindow?.refresh()
        }

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
            notifyChanged()
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
        renderCustomPropertiesPanel(customPropertiesContainer, contentWidth.coerceAtLeast(MIN_FIELD_WIDTH))

        if (shipImportEnabled) shipDefinition?.let { definition ->
            val shipPanel = content.container()
            shipPanel.xy(0.0, layout.customPropertiesTop + customPropertiesPanelHeight() + MapViewerDialogSpacing.SECTION_GAP)
            OlympiadDawnShipStatusPanel.render(
                parent = shipPanel,
                width = contentWidth,
                definition = definition,
                damage = OlympiadDawnShip.defaultDamage(templateProperties()) ?: OlympiadDawnShipDamage(),
                collapsed = shipPanelCollapsed,
                editable = false,
                onCollapsedChanged = {
                    shipPanelCollapsed = !shipPanelCollapsed
                    refreshShipPanel()
                },
                componentsCollapsed = shipComponentsCollapsed,
                onComponentsCollapsedChanged = {
                    shipComponentsCollapsed = !shipComponentsCollapsed
                    refreshShipPanel()
                },
                weaponsCollapsed = shipWeaponsCollapsed,
                onWeaponsCollapsedChanged = {
                    shipWeaponsCollapsed = !shipWeaponsCollapsed
                    refreshShipPanel()
                },
                fightersCollapsed = shipFightersCollapsed,
                onFightersCollapsedChanged = {
                    shipFightersCollapsed = !shipFightersCollapsed
                    refreshShipPanel()
                },
            )
        }

        if (!initialFocusApplied) {
            nameInput?.focus()
            initialFocusApplied = true
        }
        if (pendingNameFocus) {
            nameInput?.focus()
            pendingNameFocus = false
        }
    }

    fun renderEmbedded(parent: Container, width: Double) {
        embeddedParent = parent
        embeddedWidth = width
        renderContent(parent, width)
    }

    fun embeddedHeight(width: Double): Double =
        contentHeightFor(width)

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
        val name = MapViewerUnitTypeName.forConfirmation(nameInput?.text.orEmpty())
        val bitmap = selectedBitmap
        when {
            name.isBlank() -> {
                onTransientNotice("Enter a type name")
                return null
            }

            !customPropertiesEditor.isValid -> return null
            bitmap == null -> {
                onTransientNotice("Choose an image")
                return null
            }
        }
        val properties = templateProperties()
        val sanitizedProperties = MoveLimit.sanitize(properties)
        if (sanitizedProperties != properties) {
            onTransientNotice("moveLimit must be a finite, non-negative number and was removed.")
        }
        return MapViewerUnitTemplate(
            id = initialTemplate?.id ?: UnitTemplateId(UUID.randomUUID()),
            name = name,
            bitmap = bitmap,
            assetSource = selectedAssetSource,
            affiliation = selectedAffiliation,
            customProperties = sanitizedProperties,
        )
    }

    private fun notifyChanged() {
        createTemplateWithoutValidation()?.let(onChanged)
    }

    private fun createTemplateWithoutValidation(): MapViewerUnitTemplate? {
        val bitmap = selectedBitmap ?: return null
        if (nameValue.isBlank() || !customPropertiesEditor.isValid) return null
        return MapViewerUnitTemplate(
            id = initialTemplate?.id ?: UnitTemplateId(UUID.randomUUID()),
            name = MapViewerUnitTypeName.forLiveUpdate(nameValue),
            bitmap = bitmap,
            assetSource = selectedAssetSource,
            affiliation = selectedAffiliation,
            customProperties = MoveLimit.sanitize(templateProperties()),
        )
    }

    private fun canConfirm(): Boolean =
        MapViewerTextInputValidation.isPresent(nameValue) && selectedBitmap != null && customPropertiesEditor.isValid

    private fun renderPreview(previewFrame: Container, bitmap: Bitmap32, previewWidth: Double) {
        previewFrame.removeChildren()
        MapViewerUiBoxes.imageBox(previewFrame, bitmap, previewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
    }

    private fun renderCustomPropertiesPanel(parent: Container, width: Double) {
        MapViewerCollapsiblePanel.header(
            parent = parent,
            width = width,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            label = MapViewerCountText.of(
                customPropertiesEditor.propertyCount(),
                "additional property",
                "additional properties",
            ),
            expanded = customPropertiesExpanded,
            onToggle = {
                customPropertiesExpanded = !customPropertiesExpanded
                if (embeddedParent != null) refreshEmbedded() else dialogWindow?.refresh()
            },
        )
        if (customPropertiesExpanded) {
            val body = parent.container().xy(0.0, MapViewerDialogSpacing.INPUT_HEIGHT)
            customPropertiesEditor.renderBody(body, width) { actionParent, actionLeft, actionTop ->
                if (shipImportEnabled) {
                    renderShipImportAction(actionParent, actionLeft, actionTop)
                }
            }
        }
    }

    private fun renderShipImportAction(parent: Container, left: Double, top: Double) {
        val action = parent.container().xy(left, top)
        MapViewerLaunchUi.button(
            parent = action,
            width = MapViewerCustomPropertiesEditor.ACTION_BUTTON_WIDTH,
            height = MapViewerCustomPropertiesEditor.ACTION_BUTTON_HEIGHT,
            label = if (shipImportInProgress) "Importing..." else "Import Ship",
            disabled = shipImportInProgress,
        ) {
            importShip()
        }
        shipImportError?.let { error ->
            val marker = parent.container().xy(
                left + MapViewerCustomPropertiesEditor.ACTION_BUTTON_WIDTH + FIELD_GAP,
                top + (MapViewerCustomPropertiesEditor.ACTION_BUTTON_HEIGHT - SHIP_IMPORT_ERROR_WIDTH) * 0.5,
            )
            MapViewerTextInputValidation.validationFailedIndicator(marker)
            marker.mouse {
                onOver {
                    shipImportTooltip?.removeFromParent()
                    shipImportTooltip = MapViewerTextInputValidation.validationExplanationTooltip(
                        parent = root,
                        anchor = marker.globalPos + Vector2D(SHIP_IMPORT_ERROR_WIDTH, 0.0),
                        text = error,
                        anchorHeight = SHIP_IMPORT_ERROR_WIDTH,
                    )
                }
                onOut {
                    shipImportTooltip?.removeFromParent()
                    shipImportTooltip = null
                }
            }
        }
    }

    private fun customPropertiesPanelHeight(): Double =
        MapViewerDialogSpacing.INPUT_HEIGHT +
                if (customPropertiesExpanded) customPropertiesEditor.bodyHeight() else 0.0

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
        val nameTop = MapViewerDialogSpacing.sectionTop(browseBottom)
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
            customPropertiesTop = customPropertiesTop,
        )
    }

    private fun contentHeightFor(contentWidth: Double): Double {
        val layout = layoutForWidth(contentWidth)
        val shipHeight = if (shipImportEnabled) shipDefinition?.let {
            OlympiadDawnShipStatusPanel.height(
                it,
                shipPanelCollapsed,
                shipComponentsCollapsed,
                shipWeaponsCollapsed,
                shipFightersCollapsed,
            ) + MapViewerDialogSpacing.SECTION_GAP
        } ?: 0.0 else 0.0
        return layout.customPropertiesTop + customPropertiesPanelHeight() + shipHeight + MapViewerDialogSpacing.FORM_BOTTOM_PADDING
    }

    private fun minimumContentWidth(): Double =
        maxOf(
            MapViewerInlineInputs.minimumWidth("Name", nameMinimumWidth(nameValue)),
            affiliationMinimumWidth(),
            MIN_FIELD_WIDTH,
            BROWSE_BUTTON_WIDTH,
            MapViewerCustomPropertiesEditor.ACTION_BUTTON_WIDTH + SHIP_IMPORT_ERROR_WIDTH + FIELD_GAP,
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

    private val shipImportEnabled: Boolean
        get() = initialTemplate != null && MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL in MapViewerFeatureFlags.enabled

    private fun importShip() {
        if (shipImportInProgress) return
        val file = JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
            lastShipImportDirectory?.let { currentDirectory = it }
            fileFilter = FileNameExtensionFilter("Ship files", "json")
        }.let { chooser ->
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        } ?: return
        shipImportInProgress = true
        refreshEmbedded()
        dialogWindow?.refresh()
        thread(name = "command-gui-ship-import", isDaemon = true) {
            pendingShipImports += runCatching { OlympiadDawnShip.import(file) }
                .onSuccess { lastShipImportDirectory = file.parentFile }
        }
        dialogWindow?.refresh()
    }

    private fun consumePendingShipImport() {
        pendingShipImports.poll()?.let { result ->
            shipImportInProgress = false
            result.onSuccess { imported ->
                shipDefinition = imported
                shipPanelCollapsed = false
                shipImportError = null
                if (embeddedParent != null) {
                    createTemplate()?.let(onCreated)
                } else {
                    dialogWindow?.refresh()
                }
            }.onFailure { error ->
                shipImportError = error.message ?: "The ship file could not be imported."
                onTransientNotice("Could not import ship")
                refreshEmbedded()
            }
            dialogWindow?.refresh()
        }
    }

    private fun refreshEmbedded() {
        embeddedParent?.let { renderEmbedded(it, embeddedWidth) }
    }

    private fun refreshShipPanel() {
        if (embeddedParent != null) refreshEmbedded() else dialogWindow?.refresh()
    }

    private fun templateProperties(): Map<String, String> =
        if (shipImportEnabled) {
            shipDefinition?.let { definition ->
                OlympiadDawnShip.importedTemplateProperties(customPropertiesEditor.properties(), definition) +
                        (MoveLimit.PROPERTY_KEY to OlympiadDawnShip.importedMoveLimit(definition).toString())
            }
                ?: customPropertiesEditor.properties() + initialShipProperties
        } else {
            customPropertiesEditor.properties() + initialShipProperties
        }

    private fun loadBitmap(file: File): Bitmap32? =
        MapViewerIconLoader.load(file)

    private fun nameMinimumWidth(text: String): Double =
        maxOf(MIN_FIELD_WIDTH, MapViewerTextViews.measure(text.ifBlank { "Unit Type Name" }).width + 24.0)

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
        const val SHIP_IMPORT_ERROR_WIDTH = MapViewerTextInputValidation.VALIDATION_ICON_SIZE
        const val FIELD_GAP = MapViewerDialogSpacing.FIELD_GAP
        const val MIN_FIELD_WIDTH = MapViewerDialogSpacing.MIN_FIELD_WIDTH
        var lastShipImportDirectory: File? = null
        val SHIP_PROPERTY_KEYS = setOf(OlympiadDawnShip.DEFINITION_KEY, OlympiadDawnShip.DEFAULT_DAMAGE_KEY)
        val UNIT_TYPE_FIELD_KEYS = setOf("name", "assetId", "affiliation") + SHIP_PROPERTY_KEYS
    }
}

internal object MapViewerUnitTypeName {
    fun forLiveUpdate(value: String): String = value

    fun forConfirmation(value: String): String = value.trim()
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
    val customPropertiesTop: Double,
)
