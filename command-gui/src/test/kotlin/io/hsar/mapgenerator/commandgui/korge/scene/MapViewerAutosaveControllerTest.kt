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
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) {
            saves += 1
            true
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
        val controller = MapViewerAutosaveController(saveIntervalMs = 60_000L) {
            saves += 1
            true
        }

        controller.flush(force = true, nowMs = 1_000L)

        assertThat(saves, equalTo(1))
    }

    private data object TestLedgerEntry : LedgerEntry {
        override fun undo(controller: LedgerController) = Unit

        override fun redo(controller: LedgerController) = Unit
    }
}
