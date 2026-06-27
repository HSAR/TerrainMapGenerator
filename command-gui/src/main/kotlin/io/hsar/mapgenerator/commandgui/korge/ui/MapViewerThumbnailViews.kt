package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.korge.view.Container
import korlibs.korge.view.image

internal object MapViewerThumbnailViews {
    fun render(
        parent: Container,
        thumbnail: Bitmap32?,
        left: Double,
        top: Double,
        width: Double,
        height: Double,
    ) {
        thumbnail ?: return
        val fit = MapViewerIconFit.inside(thumbnail, width, height, maxScale = Double.POSITIVE_INFINITY)
        parent.image(thumbnail) {
            scale = fit.scale
            x = left + fit.x
            y = top + fit.y
        }
    }
}
