package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.contourgenerator.ContourTracingOptions
import io.hsar.mapgenerator.contourgenerator.TopographyGenerator
import io.hsar.mapgenerator.contourgenerator.TopographyRenderer
import io.hsar.mapgenerator.contourgenerator.TopographyRequest
import java.awt.image.BufferedImage
import kotlin.math.ceil

data class MapViewerLaunchBackground(
    val image: BufferedImage,
    val terrainProfile: String,
    val seed: Long,
)

/**
 * Produces the same seeded topographic material as the KorGE launch shell at a
 * deliberately reduced raster resolution. Compose scales it to the viewport;
 * the world-space sample extent and palette remain equivalent to KorGE.
 */
object MapViewerLaunchBackgroundRenderer {
    fun render(
        viewportWidth: Int,
        viewportHeight: Int,
        seed: Long,
        rasterScale: Double = DEFAULT_RASTER_SCALE,
    ): MapViewerLaunchBackground {
        require(viewportWidth > 0 && viewportHeight > 0)
        require(rasterScale in 0.1..1.0)
        val width = ceil(viewportWidth * rasterScale).toInt().coerceAtLeast(1)
        val height = ceil(viewportHeight * rasterScale).toInt().coerceAtLeast(1)
        val map = TopographyGenerator.generate(
            TopographyRequest(
                width = width,
                height = height,
                seed = seed,
                worldUnitsPerPixel = SOURCE_SCALE / rasterScale,
                noiseScale = NOISE_SCALE,
                contourLevels = CONTOUR_LEVELS,
                tracing = ContourTracingOptions(
                    stepPixels = 2.0,
                    seedGridStepPixels = 12,
                    maximumPointsPerDirection = 20_000,
                ),
            ),
        )
        return MapViewerLaunchBackground(
            image = TopographyRenderer.render(map),
            terrainProfile = map.resolvedTerrainProfile.name
                .split('_')
                .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercase) },
            seed = seed,
        )
    }

    private const val DEFAULT_RASTER_SCALE = 0.5
    private const val SOURCE_SCALE = 1.5
    private const val NOISE_SCALE = 475.0
    private val CONTOUR_LEVELS = (5..15).map { it * 0.05 }
}
