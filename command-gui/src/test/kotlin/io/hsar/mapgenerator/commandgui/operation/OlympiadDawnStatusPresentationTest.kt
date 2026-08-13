package io.hsar.mapgenerator.commandgui.operation

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OlympiadDawnStatusPresentationTest {
    @Test
    fun `formats fighter and hangar state without renderer types`() {
        val status = OlympiadDawnFighterLaunchStatus(destroyed = false)
        val system = OlympiadDawnShipSystem("hangar", "Hangar - Fighters [squadron-1]", setOf("squadron-1"))

        assertThat(OlympiadDawnStatusPresentation.fighterStatusText("squadron-1", null), equalTo("squadron-1"))
        assertThat(OlympiadDawnStatusPresentation.fighterStatusText("squadron-1", status), equalTo("LAUNCHED"))
        assertThat(
            OlympiadDawnStatusPresentation.fighterStatusText("squadron-1", status.copy(destroyed = true)),
            equalTo("DESTROYED"),
        )
        assertThat(OlympiadDawnStatusPresentation.systemDisplayName(system, mapOf("squadron-1" to status)), equalTo("Hangar - Launched"))
        assertThat(OlympiadDawnStatusPresentation.systemDisplayName(system, emptyMap()), equalTo(system.name))
    }

    @Test
    fun `distributes hull points across the requested rows`() {
        assertThat(OlympiadDawnStatusPresentation.hullRowSizes(5, 4), equalTo(listOf(2, 1, 1, 1)))
        assertThat(OlympiadDawnStatusPresentation.hullRowSizes(6, 4), equalTo(listOf(2, 2, 1, 1)))
        assertThat(OlympiadDawnStatusPresentation.hullRowSizes(6, 0), equalTo(emptyList()))
    }
}
