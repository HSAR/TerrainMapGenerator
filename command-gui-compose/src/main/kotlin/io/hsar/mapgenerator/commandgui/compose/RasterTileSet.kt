package io.hsar.mapgenerator.commandgui.compose

import java.awt.image.BufferedImage

internal data class RasterTile(
    val left: Int,
    val top: Int,
    val image: BufferedImage,
)

internal data class RasterTileSet(
    val width: Int,
    val height: Int,
    val tiles: List<RasterTile>,
) {
    fun visibleTiles(left: Double, top: Double, right: Double, bottom: Double): List<RasterTile> =
        tiles.filter { tile ->
            tile.left < right &&
                    tile.top < bottom &&
                    tile.left + tile.image.width > left &&
                    tile.top + tile.image.height > top
        }

    companion object {
        const val DEFAULT_TILE_SIZE = 2048

        fun from(image: BufferedImage, tileSize: Int = DEFAULT_TILE_SIZE): RasterTileSet {
            require(tileSize > 0) { "Tile size must be positive." }

            val tiles = (0 until image.height step tileSize).flatMap { top ->
                (0 until image.width step tileSize).map { left ->
                    val width = minOf(tileSize, image.width - left)
                    val height = minOf(tileSize, image.height - top)
                    RasterTile(
                        left = left,
                        top = top,
                        image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { tile ->
                            tile.createGraphics().use { graphics ->
                                graphics.drawImage(
                                    image,
                                    0,
                                    0,
                                    width,
                                    height,
                                    left,
                                    top,
                                    left + width,
                                    top + height,
                                    null,
                                )
                            }
                        },
                    )
                }
            }

            return RasterTileSet(image.width, image.height, tiles)
        }
    }
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
