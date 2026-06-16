package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Rectangle
import java.awt.image.BufferedImage

class ViewerGeometryTest {

    @Test
    fun `background is twice the map size and map is centered`() {
        val geometry = geometryFor(BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB))

        assertThat(geometry.backgroundWidth, equalTo(1600))
        assertThat(geometry.backgroundHeight, equalTo(996))
        assertThat(geometry.mapOffsetX, equalTo(400))
        assertThat(geometry.mapOffsetY, equalTo(198))
    }

    @Test
    fun `visible viewport is translated into layer coordinates`() {
        val viewport = Rectangle(450, 340, 120, 80)

        assertThat(viewport.toLayerViewport(offsetX = 400, offsetY = 300), equalTo(
            io.hsar.mapgenerator.graph.Rectangle(
                io.hsar.mapgenerator.graph.Point(50.0, 40.0),
                io.hsar.mapgenerator.graph.Point(170.0, 120.0),
            )
        ))
    }
}
