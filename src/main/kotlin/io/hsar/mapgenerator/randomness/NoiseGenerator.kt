package io.hsar.mapgenerator.randomness

import com.raylabz.opensimplex.OpenSimplexNoise
import com.raylabz.opensimplex.Range
import kotlin.random.Random

/**
 * Simplex noise generator.
 */
class NoiseGenerator(seed: Long = Random.Default.nextLong()) {
    val noise = OpenSimplexNoise(seed)

    /**
     * Generate a 2D array of Simplex noise values. Results are in the range of 0.0 to 1.0.
     */
    fun generate2DArray(width: Int, height: Int) = noise
        .getNoise2DArray(0, 0, width, height)
        .map { eachRow ->
            eachRow.map { it.getValue(DEFAULT_RANGE) }.toDoubleArray()
        }.toTypedArray()

    /**
     * Generate the Simplex noise value for the given co-ordinate. Results are in the range of 0.0 to 1.0.
     */
    fun generatePoint(x: Double, y: Double, scaleFactor: Double = 1.0) = noise
        .getNoise2D(x * scaleFactor, y * scaleFactor)
        .getValue(DEFAULT_RANGE)

    companion object {
        private val DEFAULT_RANGE = Range(0.0, 1.0)

        val DEFAULT = NoiseGenerator()
    }
}