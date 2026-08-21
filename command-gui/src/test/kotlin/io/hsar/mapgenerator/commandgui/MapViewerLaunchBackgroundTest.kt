package io.hsar.mapgenerator.commandgui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

class MapViewerLaunchBackgroundTest {
    @Test
    fun `background generation uses at most four workers`() {
        assertThat(MapViewerLaunchTilePlan.parallelism(1), equalTo(1))
        assertThat(MapViewerLaunchTilePlan.parallelism(2), equalTo(2))
        assertThat(MapViewerLaunchTilePlan.parallelism(16), equalTo(4))
        assertThat(MapViewerLaunchTilePlan.parallelism(64), equalTo(4))
    }

    @Test
    fun `default background worker runs at minimum thread priority`() = runBlocking {
        val priorities = mutableListOf<Int>()
        val coordinate = MapViewerLaunchTileCoordinate(0, 0)
        val generator = MapViewerLaunchBackgroundBatchGenerator(
            render = { request ->
                priorities += Thread.currentThread().priority
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = {},
            priorityOrder = { listOf(coordinate) },
        )

        generator.generate(MapViewerLaunchBackgroundSpec(73L, 360, 360), onTile = {})

        assertThat(priorities, equalTo(listOf(Thread.MIN_PRIORITY, Thread.MIN_PRIORITY)))
    }

    @Test
    fun `tile publication does not gate subsequent preview rendering`() = runBlocking {
        val coordinates = (0..3).map { column -> MapViewerLaunchTileCoordinate(column, 0) }
        val rendered = AtomicInteger()
        val publicationGate = CompletableDeferred<Unit>()
        val generator = MapViewerLaunchBackgroundBatchGenerator(
            parallelism = 2,
            dispatcher = Dispatchers.Unconfined,
            render = { request ->
                rendered.incrementAndGet()
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            wait = { publicationGate.await() },
            priorityOrder = { coordinates },
        )

        val generation = async {
            generator.generate(MapViewerLaunchBackgroundSpec(73L, 1440, 360), onTile = {})
        }
        withTimeout(2_000) { while (rendered.get() < coordinates.size) yield() }

        assertThat(rendered.get(), equalTo(coordinates.size))
        publicationGate.complete(Unit)
        generation.await()
    }

    @Test
    fun `background information retains the legacy diagnostic rows`() {
        val spec = MapViewerLaunchBackgroundSpec(seed = 73L, textureWidth = 1920, textureHeight = 1080)

        assertThat(spec.information.map { it.first }, equalTo(listOf(
            "Biome",
            "Seed",
            "Texture",
            "Noise scale",
            "Contour levels",
        )))
        assertThat(spec.information[1].second, equalTo("73"))
        assertThat(spec.information[2].second, equalTo("1920 x 1080"))
    }

    @Test
    fun `launch background tile requests progress from pixelated preview to full refinement`() {
        val coordinate = MapViewerLaunchTileCoordinate(1, 2)

        val preview = coordinate.request(73L, MapViewerLaunchTileQuality.PREVIEW)
        val refinement = coordinate.request(73L, MapViewerLaunchTileQuality.REFINEMENT)

        assertThat(preview.width, equalTo(54))
        assertThat(preview.worldUnitsPerPixel, equalTo(8.0))
        assertThat(refinement.width, equalTo(432))
        assertThat(refinement.worldUnitsPerPixel, equalTo(1.0))
        assertThat(preview.worldOriginX to preview.worldOriginY, equalTo(324.0 to 684.0))
        assertThat(preview.worldOriginX, equalTo(refinement.worldOriginX))
        assertThat(preview.worldOriginY, equalTo(refinement.worldOriginY))
    }

    @Test
    fun `refinement raster preserves the KorGE world tile extent`() {
        val coordinate = MapViewerLaunchTileCoordinate(1, 2)
        val refinement = coordinate.request(73L, MapViewerLaunchTileQuality.REFINEMENT)

        assertThat(MapViewerLaunchTileCoordinate.TILE_SIZE, equalTo(360))
        assertThat(MapViewerLaunchTileCoordinate.TILE_BLEED, equalTo(36))
        assertThat(MapViewerLaunchTileCoordinate.TILE_CLIP_SIZE, equalTo(362))
        assertThat(MapViewerLaunchTileCoordinate.PADDED_TILE_SIZE, equalTo(432))
        assertThat(refinement.width, equalTo(432))
        assertThat(refinement.width * refinement.worldUnitsPerPixel, equalTo(432.0))
        assertThat(coordinate.left, equalTo(360))
        assertThat(coordinate.top, equalTo(720))
    }

    @Test
    fun `tile plan matches KorGE workload`() {
        val tiles = MapViewerLaunchTilePlan.tiles(1920, 1080)

        assertThat(tiles.size, equalTo(18))
        assertThat(tiles.map(MapViewerLaunchTileCoordinate::column).toSet(), equalTo((0..5).toSet()))
        assertThat(tiles.map(MapViewerLaunchTileCoordinate::row).toSet(), equalTo((0..2).toSet()))
    }

    @Test
    fun `priority sampling matches KorGE tile centres`() {
        val request = MapViewerLaunchTileCoordinate.priorityRequest(
            MapViewerLaunchBackgroundSpec(73L, 1920, 1080),
        )

        assertThat(request.width to request.height, equalTo(6 to 3))
        assertThat(request.worldUnitsPerPixel, equalTo(360.0))
        assertThat(request.worldOriginX to request.worldOriginY, equalTo(180.0 to 180.0))
        assertThat(request.contourLevels, equalTo(emptyList()))
    }

    @Test
    fun `batch generator publishes both qualities for every scheduled tile`() = runBlocking {
        val coordinate = MapViewerLaunchTileCoordinate(0, 0)
        val updates = mutableListOf<MapViewerLaunchTileUpdate>()
        val generator = MapViewerLaunchBackgroundBatchGenerator(
            parallelism = 1,
            dispatcher = Dispatchers.Unconfined,
            render = { request -> BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB) },
            wait = {},
            priorityOrder = { listOf(coordinate) },
        )

        generator.generate(MapViewerLaunchBackgroundSpec(73L, 360, 360), updates::add)

        assertThat(updates.map { it.quality }, equalTo(listOf(
            MapViewerLaunchTileQuality.PREVIEW,
            MapViewerLaunchTileQuality.REFINEMENT,
        )))
        assertThat(updates.map { it.image.width }, equalTo(listOf(54, 432)))
    }

    @Test
    fun `de-pixelation reveal uses the KorGE smoothstep timing`() {
        assertThat(MapViewerLaunchTileReveal.previewAlpha(60.0), closeTo(0.5, 0.00001))
        assertThat(MapViewerLaunchTileReveal.refinementAlpha(140.0), closeTo(0.5, 0.00001))
        assertThat(MapViewerLaunchTileReveal.refinementAlpha(280.0), equalTo(1.0))
    }
}
