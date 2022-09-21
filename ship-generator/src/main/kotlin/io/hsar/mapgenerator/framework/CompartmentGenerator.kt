package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.MathUtils.roundWithMinValue
import io.hsar.mapgenerator.model.CompartmentSize
import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.random.RandomGenerator.randomUniform

object CompartmentGenerator {

    fun generatePotentialCompartment(
        compartmentType: CompartmentType,
        compartmentScale: CompartmentScale,
        compartmentSize: CompartmentSize,
    ): PotentialCompartment {
        val scale = compartmentScale.multiplier
        val decks = (compartmentSize.deckRange.randomUniform() * scale).roundWithMinValue(1)
        val areaMin = compartmentSize.areaRange.first * scale
        val areaMax = compartmentSize.areaRange.second * scale

        return PotentialCompartment(compartmentType, decks, areaMin to areaMax)
    }
}