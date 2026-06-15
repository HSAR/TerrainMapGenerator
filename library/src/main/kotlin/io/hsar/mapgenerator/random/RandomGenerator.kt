package io.hsar.mapgenerator.random

import java.util.Random
import java.util.concurrent.ThreadLocalRandom

object RandomGenerator {

    fun generateBoolean(random: Random = ThreadLocalRandom.current()) = random.nextBoolean()

    fun generateUniform(rangeMin: Int, rangeMax: Int, random: Random = ThreadLocalRandom.current()) =
        random.nextInt(rangeMin, rangeMax)

    fun Pair<Int, Int>.randomUniform(random: Random = ThreadLocalRandom.current()) = this.let { (min, max) -> generateUniform(min, max, random) }

    fun generateUniform(rangeMin: Double, rangeMax: Double, random: Random = ThreadLocalRandom.current()) =
        random.nextDouble(rangeMin, rangeMax)

    fun Pair<Double, Double>.randomUniform(random: Random = ThreadLocalRandom.current()) =
        this.let { (min, max) -> generateUniform(min, max, random) }

    /**
     * Notes on usage:
     * 70% of points will lie within 1 std dev.
     * 96% of points will lie within 2 std dev.
     * 99.7% of points will lie within 3 std dev.
     */
    fun generateGaussian(average: Double, stdDev: Double, random: Random = ThreadLocalRandom.current()) =
        (random.nextGaussian() * stdDev) + average

    /**
     * Notes on usage:
     * 70% of points will lie within 1 std dev.
     * 96% of points will lie within 2 std dev.
     * 99.7% of points will lie within 3 std dev.
     */
    fun Pair<Double, Double>.randomGaussian(random: Random = ThreadLocalRandom.current(), stdDevFactor: Double = 0.4) = this.toList()
        .average()
        .let { avg -> generateUniform(avg, avg * stdDevFactor, random) }
}
