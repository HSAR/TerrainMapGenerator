package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D
import java.awt.Color
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitCreateDialog(
    private val root: Container,
    private val onCreated: (MapViewerUnitTemplate) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val layer = root.container()
    private val labelRenderer = MapViewerTextBitmapRenderer
    private var selectedBitmap: Bitmap32? = null
    private var previewImage: Image? = null
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
        val panelHeight = 320.0
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

        content.image(labelRenderer.render("Create Unit Type", Palette.Fonts.STRONG.first, Palette.Colours.WHITE))

        val nameLabel = content.container()
        nameLabel.xy(0.0, 42.0)
        nameLabel.image(labelRenderer.render("Name", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val nameInput = content.uiTextInput("Unit Type Name", Size2D(260.0, 32.0)).apply {
            xy(0.0, 68.0)
            skin = ViewRenderer { }
            padding = Margin(4f, 3f, 3f, 3f)
            controller.textColor = RGBA(255, 255, 255, 255)
            font = DefaultTtfFontAsBitmap
        }

        val imageLabel = content.container()
        imageLabel.xy(0.0, 114.0)
        imageLabel.image(labelRenderer.render("Image", Palette.Fonts.MAIN.first, Palette.Colours.WHITE))

        val previewFrame = content.container()
        previewFrame.xy(0.0, 140.0)
        previewFrame.solidRect(PREVIEW_WIDTH, PREVIEW_HEIGHT, RGBA(255, 255, 255, 255))
        previewFrame.solidRect(PREVIEW_WIDTH - 4.0, PREVIEW_HEIGHT - 4.0, toRgba(Palette.Colours.DARK)) {
            x = 2.0
            y = 2.0
        }

        val previewPlaceholder = previewFrame.image(labelRenderer.render("No image selected", Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = 16.0
            y = (PREVIEW_HEIGHT - height) * 0.5
        }

        val statusLabel = content.container()
        statusLabel.xy(0.0, 196.0)

        val browseButton = content.container()
        browseButton.xy(0.0, 228.0)
        buildButton(browseButton, 140.0, 40.0, "Browse Image") {
            val file = chooseImageFile() ?: return@buildButton
            val bitmap = loadBitmap(file) ?: run {
                showStatus(statusLabel, "Could not load image")
                return@buildButton
            }

            selectedBitmap = bitmap
            previewFrame.removeChildren()
            previewFrame.solidRect(PREVIEW_WIDTH, PREVIEW_HEIGHT, RGBA(255, 255, 255, 255))
            previewFrame.solidRect(PREVIEW_WIDTH - 4.0, PREVIEW_HEIGHT - 4.0, toRgba(Palette.Colours.DARK)) {
                x = 2.0
                y = 2.0
            }
            val fittedIcon = MapViewerIconFit.inside(bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
            previewImage = previewFrame.image(bitmap) {
                scale = fittedIcon.scale
                x = 6.0 + fittedIcon.x
                y = 6.0 + fittedIcon.y
            }
            showStatus(statusLabel, file.name)
            previewPlaceholder.removeFromParent()
        }

        val createButton = content.container()
        createButton.xy(160.0, 228.0)
        buildButton(createButton, 120.0, 40.0, "Create") {
            val name = nameInput.text.trim()
            val bitmap = selectedBitmap
            when {
                name.isBlank() -> showStatus(statusLabel, "Enter a type name")
                bitmap == null -> showStatus(statusLabel, "Choose an image")
                else -> {
                    onCreated(MapViewerUnitTemplate(name = name, bitmap = bitmap))
                    close()
                }
            }
        }

        val cancelButton = content.container()
        cancelButton.xy(300.0, 228.0)
        buildButton(cancelButton, 100.0, 40.0, "Cancel") {
            close()
        }

        nameInput.focus()
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

    private fun showStatus(parent: Container, text: String) {
        parent.removeChildren()
        parent.image(labelRenderer.render(text, Palette.Fonts.MAIN.first, Palette.Colours.WHITE))
    }

    private fun chooseImageFile(): File? {
        val chooser = JFileChooser().apply {
            isAcceptAllFileFilterUsed = true
            fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")
        }

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    private fun loadBitmap(file: File): Bitmap32? {
        return MapViewerIconLoader.load(file)
    }

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private companion object {
        const val Z_INDEX = 40_000.0
        const val PREVIEW_WIDTH = 180.0
        const val PREVIEW_HEIGHT = 50.0
    }
}
