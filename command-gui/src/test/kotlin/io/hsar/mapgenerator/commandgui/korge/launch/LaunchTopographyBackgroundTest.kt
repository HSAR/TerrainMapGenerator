package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.contourgenerator.TopographyGenerator
import korlibs.image.bitmap.Bitmap32
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class LaunchTopographyBackgroundTest {
    @Test
    fun `background texture renders at one hundred and fifty percent of viewport dimensions`() {
        val background = LaunchTopographyBackground.create(1920, 1080, 73L)

        assertThat(background.seed, equalTo(73L))
        assertThat(background.textureWidth to background.textureHeight, equalTo(2880 to 1620))
        assertThat(background.available, equalTo(false))
    }

    @Test
    fun `cover placement centres and crops without distorting`() {
        val placement = LaunchBackgroundLayout.cover(
            sourceWidth = 1600,
            sourceHeight = 900,
            viewportWidth = 1000,
            viewportHeight = 1000,
        )

        assertThat(placement.scale, closeTo(1000.0 / 900.0, 0.0001))
        assertThat(placement.left, closeTo(-388.8889, 0.001))
        assertThat(placement.top, closeTo(0.0, 0.001))
    }

    @Test
    fun `tile plan covers the texture with square cores`() {
        val tiles = LaunchBackgroundTilePlan.tiles(textureWidth = 1920, textureHeight = 1080)

        assertThat(tiles, hasSize(40))
        assertThat(tiles.map(LaunchBackgroundTileCoordinate::column).toSet(), equalTo((0..7).toSet()))
        assertThat(tiles.map(LaunchBackgroundTileCoordinate::row).toSet(), equalTo((0..4).toSet()))
    }

    @Test
    fun `preview and refinement requests preserve their shared world extent`() {
        val coordinate = LaunchBackgroundTileCoordinate(column = 1, row = 2)
        val preview = coordinate.request(73L, LaunchBackgroundTileQuality.PREVIEW)
        val refinement = coordinate.request(73L, LaunchBackgroundTileQuality.REFINEMENT)

        assertThat(preview.width to preview.height, equalTo(36 to 36))
        assertThat(preview.worldUnitsPerPixel, equalTo(8.0))
        assertThat(preview.worldOriginX to preview.worldOriginY, equalTo(216.0 to 456.0))
        assertThat(preview.tracing.stepPixels, equalTo(1.0))
        assertThat(preview.tracing.seedGridStepPixels, equalTo(2))
        assertThat(preview.tracing.maximumPointsPerDirection, equalTo(2_500))
        assertThat(refinement.width to refinement.height, equalTo(288 to 288))
        assertThat(refinement.worldUnitsPerPixel, equalTo(1.0))
        assertThat(refinement.worldOriginX to refinement.worldOriginY, equalTo(216.0 to 456.0))
        assertThat(refinement.seed, equalTo(preview.seed))
        assertThat(refinement.noiseScale, equalTo(preview.noiseScale))
        assertThat(refinement.contourLevels, equalTo(preview.contourLevels))
    }

    @Test
    fun `priority noisemap samples the centre of every tile`() {
        val request = LaunchBackgroundTileCoordinate.priorityRequest(
            textureWidth = 1920,
            textureHeight = 1080,
            seed = 73L,
        )

        assertThat(request.width to request.height, equalTo(8 to 5))
        assertThat(request.worldOriginX to request.worldOriginY, equalTo(120.0 to 120.0))
        assertThat(request.worldUnitsPerPixel, equalTo(240.0))
        assertThat(request.contourLevels, equalTo(emptyList()))
    }

    @Test
    fun `priority order is strictly highest elevation first`() {
        val background = LaunchTopographyBackground.create(1280, 720, 73L)
        val request = LaunchBackgroundTileCoordinate.priorityRequest(
            background.textureWidth,
            background.textureHeight,
            background.seed,
        )
        val field = TopographyGenerator.generate(request).heightField
        val order = LaunchBackgroundTilePlan.priorityOrder(background)
        val elevations = order.map { coordinate -> field[coordinate.column, coordinate.row] }

        assertThat(elevations.zipWithNext().all { (first, second) -> first >= second }, equalTo(true))
    }

    @Test
    fun `parallelism leaves one processor free and never exceeds sixteen`() {
        assertThat(LaunchBackgroundTilePlan.parallelism(1), equalTo(1))
        assertThat(LaunchBackgroundTilePlan.parallelism(4), equalTo(3))
        assertThat(LaunchBackgroundTilePlan.parallelism(17), equalTo(16))
        assertThat(LaunchBackgroundTilePlan.parallelism(64), equalTo(16))
    }

    @Test
    fun `arrival jitter is deterministic quality aware and bounded`() {
        val tiles = LaunchBackgroundTilePlan.tiles(textureWidth = 1920, textureHeight = 1080)
        val first = tiles.map { tile ->
            LaunchBackgroundTilePlan.arrivalJitter(73L, tile, LaunchBackgroundTileQuality.PREVIEW)
        }
        val second = tiles.map { tile ->
            LaunchBackgroundTilePlan.arrivalJitter(73L, tile, LaunchBackgroundTileQuality.PREVIEW)
        }
        val refinements = tiles.map { tile ->
            LaunchBackgroundTilePlan.arrivalJitter(73L, tile, LaunchBackgroundTileQuality.REFINEMENT)
        }

        assertThat(first, equalTo(second))
        assertThat(first, everyItem(greaterThanOrEqualTo(40L)))
        assertThat(first, everyItem(lessThanOrEqualTo(320L)))
        assertThat(first == refinements, equalTo(false))
    }

    @Test
    fun `refinement probability follows the quadratic preview completion curve`() {
        assertThat(LaunchBackgroundJobScheduler.refinementProbability(0.0), equalTo(0.05))
        assertThat(LaunchBackgroundJobScheduler.refinementProbability(0.5), closeTo(0.2375, 0.00001))
        assertThat(LaunchBackgroundJobScheduler.refinementProbability(1.0), equalTo(0.8))
    }

    @Test
    fun `scheduler keeps both queues in peaks first order when refinement draws lose`() {
        val coordinates = (0..2).map { column -> LaunchBackgroundTileCoordinate(column, 0) }
        val scheduler = LaunchBackgroundJobScheduler(coordinates, draw = { 1.0 })
        val jobs = buildList {
            repeat(6) {
                scheduler.nextJob()?.also { job ->
                    add(job)
                    scheduler.complete(job)
                }
            }
        }

        assertThat(jobs.map(LaunchBackgroundRenderJob::quality), equalTo(List(3) { LaunchBackgroundTileQuality.PREVIEW } + List(3) { LaunchBackgroundTileQuality.REFINEMENT }))
        assertThat(jobs.take(3).map(LaunchBackgroundRenderJob::coordinate), equalTo(coordinates))
        assertThat(jobs.takeLast(3).map(LaunchBackgroundRenderJob::coordinate), equalTo(coordinates))
    }

    @Test
    fun `scheduler can refine a published preview while other previews remain`() {
        val coordinates = (0..2).map { column -> LaunchBackgroundTileCoordinate(column, 0) }
        val scheduler = LaunchBackgroundJobScheduler(coordinates, draw = { 0.0 })
        val preview = scheduler.nextJob()!!
        scheduler.complete(preview)

        val refinement = scheduler.nextJob()!!

        assertThat(preview.quality, equalTo(LaunchBackgroundTileQuality.PREVIEW))
        assertThat(refinement, equalTo(LaunchBackgroundRenderJob(coordinates.first(), LaunchBackgroundTileQuality.REFINEMENT)))
    }

    @Test
    fun `seeded scheduler decisions are deterministic`() {
        val coordinates = (0..7).map { column -> LaunchBackgroundTileCoordinate(column, 0) }

        fun drain(seed: Long): List<LaunchBackgroundRenderJob> {
            val random = Random(seed)
            val scheduler = LaunchBackgroundJobScheduler(coordinates, random::nextDouble)
            return buildList {
                while (size < coordinates.size * 2) {
                    scheduler.nextJob()?.also { job ->
                        add(job)
                        scheduler.complete(job)
                    }
                }
            }
        }

        assertThat(drain(91L), equalTo(drain(91L)))
    }

    @Test
    fun `generator caps active rendering while continuously producing both qualities`() = runBlocking {
        val background = LaunchTopographyBackground.create(1600, 160, 73L)
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val updates = Collections.synchronizedList(mutableListOf<LaunchBackgroundTileUpdate>())
        val order = LaunchBackgroundTilePlan.tiles(background.textureWidth, background.textureHeight)
        val generator = LaunchBackgroundTileGenerator(
            parallelism = 4,
            render = { request ->
                active.incrementAndGet().also { current ->
                    maximumActive.updateAndGet { previous -> maxOf(previous, current) }
                }
                Thread.sleep(5)
                active.decrementAndGet()
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = {},
            priorityOrder = { order },
        )

        generator.generate(background, onTile = updates::add)

        assertThat(maximumActive.get(), lessThanOrEqualTo(4))
        assertThat(updates, hasSize(order.size * 2))
        assertThat(updates.count { it.quality == LaunchBackgroundTileQuality.PREVIEW }, equalTo(order.size))
        assertThat(updates.count { it.quality == LaunchBackgroundTileQuality.REFINEMENT }, equalTo(order.size))
    }

    @Test
    fun `reveal delays release render capacity immediately`() = runBlocking {
        val background = LaunchTopographyBackground.create(800, 160, 73L)
        val order = LaunchBackgroundTilePlan.tiles(background.textureWidth, background.textureHeight)
        val revealGate = CompletableDeferred<Unit>()
        val rendered = AtomicInteger()
        val generator = LaunchBackgroundTileGenerator(
            parallelism = 2,
            render = { request ->
                rendered.incrementAndGet()
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = { revealGate.await() },
            priorityOrder = { order },
        )

        val generation = async {
            generator.generate(background, onTile = {})
        }
        withTimeout(2_000) {
            while (rendered.get() < order.size) yield()
        }

        assertThat(rendered.get(), equalTo(order.size))
        revealGate.complete(Unit)
        generation.await()
    }

    @Test
    fun `failed preview remains eligible for a successful refinement`() = runBlocking {
        val background = LaunchTopographyBackground.create(160, 160, 73L)
        val updates = mutableListOf<LaunchBackgroundTileUpdate>()
        val failures = mutableListOf<LaunchBackgroundRenderJob>()
        val generator = LaunchBackgroundTileGenerator(
            parallelism = 1,
            render = { request ->
                if (request.width == 36) error("broken preview")
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = {},
            priorityOrder = { LaunchBackgroundTilePlan.tiles(it.textureWidth, it.textureHeight) },
        )

        generator.generate(
            background,
            onTile = updates::add,
            onFailure = { job, _, _ -> failures += job },
        )

        assertThat(failures.map(LaunchBackgroundRenderJob::quality), equalTo(listOf(LaunchBackgroundTileQuality.PREVIEW)))
        assertThat(updates.map(LaunchBackgroundTileUpdate::quality), equalTo(listOf(LaunchBackgroundTileQuality.REFINEMENT)))
    }

    @Test
    fun `failed refinement leaves its preview cached`() = runBlocking {
        val background = LaunchTopographyBackground.create(160, 160, 91L)
        val generator = LaunchBackgroundTileGenerator(
            parallelism = 1,
            render = { request ->
                if (request.width == 288) error("broken refinement")
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = {},
            priorityOrder = { LaunchBackgroundTilePlan.tiles(it.textureWidth, it.textureHeight) },
        )

        generator.generate(
            background,
            onTile = { update -> background.accept(update) },
        )

        assertThat(background.tiles().single().preview != null, equalTo(true))
        assertThat(background.tiles().single().refinement, equalTo(null))
    }

    @Test
    fun `cached refinement replaces and releases its preview`() {
        val background = LaunchTopographyBackground.create(160, 160, 17L)
        val coordinate = LaunchBackgroundTileCoordinate(0, 0)

        background.accept(update(coordinate, LaunchBackgroundTileQuality.PREVIEW, 36))
        assertThat(background.tiles().single().preview != null, equalTo(true))
        background.accept(update(coordinate, LaunchBackgroundTileQuality.REFINEMENT, 288))

        assertThat(background.available, equalTo(true))
        assertThat(background.tiles().single().preview, equalTo(null))
        assertThat(background.tiles().single().refinement != null, equalTo(true))
    }

    @Test
    fun `tile reveal fades previews and refinements independently`() {
        assertThat(LaunchBackgroundTileReveal.previewAlpha(-1.0), equalTo(0.0))
        assertThat(LaunchBackgroundTileReveal.previewAlpha(60.0), closeTo(0.5, 0.00001))
        assertThat(LaunchBackgroundTileReveal.previewAlpha(120.0), equalTo(1.0))
        assertThat(LaunchBackgroundTileReveal.refinementAlpha(0.0), equalTo(0.0))
        assertThat(LaunchBackgroundTileReveal.refinementAlpha(140.0), closeTo(0.5, 0.00001))
        assertThat(LaunchBackgroundTileReveal.refinementAlpha(280.0), equalTo(1.0))
    }

    private fun update(
        coordinate: LaunchBackgroundTileCoordinate,
        quality: LaunchBackgroundTileQuality,
        size: Int,
    ) = LaunchBackgroundTileUpdate(
        coordinate = coordinate,
        quality = quality,
        bitmap = Bitmap32(size, size),
        renderMilliseconds = 1L,
        arrivalJitterMilliseconds = 40L,
    )
}
