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

        assertThat(layout.bounds.contains(Vector2D(800.0, 30.0)), equalTo(true))
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

        assertThat(collapsed.bounds.contains(Vector2D(800.0, 90.0)), equalTo(false))
        assertThat(expanded.bounds.contains(Vector2D(800.0, 90.0)), equalTo(true))
    }

    @Test
    fun `drawing colour row expands click blocking area`() {
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

        assertThat(collapsed.bounds.contains(Vector2D(800.0, 120.0)), equalTo(false))
        assertThat(expanded.bounds.contains(Vector2D(800.0, 120.0)), equalTo(true))
    }

    @Test
    fun `toolbar widens to fit added drawing modes dynamically`() {
        val compact = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = true,
            drawingModeCount = 3,
        )
        val expanded = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = true,
            drawingModeCount = 4,
        )

        assertThat(expanded.bounds.width > compact.bounds.width, equalTo(true))
    }

    @Test
    fun `top row remains centered when drawing mode row is shown`() {
        val collapsed = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = false,
            toolCount = 3,
            drawingModeCount = 4,
        )
        val expanded = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = true,
            toolCount = 3,
            drawingModeCount = 4,
        )

        assertThat(expanded.toolRowLeft, equalTo(collapsed.toolRowLeft))
    }

    @Test
    fun `drawing colour row is centered independently`() {
        val layout = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = 1600,
            viewportHeight = 900,
            drawingModesVisible = true,
            drawingColourCount = 6,
        )

        assertThat(layout.drawingColourRowLeft, equalTo((1600.0 - ((6 * 28.0) + (5 * 10.0))) * 0.5))
    }
}
