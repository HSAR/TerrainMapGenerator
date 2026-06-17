package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32

internal object MapViewerIconFit {
    fun inside(bitmap: Bitmap32, width: Double, height: Double, maxScale: Double = 1.0): Result {
        val scale = minOf(width / bitmap.width, height / bitmap.height, maxScale)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        return Result(
            scale = scale,
            x = (width - scaledWidth) * 0.5,
            y = (height - scaledHeight) * 0.5,
            width = scaledWidth,
            height = scaledHeight,
        )
    }

    internal data class Result(
        val scale: Double,
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
    )
}
