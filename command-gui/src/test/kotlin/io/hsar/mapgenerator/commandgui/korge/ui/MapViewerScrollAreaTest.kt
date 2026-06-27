package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerScrollAreaTest {

    @Test
    fun `scroll offset clamps to available content`() {
        val state = MapViewerScrollState(viewportHeight = 100.0, contentHeight = 250.0)

        assertThat(state.scrolledBy(1000.0).offset, equalTo(150.0))
        assertThat(state.scrolledBy(-1000.0).offset, equalTo(0.0))
    }

    @Test
    fun `scrolling is disabled when content fits`() {
        val state = MapViewerScrollState(viewportHeight = 250.0, contentHeight = 100.0)

        assertThat(state.needsScrolling, equalTo(false))
        assertThat(state.maxOffset, equalTo(0.0))
    }

    @Test
    fun `thumb gets smaller when content is taller than viewport`() {
        val bounds = ScreenBounds(0.0, 0.0, 18.0, 200.0)
        val state = MapViewerScrollState(viewportHeight = 200.0, contentHeight = 400.0)

        val thumb = MapViewerScrollbar.thumbBounds(bounds, state)

        assertThat(thumb.height, greaterThan(0.0))
        assertThat(thumb.height < bounds.height, equalTo(true))
    }

    @Test
    fun `thumb moves with scroll offset`() {
        val bounds = ScreenBounds(0.0, 0.0, 18.0, 200.0)
        val top = MapViewerScrollbar.thumbBounds(bounds, MapViewerScrollState(200.0, 400.0, 0.0))
        val bottom = MapViewerScrollbar.thumbBounds(bounds, MapViewerScrollState(200.0, 400.0, 200.0))

        assertThat(bottom.top > top.top, equalTo(true))
    }
}
