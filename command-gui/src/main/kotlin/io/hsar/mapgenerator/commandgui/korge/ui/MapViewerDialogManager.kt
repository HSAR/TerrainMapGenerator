package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D

internal class MapViewerDialogManager {
    private val dialogs = linkedMapOf<Any, DialogEntry>()

    fun register(
        owner: Any,
        targetKey: Any = owner,
        blocksScreenPoint: (Vector2D) -> Boolean,
        isTextInputActive: () -> Boolean = { false },
        sync: (Any) -> Unit = {},
        close: () -> Unit,
    ) {
        dialogs[owner] = DialogEntry(
            targetKey = targetKey,
            blocksScreenPoint = blocksScreenPoint,
            isTextInputActive = isTextInputActive,
            sync = sync,
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

    private data class DialogEntry(
        val targetKey: Any,
        val blocksScreenPoint: (Vector2D) -> Boolean,
        val isTextInputActive: () -> Boolean,
        val sync: (Any) -> Unit,
        val close: () -> Unit,
    )
}

internal data class MapViewerDialogPositionSync(
    val positionX: String,
    val positionY: String,
)
