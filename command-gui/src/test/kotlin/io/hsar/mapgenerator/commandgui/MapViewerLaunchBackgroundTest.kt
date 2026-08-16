package io.hsar.mapgenerator.commandgui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class MapViewerLaunchBackgroundTest {
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
        assertThat(preview.worldUnitsPerPixel, equalTo(4.0))
        assertThat(refinement.width, equalTo(432))
        assertThat(refinement.worldUnitsPerPixel, equalTo(0.5))
        assertThat(preview.worldOriginX, equalTo(refinement.worldOriginX))
        assertThat(preview.worldOriginY, equalTo(refinement.worldOriginY))
    }

    @Test
    fun `refinement raster supersamples a smaller world tile`() {
        val coordinate = MapViewerLaunchTileCoordinate(1, 2)
        val refinement = coordinate.request(73L, MapViewerLaunchTileQuality.REFINEMENT)

        assertThat(MapViewerLaunchTileCoordinate.TILE_SIZE, equalTo(180))
        assertThat(MapViewerLaunchTileCoordinate.PADDED_TILE_SIZE, equalTo(216))
        assertThat(refinement.width, equalTo(432))
        assertThat(refinement.width * refinement.worldUnitsPerPixel, equalTo(216.0))
        assertThat(coordinate.left, equalTo(180))
        assertThat(coordinate.top, equalTo(360))
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
