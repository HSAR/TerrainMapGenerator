package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerSidebarLayoutBuilderTest {

    @Test
    fun `expanded sidebar blocks clicks in both the button and the panel`() {
        val layout = MapViewerSidebarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            collapsed = false,
        )

        assertThat(layout.blocks(Vector2D(1500.0, 30.0)), equalTo(true))
        assertThat(layout.blocks(Vector2D(1400.0, 120.0)), equalTo(true))
    }

    @Test
    fun `collapsed sidebar only blocks the toggle button`() {
        val layout = MapViewerSidebarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            collapsed = true,
        )

        assertThat(layout.panelBounds == null, equalTo(true))
        assertThat(layout.blocks(Vector2D(1500.0, 30.0)), equalTo(true))
        assertThat(layout.blocks(Vector2D(1400.0, 120.0)), equalTo(false))
    }
}
