package io.hsar.mapgenerator.noise

import com.raylabz.opensimplex.OpenSimplexNoise
import com.raylabz.opensimplex.Range
import kotlin.random.Random

/**
 * Simplex noise generator.
 */
class NoiseGenerator(private val seed: Long = Random.Default.nextLong()) {
    val noise = OpenSimplexNoise(seed)

    /**
     * Generate a 2D array of Simplex Noise. Results are in the range of 0.0 to 1.0.
     */
    fun generate2DArray(height: Int, width: Int) = noise
        .getNoise2DArray(0, 0, width, height)
        .map { eachRow ->
            eachRow.map { it.getValue(DEFAULT_RANGE) }
        }

    companion object {
        private val DEFAULT_RANGE = Range(0.0, 1.0)
    }
}