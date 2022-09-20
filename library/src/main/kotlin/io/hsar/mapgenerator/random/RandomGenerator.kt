package io.hsar.mapgenerator.random

import java.util.concurrent.ThreadLocalRandom

object RandomGenerator {

    fun generateBoolean() = ThreadLocalRandom.current().nextBoolean()

    fun generateUniform(rangeMin: Int, rangeMax: Int) = ThreadLocalRandom.current().nextInt(rangeMin, rangeMax)

    fun generateUniform(rangeMin: Double, rangeMax: Double) = ThreadLocalRandom.current().nextDouble(rangeMin, rangeMax)

    /**
     * Notes on usage:
     * 70% of points will lie within 1 std dev.
     * 96% of points will lie within 2 std dev.
     * 99.7% of points will lie within 3 std dev.
     */
    fun generateGaussian(average: Double, stdDev: Double) = (ThreadLocalRandom.current().nextGaussian() * stdDev) + average
}