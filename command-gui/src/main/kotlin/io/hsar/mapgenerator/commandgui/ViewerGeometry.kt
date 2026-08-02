package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import java.awt.image.BufferedImage

/**
 * Describes the off-screen canvas used by the viewer around the rendered map image.
 *
 * @property backgroundWidth Total drawable width of the padded background area.
 * @property backgroundHeight Total drawable height of the padded background area.
 * @property mapOffsetX Horizontal position where the top-left corner of the map image is placed.
 * @property mapOffsetY Vertical position where the top-left corner of the map image is placed.
 */
internal data class ViewerGeometry(
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val mapOffsetX: Int,
    val mapOffsetY: Int,
)

/**
 * Builds the viewer geometry for a rendered map image.
 *
 * The viewer keeps the map centered inside a padded black background so the image can be
 * panned freely. The background is 166% of the image size on both axes, leaving 33% extra
 * space on each side of the centered image.
 */
internal fun geometryFor(image: BufferedImage): ViewerGeometry {
    val backgroundWidth = (image.width * 1.66).toInt()
    val backgroundHeight = (image.height * 1.66).toInt()

    return ViewerGeometry(
        backgroundWidth = backgroundWidth,
        backgroundHeight = backgroundHeight,
        mapOffsetX = (backgroundWidth - image.width) / 2,
        mapOffsetY = (backgroundHeight - image.height) / 2,
    )
}

/** Normalized map-coordinate bounds of the padded viewer background. */
fun viewerBackgroundBounds(imageWidth: Int, imageHeight: Int): MapCoordinateBounds {
    val backgroundWidth = (imageWidth * VIEWER_BACKGROUND_SCALE).toInt()
    val backgroundHeight = (imageHeight * VIEWER_BACKGROUND_SCALE).toInt()
    val offsetX = (backgroundWidth - imageWidth) / 2
    val offsetY = (backgroundHeight - imageHeight) / 2
    return MapCoordinateBounds(
        left = -offsetX.toDouble() / imageWidth,
        top = -offsetY.toDouble() / imageHeight,
        right = (backgroundWidth - offsetX).toDouble() / imageWidth,
        bottom = (backgroundHeight - offsetY).toDouble() / imageHeight,
    )
}

internal fun java.awt.Rectangle.toLayerViewport(offsetX: Int, offsetY: Int) = Rectangle(
    Point((x - offsetX).toDouble(), (y - offsetY).toDouble()),
    Point((x + width - offsetX).toDouble(), (y + height - offsetY).toDouble()),
)

private const val VIEWER_BACKGROUND_SCALE = 1.66
