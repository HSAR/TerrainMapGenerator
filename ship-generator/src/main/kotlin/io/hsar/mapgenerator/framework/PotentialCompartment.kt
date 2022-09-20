package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.model.CompartmentType

data class PotentialCompartment(
    val compartmentType: CompartmentType,
    val decks: Int,
    val potentialSizeRange: Pair<Double, Double>
)
