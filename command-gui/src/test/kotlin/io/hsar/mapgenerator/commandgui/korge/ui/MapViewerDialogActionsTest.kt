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

    @Test
    fun `footer uses row layout when wide enough`() {
        assertThat(MapViewerDialogActions.footerLayout(400.0, 2), equalTo(MapViewerDialogFooterLayout.ROW))
    }

    @Test
    fun `footer falls back to column layout when too narrow`() {
        assertThat(MapViewerDialogActions.footerLayout(200.0, 2), equalTo(MapViewerDialogFooterLayout.COLUMN))
    }

    @Test
    fun `footer minimum width is a single button width`() {
        assertThat(MapViewerDialogActions.minimumWidth(2), equalTo(MapViewerDialogActions.BUTTON_WIDTH))
    }

    @Test
    fun `dialog actions are enabled by default`() {
        val action = MapViewerDialogAction("Save") {}

        assertThat(action.enabled(), equalTo(true))
    }

    @Test
    fun `dialog actions can be disabled by predicate`() {
        val action = MapViewerDialogAction("Save", enabled = { false }) {}

        assertThat(action.enabled(), equalTo(false))
    }
}
