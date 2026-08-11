package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.ViewerPoint

internal object ComposeDrawingPresentation {
    const val screenStrokeWidth = 3f
    const val labelGap = 4.0

    fun worldStrokeWidth(zoom: Double) = screenStrokeWidth / zoom.coerceAtLeast(.0001).toFloat()

    fun minimumFreeformMapDistance(zoom: Double, mapWidth: Int, mapHeight: Int) =
        2.0 / (zoom.coerceAtLeast(.0001) * minOf(mapWidth, mapHeight).coerceAtLeast(1))

    fun labelTopLeft(bounds: MapSelectionScreenBounds, measuredWidth: Int, measuredHeight: Int) = ViewerPoint(
        x = (bounds.left + bounds.right - measuredWidth) * .5,
        y = bounds.top - measuredHeight - labelGap,
    )
}
