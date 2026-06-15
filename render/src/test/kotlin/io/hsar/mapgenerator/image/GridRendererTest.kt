package io.hsar.mapgenerator.image

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.Random

class GridRendererTest {

    @Test
    fun `grid rendering is stable for the same seed`() {
        val first = renderGrid(42L)
        val second = renderGrid(42L)

        assertThat(first.pixelData(), equalTo(second.pixelData()))
    }

    private fun renderGrid(seed: Long): BufferedImage {
        val imageBuilder = ImageBuilder(240, 180).also { it.fillTransparent() }
        GridRenderer(imageBuilder, Random(seed)).draw(
            GZDand100kmGSID = "25F XK",
            gridScale = GridRenderer.HUNDRED_M_GRID_SQUARES,
            metresPerPixel = 2.0,
        )
        return imageBuilder.build()
    }

    private fun BufferedImage.pixelData(): IntArray =
        IntArray(width * height).also { pixels ->
            getRGB(0, 0, width, height, pixels, 0, width)
        }
}
