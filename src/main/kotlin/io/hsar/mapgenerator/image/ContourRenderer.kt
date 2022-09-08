package io.hsar.mapgenerator.image

import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.LookupOp
import java.awt.image.ShortLookupTable
import kotlin.math.roundToInt


class ContourRenderer(val image: BufferedImage) {

    /**
     * Draw contour lines on the image where each contour represents one of the given values.
     */
    fun drawContours(values: List<Double>) = values.map { drawContour(it) }.last()

    fun drawContour(value: Double): BufferedImage {
        val actualThreshold = (value * RGB_MAX).roundToInt()
        val thresholdTable: ShortArray = (1..RGB_MAX).map { level ->
            if (level < actualThreshold) 0.toShort() else 255.toShort()
        }.toShortArray()

        val thresholdOp: BufferedImageOp = LookupOp(ShortLookupTable(0, thresholdTable), null)
        val thresholdImage = thresholdOp.filter(image, null)

        for (x in 0..image.width step GRID_STEP_X) {
            for (y in 0..image.height step GRID_STEP_Y) {
                // seek the value and navigate along it with gaussian sampling
            }
        }

        return thresholdImage
    }

    companion object {
        const val RGB_MAX = 255
        const val GRID_STEP_X = 10
        const val GRID_STEP_Y = 10
    }

}