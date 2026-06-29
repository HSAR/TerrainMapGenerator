package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.xy
import kotlin.math.max

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitEditDialog(
    private val root: Container,
    private val unitType: MapViewerUnitTemplate,
    initialName: String,
    initialPositionX: String,
    initialPositionY: String,
    private val initialDestroyed: Boolean,
    private val initialFacing: String,
    private val onPositionPreviewChanged: (String, String) -> Unit,
    private val onDestroyedPreviewChanged: (Boolean) -> Unit,
    private val onFacingPreviewChanged: (String) -> Unit,
    private val onSave: (String, String, String, Boolean, String) -> Boolean,
    private val onDismiss: () -> Unit,
) {
    private var nameValue = initialName
    private var positionXValue = initialPositionX
    private var positionYValue = initialPositionY
    private var destroyedValue = initialDestroyed
    private var facingValue = initialFacing
    private var isOpen = false
    private var window: MapViewerDialogWindow? = null

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        window = MapViewerDialogWindow(
            root = root,
            title = "Edit Unit",
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = ::minimumContentWidth,
            contentHeight = ::contentHeight,
            actions = listOf(
                MapViewerDialogAction("Save") { saveAndClose() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = { close() },
            renderContent = ::renderContent,
        ).also {
            it.show(viewportWidth, viewportHeight)
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
        window?.close()
        window = null
        onDismiss()
    }

    private fun renderContent(parent: Container, width: Double) {
        val fieldWidth = fieldWidth(width)
        val typePreviewWidth = fieldWidth
        val compactRow = width >= COMPACT_ROW_MIN_WIDTH
        val compactFieldWidth = if (compactRow) {
            (fieldWidth - MapViewerDialogSpacing.FIELD_GAP * 2.0) / 3.0
        } else {
            (fieldWidth - MapViewerDialogSpacing.FIELD_GAP) / 2.0
        }

        val typeLabel = parent.container()
        typeLabel.xy(0.0, 0.0)
        MapViewerTextViews.label(typeLabel, "Type", color = Palette.Colours.WHITE)

        val typePreview = parent.container()
        typePreview.xy(0.0, MapViewerDialogSpacing.LABEL_TO_FIELD_GAP)
        MapViewerUiBoxes.box(typePreview, typePreviewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
        val iconSlot = typePreview.container()
        iconSlot.xy(6.0, 6.0)
        val fittedIcon = MapViewerIconFit.inside(unitType.bitmap, ICON_SLOT_WIDTH, ICON_SLOT_HEIGHT)
        iconSlot.image(unitType.bitmap) {
            scale = fittedIcon.scale
            x = fittedIcon.x
            y = fittedIcon.y
        }
        MapViewerTextViews.leftBox(
            parent = typePreview,
            text = unitType.name,
            width = typePreviewWidth - ICON_SLOT_WIDTH - 24.0,
            height = MapViewerDialogSpacing.PREVIEW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = ICON_SLOT_WIDTH + 12.0
        }

        val nameLabel = parent.container()
        nameLabel.xy(0.0, 90.0)
        MapViewerTextViews.label(nameLabel, "Instance Name", color = Palette.Colours.WHITE)

        val nameInput = MapViewerTextInputs.input(parent, nameValue, fieldWidth, MapViewerDialogSpacing.INPUT_HEIGHT, 0.0, 118.0)
        nameInput.controller.onTextUpdated.add {
            nameValue = nameInput.text
        }

        val positionLabel = parent.container()
        positionLabel.xy(0.0, 166.0)
        MapViewerTextViews.label(positionLabel, "Position", color = Palette.Colours.WHITE)

        addField(
            parent = parent,
            label = "X",
            text = positionXValue,
            x = 0.0,
            y = 194.0,
            width = compactFieldWidth,
        ) { input ->
            positionXValue = input.text
            refreshPositionPreview()
        }

        addField(
            parent = parent,
            label = "Y",
            text = positionYValue,
            x = compactFieldWidth + MapViewerDialogSpacing.FIELD_GAP,
            y = 194.0,
            width = compactFieldWidth,
        ) { input ->
            positionYValue = input.text
            refreshPositionPreview()
        }

        val facingX = if (compactRow) (compactFieldWidth + MapViewerDialogSpacing.FIELD_GAP) * 2.0 else 0.0
        val facingY = if (compactRow) 194.0 else 262.0
        addField(
            parent = parent,
            label = "Facing",
            text = facingValue,
            x = facingX,
            y = facingY,
            width = compactFieldWidth,
        ) { input ->
            facingValue = input.text
            onFacingPreviewChanged(facingValue)
        }

        val destroyedTop = if (compactRow) 262.0 else 330.0
        val destroyedLabel = parent.container()
        destroyedLabel.xy(0.0, destroyedTop)
        MapViewerTextViews.label(destroyedLabel, "Destroyed", color = Palette.Colours.WHITE)

        val destroyedToggle = parent.container()
        destroyedToggle.xy(0.0, destroyedTop + 28.0)
        renderDestroyedToggle(destroyedToggle, fieldWidth)
        destroyedToggle.mouse {
            onClick {
                destroyedValue = !destroyedValue
                onDestroyedPreviewChanged(destroyedValue)
                renderDestroyedToggle(destroyedToggle, fieldWidth)
            }
        }

        nameInput.focus()
    }

    private fun addField(
        parent: Container,
        label: String,
        text: String,
        x: Double,
        y: Double,
        width: Double,
        onTextUpdated: (MapViewerInputField) -> Unit,
    ) {
        val labelContainer = parent.container()
        labelContainer.xy(x, y)
        MapViewerTextViews.label(labelContainer, label, color = Palette.Colours.WHITE)
        val input = MapViewerTextInputs.input(parent, text, width, MapViewerDialogSpacing.INPUT_HEIGHT, x, y + MapViewerDialogSpacing.LABEL_TO_FIELD_GAP)
        input.controller.onTextUpdated.add {
            onTextUpdated(input)
        }
    }

    private fun renderDestroyedToggle(parent: Container, width: Double) {
        parent.removeChildren()
        MapViewerUiBoxes.box(parent, width, MapViewerDialogSpacing.INPUT_HEIGHT)
        val box = parent.container()
        box.xy(10.0, 8.0)
        MapViewerUiBoxes.box(box, 20.0, 20.0)
        if (destroyedValue) {
            MapViewerTextViews.box(box, "X", 20.0, 20.0, role = MapViewerFontRole.STRONG, color = Palette.Colours.RED)
                .xy(0.0, 0.0)
        }
    }

    private fun refreshPositionPreview() {
        onPositionPreviewChanged(positionXValue, positionYValue)
    }

    private fun saveAndClose() {
        if (nameValue.trim().isBlank()) {
            return
        }

        if (onSave(nameValue.trim(), positionXValue.trim(), positionYValue.trim(), destroyedValue, facingValue.trim())) {
            dismiss()
        }
    }

    private fun contentHeight(width: Double): Double =
        if (width >= COMPACT_ROW_MIN_WIDTH) {
            COMPACT_CONTENT_HEIGHT
        } else {
            STACKED_CONTENT_HEIGHT
        }

    private fun fieldWidth(width: Double): Double =
        max(minimumFieldWidth(), width)

    private fun minimumContentWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            nameInputMinimumWidth(),
            positionInputMinimumWidth(),
            destroyedMinimumWidth(),
            MapViewerDialogActions.minimumWidth(2),
        )

    private fun minimumFieldWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            nameInputMinimumWidth(),
            positionInputMinimumWidth(),
            destroyedMinimumWidth(),
        )

    private fun minimumTypePreviewWidth(): Double =
        ICON_SLOT_WIDTH + 24.0 + MapViewerTextViews.measure(unitType.name).width

    private fun nameInputMinimumWidth(): Double =
        maxOf(MIN_FIELD_WIDTH, MapViewerTextViews.measure(nameValue.ifBlank { "Instance Name" }).width + 24.0)

    private fun positionInputMinimumWidth(): Double =
        maxOf(
            90.0,
            MapViewerTextViews.measure(positionXValue.ifBlank { "X" }).width + 24.0,
            MapViewerTextViews.measure(positionYValue.ifBlank { "Y" }).width + 24.0,
            MapViewerTextViews.measure(facingValue.ifBlank { "Facing" }).width + 24.0,
        )

    private fun destroyedMinimumWidth(): Double =
        maxOf(84.0, MapViewerTextViews.measure("Destroyed").width + 12.0)

    private companion object {
        const val PREFERRED_WIDTH = 460.0
        const val PREFERRED_HEIGHT = 540.0
        const val ICON_SLOT_WIDTH = 34.0
        const val ICON_SLOT_HEIGHT = 26.0
        const val MIN_FIELD_WIDTH = 120.0
        const val COMPACT_ROW_MIN_WIDTH = 500.0
        const val COMPACT_CONTENT_HEIGHT = 320.0
        const val STACKED_CONTENT_HEIGHT = 392.0
    }
}
