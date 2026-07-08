package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerUiBoxesTest {

    @Test
    fun `frame geometry uses border strips instead of a full backing rectangle`() {
        val geometry = MapViewerUiBoxes.frameGeometry(width = 120.0, height = 40.0, inset = 2.0)

        assertThat(geometry.fill, equalTo(UiFrameRect(2.0, 2.0, 116.0, 36.0)))
        assertThat(geometry.borders.size, equalTo(4))
        assertThat(geometry.borders.none { it.width == 120.0 && it.height == 40.0 }, equalTo(true))
    }

    @Test
    fun `default panel style matches dialog chrome opacity`() {
        val style = MapViewerUiBoxes.defaultStyle(active = true)

        assertThat(style.fillAlpha, equalTo(220))
        assertThat(style.borderAlpha, equalTo(MapViewerUiStateStyle.ACTIVE_ALPHA))
    }

    @Test
    fun `default button style is opaque`() {
        val style = MapViewerUiBoxes.defaultButtonStyle(active = true)

        assertThat(style.fillAlpha, equalTo(MapViewerUiStateStyle.ACTIVE_ALPHA))
        assertThat(style.borderAlpha, equalTo(MapViewerUiStateStyle.ACTIVE_ALPHA))
    }
}
