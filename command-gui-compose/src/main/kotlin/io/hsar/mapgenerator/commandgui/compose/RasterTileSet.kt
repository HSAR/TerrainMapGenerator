package io.hsar.mapgenerator.commandgui.compose

import java.awt.image.BufferedImage
import kotlin.math.pow

internal data class RasterTileKey(val column: Int, val row: Int)

internal data class RasterTileDescriptor(
    val key: RasterTileKey,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val right get() = left + width
    val bottom get() = top + height
    val estimatedRgbaBytes get() = width.toLong() * height * Int.SIZE_BYTES
}

internal data class RasterTile(
    val descriptor: RasterTileDescriptor,
    val drawLeft: Int,
    val drawTop: Int,
    val image: BufferedImage,
)

/** Descriptor-only raster index. Pixel copies are created only by [materialize]. */
internal class RasterTileSet private constructor(
    private val source: BufferedImage,
    val tileSize: Int,
    val tiles: List<RasterTileDescriptor>,
) {
    val width: Int get() = source.width
    val height: Int get() = source.height

    fun visibleTiles(
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        overscanTiles: Int = 0,
    ): List<RasterTileDescriptor> {
        require(overscanTiles >= 0) { "Tile overscan must not be negative." }
        val overscan = overscanTiles.toDouble() * tileSize
        val centreX = (left + right) * .5
        val centreY = (top + bottom) * .5
        fun RasterTileDescriptor.intersects(queryLeft: Double, queryTop: Double, queryRight: Double, queryBottom: Double) =
            this.left < queryRight && this.top < queryBottom && this.right > queryLeft && this.bottom > queryTop
        fun RasterTileDescriptor.distanceSquared() =
            ((this.left + width * .5) - centreX).pow(2) + ((this.top + height * .5) - centreY).pow(2)

        return tiles
            .filter { it.intersects(left - overscan, top - overscan, right + overscan, bottom + overscan) }
            .sortedWith(
                compareBy<RasterTileDescriptor> { !it.intersects(left, top, right, bottom) }
                    .thenBy { it.distanceSquared() }
                    .thenBy { it.top }
                    .thenBy { it.left },
            )
    }

    fun materialize(descriptor: RasterTileDescriptor): RasterTile {
        require(tiles.getOrNull(descriptor.key.row * columnCount + descriptor.key.column) == descriptor) {
            "Tile descriptor does not belong to this raster source."
        }
        val sourceLeft = (descriptor.left - TILE_BLEED).coerceAtLeast(0)
        val sourceTop = (descriptor.top - TILE_BLEED).coerceAtLeast(0)
        val sourceRight = (descriptor.right + TILE_BLEED).coerceAtMost(width)
        val sourceBottom = (descriptor.bottom + TILE_BLEED).coerceAtMost(height)
        val image = BufferedImage(sourceRight - sourceLeft, sourceBottom - sourceTop, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().use { graphics ->
            graphics.drawImage(
                source,
                0,
                0,
                image.width,
                image.height,
                sourceLeft,
                sourceTop,
                sourceRight,
                sourceBottom,
                null,
            )
        }
        return RasterTile(descriptor, sourceLeft, sourceTop, image)
    }

    private val columnCount = (width + tileSize - 1) / tileSize

    companion object {
        const val DEFAULT_TILE_SIZE = 1024
        private const val TILE_BLEED = 1

        fun from(image: BufferedImage, tileSize: Int = DEFAULT_TILE_SIZE): RasterTileSet {
            require(tileSize > 0) { "Tile size must be positive." }
            val tiles = (0 until image.height step tileSize).flatMapIndexed { row, top ->
                (0 until image.width step tileSize).mapIndexed { column, left ->
                    RasterTileDescriptor(
                        key = RasterTileKey(column, row),
                        left = left,
                        top = top,
                        width = minOf(tileSize, image.width - left),
                        height = minOf(tileSize, image.height - top),
                    )
                }
            }
            return RasterTileSet(image, tileSize, tiles)
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
