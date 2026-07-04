package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
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

    @Test
    fun `destroyed units setting parses saved labels`() {
        assertThat(DestroyedUnitsDisplay.fromSettingsValue("Partial"), equalTo(DestroyedUnitsDisplay.Partial))
        assertThat(DestroyedUnitsDisplay.fromSettingsValue("hide"), equalTo(DestroyedUnitsDisplay.Hide))
        assertThat(DestroyedUnitsDisplay.fromSettingsValue(null), equalTo(DestroyedUnitsDisplay.Full))
    }

    @Test
    fun `inline input minimum width includes title and dropdown`() {
        assertThat(
            MapViewerInlineInputs.minimumWidth("Destroyed Units", MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth()),
            greaterThanOrEqualTo(MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth()),
        )
    }
}
