package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CancellationException

internal data class CachedComposeRasterTile(
    val descriptor: RasterTileDescriptor,
    val image: ImageBitmap,
    val drawLeft: Int = descriptor.left,
    val drawTop: Int = descriptor.top,
) {
    val estimatedRgbaBytes get() = image.width.toLong() * image.height * Int.SIZE_BYTES
}

internal class ComposeRasterTileCache(
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val maximumInactiveBytes: Long = DEFAULT_MAXIMUM_INACTIVE_BYTES,
    private val maximumTotalBytes: Long = DEFAULT_MAXIMUM_TOTAL_BYTES,
    workerCount: Int = DEFAULT_WORKER_COUNT,
    private val tileLoader: (suspend (RasterTileSet, RasterTileDescriptor) -> CachedComposeRasterTile)? = null,
) {
    var ready by mutableStateOf<Map<RasterTileKey, CachedComposeRasterTile>>(emptyMap())
        private set

    private val permits = Semaphore(workerCount.coerceAtLeast(1))
    private val entries = LinkedHashMap<RasterTileKey, CachedComposeRasterTile>(16, .75f, true)
    private val jobs = mutableMapOf<RasterTileKey, Job>()
    private var source: RasterTileSet? = null
    private var sourceGeneration = 0L
    private var activeKeys = emptySet<RasterTileKey>()
    private var disposed = false

    val inFlightCount get() = jobs.size
    val retainedBytes get() = entries.values.sumOf(CachedComposeRasterTile::estimatedRgbaBytes)
    val inactiveBytes get() = entries.filterKeys { it !in activeKeys }.values.sumOf(CachedComposeRasterTile::estimatedRgbaBytes)

    fun request(nextSource: RasterTileSet, desired: List<RasterTileDescriptor>) {
        if (disposed) return
        if (source !== nextSource) replaceSource(nextSource)
        activeKeys = desired.mapTo(linkedSetOf(), RasterTileDescriptor::key)
        jobs.keys.filter { it !in activeKeys }.forEach { key -> jobs.remove(key)?.cancel() }
        activeKeys.forEach(entries::get)
        evict()
        publish()

        val generation = sourceGeneration
        desired.filter { it.key !in entries && it.key !in jobs }.forEach { descriptor ->
            jobs[descriptor.key] = scope.launch {
                try {
                    permits.withPermit {
                        val loaded = load(nextSource, descriptor)
                        if (!disposed && source === nextSource && generation == sourceGeneration && descriptor.key in activeKeys) {
                            entries[descriptor.key] = loaded
                            evict()
                            publish()
                        }
                    }
                } catch (_: CancellationException) {
                    // Expected when a viewport or source supersedes queued work.
                } catch (failure: Exception) {
                    logger.warn("Unable to materialize map tile {}", descriptor.key, failure)
                } finally {
                    val currentJob = currentCoroutineContext()[Job]
                    if (jobs[descriptor.key] === currentJob) jobs.remove(descriptor.key)
                }
            }
        }
    }

    fun dispose() {
        disposed = true
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        entries.clear()
        activeKeys = emptySet()
        ready = emptyMap()
        source = null
    }

    private fun replaceSource(nextSource: RasterTileSet) {
        sourceGeneration += 1L
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        entries.clear()
        ready = emptyMap()
        source = nextSource
    }

    private suspend fun load(source: RasterTileSet, descriptor: RasterTileDescriptor) = tileLoader
        ?.invoke(source, descriptor)
        ?: withContext(workerDispatcher) {
            source.materialize(descriptor).let { tile ->
                CachedComposeRasterTile(tile.descriptor, tile.image.toImageBitmap(), tile.drawLeft, tile.drawTop)
            }
        }

    private fun evict() {
        fun overBudget() = inactiveBytes > maximumInactiveBytes || retainedBytes > maximumTotalBytes
        val iterator = entries.entries.iterator()
        while (overBudget() && iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate.key !in activeKeys) iterator.remove()
        }
    }

    private fun publish() {
        ready = entries.toMap()
    }

    companion object {
        const val DEFAULT_WORKER_COUNT = 2
        const val DEFAULT_MAXIMUM_INACTIVE_BYTES = 256L * 1024L * 1024L
        const val DEFAULT_MAXIMUM_TOTAL_BYTES = 400L * 1024L * 1024L
        private val logger = LogManager.getLogger(ComposeRasterTileCache::class.java)
    }
}
