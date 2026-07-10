package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ContextMenuFeatureFlagTest {

    @Test
    fun `feature-gated menu items are hidden until the flag is enabled`() {
        val actions = listOf(
            MapPlacedObjectMenuAction(label = "Always On", onSelected = {}),
            MapPlacedObjectMenuAction(
                label = "SITREP",
                onSelected = {},
            ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
        )

        assertThat(actions.visibleFor(emptySet<MapViewerFeatureFlag>()).map { it.label }, equalTo(listOf("Always On")))
        assertThat(
            actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).map { it.label },
            equalTo(listOf("Always On", "SITREP")),
        )
    }

    @Test
    fun `sitrep remains a single action when visible`() {
        val actions = listOf(
            MapPlacedObjectMenuAction(
                label = "SITREP",
                onSelected = {},
            ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
        )

        val visible = actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).single()

        assertThat(visible.label, equalTo("SITREP"))
        assertThat(visible.submenuActions, equalTo(emptyList<MapPlacedObjectMenuAction>()))
    }
}
