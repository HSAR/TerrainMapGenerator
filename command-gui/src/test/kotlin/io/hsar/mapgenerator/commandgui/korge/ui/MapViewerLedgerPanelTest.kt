package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@OptIn(KorgeExperimental::class)
class MapViewerLedgerPanelTest {
    @Test
    fun `styled text input keeps controller edits in the displayed value`() {
        val input = MapViewerStyledTextInput(
            parent = Container(),
            width = 100.0,
            height = 24.0,
            initialText = "Turn 1",
        )

        input.text = "Custom turn"
        input.controller.select(7, 7)
        input.controller.insertText("edited ")

        assertThat(input.text, equalTo("Custom edited turn"))
    }

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
            MapViewerInlineInputs.block(MapViewerLedgerPanel.SETTINGS_INLINE_LABELS)
                .minimumWidth(MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth()),
            greaterThanOrEqualTo(MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth()),
        )
    }

    @Test
    fun `inline input block aligns controls to the widest title`() {
        val shortBlock = MapViewerInlineInputs.block(listOf("X"))
        val dialogBlock = MapViewerInlineInputs.block(listOf("Unit Name", "Facing", "Destroyed"))

        assertThat(
            dialogBlock.minimumWidth(MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth()),
            greaterThanOrEqualTo(shortBlock.minimumWidth(MapViewerLedgerPanel.destroyedUnitsDropdownMinimumWidth())),
        )
    }
}
