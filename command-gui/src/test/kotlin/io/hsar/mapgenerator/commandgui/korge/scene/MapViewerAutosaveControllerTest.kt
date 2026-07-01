package io.hsar.mapgenerator.commandgui.korge.scene

import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerController
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerAutosaveControllerTest {

    @Test
    fun `queued entries are flushed at most once per interval`() {
        var saves = 0
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) { complete ->
            saves += 1
            complete(true)
        }

        controller.enqueue(TestLedgerEntry, nowMs = 1_000L)
        controller.update(nowMs = 1_000L)
        controller.update(nowMs = 30_000L)
        controller.update(nowMs = 61_000L)

        assertThat(saves, equalTo(1))
    }

    @Test
    fun `force flush saves even when queue is empty`() {
        var saves = 0
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) { complete ->
            saves += 1
            complete(true)
        }

        controller.flush(force = true, nowMs = 1_000L)

        assertThat(saves, equalTo(1))
    }

    @Test
    fun `async save clears queued entries after completion`() {
        var completion: ((Boolean) -> Unit)? = null
        var saves = 0
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) { complete ->
            saves += 1
            completion = complete
        }

        controller.enqueue(TestLedgerEntry, nowMs = 1_000L)
        controller.update(nowMs = 61_000L)
        controller.update(nowMs = 122_000L)
        completion?.invoke(true)
        controller.update(nowMs = 183_000L)

        assertThat(saves, equalTo(1))
    }

    @Test
    fun `async save failure keeps queued entries for a later retry`() {
        var completion: ((Boolean) -> Unit)? = null
        var saves = 0
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) { complete ->
            saves += 1
            completion = complete
        }

        controller.enqueue(TestLedgerEntry, nowMs = 1_000L)
        controller.update(nowMs = 61_000L)
        completion?.invoke(false)
        controller.update(nowMs = 122_000L)

        assertThat(saves, equalTo(2))
    }

    private data object TestLedgerEntry : LedgerEntry {
        override fun undo(controller: LedgerController) = Unit

        override fun redo(controller: LedgerController) = Unit
    }
}
