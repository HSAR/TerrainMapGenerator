package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
import io.hsar.mapgenerator.commandgui.MapViewerLoadLimits
import io.hsar.mapgenerator.commandgui.MapViewerImageLoader
import io.hsar.mapgenerator.commandgui.validateMapViewerImageDimensions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class MapViewerIconLoaderTest {

    @Test
    fun `image metadata accepts supported boundaries without allocating their pixels`() {
        // Arrange / Act / Assert
        validateMapViewerImageDimensions(16_384, 1, "axis.png")
        validateMapViewerImageDimensions(10_000, 10_000, "pixels.png")
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            validateMapViewerImageDimensions(16_385, 1, "wide.png")
        }
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            validateMapViewerImageDimensions(10_000, 10_001, "dense.png")
        }
    }

    @Test
    fun `checked image loading rejects dimensions before raster allocation`() {
        val bytes = """
            <svg xmlns="http://www.w3.org/2000/svg" width="101" height="10"></svg>
        """.trimIndent().toByteArray()

        val failure = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            MapViewerImageLoader.loadChecked(bytes, "large.svg", limits = MapViewerLoadLimits(maximumImageDimension = 100))
        }

        assertThat(failure.message.orEmpty().contains("supported limit is 100 pixels"), equalTo(true))
    }

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
    fun `preserves the full svg canvas when content reaches its edges`() {
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
            assertThat(bitmap.width, equalTo(316))
            assertThat(bitmap.height, equalTo(216))
            assertThat(bounds.isPresent, equalTo(true))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `renders svg text elements inside unit icons`() {
        val file = File.createTempFile("unit-icon-text", ".svg").apply {
            writeText(
                """
                <svg xmlns="http://www.w3.org/2000/svg" version="1.2" baseProfile="tiny" width="236" height="256" viewBox="41 26 118 128">
                  <path d="M 155,150 C 155,50 115,30 100,30 85,30 45,50 45,150" stroke-width="4" stroke="black" fill="rgb(128,224,255)" fill-opacity="1"></path>
                  <text x="100" y="103" text-anchor="middle" font-size="45" font-family="Arial" font-weight="bold" dominant-baseline="middle" stroke-width="3" stroke="none" fill="black">A</text>
                </svg>
                """.trimIndent(),
            )
        }

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            assertThat(bitmap!!.countBlackPixelsInCenter(), equalTo(true))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `centers svg text on the requested anchor point`() {
        val file = File.createTempFile("unit-icon-text-centre", ".svg").apply {
            writeText(
                """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
                  <text x="100" y="100" text-anchor="middle" dominant-baseline="middle" font-size="72" font-family="Arial" font-weight="bold" fill="black">A</text>
                </svg>
                """.trimIndent(),
            )
        }

        try {
            val bitmap = MapViewerIconLoader.load(file)

            assertThat(bitmap, notNullValue())
            val centroid = bitmap!!.darkPixelCentroid()
            assertThat(centroid, notNullValue())
            assertThat(centroid!!.first, closeTo(100.0, 8.0))
            assertThat(centroid.second, closeTo(100.0, 8.0))
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
            assertThat(bitmap.premultiplied, equalTo(true))
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

    private fun korlibs.image.bitmap.Bitmap32.countBlackPixelsInCenter(): Boolean =
        (height / 3 until height * 2 / 3).any { y ->
            (width / 3 until width * 2 / 3).any { x ->
                val pixel = getRgba(x, y)
                pixel.a > 0 && pixel.r < 24 && pixel.g < 24 && pixel.b < 24
            }
        }

    private fun korlibs.image.bitmap.Bitmap32.darkPixelCentroid(): Pair<Double, Double>? {
        var count = 0
        var sumX = 0.0
        var sumY = 0.0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = getRgba(x, y)
                if (pixel.a == 0 || pixel.r >= 24 || pixel.g >= 24 || pixel.b >= 24) {
                    continue
                }

                count += 1
                sumX += x.toDouble()
                sumY += y.toDouble()
            }
        }

        return if (count == 0) {
            null
        } else {
            sumX / count to sumY / count
        }
    }

    private data class OpaqueBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val isPresent: Boolean = left <= right && top <= bottom
    }
}
