package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.image.Palette
import java.awt.Color

/**
 * Describes the colours used when rendering grid overlays.
 *
 * This is intentionally small for now, but gives us a single place to vary grid styling later
 * based on image content or viewer mode.
 */
data class GridPalette(
    val lineColour: Color,
)

object GridPalettes {
    val DEFAULT = GridPalette(lineColour = Palette.Colours.MAIN)
    val LEGACY = GridPalette(lineColour = Palette.Colours.BACKING)
}
