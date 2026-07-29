package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import java.awt.image.BufferedImage

internal object MapViewerThumbnailViews {
    fun render(
        parent: Container,
        thumbnail: BufferedImage?,
        left: Double,
        top: Double,
        width: Double,
        height: Double,
    ) = render(parent, thumbnail?.toBitmap32(), left, top, width, height)

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
        MapViewerImageViews.icon(parent, thumbnail) {
            scale = fit.scale
            x = left + fit.x
            y = top + fit.y
        }
    }

    private fun BufferedImage.toBitmap32() =
        Bitmap32(width, height) { x, y ->
            getRGB(x, y).let { argb ->
                RGBA(
                    (argb ushr 16) and 0xFF,
                    (argb ushr 8) and 0xFF,
                    argb and 0xFF,
                    (argb ushr 24) and 0xFF,
                )
            }
        }.premultipliedIfRequired()
}
