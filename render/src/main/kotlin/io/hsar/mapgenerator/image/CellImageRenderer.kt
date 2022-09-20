package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.TerrainMapGenerator
import io.hsar.mapgenerator.graph.Cell
import io.hsar.mapgenerator.random.NoiseGenerator
import java.awt.Color
import kotlin.math.abs

class CellImageRenderer(private val imageBuilder: io.hsar.mapgenerator.image.ImageBuilder) {
    fun drawCell(cell: Cell) = with(imageBuilder) {
//        drawShapeFill(cell.shape, determineCellColor(cell))
//        drawShapeOutline(cell.shape, Color.DARK_GRAY)
        drawPoint(cell.site, color = Color.RED)
    }

    private fun determineCellColor(cell: Cell): Color = cell.shape
        .map { (x, y) ->
            determineColor(
                NoiseGenerator.Companion.DEFAULT.generatePoint(
                    x, y,
                    TerrainMapGenerator.SAMPLE_SIZE
                )
            )
        }
        .minByOrNull { CellPalette.COLORS.indexOf(it) }!!

    /**
     * Based on a Double value between 0.0 and 1.0, issue a colour based on linearly rationing out the palette colours.
     */
    private fun determineColor(value: Double): Color {
        // Select closest key by smallest difference
        val key = CellPalette.COLOR_MAP.keys.minByOrNull { v -> abs(v - value) }!!
        return CellPalette.COLOR_MAP.getOrElse(key) { throw IllegalStateException("Failed to find a matching color for value $value.") }
    }

    object CellPalette {
        val COLORS = listOf(
            Palette.Colours.BLUE, // Blue
            Palette.Colours.DARK,
            Palette.Colours.MAIN,
            Palette.Colours.LIGHT,
            Palette.Colours.BACKING,
            Palette.Colours.WHITE,
        )

        private val segments = COLORS.size.toDouble()

        val COLOR_MAP: Map<Double, Color> = COLORS
            .mapIndexed { index, color ->
                val endOfSegment = (index + 1) / segments
                val beginningOfSegment = index / segments
                val midPoint = (beginningOfSegment + endOfSegment) / 2
                midPoint to color
            }.toMap()
    }
}