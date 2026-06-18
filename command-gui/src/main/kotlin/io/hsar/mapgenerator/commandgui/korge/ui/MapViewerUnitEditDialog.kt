package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.ViewRenderer
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
    private val onSave: (String, String, String) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val layer = root.container()
    private val labelRenderer = MapViewerTextBitmapRenderer
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
        val panelHeight = 356.0
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

        content.image(labelRenderer.render("Edit Unit", Palette.Fonts.STRONG.first, Palette.Colours.WHITE))

        val typeLabel = content.container()
        typeLabel.xy(0.0, 42.0)
        typeLabel.image(labelRenderer.render("Type", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

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
        typePreview.image(labelRenderer.render(unitType.name, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = 20.0 + fittedIcon.width
            y = (PREVIEW_HEIGHT - height) * 0.5
        }

        val nameLabel = content.container()
        nameLabel.xy(0.0, 130.0)
        nameLabel.image(labelRenderer.render("Instance Name", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val nameInput = content.uiTextInput(initialName, Size2D(260.0, 32.0)).apply {
            xy(0.0, 156.0)
            skin = ViewRenderer { }
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = DefaultTtfFontAsBitmap
        }

        val positionLabel = content.container()
        positionLabel.xy(0.0, 198.0)
        positionLabel.image(labelRenderer.render("Position", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val positionXLabel = content.container()
        positionXLabel.xy(0.0, 224.0)
        positionXLabel.image(labelRenderer.render("X", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val positionYLabel = content.container()
        positionYLabel.xy(150.0, 224.0)
        positionYLabel.image(labelRenderer.render("Y", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val positionXInput = content.uiTextInput(initialPositionX, Size2D(120.0, 32.0)).apply {
            xy(0.0, 250.0)
            skin = ViewRenderer { }
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = DefaultTtfFontAsBitmap
        }

        val positionYInput = content.uiTextInput(initialPositionY, Size2D(120.0, 32.0)).apply {
            xy(150.0, 250.0)
            skin = ViewRenderer { }
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = DefaultTtfFontAsBitmap
        }

        val saveButton = content.container()
        saveButton.xy(0.0, 302.0)
        buildButton(saveButton, 110.0, 40.0, "Save") {
            val name = nameInput.text.trim()
            val positionX = positionXInput.text.trim()
            val positionY = positionYInput.text.trim()
            if (name.isBlank()) {
                return@buildButton
            }

            onSave(name, positionX, positionY)
            close()
        }

        val cancelButton = content.container()
        cancelButton.xy(130.0, 302.0)
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
        parent.image(labelRenderer.render(label, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = (width - this.width) * 0.5
            y = (height - this.height) * 0.5
        }
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
