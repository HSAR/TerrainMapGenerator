package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container

internal class MapViewerObjectNameDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    initialName: String?,
    private val onSaved: (String?) -> Unit,
) {
    private var nameValue = initialName.orEmpty()
    private var input: MapViewerInputField? = null
    private var window: MapViewerDialogWindow? = null

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) return false
        val stage = root.stage ?: return false
        window = MapViewerDialogWindow(
            root = root,
            title = "Edit Name",
            persistence = MapViewerDialogPersistence("object-name"),
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = { CONTENT_WIDTH },
            contentHeight = { INPUT_HEIGHT },
            actions = listOf(
                MapViewerDialogAction("Save") { saveAndClose() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = ::close,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogManager.register(
            owner = this,
            targetKey = dialogKey,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            isTextInputActive = { input?.isFocused == true },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        return true
    }

    private fun renderContent(parent: Container, width: Double, contentViewportHeight: Double) {
        input = MapViewerTextInputs.input(
            parent = parent,
            text = nameValue,
            width = width,
            height = INPUT_HEIGHT,
            alignment = TextAlignment.MIDDLE_LEFT,
            onLogicalTextUpdated = { nameValue = it },
        ).also { field -> field.onClick { field.focus() } }
    }

    private fun saveAndClose() {
        onSaved(nameValue.trim().takeIf(String::isNotEmpty))
        close()
    }

    private fun close() {
        dialogManager.unregister(this)
        window?.close()
        window = null
    }

    private companion object {
        const val PREFERRED_WIDTH = 480.0
        const val PREFERRED_HEIGHT = 210.0
        const val CONTENT_WIDTH = 360.0
        const val INPUT_HEIGHT = MapViewerDialogSpacing.INPUT_HEIGHT
    }
}
