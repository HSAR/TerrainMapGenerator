package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogActionsTest {

    @Test
    fun `one action is centered`() {
        assertThat(MapViewerDialogActions.centerRatios(1), equalTo(listOf(0.5)))
    }

    @Test
    fun `two actions are evenly spaced`() {
        assertThat(MapViewerDialogActions.centerRatios(2), equalTo(listOf(1.0 / 3.0, 2.0 / 3.0)))
    }

    @Test
    fun `three actions are evenly spaced`() {
        assertThat(MapViewerDialogActions.centerRatios(3), equalTo(listOf(0.25, 0.5, 0.75)))
    }
}
