package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
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
            assertThat(bitmap!!.width, equalTo(12))
            assertThat(bitmap.height, equalTo(12))
            assertThat(bitmap.any { it.value != 0 }, equalTo(true))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `preserves the full svg canvas instead of trimming it`() {
        val file = File.createTempFile("unit-icon-canvas", ".svg").apply {
            writeText(
                """
                <svg xmlns="http://www.w3.org/2000/svg" version="1.2" baseProfile="tiny" width="316" height="216" viewBox="21 46 158 108">
                  <path d="M25,50 l150,0 0,100 -150,0 z" stroke-width="4" stroke="black" fill="rgb(128,224,255)" fill-opacity="1"></path>
                  <path d="M60,85 l40,15 40,-15 0,30 -40,-15 -40,15 z" stroke-width="3" stroke="none" fill="black"></path>
                </svg>
                """.trimIndent(),
            )
        }

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            val bounds = bitmap!!.opaqueBounds()
            assertThat(bitmap.width < 316, equalTo(true))
            assertThat(bitmap.height < 216, equalTo(true))
            assertThat(bounds.left > 0, equalTo(true))
            assertThat(bounds.top > 0, equalTo(true))
            assertThat(bounds.right < bitmap.width - 1, equalTo(true))
            assertThat(bounds.bottom < bitmap.height - 1, equalTo(true))
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

    @Test
    fun `loads raster unit icon colours without shifting channels`() {
        val file = File.createTempFile("unit-icon", ".png")
        val image = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0xFFFF0000.toInt())
        image.setRGB(1, 0, 0xFF0000FF.toInt())
        ImageIO.write(image, "png", file)

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            assertThat(bitmap!!.getRgba(0, 0), equalTo(RGBA(255, 0, 0, 255)))
            assertThat(bitmap.getRgba(1, 0), equalTo(RGBA(0, 0, 255, 255)))
        } finally {
            file.delete()
        }
    }

    private fun korlibs.image.bitmap.Bitmap32.opaqueBounds(): OpaqueBounds {
        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (getRgba(x, y).a == 0) {
                    continue
                }

                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }

        return OpaqueBounds(left = left, top = top, right = right, bottom = bottom)
    }

    private data class OpaqueBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )
}
