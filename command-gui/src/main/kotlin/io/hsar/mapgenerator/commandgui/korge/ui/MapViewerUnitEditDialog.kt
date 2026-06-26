package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitEditDialog(
    private val root: Container,
    private val unitType: MapViewerUnitTemplate,
    private val initialName: String,
    private val initialPositionX: String,
    private val initialPositionY: String,
    private val initialDestroyed: Boolean,
    private val initialFacing: String,
    private val onPositionPreviewChanged: (String, String) -> Unit,
    private val onDestroyedPreviewChanged: (Boolean) -> Unit,
    private val onFacingPreviewChanged: (String) -> Unit,
    private val onSave: (String, String, String, Boolean, String) -> Boolean,
    private val onDismiss: () -> Unit,
) {
    private val layer = root.container()
    private var isOpen = false

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        layer.zIndex = Z_INDEX
        layer.mouseEnabled = true
        layer.solidRect(viewportWidth.toDouble(), viewportHeight.toDouble(), RGBA(0, 0, 0, 160))
        layer.mouse {
            onClick {
                // Consume clicks on the backdrop.
            }
        }

        val panelWidth = 420.0
        val panelHeight = 500.0
        val panelLeft = (viewportWidth - panelWidth) * 0.5
        val panelTop = (viewportHeight - panelHeight) * 0.5

        val panel = layer.container()
        panel.xy(panelLeft, panelTop)
        MapViewerUiBoxes.box(panel, panelWidth, panelHeight)

        val content = panel.container()
        content.xy(20.0, 18.0)

        MapViewerTextViews.label(content, "Edit Unit", role = MapViewerFontRole.STRONG, color = Palette.Colours.WHITE)

        val typeLabel = content.container()
        typeLabel.xy(0.0, 42.0)
        MapViewerTextViews.label(typeLabel, "Type", color = Palette.Colours.WHITE)

        val typePreview = content.container()
        typePreview.xy(0.0, 68.0)
        MapViewerUiBoxes.box(typePreview, PREVIEW_WIDTH, PREVIEW_HEIGHT)
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
            width = PREVIEW_WIDTH - ICON_SLOT_WIDTH - 24.0,
            height = PREVIEW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = ICON_SLOT_WIDTH + 12.0
        }

        val nameLabel = content.container()
        nameLabel.xy(0.0, 130.0)
        MapViewerTextViews.label(nameLabel, "Instance Name", color = Palette.Colours.WHITE)

        val nameInput = MapViewerTextInputs.input(content, initialName, 260.0, 32.0, 0.0, 156.0)

        val positionLabel = content.container()
        positionLabel.xy(0.0, 198.0)
        MapViewerTextViews.label(positionLabel, "Position", color = Palette.Colours.WHITE)

        val positionXLabel = content.container()
        positionXLabel.xy(0.0, 224.0)
        MapViewerTextViews.label(positionXLabel, "X", color = Palette.Colours.WHITE)

        val positionYLabel = content.container()
        positionYLabel.xy(150.0, 224.0)
        MapViewerTextViews.label(positionYLabel, "Y", color = Palette.Colours.WHITE)

        val positionXInput = MapViewerTextInputs.input(content, initialPositionX, 120.0, 32.0, 0.0, 250.0)

        val positionYInput = MapViewerTextInputs.input(content, initialPositionY, 120.0, 32.0, 150.0, 250.0)

        fun refreshPositionPreview() {
            onPositionPreviewChanged(positionXInput.text, positionYInput.text)
        }

        positionXInput.controller.onTextUpdated.add {
            refreshPositionPreview()
        }
        positionYInput.controller.onTextUpdated.add {
            refreshPositionPreview()
        }

        val destroyedLabel = content.container()
        destroyedLabel.xy(0.0, 292.0)
        MapViewerTextViews.label(destroyedLabel, "Destroyed", color = Palette.Colours.WHITE)

        var destroyedValue = initialDestroyed
        val destroyedToggle = content.container()
        destroyedToggle.xy(0.0, 318.0)

        fun refreshDestroyedToggle() {
            onDestroyedPreviewChanged(destroyedValue)
            destroyedToggle.removeChildren()
            MapViewerUiBoxes.box(destroyedToggle, 260.0, 32.0)

            val box = destroyedToggle.container()
            box.xy(10.0, 6.0)
            MapViewerUiBoxes.box(box, 20.0, 20.0)
            if (destroyedValue) {
                MapViewerTextViews.box(box, "X", 20.0, 20.0, role = MapViewerFontRole.STRONG, color = Palette.Colours.RED)
                    .xy(0.0, 0.0)
            }
        }

        refreshDestroyedToggle()
        destroyedToggle.mouse {
            onClick {
                destroyedValue = !destroyedValue
                refreshDestroyedToggle()
            }
        }

        val facingLabel = content.container()
        facingLabel.xy(0.0, 360.0)
        MapViewerTextViews.label(facingLabel, "Facing", color = Palette.Colours.WHITE)

        val facingInput = MapViewerTextInputs.input(content, initialFacing, 120.0, 32.0, 0.0, 386.0)
        facingInput.controller.onTextUpdated.add {
            onFacingPreviewChanged(facingInput.text)
        }

        val saveButton = content.container()
        saveButton.xy(0.0, 440.0)
        saveButton.zIndex = 10.0
        buildButton(saveButton, 110.0, 40.0, "Save") {
            val name = nameInput.text.trim()
            val positionX = positionXInput.text.trim()
            val positionY = positionYInput.text.trim()
            if (name.isBlank()) {
                return@buildButton
            }

            if (onSave(name, positionX, positionY, destroyedValue, facingInput.text.trim())) {
                close()
            }
        }

        val cancelButton = content.container()
        cancelButton.xy(130.0, 440.0)
        cancelButton.zIndex = 10.0
        buildButton(cancelButton, 110.0, 40.0, "Cancel") {
            onDestroyedPreviewChanged(initialDestroyed)
            onFacingPreviewChanged(initialFacing)
            close()
        }

        nameInput.focus()
        nameInput.selectAll()
    }

    fun close() {
        if (!isOpen) {
            return
        }

        isOpen = false
        layer.removeFromParent()
        onDismiss()
    }

    private fun buildButton(parent: Container, width: Double, height: Double, label: String, onActivate: () -> Unit) {
        MapViewerUiBoxes.button(parent, width, height)
        MapViewerTextViews.box(parent, label, width, height, color = Palette.Colours.WHITE)
        parent.mouse {
            onClick {
                onActivate()
            }
        }
    }

    private companion object {
        const val Z_INDEX = 50_000.0
        const val PREVIEW_WIDTH = 180.0
        const val PREVIEW_HEIGHT = 50.0
        const val ICON_SLOT_WIDTH = 34.0
        const val ICON_SLOT_HEIGHT = 26.0
    }
}
