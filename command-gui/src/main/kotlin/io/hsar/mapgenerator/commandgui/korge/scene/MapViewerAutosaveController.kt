package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry

internal class MapViewerAutosaveController(
    private val saveIntervalMs: Long = 60_000L,
    private val saveOperation: () -> Unit,
) {
    private val queuedEntries = ArrayDeque<LedgerEntry>()
    private var queuedAtMs: Long? = null

    fun enqueue(entry: LedgerEntry, nowMs: Long = System.currentTimeMillis()) {
        if (queuedEntries.isEmpty()) {
            queuedAtMs = nowMs
        }
        queuedEntries.addLast(entry)
    }

    fun update(nowMs: Long = System.currentTimeMillis()) {
        val queuedAt = queuedAtMs ?: return
        if (queuedEntries.isEmpty() || nowMs - queuedAt < saveIntervalMs) {
            return
        }

        saveOperation()
        queuedEntries.clear()
        queuedAtMs = null
    }

    fun flush(force: Boolean = false, nowMs: Long = System.currentTimeMillis()) {
        if (!force && queuedEntries.isEmpty()) {
            return
        }

        saveOperation()
        queuedEntries.clear()
        queuedAtMs = null
    }
}
