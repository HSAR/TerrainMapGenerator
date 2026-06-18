package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test

class MapViewerInteractionTest {

    @Test
    fun `freeform point spacing is scaled for normalized map coordinates`() {
        assertThat(
            minimumFreeformPointDistance(mapWidth = 1920, mapHeight = 1080),
            closeTo(2.0 / 1080.0, 0.0000001),
        )
    }
}
