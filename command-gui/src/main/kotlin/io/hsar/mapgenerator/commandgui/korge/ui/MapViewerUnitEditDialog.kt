package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D
import java.awt.Color

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitEditDialog(
    private val root: Container,
    private val unitType: MapViewerUnitTemplate,
    private val initialName: String,
    private val initialPositionX: String,
    private val initialPositionY: String,
    private val initialDestroyed: Boolean,
    private val onSave: (String, String, String, Boolean) -> Unit,
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
        val panelHeight = 440.0
        val panelLeft = (viewportWidth - panelWidth) * 0.5
        val panelTop = (viewportHeight - panelHeight) * 0.5

        val panel = layer.container()
        panel.xy(panelLeft, panelTop)
        panel.solidRect(panelWidth, panelHeight, RGBA(255, 255, 255, 255))
        panel.solidRect(panelWidth - 4.0, panelHeight - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }

        val content = panel.container()
        content.xy(20.0, 18.0)

        MapViewerTextViews.label(content, "Edit Unit", role = MapViewerFontRole.STRONG, color = Palette.Colours.WHITE)

        val typeLabel = content.container()
        typeLabel.xy(0.0, 42.0)
        MapViewerTextViews.label(typeLabel, "Type", color = Palette.Colours.WHITE)

        val typePreview = content.container()
        typePreview.xy(0.0, 68.0)
        typePreview.solidRect(PREVIEW_WIDTH, PREVIEW_HEIGHT, RGBA(255, 255, 255, 255))
        typePreview.solidRect(PREVIEW_WIDTH - 4.0, PREVIEW_HEIGHT - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }
        val fittedIcon = MapViewerIconFit.inside(unitType.bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
        typePreview.image(unitType.bitmap) {
            scale = fittedIcon.scale
            x = 6.0 + fittedIcon.x
            y = 6.0 + fittedIcon.y
        }
        MapViewerTextViews.leftBox(
            parent = typePreview,
            text = unitType.name,
            width = PREVIEW_WIDTH - 20.0 - fittedIcon.width,
            height = PREVIEW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = 20.0 + fittedIcon.width
        }

        val nameLabel = content.container()
        nameLabel.xy(0.0, 130.0)
        MapViewerTextViews.label(nameLabel, "Instance Name", color = Palette.Colours.WHITE)

        val nameInput = content.uiTextInput(initialName, Size2D(260.0, 32.0)).apply {
            xy(0.0, 156.0)
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = MapViewerKorgeFonts.font(MapViewerFontRole.MAIN)
        }

        val positionLabel = content.container()
        positionLabel.xy(0.0, 198.0)
        MapViewerTextViews.label(positionLabel, "Position", color = Palette.Colours.WHITE)

        val positionXLabel = content.container()
        positionXLabel.xy(0.0, 224.0)
        MapViewerTextViews.label(positionXLabel, "X", color = Palette.Colours.WHITE)

        val positionYLabel = content.container()
        positionYLabel.xy(150.0, 224.0)
        MapViewerTextViews.label(positionYLabel, "Y", color = Palette.Colours.WHITE)

        val positionXInput = content.uiTextInput(initialPositionX, Size2D(120.0, 32.0)).apply {
            xy(0.0, 250.0)
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = MapViewerKorgeFonts.font(MapViewerFontRole.MAIN)
        }

        val positionYInput = content.uiTextInput(initialPositionY, Size2D(120.0, 32.0)).apply {
            xy(150.0, 250.0)
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = MapViewerKorgeFonts.font(MapViewerFontRole.MAIN)
        }

        val destroyedLabel = content.container()
        destroyedLabel.xy(0.0, 292.0)
        MapViewerTextViews.label(destroyedLabel, "Destroyed", color = Palette.Colours.WHITE)

        var destroyedValue = initialDestroyed
        val destroyedToggle = content.container()
        destroyedToggle.xy(0.0, 318.0)

        fun refreshDestroyedToggle() {
            destroyedToggle.removeChildren()
            destroyedToggle.solidRect(260.0, 32.0, RGBA(255, 255, 255, 255))
            destroyedToggle.solidRect(256.0, 28.0, toRgba(Palette.Colours.DARK)) {
                x = 2.0
                y = 2.0
            }

            val box = destroyedToggle.container()
            box.xy(10.0, 6.0)
            box.solidRect(20.0, 20.0, RGBA(255, 255, 255, 255))
            box.solidRect(16.0, 16.0, toRgba(Palette.Colours.DARK)) {
                x = 2.0
                y = 2.0
            }
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

        val saveButton = content.container()
        saveButton.xy(0.0, 360.0)
        saveButton.zIndex = 10.0
        buildButton(saveButton, 110.0, 40.0, "Save") {
            val name = nameInput.text.trim()
            val positionX = positionXInput.text.trim()
            val positionY = positionYInput.text.trim()
            if (name.isBlank()) {
                return@buildButton
            }

            onSave(name, positionX, positionY, destroyedValue)
            close()
        }

        val cancelButton = content.container()
        cancelButton.xy(130.0, 360.0)
        cancelButton.zIndex = 10.0
        buildButton(cancelButton, 110.0, 40.0, "Cancel") {
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
        parent.solidRect(width, height, RGBA(255, 255, 255, 255))
        parent.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }
        MapViewerTextViews.box(parent, label, width, height, color = Palette.Colours.WHITE)
        parent.mouse {
            onClick {
                onActivate()
            }
        }
    }

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private companion object {
        const val Z_INDEX = 50_000.0
        const val PREVIEW_WIDTH = 180.0
        const val PREVIEW_HEIGHT = 50.0
    }
}
