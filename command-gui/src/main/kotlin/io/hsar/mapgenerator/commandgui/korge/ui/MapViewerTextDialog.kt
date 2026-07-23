package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal class MapViewerTextDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val title: String,
    private val text: String,
    private val copyText: String = text,
    private val actions: List<MapViewerDialogAction> = emptyList(),
    private val onCopy: (() -> Unit)? = null,
    private val preferredWidth: Double = PREFERRED_WIDTH,
    private val preferredHeight: Double = PREFERRED_HEIGHT,
) {
    private var window: MapViewerDialogWindow? = null

    fun open(): Boolean {
        if (dialogManager.hasTarget(dialogKey)) {
            return false
        }
        val stage = root.stage ?: return false
        val footerActions = if (actions.isEmpty()) {
            listOf(
                MapViewerDialogAction("Copy") { copyToClipboard() },
                MapViewerDialogAction("Dismiss") { close() },
            )
        } else {
            actions
        }
        window = MapViewerDialogWindow(
            root = root,
            title = title,
            persistence = MapViewerDialogPersistence("text"),
            preferredWidth = preferredWidth,
            preferredHeight = preferredHeight,
            contentMinimumWidth = { CONTENT_MIN_WIDTH },
            contentHeight = ::contentHeight,
            actions = footerActions,
            onClose = ::close,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogManager.register(
            owner = this,
            targetKey = dialogKey,
            blocksScreenPoint = { point -> window?.blocksScreenPoint(point) == true },
            close = ::close,
        )
        window?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        return true
    }

    private fun renderContent(parent: Container, width: Double) {
        val body = parent.container()
        body.xy(0.0, 0.0)
        val wrapped = MapViewerWrappedText.wrap(text, width, TEXT_SIZE)
        val textHeight = MapViewerWrappedText.height(wrapped.size)
        MapViewerTextViews.box(
            parent = body,
            text = wrapped.joinToString("\n"),
            width = width,
            height = textHeight,
            size = TEXT_SIZE,
            color = Palette.Colours.WHITE,
            alignment = TextAlignment.TOP_LEFT,
        )
    }

    private fun contentHeight(width: Double): Double =
        MapViewerWrappedText.height(MapViewerWrappedText.wrap(text, width, TEXT_SIZE).size)

    private fun close() {
        dialogManager.unregister(this)
        window?.close()
        window = null
    }

    private fun copyToClipboard() {
        runCatching {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(copyText), null)
            onCopy?.invoke()
        }
    }

    private companion object {
        const val PREFERRED_WIDTH = 820.0
        const val PREFERRED_HEIGHT = 640.0
        const val CONTENT_MIN_WIDTH = 560.0
        const val TEXT_SIZE = 22.0
    }
}
