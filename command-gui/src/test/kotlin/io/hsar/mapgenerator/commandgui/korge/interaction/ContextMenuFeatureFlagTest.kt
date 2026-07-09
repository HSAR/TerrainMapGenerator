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
                label = "Experimental",
                onSelected = {},
            ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
        )

        assertThat(actions.visibleFor(emptySet<MapViewerFeatureFlag>()).map { it.label }, equalTo(listOf("Always On")))
        assertThat(
            actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).map { it.label },
            equalTo(listOf("Always On", "Experimental")),
        )
    }

    @Test
    fun `feature-gated submenus are removed when their children are hidden`() {
        val actions = listOf(
            MapPlacedObjectMenuAction(
                label = "Parent",
                onSelected = {},
                submenuActions = listOf(
                    MapPlacedObjectMenuAction(
                        label = "Child",
                        onSelected = {},
                    ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
                ),
            ),
        )

        assertThat(actions.visibleFor(emptySet<MapViewerFeatureFlag>()), equalTo(emptyList<MapPlacedObjectMenuAction>()))
        assertThat(actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).single().submenuActions.single().label, equalTo("Child"))
    }

    @Test
    fun `disabled actions remain visible when their feature flag is enabled`() {
        val actions = listOf(
            MapPlacedObjectMenuAction(
                label = "NAVREP",
                onSelected = {},
                enabled = false,
                disabledTooltipText = "This unit does not have a unitClass setting.",
            ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
        )

        val visible = actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).single()

        assertThat(visible.label, equalTo("NAVREP"))
        assertThat(visible.enabled, equalTo(false))
        assertThat(visible.disabledTooltipText, equalTo("This unit does not have a unitClass setting."))
    }

    @Test
    fun `disabled actions can explain an unrecognised unit class`() {
        val actions = listOf(
            MapPlacedObjectMenuAction(
                label = "NAVREP",
                onSelected = {},
                enabled = false,
                disabledTooltipText = "The supplied unitClass value \"XQ\" was not recognised. NAVREP is only available to units with unitClass FF, F, DD, CL, CA, CVE, CVL, or BC.",
            ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL),
        )

        val visible = actions.visibleFor(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)).single()

        assertThat(visible.enabled, equalTo(false))
        assertThat(
            visible.disabledTooltipText,
            equalTo("The supplied unitClass value \"XQ\" was not recognised. NAVREP is only available to units with unitClass FF, F, DD, CL, CA, CVE, CVL, or BC."),
        )
    }
}
