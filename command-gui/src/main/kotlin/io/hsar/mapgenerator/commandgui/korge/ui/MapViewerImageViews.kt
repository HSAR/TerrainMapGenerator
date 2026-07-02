package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.image

internal object MapViewerImageViews {
    fun icon(parent: Container, bitmap: Bitmap32, block: Image.() -> Unit = {}): Image =
        parent.image(bitmap) {
            smoothing = true
            block()
        }
}
