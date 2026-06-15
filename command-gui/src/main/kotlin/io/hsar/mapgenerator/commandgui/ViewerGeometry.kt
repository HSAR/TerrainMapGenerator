package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.image.BufferedImage

internal data class ViewerGeometry(
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val mapOffsetX: Int,
    val mapOffsetY: Int,
)

internal fun geometryFor(image: BufferedImage): ViewerGeometry {
    val backgroundWidth = image.width * 2
    val backgroundHeight = image.height * 2

    return ViewerGeometry(
        backgroundWidth = backgroundWidth,
        backgroundHeight = backgroundHeight,
        mapOffsetX = backgroundWidth / 4,
        mapOffsetY = backgroundHeight / 4,
    )
}

internal fun java.awt.Rectangle.toLayerViewport(offsetX: Int, offsetY: Int) = Rectangle(
    Point((x - offsetX).toDouble(), (y - offsetY).toDouble()),
    Point((x + width - offsetX).toDouble(), (y + height - offsetY).toDouble()),
)
