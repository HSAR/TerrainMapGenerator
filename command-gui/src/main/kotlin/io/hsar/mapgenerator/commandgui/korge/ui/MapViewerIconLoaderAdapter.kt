package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.MapViewerImageLoader
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import java.io.File

internal object MapViewerIconLoader {
    fun load(file: File): Bitmap32? = MapViewerImageLoader.load(file)?.toBitmap32()?.premultipliedIfRequired()
    fun load(bytes: ByteArray, fileName: String): Bitmap32? = MapViewerImageLoader.load(bytes, fileName)?.toBitmap32()?.premultipliedIfRequired()

    private fun java.awt.image.BufferedImage.toBitmap32() = Bitmap32(width, height) { x, y ->
        getRGB(x, y).let { argb -> RGBA(argb ushr 16 and 0xff, argb ushr 8 and 0xff, argb and 0xff, argb ushr 24 and 0xff) }
    }
}
