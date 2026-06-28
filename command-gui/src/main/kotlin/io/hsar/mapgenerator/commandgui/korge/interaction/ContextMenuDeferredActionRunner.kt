package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater

internal class ContextMenuDeferredActionRunner(
    private val root: Container,
) {
    private var pendingAction: (() -> Unit)? = null

    init {
        root.addUpdater {
            val action = pendingAction ?: return@addUpdater
            val input = root.stage?.views?.input ?: return@addUpdater
            if (input.mouseButtons == 0) {
                pendingAction = null
                action()
            }
        }
    }

    fun launch(action: () -> Unit) {
        pendingAction = action
    }
}
