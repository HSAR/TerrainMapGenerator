package io.hsar.mapgenerator.commandgui.compose

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.ServiceLoader
import io.hsar.mapgenerator.commandgui.MapViewerBackend
import io.hsar.mapgenerator.commandgui.MapViewerBackendProvider

class RasterTileSetTest {
    @Test
    fun `compose backend is discoverable through the facade service`() {
        // Arrange / Act
        val backends = ServiceLoader.load(MapViewerBackendProvider::class.java).map { it.backend }.toSet()

        // Assert
        assertThat(backends, equalTo(setOf(MapViewerBackend.COMPOSE)))
    }

    @Test
    fun `splits an image into bounded edge-aware tiles without changing pixels`() {
        // Arrange
        val image = BufferedImage(5, 3, BufferedImage.TYPE_INT_ARGB).also {
            for (y in 0 until it.height) {
                for (x in 0 until it.width) {
                    it.setRGB(x, y, 0xFF000000.toInt() or (x shl 8) or y)
                }
            }
        }

        // Act
        val tiles = RasterTileSet.from(image, tileSize = 2)

        // Assert
        assertThat(tiles.tiles.size, equalTo(6))
        assertThat(tiles.tiles.maxOf { it.image.width }, equalTo(2))
        assertThat(tiles.tiles.maxOf { it.image.height }, equalTo(2))
        assertThat(
            tiles.tiles.single { it.left == 4 && it.top == 2 }.image.getRGB(0, 0),
            equalTo(image.getRGB(4, 2)),
        )
    }

    @Test
    fun `rejects a non-positive tile size`() {
        // Arrange
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

        // Act / Assert
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            RasterTileSet.from(image, tileSize = 0)
        }
    }

    @Test
    fun `visible tile query excludes tiles outside the camera rectangle`() {
        // Arrange
        val tileSet = RasterTileSet.from(
            BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB),
            tileSize = 4,
        )

        // Act
        val visible = tileSet.visibleTiles(left = 4.1, top = 0.0, right = 8.0, bottom = 3.9)

        // Assert
        assertThat(visible.map { it.left to it.top }, equalTo(listOf(4 to 0)))
    }
}
