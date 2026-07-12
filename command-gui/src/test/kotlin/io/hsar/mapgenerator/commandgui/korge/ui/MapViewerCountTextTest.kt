package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

internal class MapViewerCountTextTest {
    @Test
    fun `uses singular label for one entry`() {
        assertThat(MapViewerCountText.of(1, "reference"), equalTo("1 reference"))
    }

    @Test
    fun `uses plural label for zero and multiple entries`() {
        assertThat(MapViewerCountText.of(0, "system"), equalTo("0 systems"))
        assertThat(MapViewerCountText.of(2, "system"), equalTo("2 systems"))
    }
}
