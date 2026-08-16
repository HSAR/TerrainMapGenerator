package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerLaunchBackgroundTest {
    @Test
    fun `launch background retains viewport aspect and seeded topography metadata`() {
        val background = MapViewerLaunchBackgroundRenderer.render(
            viewportWidth = 80,
            viewportHeight = 48,
            seed = 1234L,
            rasterScale = 0.25,
        )

        assertThat(background.image.width, equalTo(20))
        assertThat(background.image.height, equalTo(12))
        assertThat(background.terrainProfile.length, greaterThan(0))
        assertThat(background.seed, equalTo(1234L))
    }
}
