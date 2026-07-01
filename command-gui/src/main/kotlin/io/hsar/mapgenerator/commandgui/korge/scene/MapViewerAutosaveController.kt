package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry

internal class MapViewerAutosaveController(
    private val saveIntervalMs: Long = 60_000L,
    private val saveOperation: ((Boolean) -> Unit) -> Unit,
) {
    constructor(
        saveIntervalMs: Long = 60_000L,
        saveOperation: () -> Boolean,
    ) : this(saveIntervalMs, { complete -> complete(saveOperation()) })

    private val queuedEntries = ArrayDeque<LedgerEntry>()
    private var queuedAtMs: Long? = null
    private var saveInProgress = false

    fun enqueue(entry: LedgerEntry, nowMs: Long = System.currentTimeMillis()) {
        if (queuedEntries.isEmpty()) {
            queuedAtMs = nowMs
        }
        queuedEntries.addLast(entry)
    }

    fun update(nowMs: Long = System.currentTimeMillis()) {
        val queuedAt = queuedAtMs ?: return
        if (queuedEntries.isEmpty() || saveInProgress || nowMs - queuedAt < saveIntervalMs) {
            return
        }

        startSave(nowMs)
    }

    fun flush(force: Boolean = false, nowMs: Long = System.currentTimeMillis()) {
        if ((!force && queuedEntries.isEmpty()) || saveInProgress) {
            return
        }

        startSave(nowMs)
    }

    private fun startSave(nowMs: Long) {
        saveInProgress = true
        saveOperation { success ->
            if (success) {
                queuedEntries.clear()
                queuedAtMs = null
            } else {
                queuedAtMs = nowMs
            }
            saveInProgress = false
        }
    }
}
