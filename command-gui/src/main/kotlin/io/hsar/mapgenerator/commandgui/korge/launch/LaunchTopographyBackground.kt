package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.contourgenerator.ContourTracingOptions
import io.hsar.mapgenerator.contourgenerator.TopographyGenerator
import io.hsar.mapgenerator.contourgenerator.TopographyRenderer
import io.hsar.mapgenerator.contourgenerator.TopographyRequest
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.util.PriorityQueue
import java.util.Random
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.associate
import kotlin.collections.emptyList
import kotlin.collections.flatMap
import kotlin.collections.getValue
import kotlin.collections.isNotEmpty
import kotlin.collections.linkedMapOf
import kotlin.collections.map
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.sortedWith
import kotlin.collections.toList
import kotlin.collections.withIndex
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class LaunchBackgroundPlacement(
    val left: Double,
    val top: Double,
    val scale: Double,
)

internal object LaunchBackgroundLayout {
    fun cover(sourceWidth: Int, sourceHeight: Int, viewportWidth: Int, viewportHeight: Int): LaunchBackgroundPlacement {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(viewportWidth > 0 && viewportHeight > 0)
        val scale = maxOf(viewportWidth.toDouble() / sourceWidth, viewportHeight.toDouble() / sourceHeight)
        return LaunchBackgroundPlacement(
            left = (viewportWidth - sourceWidth * scale) * 0.5,
            top = (viewportHeight - sourceHeight * scale) * 0.5,
            scale = scale,
        )
    }

    fun sourceSize(viewportWidth: Int, viewportHeight: Int): Pair<Int, Int> {
        require(viewportWidth > 0 && viewportHeight > 0)
        return (viewportWidth * SOURCE_SCALE).roundToInt().coerceAtLeast(1) to
                (viewportHeight * SOURCE_SCALE).roundToInt().coerceAtLeast(1)
    }

    private const val SOURCE_SCALE = 1.5
}

internal enum class LaunchBackgroundTileQuality {
    PREVIEW,
    REFINEMENT,
}

internal data class LaunchBackgroundTileCoordinate(
    val column: Int,
    val row: Int,
) {
    val left: Int = column * TILE_SIZE
    val top: Int = row * TILE_SIZE

    fun request(seed: Long, quality: LaunchBackgroundTileQuality): TopographyRequest =
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
        const val TILE_SIZE = 240
        const val TILE_BLEED = 24
        const val PADDED_TILE_SIZE = TILE_SIZE + TILE_BLEED * 2
        const val PREVIEW_DIVISOR = 8
        const val PREVIEW_PADDED_TILE_SIZE = PADDED_TILE_SIZE / PREVIEW_DIVISOR
        private const val NOISE_SCALE = 475.0
        private val CONTOUR_LEVELS = (5..15).map { it * 0.05 }

        fun priorityRequest(textureWidth: Int, textureHeight: Int, seed: Long): TopographyRequest =
            TopographyRequest(
                width = ceil(textureWidth.toDouble() / TILE_SIZE).toInt(),
                height = ceil(textureHeight.toDouble() / TILE_SIZE).toInt(),
                seed = seed,
                worldUnitsPerPixel = TILE_SIZE.toDouble(),
                worldOriginX = TILE_SIZE * 0.5,
                worldOriginY = TILE_SIZE * 0.5,
                noiseScale = NOISE_SCALE,
                contourLevels = emptyList(),
            )
    }
}

private data class LaunchBackgroundTileSettings(
    val paddedSize: Int,
    val worldUnitsPerPixel: Double,
    val tracing: ContourTracingOptions,
)

private fun LaunchBackgroundTileQuality.settings(): LaunchBackgroundTileSettings = when (this) {
    LaunchBackgroundTileQuality.PREVIEW -> LaunchBackgroundTileSettings(
        paddedSize = LaunchBackgroundTileCoordinate.PREVIEW_PADDED_TILE_SIZE,
        worldUnitsPerPixel = LaunchBackgroundTileCoordinate.PREVIEW_DIVISOR.toDouble(),
        tracing = ContourTracingOptions(
            stepPixels = 1.0,
            seedGridStepPixels = 2,
            maximumPointsPerDirection = 2_500,
        ),
    )

    LaunchBackgroundTileQuality.REFINEMENT -> LaunchBackgroundTileSettings(
        paddedSize = LaunchBackgroundTileCoordinate.PADDED_TILE_SIZE,
        worldUnitsPerPixel = 1.0,
        tracing = ContourTracingOptions(
            stepPixels = 2.0,
            seedGridStepPixels = 12,
            maximumPointsPerDirection = 20_000,
        ),
    )
}

internal data class LaunchBackgroundTileUpdate(
    val coordinate: LaunchBackgroundTileCoordinate,
    val quality: LaunchBackgroundTileQuality,
    val bitmap: Bitmap32,
    val renderMilliseconds: Long,
    val arrivalJitterMilliseconds: Long,
)

internal data class LaunchBackgroundTileSnapshot(
    val coordinate: LaunchBackgroundTileCoordinate,
    val preview: Bitmap32?,
    val refinement: Bitmap32?,
)

internal object LaunchBackgroundTilePlan {
    fun tiles(textureWidth: Int, textureHeight: Int): List<LaunchBackgroundTileCoordinate> {
        require(textureWidth > 0 && textureHeight > 0)
        val columns = ceil(textureWidth.toDouble() / LaunchBackgroundTileCoordinate.TILE_SIZE).toInt()
        val rows = ceil(textureHeight.toDouble() / LaunchBackgroundTileCoordinate.TILE_SIZE).toInt()
        return (0 until rows)
            .flatMap { row -> (0 until columns).map { column -> LaunchBackgroundTileCoordinate(column, row) } }
    }

    fun priorityOrder(background: LaunchTopographyBackground): List<LaunchBackgroundTileCoordinate> {
        val field = TopographyGenerator.generate(
            LaunchBackgroundTileCoordinate.priorityRequest(
                textureWidth = background.textureWidth,
                textureHeight = background.textureHeight,
                seed = background.seed,
            ),
        ).heightField
        return tiles(background.textureWidth, background.textureHeight)
            .sortedWith(
                compareByDescending<LaunchBackgroundTileCoordinate> { coordinate -> field[coordinate.column, coordinate.row] }
                    .thenBy { coordinate -> tieBreak(background.seed, coordinate) },
            )
    }

    fun arrivalJitter(seed: Long, coordinate: LaunchBackgroundTileCoordinate, quality: LaunchBackgroundTileQuality): Long =
        Math.floorMod(
            seed xor
                    coordinate.column.toLong() * COLUMN_SALT xor
                    coordinate.row.toLong() * ROW_SALT xor
                    quality.ordinal.toLong() * QUALITY_SALT,
            JITTER_RANGE_MILLISECONDS,
        ) + MINIMUM_JITTER_MILLISECONDS

    fun parallelism(availableProcessors: Int): Int =
        (availableProcessors - 1).coerceIn(1, MAXIMUM_PARALLELISM)

    private fun tieBreak(seed: Long, coordinate: LaunchBackgroundTileCoordinate): Long =
        seed xor
                coordinate.column.toLong() * COLUMN_SALT xor
                coordinate.row.toLong() * ROW_SALT

    private const val MAXIMUM_PARALLELISM = 16
    private const val MINIMUM_JITTER_MILLISECONDS = 40L
    private const val MAXIMUM_JITTER_MILLISECONDS = 320L
    private const val JITTER_RANGE_MILLISECONDS = MAXIMUM_JITTER_MILLISECONDS - MINIMUM_JITTER_MILLISECONDS + 1L
    private const val COLUMN_SALT = 0x45D9F3BL
    private const val ROW_SALT = 0x119DE1F3L
    private const val QUALITY_SALT = 0x9E3779B9L
}

internal data class LaunchBackgroundRenderJob(
    val coordinate: LaunchBackgroundTileCoordinate,
    val quality: LaunchBackgroundTileQuality,
)

internal class LaunchBackgroundJobScheduler(
    priorityOrder: List<LaunchBackgroundTileCoordinate>,
    private val draw: () -> Double,
) {
    private val rank = priorityOrder.withIndex().associate { (index, coordinate) -> coordinate to index }
    private val previews = ArrayDeque(priorityOrder)
    private val refinements = PriorityQueue(compareBy<LaunchBackgroundTileCoordinate> { coordinate -> rank.getValue(coordinate) })
    private val totalPreviews = priorityOrder.size
    private var completedPreviews = 0

    val previewCompletionFraction: Double
        get() = if (totalPreviews == 0) 1.0 else completedPreviews.toDouble() / totalPreviews

    val hasQueuedJobs: Boolean
        get() = previews.isNotEmpty() || refinements.isNotEmpty()

    fun nextJob(): LaunchBackgroundRenderJob? = when {
        previews.isEmpty() -> refinements.poll()?.refinementJob()
        refinements.isEmpty() -> previews.removeFirstOrNull()?.previewJob()
        draw() < refinementProbability(previewCompletionFraction) -> refinements.poll().refinementJob()
        else -> previews.removeFirst().previewJob()
    }

    fun complete(job: LaunchBackgroundRenderJob) {
        if (job.quality == LaunchBackgroundTileQuality.PREVIEW) {
            completedPreviews++
            refinements += job.coordinate
        }
    }

    companion object {
        fun refinementProbability(previewCompletionFraction: Double): Double =
            0.05 + 0.75 * previewCompletionFraction.coerceIn(0.0, 1.0).let { fraction -> fraction * fraction }
    }
}

private fun LaunchBackgroundTileCoordinate.previewJob(): LaunchBackgroundRenderJob =
    LaunchBackgroundRenderJob(this, LaunchBackgroundTileQuality.PREVIEW)

private fun LaunchBackgroundTileCoordinate.refinementJob(): LaunchBackgroundRenderJob =
    LaunchBackgroundRenderJob(this, LaunchBackgroundTileQuality.REFINEMENT)

@OptIn(ExperimentalCoroutinesApi::class)
internal class LaunchBackgroundTileGenerator(
    private val parallelism: Int = LaunchBackgroundTilePlan.parallelism(Runtime.getRuntime().availableProcessors()),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(parallelism),
    private val render: (TopographyRequest) -> BufferedImage = { request ->
        TopographyRenderer.render(TopographyGenerator.generate(request))
    },
    private val wait: suspend (Long) -> Unit = { milliseconds -> delay(milliseconds) },
    private val priorityOrder: (LaunchTopographyBackground) -> List<LaunchBackgroundTileCoordinate> =
        LaunchBackgroundTilePlan::priorityOrder,
) {
    init {
        require(parallelism in 1..16)
    }

    suspend fun generate(
        background: LaunchTopographyBackground,
        onTile: suspend (LaunchBackgroundTileUpdate) -> Unit,
        onFailure: suspend (LaunchBackgroundRenderJob, Throwable, Long) -> Unit = { _, _, _ -> },
    ): Unit = coroutineScope {
        val random = Random(background.seed xor SCHEDULER_RANDOM_SALT)
        val scheduler = LaunchBackgroundJobScheduler(priorityOrder(background), random::nextDouble)
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
                        runCatching { render(job.coordinate.request(background.seed, job.quality)) }
                            .fold(
                                onSuccess = { image ->
                                    GenerationEvent.Rendered(
                                        job = job,
                                        update = LaunchBackgroundTileUpdate(
                                            coordinate = job.coordinate,
                                            quality = job.quality,
                                            bitmap = image.toBMP32().premultipliedIfRequired(),
                                            renderMilliseconds = startedAt.elapsedMilliseconds(),
                                            arrivalJitterMilliseconds = LaunchBackgroundTilePlan.arrivalJitter(
                                                background.seed,
                                                job.coordinate,
                                                job.quality,
                                            ),
                                        ),
                                    )
                                },
                                onFailure = { error ->
                                    GenerationEvent.Failed(job, error, startedAt.elapsedMilliseconds())
                                },
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
                        val publicationError = runCatching {
                            wait(event.update.arrivalJitterMilliseconds)
                            onTile(event.update)
                        }.exceptionOrNull()
                        events.send(GenerationEvent.Published(event.job, publicationError, event.update.renderMilliseconds))
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
                    event.error?.let { error -> onFailure(event.job, error, event.renderMilliseconds) }
                    scheduler.complete(event.job)
                    fillRenderSlots()
                }
            }
        }
        events.close()
        Unit
    }

    private sealed interface GenerationEvent {
        data class Rendered(
            val job: LaunchBackgroundRenderJob,
            val update: LaunchBackgroundTileUpdate,
        ) : GenerationEvent

        data class Failed(
            val job: LaunchBackgroundRenderJob,
            val error: Throwable,
            val renderMilliseconds: Long,
        ) : GenerationEvent

        data class Published(
            val job: LaunchBackgroundRenderJob,
            val error: Throwable?,
            val renderMilliseconds: Long,
        ) : GenerationEvent
    }

    private companion object {
        const val SCHEDULER_RANDOM_SALT = -7046029254386353131L
    }
}

internal class LaunchTopographyBackground private constructor(
    val seed: Long,
    val textureWidth: Int,
    val textureHeight: Int,
) {
    private val completedTiles = linkedMapOf<LaunchBackgroundTileCoordinate, LaunchBackgroundTileSnapshot>()
    val available: Boolean
        get() = completedTiles.isNotEmpty()

    fun accept(update: LaunchBackgroundTileUpdate) {
        val current = completedTiles[update.coordinate]
        completedTiles[update.coordinate] = when (update.quality) {
            LaunchBackgroundTileQuality.PREVIEW -> LaunchBackgroundTileSnapshot(
                coordinate = update.coordinate,
                preview = update.bitmap,
                refinement = current?.refinement,
            )

            LaunchBackgroundTileQuality.REFINEMENT -> LaunchBackgroundTileSnapshot(
                coordinate = update.coordinate,
                preview = null,
                refinement = update.bitmap,
            )
        }
    }

    fun tiles(): List<LaunchBackgroundTileSnapshot> = completedTiles.values.toList()

    companion object {
        fun create(viewportWidth: Int, viewportHeight: Int, seed: Long): LaunchTopographyBackground =
            LaunchBackgroundLayout.sourceSize(viewportWidth, viewportHeight).let { (width, height) ->
                LaunchTopographyBackground(seed, width, height)
            }
    }
}

internal object LaunchBackgroundTileReveal {
    const val PREVIEW_FADE_MILLISECONDS = 120.0
    const val REFINEMENT_FADE_MILLISECONDS = 280.0

    fun previewAlpha(elapsedMilliseconds: Double): Double =
        smoothstep(elapsedMilliseconds / PREVIEW_FADE_MILLISECONDS)

    fun refinementAlpha(elapsedMilliseconds: Double): Double =
        smoothstep(elapsedMilliseconds / REFINEMENT_FADE_MILLISECONDS)

    private fun smoothstep(value: Double): Double =
        value.coerceIn(0.0, 1.0).let { progress -> progress * progress * (3.0 - 2.0 * progress) }
}

private fun Long.elapsedMilliseconds(): Long =
    (System.nanoTime() - this) / 1_000_000
