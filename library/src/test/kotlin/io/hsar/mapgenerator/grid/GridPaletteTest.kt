package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class GridPaletteTest {

    @Test
    fun `default grid palette uses the darker main colour`() {
        assertThat(GridPalettes.DEFAULT.lineColour, equalTo(Palette.Colours.MAIN))
    }

    @Test
    fun `legacy grid palette keeps the previous backing colour`() {
        assertThat(GridPalettes.LEGACY.lineColour, equalTo(Palette.Colours.BACKING))
    }
}
