package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerInputClickPlacementTest {
    @Test
    fun `click inside rendered text does not select the end`() {
        val isBelowText = MapViewerInputClickPlacement.isBelowText(
            localY = 17.0,
            visualText = "Alpha\nbeta",
            lineHeight = 12.0,
            textTop = 2.0,
        )

        assertThat(isBelowText, equalTo(false))
    }

    @Test
    fun `click below rendered text selects the end`() {
        val isBelowText = MapViewerInputClickPlacement.isBelowText(
            localY = 27.0,
            visualText = "Alpha\nbeta",
            lineHeight = 12.0,
            textTop = 2.0,
        )

        assertThat(isBelowText, equalTo(true))
    }
}
