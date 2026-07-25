package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerToolTest {

    @Test
    fun `only select tool allows placement dragging`() {
        assertThat(MapViewerTool.SELECT.canStartPlacementDrag(), equalTo(true))
        assertThat(MapViewerTool.MEASURE.canStartPlacementDrag(), equalTo(false))
        assertThat(MapViewerTool.DRAW.canStartPlacementDrag(), equalTo(false))
    }
}
