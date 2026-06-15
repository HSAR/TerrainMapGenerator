package io.hsar.mapgenerator

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class TerrainMapGeneratorTerrainResolutionTest {

    @Test
    fun `height samples stay stable at equivalent relative coordinates`() {
        val seed = 42L
        val smallGenerator = TerrainMapGenerator(
            metresPerPixel = 2.0,
            metresPerContour = 10.0,
            width = 5,
            height = 5,
            seed = seed,
        )
        val largeGenerator = TerrainMapGenerator(
            metresPerPixel = 1.0,
            metresPerContour = 10.0,
            width = 10,
            height = 10,
            seed = seed,
        )

        val smallImage = smallGenerator.generateHeightImage()
        val largeImage = largeGenerator.generateHeightImage()

        (0 until 5).forEach { x ->
            (0 until 5).forEach { y ->
                assertThat(largeImage.getRGB(x * 2, y * 2), equalTo(smallImage.getRGB(x, y)))
            }
        }
    }
}
