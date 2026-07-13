package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import java.awt.Color

internal data class GridOverlayState(
    val viewport: Rectangle,
    val mapWorldX: Double,
    val mapWorldY: Double,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val scale: Double,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val visibleCanvasLeft: Double,
    val visibleCanvasTop: Double,
    val visibleCanvasRight: Double,
    val visibleCanvasBottom: Double,
    val activeReference: GridCursorReference?,
    val measurement: GridMeasurement?,
    val gridLayer: GridLayer,
)

internal data class GridCursorReference(
    val screenX: Double,
    val screenY: Double,
    val text: String,
    val color: Color? = null,
)
