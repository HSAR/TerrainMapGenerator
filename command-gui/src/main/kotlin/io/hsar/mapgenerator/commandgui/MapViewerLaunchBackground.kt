package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.contourgenerator.ContourTracingOptions
import io.hsar.mapgenerator.contourgenerator.TerrainProfile
import io.hsar.mapgenerator.contourgenerator.TopographyGenerator
import io.hsar.mapgenerator.contourgenerator.TopographyRenderer
import io.hsar.mapgenerator.contourgenerator.TopographyRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.util.PriorityQueue
import java.util.Random
import java.util.concurrent.Executors
import kotlin.collections.ArrayDeque
import kotlin.math.ceil
import kotlin.math.roundToInt

data class MapViewerLaunchBackgroundSpec(
    val seed: Long,
    val textureWidth: Int,
    val textureHeight: Int,
) {
    init {
        require(textureWidth > 0 && textureHeight > 0)
    }

    val biome: String
        get() = biome(seed)

    val information: List<Pair<String, String>>
        get() = listOf(
            "Biome" to biome,
            "Seed" to seed.toString(),
            "Texture" to "$textureWidth x $textureHeight",
            "Noise scale" to "475.0",
            "Contour levels" to "11",
        )

    companion object {
        fun biome(seed: Long): String = TerrainProfile.SEEDED.resolve(seed).name
            .split('_')
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercase) }

        fun forViewport(viewportWidth: Int, viewportHeight: Int, seed: Long) =
            MapViewerLaunchBackgroundLayout.sourceSize(viewportWidth, viewportHeight).let { (width, height) ->
                MapViewerLaunchBackgroundSpec(seed, width, height)
            }
    }
}

data class MapViewerLaunchBackgroundPlacement(val left: Double, val top: Double, val scale: Double)

object MapViewerLaunchBackgroundLayout {
    fun sourceSize(viewportWidth: Int, viewportHeight: Int): Pair<Int, Int> {
        require(viewportWidth > 0 && viewportHeight > 0)
        return (viewportWidth * SOURCE_SCALE).roundToInt().coerceAtLeast(1) to
                (viewportHeight * SOURCE_SCALE).roundToInt().coerceAtLeast(1)
    }

    fun cover(sourceWidth: Int, sourceHeight: Int, viewportWidth: Int, viewportHeight: Int): MapViewerLaunchBackgroundPlacement {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(viewportWidth > 0 && viewportHeight > 0)
        val scale = maxOf(viewportWidth.toDouble() / sourceWidth, viewportHeight.toDouble() / sourceHeight)
        return MapViewerLaunchBackgroundPlacement(
            left = (viewportWidth - sourceWidth * scale) * 0.5,
            top = (viewportHeight - sourceHeight * scale) * 0.5,
            scale = scale,
        )
    }

    private const val SOURCE_SCALE = 1.5
}

enum class MapViewerLaunchTileQuality { PREVIEW, REFINEMENT }

data class MapViewerLaunchTileCoordinate(val column: Int, val row: Int) {
    val left = column * TILE_SIZE
    val top = row * TILE_SIZE

    fun request(seed: Long, quality: MapViewerLaunchTileQuality): TopographyRequest =
        quality.settings().let { settings ->
            TopographyRequest(
                width = settings.paddedSize,
                height = settings.paddedSize,
                seed = seed,
                worldUnitsPerPixel = settings.worldUnitsPerPixel,
                worldOriginX = (left - TILE_BLEED).toDouble(),
                worldOriginY = (top - TILE_BLEED).toDouble(),
                noiseScale = NOISE_SCALE,
                contourLevels = CONTOUR_LEVELS,
                tracing = settings.tracing,
            )
        }

    companion object {
        const val TILE_SIZE = 360
        const val TILE_BLEED = 36
        const val TILE_CLIP_OVERLAP = 2
        const val TILE_CLIP_SIZE = TILE_SIZE + TILE_CLIP_OVERLAP
        const val PADDED_TILE_SIZE = TILE_SIZE + TILE_BLEED * 2
        const val PREVIEW_DIVISOR = 8
        const val PREVIEW_PADDED_TILE_SIZE = PADDED_TILE_SIZE / PREVIEW_DIVISOR
        private const val NOISE_SCALE = 475.0
        private val CONTOUR_LEVELS = (5..15).map { it * 0.05 }

        internal fun priorityRequest(spec: MapViewerLaunchBackgroundSpec) = TopographyRequest(
            width = ceil(spec.textureWidth.toDouble() / TILE_SIZE).toInt(),
            height = ceil(spec.textureHeight.toDouble() / TILE_SIZE).toInt(),
            seed = spec.seed,
            worldUnitsPerPixel = TILE_SIZE.toDouble(),
            worldOriginX = TILE_SIZE * 0.5,
            worldOriginY = TILE_SIZE * 0.5,
            noiseScale = NOISE_SCALE,
            contourLevels = emptyList(),
        )
    }
}

private data class MapViewerLaunchTileSettings(
    val paddedSize: Int,
    val worldUnitsPerPixel: Double,
    val tracing: ContourTracingOptions,
)

private fun MapViewerLaunchTileQuality.settings() = when (this) {
    MapViewerLaunchTileQuality.PREVIEW -> MapViewerLaunchTileSettings(
        MapViewerLaunchTileCoordinate.PREVIEW_PADDED_TILE_SIZE,
        MapViewerLaunchTileCoordinate.PREVIEW_DIVISOR.toDouble(),
        ContourTracingOptions(stepPixels = 1.0, seedGridStepPixels = 2, maximumPointsPerDirection = 2_500),
    )
    MapViewerLaunchTileQuality.REFINEMENT -> MapViewerLaunchTileSettings(
        MapViewerLaunchTileCoordinate.PADDED_TILE_SIZE,
        1.0,
        ContourTracingOptions(stepPixels = 2.0, seedGridStepPixels = 12, maximumPointsPerDirection = 20_000),
    )
}

data class MapViewerLaunchTileUpdate(
    val coordinate: MapViewerLaunchTileCoordinate,
    val quality: MapViewerLaunchTileQuality,
    val image: BufferedImage,
    val renderMilliseconds: Long,
    val arrivalJitterMilliseconds: Long,
)

data class MapViewerLaunchRenderJob(
    val coordinate: MapViewerLaunchTileCoordinate,
    val quality: MapViewerLaunchTileQuality,
)

object MapViewerLaunchTilePlan {
    fun tiles(textureWidth: Int, textureHeight: Int): List<MapViewerLaunchTileCoordinate> {
        require(textureWidth > 0 && textureHeight > 0)
        val columns = ceil(textureWidth.toDouble() / MapViewerLaunchTileCoordinate.TILE_SIZE).toInt()
        val rows = ceil(textureHeight.toDouble() / MapViewerLaunchTileCoordinate.TILE_SIZE).toInt()
        return (0 until rows).flatMap { row ->
            (0 until columns).map { column -> MapViewerLaunchTileCoordinate(column, row) }
        }
    }

    fun priorityOrder(spec: MapViewerLaunchBackgroundSpec): List<MapViewerLaunchTileCoordinate> {
        val field = TopographyGenerator.generate(MapViewerLaunchTileCoordinate.priorityRequest(spec)).heightField
        return tiles(spec.textureWidth, spec.textureHeight).sortedWith(
            compareByDescending<MapViewerLaunchTileCoordinate> { coordinate -> field[coordinate.column, coordinate.row] }
                .thenBy { coordinate -> tieBreak(spec.seed, coordinate) },
        )
    }

    fun arrivalJitter(seed: Long, coordinate: MapViewerLaunchTileCoordinate, quality: MapViewerLaunchTileQuality): Long =
        Math.floorMod(
            seed xor coordinate.column.toLong() * COLUMN_SALT xor coordinate.row.toLong() * ROW_SALT xor
                    quality.ordinal.toLong() * QUALITY_SALT,
            JITTER_RANGE_MILLISECONDS,
        ) + MINIMUM_JITTER_MILLISECONDS

    fun parallelism(availableProcessors: Int) = availableProcessors
        .also { require(it > 0) }
        .coerceAtMost(MAXIMUM_PARALLELISM)

    private fun tieBreak(seed: Long, coordinate: MapViewerLaunchTileCoordinate) =
        seed xor coordinate.column.toLong() * COLUMN_SALT xor coordinate.row.toLong() * ROW_SALT

    private const val MINIMUM_JITTER_MILLISECONDS = 40L
    private const val MAXIMUM_PARALLELISM = 4
    private const val MAXIMUM_JITTER_MILLISECONDS = 320L
    private const val JITTER_RANGE_MILLISECONDS = MAXIMUM_JITTER_MILLISECONDS - MINIMUM_JITTER_MILLISECONDS + 1L
    private const val COLUMN_SALT = 0x45D9F3BL
    private const val ROW_SALT = 0x119DE1F3L
    private const val QUALITY_SALT = 0x9E3779B9L
}

internal object MapViewerLaunchBackgroundWork {
    val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(4) { task ->
        Thread(task, "launch-background-generator").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }.asCoroutineDispatcher()
}

internal class MapViewerLaunchJobScheduler(
    priorityOrder: List<MapViewerLaunchTileCoordinate>,
    private val draw: () -> Double,
) {
    private val rank = priorityOrder.withIndex().associate { (index, coordinate) -> coordinate to index }
    private val previews = ArrayDeque(priorityOrder)
    private val refinements = PriorityQueue(compareBy<MapViewerLaunchTileCoordinate> { rank.getValue(it) })
    private val totalPreviews = priorityOrder.size
    private var completedPreviews = 0

    val hasQueuedJobs get() = previews.isNotEmpty() || refinements.isNotEmpty()

    fun nextJob(): MapViewerLaunchRenderJob? = when {
        previews.isEmpty() -> refinements.poll()?.job(MapViewerLaunchTileQuality.REFINEMENT)
        refinements.isEmpty() -> previews.removeFirstOrNull()?.job(MapViewerLaunchTileQuality.PREVIEW)
        draw() < refinementProbability() -> refinements.poll().job(MapViewerLaunchTileQuality.REFINEMENT)
        else -> previews.removeFirst().job(MapViewerLaunchTileQuality.PREVIEW)
    }

    fun complete(job: MapViewerLaunchRenderJob) {
        if (job.quality == MapViewerLaunchTileQuality.PREVIEW) {
            completedPreviews++
            refinements += job.coordinate
        }
    }

    private fun refinementProbability() = 0.05 + 0.75 *
            (if (totalPreviews == 0) 1.0 else completedPreviews.toDouble() / totalPreviews)
                .coerceIn(0.0, 1.0).let { it * it }
}

private fun MapViewerLaunchTileCoordinate.job(quality: MapViewerLaunchTileQuality) =
    MapViewerLaunchRenderJob(this, quality)

class MapViewerLaunchBackgroundBatchGenerator internal constructor(
    private val parallelism: Int = MapViewerLaunchTilePlan.parallelism(Runtime.getRuntime().availableProcessors()),
    private val dispatcher: CoroutineDispatcher = MapViewerLaunchBackgroundWork.dispatcher,
    private val render: (TopographyRequest) -> BufferedImage = { request ->
        TopographyRenderer.render(TopographyGenerator.generate(request))
    },
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val priorityOrder: (MapViewerLaunchBackgroundSpec) -> List<MapViewerLaunchTileCoordinate> =
        MapViewerLaunchTilePlan::priorityOrder,
) {
    init {
        require(parallelism in 1..16)
    }

    constructor() : this(parallelism = MapViewerLaunchTilePlan.parallelism(Runtime.getRuntime().availableProcessors()))

    suspend fun generate(
        spec: MapViewerLaunchBackgroundSpec,
        onTile: suspend (MapViewerLaunchTileUpdate) -> Unit,
        onFailure: suspend (MapViewerLaunchRenderJob, Throwable, Long) -> Unit = { _, _, _ -> },
    ): Unit = coroutineScope {
        val orderedTiles = withContext(dispatcher) { priorityOrder(spec) }
        val scheduler = MapViewerLaunchJobScheduler(orderedTiles, Random(spec.seed xor SCHEDULER_RANDOM_SALT)::nextDouble)
        val events = Channel<GenerationEvent>(Channel.UNLIMITED)
        var activeRenders = 0
        var pendingPublications = 0

        fun fillRenderSlots() {
            while (activeRenders < parallelism) {
                val job = scheduler.nextJob() ?: return
                activeRenders++
                launch(dispatcher) {
                    val startedAt = System.nanoTime()
                    events.send(
                        runCatching { render(job.coordinate.request(spec.seed, job.quality)) }.fold(
                            onSuccess = { image -> GenerationEvent.Rendered(job, MapViewerLaunchTileUpdate(
                                job.coordinate,
                                job.quality,
                                image,
                                startedAt.elapsedMilliseconds(),
                                MapViewerLaunchTilePlan.arrivalJitter(spec.seed, job.coordinate, job.quality),
                            )) },
                            onFailure = { GenerationEvent.Failed(job, it, startedAt.elapsedMilliseconds()) },
                        ),
                    )
                }
            }
        }

        fillRenderSlots()
        while (activeRenders > 0 || pendingPublications > 0 || scheduler.hasQueuedJobs) {
            when (val event = events.receive()) {
                is GenerationEvent.Rendered -> {
                    activeRenders--
                    pendingPublications++
                    launch {
                        val failure = runCatching {
                            wait(event.update.arrivalJitterMilliseconds)
                            onTile(event.update)
                        }.exceptionOrNull()
                        events.send(GenerationEvent.Published(event.job, failure, event.update.renderMilliseconds))
                    }
                    fillRenderSlots()
                }
                is GenerationEvent.Failed -> {
                    activeRenders--
                    onFailure(event.job, event.error, event.renderMilliseconds)
                    scheduler.complete(event.job)
                    fillRenderSlots()
                }
                is GenerationEvent.Published -> {
                    pendingPublications--
                    event.error?.let { onFailure(event.job, it, event.renderMilliseconds) }
                    scheduler.complete(event.job)
                    fillRenderSlots()
                }
            }
        }
        events.close()
    }

    private sealed interface GenerationEvent {
        data class Rendered(val job: MapViewerLaunchRenderJob, val update: MapViewerLaunchTileUpdate) : GenerationEvent
        data class Failed(val job: MapViewerLaunchRenderJob, val error: Throwable, val renderMilliseconds: Long) : GenerationEvent
        data class Published(val job: MapViewerLaunchRenderJob, val error: Throwable?, val renderMilliseconds: Long) : GenerationEvent
    }

    private companion object {
        const val SCHEDULER_RANDOM_SALT = -7046029254386353131L
    }
}

object MapViewerLaunchTileReveal {
    const val PREVIEW_FADE_MILLISECONDS = 120.0
    const val REFINEMENT_FADE_MILLISECONDS = 280.0

    fun previewAlpha(elapsedMilliseconds: Double) = smoothstep(elapsedMilliseconds / PREVIEW_FADE_MILLISECONDS)
    fun refinementAlpha(elapsedMilliseconds: Double) = smoothstep(elapsedMilliseconds / REFINEMENT_FADE_MILLISECONDS)

    private fun smoothstep(value: Double) = value.coerceIn(0.0, 1.0).let { it * it * (3.0 - 2.0 * it) }
}

private fun Long.elapsedMilliseconds() = (System.nanoTime() - this) / 1_000_000
