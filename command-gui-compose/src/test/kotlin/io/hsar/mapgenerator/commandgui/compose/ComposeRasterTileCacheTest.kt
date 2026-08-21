package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

class ComposeRasterTileCacheTest {
    @Test
    fun `cache runs no more than two tile workers`() = runBlocking {
        // Arrange
        val gates = List(3) { CompletableDeferred<Unit>() }
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val cache = ComposeRasterTileCache(
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
            tileLoader = { _, descriptor ->
                val count = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, count) }
                try {
                    gates[descriptor.key.column].await()
                    CachedComposeRasterTile(descriptor, ImageBitmap(descriptor.width, descriptor.height))
                } finally {
                    active.decrementAndGet()
                }
            },
        )
        val source = RasterTileSet.from(BufferedImage(6, 2, BufferedImage.TYPE_INT_ARGB), tileSize = 2)

        // Act
        cache.request(source, source.tiles)
        eventually { maximum.get() == 2 }

        // Assert
        assertThat(maximum.get(), equalTo(2))
        gates.forEach { it.complete(Unit) }
        eventually { cache.ready.size == 3 }
        cache.dispose()
    }

    @Test
    fun `source replacement cancels obsolete work and rejects its completion`() = runBlocking {
        // Arrange
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        val first = RasterTileSet.from(BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), tileSize = 2)
        val second = RasterTileSet.from(BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), tileSize = 3)
        val cache = ComposeRasterTileCache(
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
            tileLoader = { source, descriptor ->
                (if (source === first) firstGate else secondGate).await()
                CachedComposeRasterTile(descriptor, ImageBitmap(descriptor.width, descriptor.height))
            },
        )

        // Act
        cache.request(first, first.tiles)
        cache.request(second, second.tiles)
        firstGate.complete(Unit)
        secondGate.complete(Unit)
        eventually { cache.ready.isNotEmpty() }

        // Assert
        assertThat(cache.ready.values.single().descriptor.width, equalTo(3))
        cache.dispose()
    }

    @Test
    fun `rapid pan cancels obsolete work and can request a returning tile again`() = runBlocking {
        // Arrange
        val source = RasterTileSet.from(BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB), tileSize = 2)
        val firstZero = CompletableDeferred<Unit>()
        val secondZero = CompletableDeferred<Unit>()
        val one = CompletableDeferred<Unit>()
        val zeroStarts = AtomicInteger()
        val cache = ComposeRasterTileCache(
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
            tileLoader = { _, descriptor ->
                val gate = if (descriptor.key.column == 1) one else if (zeroStarts.getAndIncrement() == 0) firstZero else secondZero
                gate.await()
                CachedComposeRasterTile(descriptor, ImageBitmap(descriptor.width, descriptor.height))
            },
        )

        // Act
        cache.request(source, listOf(source.tiles[0]))
        eventually { zeroStarts.get() == 1 }
        cache.request(source, listOf(source.tiles[1]))
        cache.request(source, listOf(source.tiles[0]))
        secondZero.complete(Unit)
        one.complete(Unit)
        firstZero.complete(Unit)
        eventually { source.tiles[0].key in cache.ready }

        // Assert
        assertThat(zeroStarts.get(), equalTo(2))
        assertThat(cache.ready.keys, equalTo(setOf(source.tiles[0].key)))
        cache.dispose()
    }

    @Test
    fun `inactive least recently used tiles are evicted to the byte budget`() = runBlocking {
        // Arrange
        val source = RasterTileSet.from(BufferedImage(6, 2, BufferedImage.TYPE_INT_ARGB), tileSize = 2)
        val cache = ComposeRasterTileCache(
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
            maximumInactiveBytes = 16,
            maximumTotalBytes = 48,
            tileLoader = { _, descriptor -> CachedComposeRasterTile(descriptor, ImageBitmap(descriptor.width, descriptor.height)) },
        )

        // Act
        source.tiles.forEach { descriptor ->
            cache.request(source, listOf(descriptor))
            eventually { descriptor.key in cache.ready }
        }

        // Assert
        assertThat(cache.ready.keys, equalTo(setOf(source.tiles[1].key, source.tiles[2].key)))
        assertThat(cache.inactiveBytes, equalTo(16L))
        assertThat(cache.retainedBytes, equalTo(32L))
        cache.dispose()
    }

    private suspend fun eventually(condition: () -> Boolean) {
        repeat(200) {
            if (condition()) return
            kotlinx.coroutines.delay(5)
        }
        error("Condition was not met")
    }
}
