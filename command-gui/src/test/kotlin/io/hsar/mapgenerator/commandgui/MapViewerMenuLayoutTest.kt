package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MapViewerMenuLayoutTest {
    @Test
    fun `root and tall menu clamp to viewer`() {
        val root = MapViewerMenuLayoutBuilder.root(790.0, 590.0, 220.0, 500.0, 800.0, 600.0)

        assertThat(root, equalTo(MapViewerChromeBounds(580.0, 100.0, 220.0, 500.0)))
    }

    @Test
    fun `child aligns to row and flips left when right side is unavailable`() {
        val parent = MapViewerChromeBounds(580.0, 100.0, 220.0, 300.0)
        val child = MapViewerMenuLayoutBuilder.child(parent, 2, 220.0, 180.0, 800.0, 600.0)

        assertThat(child.left, equalTo(352.0))
        assertThat(child.top, equalTo(192.0))
        assertThat(MapViewerMenuTokens.CLOSE_DELAY_MILLIS, equalTo(180L))
    }

    @ParameterizedTest
    @ValueSource(doubles = [1.0, 1.25, 1.5])
    fun `menu tokens preserve exact physical measurements across desktop scales`(scale: Double) {
        assertThat(MapViewerMenuTokens.ROW_HEIGHT * scale, equalTo(44.0 * scale))
        assertThat(MapViewerMenuTokens.PADDING * scale, equalTo(4.0 * scale))
        assertThat(MapViewerMenuTokens.BORDER_WIDTH * scale, equalTo(2.0 * scale))
        assertThat(MapViewerMenuTokens.SUBMENU_CORRIDOR * scale, equalTo(8.0 * scale))
    }
}
