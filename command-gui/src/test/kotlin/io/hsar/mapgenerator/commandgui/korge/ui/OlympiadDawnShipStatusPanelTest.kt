package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip
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

    @Test
    fun `includes expanded fighter health and weapon rows in panel height`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"weapons":[],"class":"Carrier","name":"Test","fighters":[{"name":"fighters","type":"standard","id":"standard-1","mods":[]}]}""",
        )

        val collapsedFighters = OlympiadDawnShipStatusPanel.height(definition, collapsed = false, componentsCollapsed = true, weaponsCollapsed = true, fightersCollapsed = true)
        val expandedFighters = OlympiadDawnShipStatusPanel.height(definition, collapsed = false, componentsCollapsed = true, weaponsCollapsed = true, fightersCollapsed = false)

        assertThat(expandedFighters - collapsedFighters, equalTo(60.0))
    }
}
