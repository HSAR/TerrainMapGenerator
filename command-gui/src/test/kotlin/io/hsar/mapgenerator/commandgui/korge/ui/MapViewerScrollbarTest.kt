package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerScrollbarTest {

    @Test
    fun `drag offset increases when pointer moves downward`() {
        val bounds = ScreenBounds(0.0, 0.0, 18.0, 200.0)
        val state = MapViewerScrollState(viewportHeight = 100.0, contentHeight = 300.0)

        val dragOffset = MapViewerScrollbar.dragOffset(
            bounds = bounds,
            state = state,
            dragStartPointerY = 20.0,
            dragStartOffset = 0.0,
            currentPointerY = 60.0,
        )

        assertThat(dragOffset, greaterThan(0.0))
    }
}
