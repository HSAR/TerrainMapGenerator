package io.hsar.mapgenerator.image

import java.awt.image.BufferedImage

class ContourRenderer(val image: BufferedImage) {

    /**
     * Draw contour lines on the image where each contour represents one of the given values.
     */
    fun drawContours(values: List<Double>) = values.map { drawContour(it) }.last()

    fun drawContour(value: Double): BufferedImage {
        for (x in 0..image.width step GRID_STEP_X) {
            for (y in 0..image.height step GRID_STEP_Y) {
                // seek the value and navigate along it with gaussian sampling
            }
        }
        return image
    }

    companion object {
        const val GRID_STEP_X = 10
        const val GRID_STEP_Y = 10
    }

}