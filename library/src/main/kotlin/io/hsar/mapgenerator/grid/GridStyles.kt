package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.image.Palette

object GridStyles {
    val GRID_PALETTE = GridPalettes.DEFAULT

    fun mapIdPadding(mapWidth: Int, mapHeight: Int): Point =
        GridLayoutScale.mapIdPadding(mapWidth, mapHeight)

    fun gridLineIdPadding(mapWidth: Int, mapHeight: Int): Point =
        GridLayoutScale.gridLinePadding(mapWidth, mapHeight)

    fun mapIdFont(mapWidth: Int, mapHeight: Int) =
        Palette.Fonts.STRONG.first.deriveFont((GridLayoutScale.MAP_ID_FONT_SIZE * fontScale(mapWidth, mapHeight)).toFloat()) to Palette.Colours.DARK

    fun gridLineIdFont(mapWidth: Int, mapHeight: Int) =
        Palette.Fonts.BASE.first.deriveFont((GridLayoutScale.GRID_LINE_FONT_SIZE * fontScale(mapWidth, mapHeight)).toFloat()) to Palette.Colours.MAIN

    fun majorGridLineIdFont(mapWidth: Int, mapHeight: Int) =
        Palette.Fonts.STRONG.first.deriveFont((GridLayoutScale.GRID_LINE_FONT_SIZE * fontScale(mapWidth, mapHeight)).toFloat()) to Palette.Fonts.STRONG.second

    val GRID_LINE_COLOUR = GRID_PALETTE.lineColour

    fun fontScale(mapWidth: Int, mapHeight: Int): Double =
        GridLayoutScale.labelScale(mapWidth, mapHeight)
}
