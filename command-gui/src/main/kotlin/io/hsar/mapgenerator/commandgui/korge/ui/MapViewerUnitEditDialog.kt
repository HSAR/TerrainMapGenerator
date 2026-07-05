package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import kotlin.math.max

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitEditDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val unitType: MapViewerUnitTemplate,
    initialName: String,
    initialPositionX: String,
    initialPositionY: String,
    private val initialDestroyed: Boolean,
    private val initialFacing: String,
    initialCustomProperties: Map<String, String>,
    private val onPositionPreviewChanged: (String, String) -> Unit,
    private val onDestroyedPreviewChanged: (Boolean) -> Unit,
    private val onFacingPreviewChanged: (String) -> Unit,
    private val isPositionValid: (String, String) -> Boolean = { _, _ -> true },
    private val isFacingValid: (String) -> Boolean = { true },
    private val onSave: (String, String, String, Boolean, String, Map<String, String>) -> Boolean,
    private val onDismiss: () -> Unit,
) {
    private var nameValue = initialName
    private var positionXValue = initialPositionX
    private var positionYValue = initialPositionY
    private var destroyedValue = initialDestroyed
    private var facingValue = initialFacing
    private var nameInput: MapViewerInputField? = null
    private var positionXInput: MapViewerInputField? = null
    private var positionYInput: MapViewerInputField? = null
    private var facingInput: MapViewerInputField? = null
    private var isOpen = false
    private var window: MapViewerDialogWindow? = null
    private var initialFocusApplied = false
    private var nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
    private var positionWasValid = isPositionValid(positionXValue, positionYValue)
    private var facingWasValid = isFacingValid(facingValue)
    private var pendingNameFocus = false
    private val customPropertiesEditor = MapViewerCustomPropertiesEditor(
        initialProperties = initialCustomProperties,
        reservedKeys = UNIT_FIELD_KEYS,
        onRowsChanged = { window?.refresh() },
        onValidationChanged = { window?.refresh() },
        tooltipRoot = root,
    )

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        window = MapViewerDialogWindow(
            root = root,
            title = "Edit Unit",
            titleProvider = { MapViewerDialogTitles.unit("Edit Unit", nameValue) },
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = ::minimumContentWidth,
            contentHeight = ::contentHeight,
            actions = listOf(
                MapViewerDialogAction("Save", enabled = { canSave() }) { saveAndClose() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = { close() },
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
        ).also {
            it.show(viewportWidth, viewportHeight)
            dialogManager.register(
                owner = this,
                targetKey = dialogKey,
                blocksScreenPoint = { point -> it.blocksScreenPoint(point) },
                isTextInputActive = { isTextInputActive },
                sync = { payload ->
                    (payload as? MapViewerDialogPositionSync)?.let(::syncFields)
                },
                close = { close() },
            )
        }
    }

    fun close() {
        if (!isOpen) {
            return
        }

        onDestroyedPreviewChanged(initialDestroyed)
        onFacingPreviewChanged(initialFacing)
        dismiss()
    }

    private fun dismiss() {
        if (!isOpen) {
            return
        }

        isOpen = false
        dialogManager.unregister(this)
        customPropertiesEditor.hideTooltip()
        window?.close()
        window = null
        onDismiss()
    }

    fun blocksScreenPoint(point: korlibs.math.geom.Vector2D): Boolean =
        window?.blocksScreenPoint(point) == true

    val isTextInputActive: Boolean
        get() = nameInput?.isFocused == true ||
                positionXInput?.isFocused == true ||
                positionYInput?.isFocused == true ||
                facingInput?.isFocused == true ||
                customPropertiesEditor.isTextInputActive

    private fun renderContent(parent: Container, width: Double) {
        val fieldWidth = fieldWidth(width)
        val typePreviewWidth = fieldWidth
        val positionFieldWidth = (fieldWidth - MapViewerDialogSpacing.FIELD_GAP) / 2.0
        val inlineBlock = MapViewerInlineInputs.block(UNIT_INLINE_LABELS)

        val typePreview = parent.container()
        typePreview.xy(0.0, TYPE_PREVIEW_TOP)
        MapViewerUiBoxes.imageBox(typePreview, unitType.bitmap, typePreviewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
        MapViewerTextViews.leftBox(
            parent = typePreview,
            text = unitType.name,
            width = typePreviewWidth - ICON_SLOT_WIDTH - 24.0,
            height = MapViewerDialogSpacing.PREVIEW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = ICON_SLOT_WIDTH + 12.0
        }

        MapViewerTextViews.box(
            parent = parent,
            text = "X",
            width = positionFieldWidth,
            height = POSITION_LABEL_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = 0.0
            y = POSITION_LABEL_TOP
        }
        MapViewerTextViews.box(
            parent = parent,
            text = "Y",
            width = positionFieldWidth,
            height = POSITION_LABEL_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = positionFieldWidth + MapViewerDialogSpacing.FIELD_GAP
            y = POSITION_LABEL_TOP
        }

        positionXInput = MapViewerTextInputs.input(
            parent = parent,
            text = positionXValue,
            x = 0.0,
            y = POSITION_INPUT_TOP,
            width = positionFieldWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(isPositionValid(positionXValue, positionYValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                positionXValue = input.text
                refreshPositionPreview()
                refreshIfPositionValidityChanged()
            }
        }

        positionYInput = MapViewerTextInputs.input(
            parent = parent,
            text = positionYValue,
            x = positionFieldWidth + MapViewerDialogSpacing.FIELD_GAP,
            y = POSITION_INPUT_TOP,
            width = positionFieldWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(isPositionValid(positionXValue, positionYValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                positionYValue = input.text
                refreshPositionPreview()
                refreshIfPositionValidityChanged()
            }
        }

        nameInput = inlineBlock.text(
            parent = parent,
            label = "Unit Name",
            text = nameValue,
            x = 0.0,
            y = NAME_TOP,
            width = fieldWidth,
            inputMinimumWidth = nameTextInputMinimumWidth(),
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(MapViewerTextInputValidation.isPresent(nameValue)),
        )
        nameInput?.controller?.onTextUpdated?.add {
            val wasValid = nameWasValid
            nameValue = nameInput?.text.orEmpty()
            nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
            if (nameWasValid != wasValid) {
                pendingNameFocus = true
                window?.refresh()
            }
        }

        facingInput = inlineBlock.text(
            parent = parent,
            label = "Facing",
            text = facingValue,
            x = 0.0,
            y = FACING_TOP,
            width = fieldWidth,
            inputMinimumWidth = facingTextInputMinimumWidth(),
            frameStyle = MapViewerTextInputValidation.frameStyle(isFacingValid(facingValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                facingValue = input.text
                onFacingPreviewChanged(facingValue)
                refreshIfFacingValidityChanged()
            }
        }

        inlineBlock.control(
            parent = parent,
            label = "Destroyed",
            x = 0.0,
            y = DESTROYED_TOP,
            width = fieldWidth,
            inputMinimumWidth = DESTROYED_CHECKBOX_SIZE,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
        ) { input, _ ->
            renderDestroyedToggle(input)
            input.mouse {
                onClick {
                    destroyedValue = !destroyedValue
                    onDestroyedPreviewChanged(destroyedValue)
                    renderDestroyedToggle(input)
                }
            }
        }

        val customPropertiesContainer = parent.container()
        customPropertiesContainer.xy(0.0, CUSTOM_PROPERTIES_TOP)
        customPropertiesEditor.render(customPropertiesContainer, fieldWidth)

        if (!initialFocusApplied) {
            nameInput?.focus()
            initialFocusApplied = true
        }
        if (pendingNameFocus) {
            nameInput?.focus()
            pendingNameFocus = false
        }
    }

    private fun syncFields(sync: MapViewerDialogPositionSync) {
        positionXValue = sync.positionX
        positionYValue = sync.positionY
        positionXInput?.text = sync.positionX
        positionYInput?.text = sync.positionY
    }

    private fun renderDestroyedToggle(parent: Container) {
        parent.removeChildren()
        val box = parent.container()
        box.xy(0.0, (MapViewerDialogSpacing.INPUT_HEIGHT - DESTROYED_CHECKBOX_SIZE) / 2.0)
        MapViewerUiBoxes.box(box, DESTROYED_CHECKBOX_SIZE, DESTROYED_CHECKBOX_SIZE)
        if (destroyedValue) {
            MapViewerTextViews.box(
                box,
                "X",
                DESTROYED_CHECKBOX_SIZE,
                DESTROYED_CHECKBOX_SIZE,
                role = MapViewerFontRole.STRONG,
                color = Palette.Colours.RED,
            )
                .xy(0.0, 0.0)
        }
    }

    private fun refreshPositionPreview() {
        onPositionPreviewChanged(positionXValue, positionYValue)
    }

    private fun refreshIfPositionValidityChanged() {
        val isValid = isPositionValid(positionXValue, positionYValue)
        if (isValid != positionWasValid) {
            positionWasValid = isValid
            window?.refresh()
        }
    }

    private fun refreshIfFacingValidityChanged() {
        val isValid = isFacingValid(facingValue)
        if (isValid != facingWasValid) {
            facingWasValid = isValid
            window?.refresh()
        }
    }

    private fun saveAndClose() {
        if (!canSave()) {
            return
        }

        if (onSave(
                nameValue.trim(),
                positionXValue.trim(),
                positionYValue.trim(),
                destroyedValue,
                facingValue.trim(),
                customPropertiesEditor.properties(),
            )
        ) {
            dismiss()
        }
    }

    private fun contentHeight(width: Double): Double =
        CUSTOM_PROPERTIES_TOP + customPropertiesEditor.height()

    private fun canSave(): Boolean =
        MapViewerTextInputValidation.isPresent(nameValue) &&
                isPositionValid(positionXValue, positionYValue) &&
                isFacingValid(facingValue) &&
                customPropertiesEditor.isValid

    private fun fieldWidth(width: Double): Double =
        max(minimumFieldWidth(), width)

    private fun minimumContentWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            inlineBlockMinimumWidth(nameTextInputMinimumWidth()),
            positionInputMinimumWidth(),
            inlineBlockMinimumWidth(facingTextInputMinimumWidth()),
            inlineBlockMinimumWidth(DESTROYED_CHECKBOX_SIZE),
            customPropertiesEditor.minimumWidth(),
            MapViewerDialogActions.minimumWidth(2),
        )

    private fun minimumFieldWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            inlineBlockMinimumWidth(nameTextInputMinimumWidth()),
            positionInputMinimumWidth(),
            inlineBlockMinimumWidth(facingTextInputMinimumWidth()),
            inlineBlockMinimumWidth(DESTROYED_CHECKBOX_SIZE),
            customPropertiesEditor.minimumWidth(),
        )

    private fun minimumTypePreviewWidth(): Double =
        max(
            ICON_SLOT_WIDTH + 24.0 + MapViewerTextViews.measure(unitType.name).width,
            MapViewerTextViews.measure(MapViewerDialogTitles.unit("Edit Unit", nameValue)).width + 24.0,
        )

    private fun nameTextInputMinimumWidth(): Double =
        maxOf(MIN_FIELD_WIDTH, MapViewerTextViews.measure(nameValue.ifBlank { "Unit Name" }).width + 24.0)

    private fun positionInputMinimumWidth(): Double =
        positionAxisInputMinimumWidth(positionXValue, "X") +
                MapViewerDialogSpacing.FIELD_GAP +
                positionAxisInputMinimumWidth(positionYValue, "Y")

    private fun positionAxisInputMinimumWidth(value: String, fallback: String): Double =
        maxOf(POSITION_INPUT_MINIMUM_WIDTH, MapViewerTextViews.measure(value.ifBlank { fallback }).width + 24.0)

    private fun facingTextInputMinimumWidth(): Double =
        maxOf(POSITION_INPUT_MINIMUM_WIDTH, MapViewerTextViews.measure(facingValue.ifBlank { "Facing" }).width + 24.0)

    private fun inlineBlockMinimumWidth(inputMinimumWidth: Double): Double =
        MapViewerInlineInputs.block(UNIT_INLINE_LABELS).minimumWidth(inputMinimumWidth)

    private companion object {
        const val PREFERRED_WIDTH = 460.0
        const val PREFERRED_HEIGHT = 540.0
        const val ICON_SLOT_WIDTH = 34.0
        const val ICON_SLOT_HEIGHT = 26.0
        const val MIN_FIELD_WIDTH = 120.0
        const val POSITION_INPUT_MINIMUM_WIDTH = 90.0
        const val DESTROYED_CHECKBOX_SIZE = 20.0
        const val TYPE_PREVIEW_TOP = 0.0
        const val POSITION_LABEL_TOP = 68.0
        const val POSITION_LABEL_HEIGHT = 22.0
        const val POSITION_INPUT_TOP = 96.0
        const val NAME_TOP = 150.0
        const val FACING_TOP = 204.0
        const val DESTROYED_TOP = 258.0
        const val CUSTOM_PROPERTIES_TOP = 312.0
        val UNIT_INLINE_LABELS = listOf("Unit Name", "Facing", "Destroyed")
        val UNIT_FIELD_KEYS = setOf(
            "type",
            "unitTemplateId",
            "instanceName",
            "destroyed",
            "facingDegrees",
            "mapCoordinate",
            "normalizedWidth",
            "normalizedHeight",
        )
    }
}
