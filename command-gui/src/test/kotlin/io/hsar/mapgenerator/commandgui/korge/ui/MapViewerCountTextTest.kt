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

    @Test
    fun `uses supplied irregular plural label`() {
        assertThat(
            MapViewerCountText.of(0, "additional property", "additional properties"),
            equalTo("0 additional properties"),
        )
        assertThat(
            MapViewerCountText.of(1, "additional property", "additional properties"),
            equalTo("1 additional property"),
        )
        assertThat(
            MapViewerCountText.of(2, "additional property", "additional properties"),
            equalTo("2 additional properties"),
        )
    }
}
