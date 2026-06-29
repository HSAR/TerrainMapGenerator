package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogTitlesTest {
    @Test
    fun `unit type titles include the current unit type name`() {
        assertThat(
            MapViewerDialogTitles.unitType("Edit Unit Type", "AT Inf"),
            equalTo("Edit Unit Type - AT Inf"),
        )
    }

    @Test
    fun `blank unit type names do not add a suffix`() {
        assertThat(
            MapViewerDialogTitles.unitType("Create Unit Type", "   "),
            equalTo("Create Unit Type"),
        )
    }
}
