package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import korlibs.math.geom.Vector2D

internal typealias MapViewerPersistedDialog = io.hsar.mapgenerator.commandgui.save.MapViewerPersistedDialog
internal typealias MapViewerDialogBounds = io.hsar.mapgenerator.commandgui.save.MapViewerDialogBounds

internal class MapViewerDialogManager(
    private val viewportProvider: (() -> CameraViewport)? = null,
) {
    private val dialogs = linkedMapOf<Any, DialogEntry>()
    private val minimizedDialogs = linkedMapOf<Any, MinimizedDialogEntry>()
    private var dockViewportWidth = 0

    val supportsViewerWindowControls: Boolean
        get() = viewportProvider != null

    fun viewport(): CameraViewport? = viewportProvider?.invoke()

    fun dockMinimized(owner: Any, width: Double, viewportWidth: Int, moveTo: (Vector2D) -> Unit) {
        dockViewportWidth = viewportWidth
        minimizedDialogs[owner] = MinimizedDialogEntry(width, moveTo)
        layoutMinimizedDialogs()
    }

    fun undockMinimized(owner: Any) {
        if (minimizedDialogs.remove(owner) != null) layoutMinimizedDialogs()
    }

    fun resizeDock(viewportWidth: Int) {
        dockViewportWidth = viewportWidth
        layoutMinimizedDialogs()
    }

    fun register(
        owner: Any,
        targetKey: Any = owner,
        blocksScreenPoint: (Vector2D) -> Boolean,
        isTextInputActive: () -> Boolean = { false },
        sync: (Any) -> Unit = {},
        persistedDialog: (() -> MapViewerPersistedDialog?)? = null,
        close: () -> Unit,
    ) {
        dialogs[owner] = DialogEntry(
            targetKey = targetKey,
            blocksScreenPoint = blocksScreenPoint,
            isTextInputActive = isTextInputActive,
            sync = sync,
            persistedDialog = persistedDialog,
            close = close,
        )
    }

    fun unregister(owner: Any) {
        dialogs.remove(owner)
    }

    fun closeTarget(targetKey: Any) {
        dialogs
            .filterValues { it.targetKey == targetKey }
            .keys
            .toList()
            .forEach { owner -> dialogs[owner]?.close?.invoke() }
    }

    fun closeAll() {
        dialogs.keys.toList().forEach { owner -> dialogs[owner]?.close?.invoke() }
    }

    fun updateTarget(targetKey: Any, payload: Any) {
        dialogs
            .filterValues { it.targetKey == targetKey }
            .values
            .forEach { it.sync(payload) }
    }

    fun broadcast(payload: Any) {
        dialogs.values.forEach { it.sync(payload) }
    }

    fun hasTarget(targetKey: Any): Boolean =
        dialogs.values.any { it.targetKey == targetKey }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        dialogs.values.any { it.blocksScreenPoint(point) }

    fun isTextInputActive(): Boolean =
        dialogs.values.any { it.isTextInputActive() }

    fun hasOpenDialogs(): Boolean =
        dialogs.isNotEmpty()

    fun persistedDialogs(): List<MapViewerPersistedDialog> =
        dialogs.values.mapNotNull { it.persistedDialog?.invoke() }

    private fun layoutMinimizedDialogs() {
        var left = MapViewerDialogLayoutBuilder.OUTER_PADDING
        var top = MapViewerDialogLayoutBuilder.OUTER_PADDING
        minimizedDialogs.values.forEach { dialog ->
            if (left > MapViewerDialogLayoutBuilder.OUTER_PADDING &&
                left + dialog.width > dockViewportWidth - MapViewerDialogLayoutBuilder.OUTER_PADDING
            ) {
                left = MapViewerDialogLayoutBuilder.OUTER_PADDING
                top += MapViewerDialogLayoutBuilder.CLOSE_BUTTON_SIZE + MINIMIZED_DIALOG_GAP
            }
            dialog.moveTo(Vector2D(left, top))
            left += dialog.width + MINIMIZED_DIALOG_GAP
        }
    }

    private data class DialogEntry(
        val targetKey: Any,
        val blocksScreenPoint: (Vector2D) -> Boolean,
        val isTextInputActive: () -> Boolean,
        val sync: (Any) -> Unit,
        val persistedDialog: (() -> MapViewerPersistedDialog?)?,
        val close: () -> Unit,
    )

    private data class MinimizedDialogEntry(
        val width: Double,
        val moveTo: (Vector2D) -> Unit,
    )

    private companion object {
        const val MINIMIZED_DIALOG_GAP = 8.0
    }
}

internal data class MapViewerDialogPositionSync(
    val positionX: String,
    val positionY: String,
)
