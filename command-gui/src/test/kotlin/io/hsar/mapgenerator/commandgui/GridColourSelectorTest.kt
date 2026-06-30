package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class GridColourSelectorTest {
    @Test
    fun `saved preset hex resolves to that preset`() {
        val mapImage = image(Color.WHITE)

        val colour = GridColourSelector.resolve("#403A3A", mapImage)

        assertThat(colour, equalTo(Palette.Colours.MAIN))
    }

    @Test
    fun `missing value selects preset with strongest average map contrast`() {
        val mapImage = image(Color.WHITE)

        val colour = GridColourSelector.resolve(null, mapImage)

        assertThat(colour, equalTo(Palette.Colours.DARK))
    }

    @Test
    fun `unsupported saved value falls back to contrast analysis`() {
        val mapImage = image(Color.BLACK)

        val colour = GridColourSelector.resolve("#FF0000", mapImage)

        assertThat(colour, equalTo(Palette.Colours.LIGHT))
    }

    @Test
    fun `hex output uses six digit uppercase colour`() {
        assertThat(GridColourSelector.toHex(Palette.Colours.DARK), equalTo("#120909"))
    }

    private fun image(color: Color): BufferedImage =
        BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB).also { image ->
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    image.setRGB(x, y, color.rgb)
                }
            }
        }
}
