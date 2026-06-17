package io.hsar.mapgenerator.commandgui.korge.drawing

import java.awt.Color

internal data class MapDrawingPoint(
    val x: Double,
    val y: Double,
)

internal sealed interface MapDrawing {
    val color: Color
}

internal data class FreeformDrawing(
    val points: List<MapDrawingPoint>,
    override val color: Color,
) : MapDrawing

internal data class RectangleDrawing(
    val start: MapDrawingPoint,
    val end: MapDrawingPoint,
    override val color: Color,
) : MapDrawing

internal data class CircleDrawing(
    val center: MapDrawingPoint,
    val edge: MapDrawingPoint,
    override val color: Color,
) : MapDrawing

internal data class MapDrawingState(
    val drawings: List<MapDrawing> = emptyList(),
    val preview: MapDrawing? = null,
)
