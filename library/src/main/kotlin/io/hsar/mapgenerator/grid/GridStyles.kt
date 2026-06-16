package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.image.Palette

object GridStyles {
    val GRID_PALETTE = GridPalettes.DEFAULT

    val MAP_ID_PADDING = Point(25.0, 50.0)
    val MAP_ID_FONT = Palette.Fonts.STRONG.first.deriveFont(26f) to Palette.Colours.DARK

    val GRID_LINE_ID_PADDING = Point(55.0, 40.0)
    val GRID_LINE_ID_FONT = Palette.Fonts.BASE.first.deriveFont(22f) to Palette.Colours.MAIN
    val MAJOR_GRID_LINE_ID_FONT = Palette.Fonts.STRONG.first.deriveFont(22f) to Palette.Fonts.STRONG.second

    val GRID_LINE_COLOUR = GRID_PALETTE.lineColour
}
