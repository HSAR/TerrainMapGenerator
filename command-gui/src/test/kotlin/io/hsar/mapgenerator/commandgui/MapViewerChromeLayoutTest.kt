package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MapViewerChromeLayoutTest {
    @Test
    fun `positions four floating buttons with legacy padding and gaps`() {
        val layout = MapViewerChromeLayoutBuilder.build(1200.0)

        assertThat(layout.game, equalTo(MapViewerChromeBounds(16.0, 16.0, 120.0, 40.0)))
        assertThat(layout.notes.left, equalTo(148.0))
        assertThat(layout.units.left, equalTo(1064.0))
        assertThat(layout.graphics.left, equalTo(932.0))
        assertThat(layout.toolRail.top, equalTo(16.0))
    }

    @Test
    fun `moves centred tool rail below buttons when groups intersect`() {
        val layout = MapViewerChromeLayoutBuilder.build(800.0)

        assertThat(layout.toolRail.top, equalTo(68.0))
    }

    @ParameterizedTest
    @ValueSource(doubles = [1.0, 1.25, 1.5])
    fun `logical chrome geometry converts exactly at supported desktop scales`(scale: Double) {
        val layout = MapViewerChromeLayoutBuilder.build(1200.0)

        assertThat(layout.game.left * scale, equalTo(16.0 * scale))
        assertThat(layout.game.width * scale, equalTo(120.0 * scale))
        assertThat(layout.notes.left * scale, equalTo(148.0 * scale))
        assertThat(layout.toolRail.top * scale, equalTo(16.0 * scale))
    }
}
