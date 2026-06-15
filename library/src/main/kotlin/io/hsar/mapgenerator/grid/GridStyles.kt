package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.image.Palette
import java.awt.Font

object GridStyles {
    val MAP_ID_PADDING = Point(25.0, 50.0)
    val MAP_ID_FONT = Font("TVNordEF-BoldCon", Font.PLAIN, 24) to Palette.Colours.BACKING

    val GRID_LINE_ID_PADDING = Point(55.0, 40.0)
    val GRID_LINE_ID_FONT = Font("TVNordEF-RegularCon", Font.PLAIN, 18) to Palette.Colours.BACKING

    val GRID_LINE_COLOUR = Palette.Colours.BACKING
}
