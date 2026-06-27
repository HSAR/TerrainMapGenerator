package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test

class MapViewerDialogLayoutBuilderTest {

    @Test
    fun `default bounds are centred in viewport`() {
        val bounds = MapViewerDialogLayoutBuilder.defaultBounds(
            viewportWidth = 1000,
            viewportHeight = 800,
            preferredWidth = 400.0,
            preferredHeight = 300.0,
        )

        assertThat(bounds.left, equalTo(300.0))
        assertThat(bounds.top, equalTo(250.0))
    }

    @Test
    fun `dragged bounds are clamped inside viewport`() {
        val bounds = MapViewerDialogBounds(-200.0, -100.0, 420.0, 360.0)

        val clamped = with(MapViewerDialogLayoutBuilder) {
            bounds.clampedTo(viewportWidth = 1000, viewportHeight = 800)
        }

        assertThat(clamped.left, greaterThanOrEqualTo(MapViewerDialogLayoutBuilder.OUTER_PADDING))
        assertThat(clamped.top, greaterThanOrEqualTo(MapViewerDialogLayoutBuilder.OUTER_PADDING))
    }

    @Test
    fun `resized bounds respect minimum size and viewport maximum`() {
        val small = MapViewerDialogLayoutBuilder.clampSize(
            size = MapViewerDialogSize(100.0, 100.0),
            viewportWidth = 1000,
            viewportHeight = 800,
        )
        val large = MapViewerDialogLayoutBuilder.clampSize(
            size = MapViewerDialogSize(2000.0, 2000.0),
            viewportWidth = 1000,
            viewportHeight = 800,
        )

        assertThat(small.width, equalTo(MapViewerDialogLayoutBuilder.MIN_WIDTH))
        assertThat(small.height, equalTo(MapViewerDialogLayoutBuilder.MIN_HEIGHT))
        assertThat(large.width, lessThanOrEqualTo(1000.0 - MapViewerDialogLayoutBuilder.OUTER_PADDING * 2.0))
        assertThat(large.height, lessThanOrEqualTo(800.0 - MapViewerDialogLayoutBuilder.OUTER_PADDING * 2.0))
    }

    @Test
    fun `content viewport starts below title bar`() {
        val layout = MapViewerDialogLayoutBuilder.build(
            MapViewerDialogBounds(0.0, 0.0, 460.0, 540.0),
        )

        assertThat(
            layout.contentViewport.top,
            equalTo(MapViewerDialogLayoutBuilder.BORDER_INSET + MapViewerDialogLayoutBuilder.TITLE_BAR_HEIGHT + MapViewerDialogLayoutBuilder.FOOTER_TOP_GAP),
        )
    }

    @Test
    fun `footer appears only when action buttons exist`() {
        val withoutActions = MapViewerDialogLayoutBuilder.build(MapViewerDialogBounds(0.0, 0.0, 460.0, 540.0))
        val withActions = MapViewerDialogLayoutBuilder.build(MapViewerDialogBounds(0.0, 0.0, 460.0, 540.0), actionButtonCount = 2)

        assertThat(withoutActions.footer, equalTo(null))
        assertThat(withActions.footer != null, equalTo(true))
    }
}
