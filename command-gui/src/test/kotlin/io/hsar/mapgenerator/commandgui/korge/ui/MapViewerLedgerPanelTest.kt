package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerLedgerPanelTest {
    @Test
    fun `ledger panel tabs show turns then settings`() {
        assertThat(LedgerPanelTab.entries.map { it.label }, equalTo(listOf("Turns", "Settings")))
    }

    @Test
    fun `destroyed units setting offers all display modes`() {
        assertThat(DestroyedUnitsDisplay.entries.map { it.label }, equalTo(listOf("Full", "Partial", "Hide")))
    }
}
