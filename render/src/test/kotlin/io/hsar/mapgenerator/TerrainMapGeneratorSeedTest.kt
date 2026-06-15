package io.hsar.mapgenerator

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class TerrainMapGeneratorSeedTest {

    @Test
    fun `graph image is stable for the same seed`() {
        val seed = 42L
        val generatorA = TerrainMapGenerator(
            metresPerPixel = 2.0,
            metresPerContour = 10.0,
            width = 160,
            height = 120,
            seed = seed,
        )
        val generatorB = TerrainMapGenerator(
            metresPerPixel = 2.0,
            metresPerContour = 10.0,
            width = 160,
            height = 120,
            seed = seed,
        )

        assertThat(
            generatorA.generateGraphImage().pixelData(),
            equalTo(generatorB.generateGraphImage().pixelData())
        )
    }

    @Test
    fun `graph point count depends on physical map height rather than zoomed pixels`() {
        val seed = 42L
        val baseGenerator = TerrainMapGenerator(
            metresPerPixel = 2.0,
            metresPerContour = 10.0,
            width = 1920,
            height = 1080,
            seed = seed,
        )
        val zoomedGenerator = TerrainMapGenerator(
            metresPerPixel = 1.0,
            metresPerContour = 10.0,
            width = 3840,
            height = 2160,
            seed = seed,
        )

        assertThat(baseGenerator.graphPointCount(), equalTo(zoomedGenerator.graphPointCount()))
    }

    @Test
    fun `generated grid uses kilometre squares with two digit coordinates`() {
        val gridLayer = TerrainMapGenerator(
            metresPerPixel = 2.0,
            metresPerContour = 10.0,
            width = 1920,
            height = 1080,
            seed = 42L,
        ).generateGridLayer()

        assertThat(gridLayer.gridScale, equalTo(1_000))
        assertThat(gridLayer.detailLevel, equalTo(2))
    }

    private fun java.awt.image.BufferedImage.pixelData(): IntArray =
        IntArray(width * height).also { pixels ->
            getRGB(0, 0, width, height, pixels, 0, width)
        }
}
