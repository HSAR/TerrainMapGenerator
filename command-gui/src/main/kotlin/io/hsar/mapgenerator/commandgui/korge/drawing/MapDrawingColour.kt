package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.image.Palette
import java.awt.Color

internal enum class MapDrawingColour(
    val label: String,
    val color: Color,
) {
    RED("Red", Palette.Colours.RED),
    BLUE("Blue", Palette.Colours.BLUE),
    GREEN("Green", Palette.Colours.GREEN),
    YELLOW("Yellow", Palette.Colours.YELLOW),
    WHITE("White", Palette.Colours.WHITE),
    LIGHT("Light", Palette.Colours.LIGHT),
    MAIN("Main", Palette.Colours.MAIN),
    DARK("Dark", Palette.Colours.DARK),
}
