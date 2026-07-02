package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class GridLinePlacement(
    val verticalLines: List<GridPlacedLine>,
    val horizontalLines: List<GridPlacedLine>,
)

internal data class GridPlacedLine(
    val id: Double,
    val mapPosition: Double,
)

internal object GridLinePlacementBuilder {
    fun build(viewport: Rectangle, layer: GridLayer): GridLinePlacement =
        GridLinePlacement(
            verticalLines = lineIndices(viewport.topLeft.x, viewport.topRight.x, layer.verticalLineCount, verticalLineStart(layer), layer)
                .map { lineIndex ->
                    GridPlacedLine(
                        id = layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) + lineIndex,
                        mapPosition = verticalLineStart(layer) + lineIndex * layer.pixelsPerLine,
                    )
                },
            horizontalLines = lineIndices(viewport.topLeft.y, viewport.bottomRight.y, layer.horizontalLineCount, horizontalLineStart(layer), layer)
                .map { lineIndex ->
                    GridPlacedLine(
                        id = layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale) - lineIndex,
                        mapPosition = horizontalLineStart(layer) + lineIndex * layer.pixelsPerLine,
                    )
                },
        )

    private fun lineIndices(viewportStart: Double, viewportEnd: Double, lineCount: Int, firstPosition: Double, layer: GridLayer): IntRange {
        val start = max(0, ceil((viewportStart - firstPosition) / layer.pixelsPerLine).toInt())
        val end = min(lineCount, floor((viewportEnd - firstPosition) / layer.pixelsPerLine).toInt())
        return if (start <= end) start..end else IntRange.EMPTY
    }

    private fun verticalLineStart(layer: GridLayer): Double =
        (layer.verticalLineNumberStart.lineNumberCeil(layer.gridScale) - layer.verticalLineNumberStart.lineNumber(layer.gridScale)) *
                layer.pixelsPerLine

    private fun horizontalLineStart(layer: GridLayer): Double =
        (layer.horizontalLineNumberStart.lineNumber(layer.gridScale) - layer.horizontalLineNumberStart.lineNumberFloor(layer.gridScale)) *
                layer.pixelsPerLine
}
