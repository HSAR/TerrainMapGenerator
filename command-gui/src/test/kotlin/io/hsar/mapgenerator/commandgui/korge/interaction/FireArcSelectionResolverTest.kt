package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class FireArcSelectionResolverTest {
    @Test
    fun `all units and none ignore selection`() {
        assertThat(resolve(FireArcDisplayMode.AllUnits, emptyList(), null, "target"), equalTo(true))
        assertThat(resolve(FireArcDisplayMode.None, listOf(UnitAffiliation.Friend), UnitAffiliation.Hostile, "target"), equalTo(false))
    }

    @Test
    fun `selected only requires a single affiliation`() {
        assertThat(resolve(FireArcDisplayMode.SelectedOnly, listOf(UnitAffiliation.Friend), UnitAffiliation.Friend, "selected"), equalTo(true))
        assertThat(resolve(FireArcDisplayMode.SelectedOnly, listOf(UnitAffiliation.Friend, UnitAffiliation.Hostile), UnitAffiliation.Friend, "selected"), equalTo(false))
        assertThat(resolve(FireArcDisplayMode.SelectedOnly, emptyList(), UnitAffiliation.Friend, "selected"), equalTo(false))
    }

    @Test
    fun `friendlies use the selected affiliation`() {
        assertThat(resolve(FireArcDisplayMode.FriendliesOfSelected, listOf(UnitAffiliation.Friend), UnitAffiliation.Friend, "target"), equalTo(true))
        assertThat(resolve(FireArcDisplayMode.FriendliesOfSelected, listOf(UnitAffiliation.Neutral), UnitAffiliation.Friend, "target"), equalTo(false))
        assertThat(resolve(FireArcDisplayMode.FriendliesOfSelected, listOf(null), null, "target"), equalTo(true))
    }

    @Test
    fun `hostiles are reciprocal only for friend and hostile`() {
        assertThat(resolve(FireArcDisplayMode.HostilesOfSelected, listOf(UnitAffiliation.Friend), UnitAffiliation.Hostile, "target"), equalTo(true))
        assertThat(resolve(FireArcDisplayMode.HostilesOfSelected, listOf(UnitAffiliation.Hostile), UnitAffiliation.Friend, "target"), equalTo(true))
        assertThat(resolve(FireArcDisplayMode.HostilesOfSelected, listOf(UnitAffiliation.Neutral), UnitAffiliation.Friend, "target"), equalTo(false))
        assertThat(resolve(FireArcDisplayMode.HostilesOfSelected, listOf(null), UnitAffiliation.Hostile, "target"), equalTo(false))
    }

    @Test
    fun `settings fallback to all units`() {
        assertThat(FireArcDisplayMode.fromSettingsValue(null), equalTo(FireArcDisplayMode.AllUnits))
        assertThat(FireArcDisplayMode.fromSettingsValue("not-valid"), equalTo(FireArcDisplayMode.AllUnits))
    }

    private fun resolve(
        mode: FireArcDisplayMode,
        selectedAffiliations: List<UnitAffiliation?>,
        targetAffiliation: UnitAffiliation?,
        targetId: String,
    ): Boolean = FireArcSelectionResolver.shouldDraw(
        mode = mode,
        selectedUnitIds = setOf("selected"),
        selectedAffiliations = selectedAffiliations,
        targetId = targetId,
        targetAffiliation = targetAffiliation,
    )
}
