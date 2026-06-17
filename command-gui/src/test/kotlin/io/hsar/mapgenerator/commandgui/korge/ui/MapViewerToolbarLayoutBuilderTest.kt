package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerToolbarLayoutBuilderTest {

    @Test
    fun `top level toolbar blocks clicks in its button row`() {
        val layout = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
        )

        assertThat(layout.contains(Vector2D(800.0, 30.0)), equalTo(true))
    }

    @Test
    fun `drawing mode row expands click blocking area`() {
        val collapsed = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = false,
        )
        val expanded = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = true,
        )

        assertThat(collapsed.contains(Vector2D(800.0, 90.0)), equalTo(false))
        assertThat(expanded.contains(Vector2D(800.0, 90.0)), equalTo(true))
    }
}
