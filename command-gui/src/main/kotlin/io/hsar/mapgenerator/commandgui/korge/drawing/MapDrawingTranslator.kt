package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta

internal object MapDrawingTranslator {
    fun translate(drawing: MapDrawing, delta: MapCoordinateDelta): MapDrawing =
        when (drawing) {
            is FreeformDrawing -> drawing.copy(points = drawing.points.map { it + delta })
            is RectangleDrawing -> drawing.copy(start = drawing.start + delta, end = drawing.end + delta)
            is CircleDrawing -> drawing.copy(center = drawing.center + delta)
        }

    private operator fun MapDrawingPoint.plus(delta: MapCoordinateDelta): MapDrawingPoint =
        MapDrawingPoint(
            x = x + delta.x,
            y = y + delta.y,
        )
}
