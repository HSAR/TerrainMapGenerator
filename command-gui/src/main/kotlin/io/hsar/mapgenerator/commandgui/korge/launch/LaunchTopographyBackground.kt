package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.contourgenerator.ContourTracingOptions
import io.hsar.mapgenerator.contourgenerator.TopographyGenerator
import io.hsar.mapgenerator.contourgenerator.TopographyRenderer
import io.hsar.mapgenerator.contourgenerator.TopographyRequest
import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.image
import korlibs.korge.view.xy
import java.awt.image.BufferedImage
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

internal data class LaunchBackgroundStage(
    val index: Int,
    val divisor: Int,
    val width: Int,
    val height: Int,
    val smooth: Boolean,
) {
    val worldUnitsPerPixel: Double = divisor.toDouble()

    fun request(seed: Long): TopographyRequest = TopographyRequest(
        width = width,
        height = height,
        seed = seed,
        worldUnitsPerPixel = worldUnitsPerPixel,
        noiseScale = NOISE_SCALE,
        contourLevels = CONTOUR_LEVELS,
        tracing = ContourTracingOptions(
            stepPixels = (FINAL_TRACE_STEP_PIXELS / divisor).coerceAtLeast(MINIMUM_TRACE_STEP_PIXELS),
            seedGridStepPixels = (FINAL_SEED_GRID_STEP_PIXELS.toDouble() / divisor).roundToInt().coerceAtLeast(MINIMUM_SEED_GRID_STEP_PIXELS),
            maximumPointsPerDirection = (FINAL_MAXIMUM_POINTS / divisor).coerceAtLeast(MINIMUM_MAXIMUM_POINTS),
        ),
    )

    private companion object {
        const val NOISE_SCALE = 475.0
        const val FINAL_TRACE_STEP_PIXELS = 2.0
        const val MINIMUM_TRACE_STEP_PIXELS = 1.0
        const val FINAL_SEED_GRID_STEP_PIXELS = 12
        const val MINIMUM_SEED_GRID_STEP_PIXELS = 2
        const val FINAL_MAXIMUM_POINTS = 20_000
        const val MINIMUM_MAXIMUM_POINTS = 2_500
        val CONTOUR_LEVELS = (5..15).map { it * 0.05 }
    }
}

internal object LaunchBackgroundProgression {
    fun stages(viewportWidth: Int, viewportHeight: Int): List<LaunchBackgroundStage> =
        LaunchBackgroundLayout.sourceSize(viewportWidth, viewportHeight).let { (finalWidth, finalHeight) ->
            STAGE_DIVISORS.mapIndexed { index, divisor ->
                LaunchBackgroundStage(
                    index = index,
                    divisor = divisor,
                    width = ceil(finalWidth.toDouble() / divisor).toInt(),
                    height = ceil(finalHeight.toDouble() / divisor).toInt(),
                    smooth = divisor == 1,
                )
            }
        }

    fun generate(
        viewportWidth: Int,
        viewportHeight: Int,
        seed: Long,
        generate: (TopographyRequest) -> BufferedImage = ::generateTopography,
        onStage: (LaunchTopographyBackground, Long) -> Unit,
        onFailure: (LaunchBackgroundStage, Throwable, Long) -> Unit = { _, _, _ -> },
    ): LaunchTopographyBackground = stages(viewportWidth, viewportHeight)
        .fold(LaunchTopographyBackground.empty(seed)) { best, stage ->
            val startedAt = System.nanoTime()
            runCatching { generate(stage.request(seed)).toBMP32().premultipliedIfRequired() }
                .fold(
                    onSuccess = { bitmap ->
                        LaunchTopographyBackground(seed, stage, bitmap)
                            .also { background -> onStage(background, startedAt.elapsedMilliseconds()) }
                    },
                    onFailure = { error ->
                        onFailure(stage, error, startedAt.elapsedMilliseconds())
                        best
                    },
                )
        }

    private fun generateTopography(request: TopographyRequest): BufferedImage =
        TopographyRenderer.render(TopographyGenerator.generate(request))

    private val STAGE_DIVISORS = listOf(8, 4, 2, 1)
}

internal class LaunchTopographyBackground internal constructor(
    val seed: Long,
    val stage: LaunchBackgroundStage?,
    private val bitmap: Bitmap32?,
) {
    val available: Boolean = bitmap != null
    val quality: Int = stage?.index ?: -1

    fun renderRaster(parent: Container, viewportWidth: Int, viewportHeight: Int): Image? =
        bitmap?.let { source ->
            val placement = LaunchBackgroundLayout.cover(source.width, source.height, viewportWidth, viewportHeight)
            parent.image(source) {
                smoothing = this@LaunchTopographyBackground.stage?.smooth == true
                scaleX = placement.scale
                scaleY = placement.scale
                xy(placement.left, placement.top)
            }
        }

    companion object {
        fun empty(seed: Long): LaunchTopographyBackground =
            LaunchTopographyBackground(seed, null, null)
    }
}

internal object LaunchBackgroundCrossfade {
    const val DURATION_MILLISECONDS = 300.0

    fun alpha(elapsedMilliseconds: Double): Double =
        (elapsedMilliseconds / DURATION_MILLISECONDS)
            .coerceIn(0.0, 1.0)
            .let { progress -> progress * progress * (3.0 - 2.0 * progress) }
}

private fun Long.elapsedMilliseconds(): Long =
    (System.nanoTime() - this) / 1_000_000
