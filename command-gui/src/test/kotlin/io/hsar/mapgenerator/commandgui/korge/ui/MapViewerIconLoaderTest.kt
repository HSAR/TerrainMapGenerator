package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class MapViewerIconLoaderTest {

    @Test
    fun `loads svg unit icons into a bitmap`() {
        val file = File.createTempFile("unit-icon", ".svg").apply {
            writeText(
                """
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12">
                  <rect x="1" y="1" width="10" height="10" fill="#ff0000"/>
                </svg>
                """.trimIndent(),
            )
        }

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            assertThat(bitmap!!.width <= 12, equalTo(true))
            assertThat(bitmap.height <= 12, equalTo(true))
            assertThat(bitmap.any { it.value != 0 }, equalTo(true))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `loads raster unit icons without forcing them square`() {
        val file = File.createTempFile("unit-icon", ".png")
        val image = BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(0, 0, 0, 0)
            graphics.fillRect(0, 0, 10, 8)
            graphics.color = Color.RED
            graphics.fillRect(3, 2, 4, 2)
            ImageIO.write(image, "png", file)
        } finally {
            graphics.dispose()
        }

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            assertThat(bitmap!!.width, equalTo(4))
            assertThat(bitmap.height, equalTo(2))
        } finally {
            file.delete()
        }
    }
}
