package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnFighterLaunchStatus
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipWeapon
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnSquadronStatus
import io.hsar.mapgenerator.commandgui.nextTestUuid
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OlympiadDawnShipStatusPanelTest {
    @Test
    fun `squadron panel includes its health and weapons when expanded`() {
        val status = OlympiadDawnSquadronStatus(
            carrierId = MapPlacedObjectId(nextTestUuid()),
            squadronId = "squadron-1",
            name = "Fighter",
            count = 6,
            remaining = 4,
            weapons = listOf(OlympiadDawnShipWeapon(null, "Class 1 Beam Turret")),
        )

        assertThat(OlympiadDawnSquadronStatusPanel.height(status, collapsed = true), equalTo(34.0))
        assertThat(OlympiadDawnSquadronStatusPanel.height(status, collapsed = false), equalTo(94.0))
    }

    @Test
    fun `labels fighter rows and hangars from their launch status`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"hangar","id":"hangar-1"}],"weapons":[],"class":"Carrier","name":"Test","fighters":[{"name":"fighters","type":"standard","id":"squadron-1","mods":[],"hangar":"hangar-1"}]}""",
        )
        val launched = mapOf("squadron-1" to OlympiadDawnFighterLaunchStatus(destroyed = false))

        assertThat(OlympiadDawnShipStatusPanel.fighterStatusText("squadron-1", null), equalTo("squadron-1"))
        assertThat(OlympiadDawnShipStatusPanel.fighterStatusText("squadron-1", launched["squadron-1"]), equalTo("LAUNCHED"))
        assertThat(OlympiadDawnShipStatusPanel.fighterStatusText("squadron-1", OlympiadDawnFighterLaunchStatus(destroyed = true)), equalTo("DESTROYED"))
        assertThat(OlympiadDawnShipStatusPanel.systemDisplayName(definition.systems.single(), launched), equalTo("Hangar - Launched"))
        assertThat(OlympiadDawnShipStatusPanel.systemDisplayName(definition.systems.single(), emptyMap()), equalTo("Hangar - Fighters [squadron-1]"))
    }

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
