package io.hsar.mapgenerator.image

import io.hsar.mapgenerator.image.CellImageRenderer.Palette.COLOUR_MAP
import io.hsar.mapgenerator.map.Cell
import io.hsar.mapgenerator.randomness.NoiseGenerator
import java.awt.Color
import kotlin.math.abs

object CellImageRenderer {
    fun drawCell(imageBuilder: ImageBuilder, cell: Cell) = with(imageBuilder) {
        drawShapeFill(cell.shape, determineCellColor(cell))
        drawShapeOutline(cell.shape, Color.DARK_GRAY)
        drawPoint(cell.site, color = Color.RED)
    }

    private fun determineCellColor(cell: Cell): Color = cell.shape.map { (x, y) -> determineColor(NoiseGenerator.DEFAULT.generatePoint(x, y)) }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }!!
        .key

    /**
     * Based on a Double value between 0.0 and 1.0, issue a colour based on linearly rationing out the palette colours.
     */
    private fun determineColor(value: Double): Color {
        // Select closest key by smallest difference
        val key = COLOUR_MAP.keys.minByOrNull { v -> abs(v - value) }!!
        return COLOUR_MAP.getOrElse(key) { throw IllegalStateException("Failed to find a matching color for value $value.") }
    }

    object Palette {
        val COLORS = listOf(
            Color.decode("#276A88"), // Blue
            Color.decode("#120909"),
            Color.decode("#403A3A"),
            Color.decode("#706B6B"),
            Color.decode("#9F9D9C"),
            Color.decode("#FFFFFF"),
        )

        val COLOUR_MAP: Map<Double, Color> = COLORS
            .mapIndexed { index, color ->
                (index / COLORS.size.toDouble()) to color
            }.toMap()
    }
}