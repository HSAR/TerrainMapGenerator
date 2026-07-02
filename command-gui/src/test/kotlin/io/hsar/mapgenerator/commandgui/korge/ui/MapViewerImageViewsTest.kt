package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerImageViewsTest {

    @Test
    fun `icon images enable smoothing for scaled rendering`() {
        val bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) }

        val image = MapViewerImageViews.icon(Container(), bitmap)

        assertThat(image.smoothing, equalTo(true))
    }

    @Test
    fun `image boxes enable smoothing for scaled unit previews`() {
        val parent = Container()
        val bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) }

        MapViewerUiBoxes.imageBox(parent, bitmap, maxWidth = 24.0, maxHeight = 24.0)

        assertThat(parent.images().single().smoothing, equalTo(true))
    }

    private fun Container.images(): List<Image> =
        children.flatMap { child ->
            listOfNotNull(child as? Image) + ((child as? Container)?.images().orEmpty())
        }
}
