package io.hsar.mapgenerator.grid

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.Random

class GridLayerBuilderTest {

    @Test
    fun `grid layer layout is stable for the same seed`() {
        val seed = 42L

        val first = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 100,
            metresPerPixel = 2.0,
            random = Random(seed),
        )
        val second = GridLayerBuilder.build(
            width = 1920,
            height = 1080,
            gridScale = 100,
            metresPerPixel = 2.0,
            random = Random(seed),
        )

        assertThat(first, equalTo(second))
    }
}
