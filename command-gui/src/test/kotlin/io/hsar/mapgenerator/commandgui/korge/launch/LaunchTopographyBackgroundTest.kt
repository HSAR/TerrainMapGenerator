package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.contourgenerator.TopographyRequest
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class LaunchTopographyBackgroundTest {
    @Test
    fun `empty background draws immediately without generating terrain`() {
        val background = LaunchTopographyBackground.empty(73L)

        assertThat(background.seed, equalTo(73L))
        assertThat(background.available, equalTo(false))
        assertThat(background.quality, equalTo(-1))
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
    fun `source size renders at one hundred and fifty percent of viewport dimensions`() {
        val sourceSize = LaunchBackgroundLayout.sourceSize(1920, 1080)

        assertThat(sourceSize, equalTo(2880 to 1620))
    }

    @Test
    fun `progression uses four aligned tactical resolutions`() {
        val stages = LaunchBackgroundProgression.stages(1280, 720)

        assertThat(stages.map { it.width to it.height }, equalTo(listOf(240 to 135, 480 to 270, 960 to 540, 1920 to 1080)))
        assertThat(stages.map(LaunchBackgroundStage::worldUnitsPerPixel), equalTo(listOf(8.0, 4.0, 2.0, 1.0)))
        assertThat(stages.map(LaunchBackgroundStage::smooth), equalTo(listOf(false, false, false, true)))
        assertThat(stages.map { it.width * it.worldUnitsPerPixel }, equalTo(List(4) { 1920.0 }))
        assertThat(stages.map { it.height * it.worldUnitsPerPixel }, equalTo(List(4) { 1080.0 }))
    }

    @Test
    fun `progression rounds fractional source dimensions consistently`() {
        val stages = LaunchBackgroundProgression.stages(801, 601)

        assertThat(stages.map { it.width to it.height }, equalTo(listOf(151 to 113, 301 to 226, 601 to 451, 1202 to 902)))
        assertThat(stages.last().width to stages.last().height, equalTo(1202 to 902))
    }

    @Test
    fun `stage requests preserve terrain settings while scaling tracing`() {
        val requests = LaunchBackgroundProgression.stages(1280, 720).map { it.request(73L) }

        assertThat(requests.map(TopographyRequest::seed).distinct(), equalTo(listOf(73L)))
        assertThat(requests.map(TopographyRequest::noiseScale).distinct(), equalTo(listOf(475.0)))
        assertThat(requests.map(TopographyRequest::contourLevels).distinct().size, equalTo(1))
        assertThat(requests.map { it.tracing.stepPixels }, equalTo(listOf(1.0, 1.0, 1.0, 2.0)))
        assertThat(requests.map { it.tracing.seedGridStepPixels }, equalTo(listOf(2, 3, 6, 12)))
        assertThat(requests.map { it.tracing.maximumPointsPerDirection }, equalTo(listOf(2_500, 5_000, 10_000, 20_000)))
    }

    @Test
    fun `raster sampling remains pixelated until the final stage`() {
        val sampling = LaunchBackgroundProgression.stages(1280, 720).map { stage ->
            LaunchTopographyBackground(73L, stage, Bitmap32(stage.width, stage.height))
                .renderRaster(Container(), 1280, 720)
                ?.smoothing
        }

        assertThat(sampling, equalTo(listOf(false, false, false, true)))
    }

    @Test
    fun `final raster downsamples and is resampled with cover placement after resize`() {
        val finalStage = LaunchBackgroundProgression.stages(1000, 600).last()
        val background = LaunchTopographyBackground(73L, finalStage, Bitmap32(finalStage.width, finalStage.height))

        val initial = background.renderRaster(Container(), 1000, 600)
        val resized = background.renderRaster(Container(), 800, 800)

        assertThat(initial?.smoothing, equalTo(true))
        assertThat(initial?.scaleX, closeTo(2.0 / 3.0, 0.00001))
        assertThat(initial?.scaleY, closeTo(2.0 / 3.0, 0.00001))
        assertThat(resized?.smoothing, equalTo(true))
        assertThat(resized?.scaleX, closeTo(8.0 / 9.0, 0.00001))
        assertThat(resized?.scaleY, closeTo(8.0 / 9.0, 0.00001))
        assertThat(resized?.x, closeTo(-266.66667, 0.0001))
        assertThat(resized?.y, closeTo(0.0, 0.0001))
    }

    @Test
    fun `progression publishes successful stages and continues after failure`() {
        val published = mutableListOf<Int>()
        val failures = mutableListOf<Int>()

        val best = LaunchBackgroundProgression.generate(
            viewportWidth = 1280,
            viewportHeight = 720,
            seed = 73L,
            generate = { request ->
                if (request.width == 480) error("broken second stage")
                BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB)
            },
            onStage = { background, _ -> published += background.quality },
            onFailure = { stage, _, _ -> failures += stage.index },
        )

        assertThat(published, equalTo(listOf(0, 2, 3)))
        assertThat(failures, equalTo(listOf(1)))
        assertThat(best.quality, equalTo(3))
        assertThat(best.available, equalTo(true))
    }

    @Test
    fun `complete progression failure preserves black fallback`() {
        val published = mutableListOf<Int>()
        val failures = mutableListOf<Int>()

        val best = LaunchBackgroundProgression.generate(
            viewportWidth = 320,
            viewportHeight = 180,
            seed = 91L,
            generate = { error("broken generator") },
            onStage = { background, _ -> published += background.quality },
            onFailure = { stage, _, _ -> failures += stage.index },
        )

        assertThat(published, equalTo(emptyList()))
        assertThat(failures, equalTo(listOf(0, 1, 2, 3)))
        assertThat(best.available, equalTo(false))
    }

    @Test
    fun `crossfade uses clamped smoothstep interpolation`() {
        val alphas = listOf(-1.0, 0.0, 75.0, 150.0, 300.0, 400.0).map(LaunchBackgroundCrossfade::alpha)

        assertThat(alphas[0], equalTo(0.0))
        assertThat(alphas[1], equalTo(0.0))
        assertThat(alphas[2], closeTo(0.15625, 0.00001))
        assertThat(alphas[3], closeTo(0.5, 0.00001))
        assertThat(alphas[4], equalTo(1.0))
        assertThat(alphas[5], equalTo(1.0))
    }
}
