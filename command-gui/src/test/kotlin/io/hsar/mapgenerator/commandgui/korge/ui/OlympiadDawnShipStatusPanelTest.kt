package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OlympiadDawnShipStatusPanelTest {
    @Test
    fun `distributes five hull points across four rows`() {
        assertThat(OlympiadDawnShipStatusPanel.hullRowSizes(5, 4), equalTo(listOf(2, 1, 1, 1)))
    }

    @Test
    fun `distributes six hull points across four rows`() {
        assertThat(OlympiadDawnShipStatusPanel.hullRowSizes(6, 4), equalTo(listOf(2, 2, 1, 1)))
    }
}
