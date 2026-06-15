package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.image.Palette
import java.awt.Font

object GridStyles {
    val GRID_PALETTE = GridPalettes.DEFAULT

    val MAP_ID_PADDING = Point(25.0, 50.0)
    val MAP_ID_FONT = Font("TVNordEF-BoldCon", Font.PLAIN, 26) to Palette.Colours.MAIN

    val GRID_LINE_ID_PADDING = Point(55.0, 40.0)
    val GRID_LINE_ID_FONT = Font("TVNordEF-RegularCon", Font.PLAIN, 22) to Palette.Colours.MAIN

    val GRID_LINE_COLOUR = GRID_PALETTE.lineColour
}
