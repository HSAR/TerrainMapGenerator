package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitCreateDialog(
    private val root: Container,
    private val dialogTitle: String = "Create Unit Type",
    private val confirmLabel: String = "Create",
    private val initialTemplate: MapViewerUnitTemplate? = null,
    private val onCreated: (MapViewerUnitTemplate) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val layer = root.container()
    private var selectedBitmap: Bitmap32? = null
    private var selectedAssetSource: AssetSource? = null
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
        MapViewerUiBoxes.box(panel, panelWidth, panelHeight)

        val content = panel.container()
        content.xy(20.0, 18.0)

        MapViewerTextViews.label(content, dialogTitle, role = MapViewerFontRole.STRONG, color = Palette.Colours.WHITE)

        val nameLabel = content.container()
        nameLabel.xy(0.0, 42.0)
        MapViewerTextViews.label(nameLabel, "Name", color = Palette.Colours.WHITE)

        val nameInput = MapViewerTextInputs.input(content, initialTemplate?.name ?: "Unit Type Name", 260.0, 32.0, 0.0, 68.0)

        val imageLabel = content.container()
        imageLabel.xy(0.0, 114.0)
        MapViewerTextViews.label(imageLabel, "Image", color = Palette.Colours.WHITE)

        val previewFrame = content.container()
        previewFrame.xy(0.0, 140.0)
        MapViewerUiBoxes.box(previewFrame, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        val previewPlaceholder = MapViewerTextViews.box(previewFrame, "No image selected", PREVIEW_WIDTH, PREVIEW_HEIGHT, color = Palette.Colours.WHITE)

        initialTemplate?.let { template ->
            selectedBitmap = template.bitmap
            selectedAssetSource = template.assetSource
            previewFrame.removeChildren()
            MapViewerUiBoxes.box(previewFrame, PREVIEW_WIDTH, PREVIEW_HEIGHT)
            val fittedIcon = MapViewerIconFit.inside(template.bitmap, PREVIEW_WIDTH - 12.0, PREVIEW_HEIGHT - 12.0)
            previewImage = previewFrame.image(template.bitmap) {
                scale = fittedIcon.scale
                x = 6.0 + fittedIcon.x
                y = 6.0 + fittedIcon.y
            }
            previewPlaceholder.removeFromParent()
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
            selectedAssetSource = AssetSource.fromFile(file)
            previewFrame.removeChildren()
            MapViewerUiBoxes.box(previewFrame, PREVIEW_WIDTH, PREVIEW_HEIGHT)
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
        buildButton(createButton, 120.0, 40.0, confirmLabel) {
            val name = nameInput.text.trim()
            val bitmap = selectedBitmap
            when {
                name.isBlank() -> showStatus(statusLabel, "Enter a type name")
                bitmap == null -> showStatus(statusLabel, "Choose an image")
                else -> {
                    onCreated(
                        MapViewerUnitTemplate(
                            id = initialTemplate?.id ?: UnitTemplateId(UUID.randomUUID()),
                            name = name,
                            bitmap = bitmap,
                            assetSource = selectedAssetSource,
                        ),
                    )
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
        MapViewerUiBoxes.button(parent, width, height)
        MapViewerTextViews.box(parent, label, width, height, color = Palette.Colours.WHITE)
        parent.mouse {
            onClick {
                onActivate()
            }
        }
    }

    private fun showStatus(parent: Container, text: String) {
        parent.removeChildren()
        MapViewerTextViews.leftBox(parent, text, 360.0, 28.0, color = Palette.Colours.WHITE)
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

    private companion object {
        const val Z_INDEX = 40_000.0
        const val PREVIEW_WIDTH = 180.0
        const val PREVIEW_HEIGHT = 50.0
    }
}
