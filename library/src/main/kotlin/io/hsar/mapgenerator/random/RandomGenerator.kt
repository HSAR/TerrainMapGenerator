package io.hsar.mapgenerator.random

import java.util.concurrent.ThreadLocalRandom

object RandomGenerator {

    fun generateBoolean() = ThreadLocalRandom.current().nextBoolean()

    fun generateUniform(rangeMin: Int, rangeMax: Int) = ThreadLocalRandom.current().nextInt(rangeMin, rangeMax)

    fun Pair<Int, Int>.randomUniform() = this.let { (min, max) -> generateUniform(min, max) }

    fun generateUniform(rangeMin: Double, rangeMax: Double) = ThreadLocalRandom.current().nextDouble(rangeMin, rangeMax)

    fun Pair<Double, Double>.randomUniform() = this.let { (min, max) -> generateUniform(min, max) }

    /**
     * Notes on usage:
     * 70% of points will lie within 1 std dev.
     * 96% of points will lie within 2 std dev.
     * 99.7% of points will lie within 3 std dev.
     */
    fun generateGaussian(average: Double, stdDev: Double) =
        (ThreadLocalRandom.current().nextGaussian() * stdDev) + average

    /**
     * Notes on usage:
     * 70% of points will lie within 1 std dev.
     * 96% of points will lie within 2 std dev.
     * 99.7% of points will lie within 3 std dev.
     */
    fun Pair<Double, Double>.randomGaussian(stdDevFactor: Double = 0.4) = this.toList()
        .average()
        .let { avg -> generateUniform(avg, avg * stdDevFactor) }
}