package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO

class MapViewerAppIconExporterTest {
    @Test
    fun `renders a transparent square icon with red selected marker`() {
        val icon = MapViewerAppIconExporter.render(256)

        assertThat(icon.width, equalTo(256))
        assertThat(icon.height, equalTo(256))
        assertThat(icon.getRgba(0, 0).a, equalTo(0))
        assertThat(icon.getRgba(128, 128).a, greaterThan(0))
        assertThat(icon.getRgba(128, 80).r, equalTo(Palette.Colours.RED.red))
        listOf(33 to 33, 222 to 33, 222 to 222, 33 to 222)
            .forEach { (x, y) -> assertThat(icon.getRgba(x, y).a, greaterThan(200)) }
    }

    @Test
    fun `allows every positive power of two size`() {
        listOf(1, 2, 4, 8, 16, 32, 64, 128, 256).forEach { size ->
            val icon = MapViewerAppIconExporter.render(size)

            assertThat(icon.width, equalTo(size))
            assertThat(icon.height, equalTo(size))
        }
    }

    @Test
    fun `rejects non power of two sizes`() {
        val failure = runCatching { MapViewerAppIconExporter.render(255) }.exceptionOrNull()

        assertThat(failure is IllegalArgumentException, equalTo(true))
    }

    @Test
    fun `exports an alpha preserving PNG using palette red`() {
        val target = File.createTempFile("oct-app-icon", ".png")
        target.deleteOnExit()

        MapViewerAppIconExporter.export(target.toPath(), 256)

        ImageIO.read(target).also { image ->
            assertThat(image.getRGB(0, 0) ushr 24, equalTo(0))
            assertThat(image.getRGB(128, 80) shr 16 and 0xFF, equalTo(Palette.Colours.RED.red))
            assertThat(image.getRGB(33, 33) ushr 24, equalTo(230))
            assertThat(image.getRGB(31, 34) shr 16 and 0xFF, equalTo(Palette.Colours.DARK.red))
        }
    }
}
