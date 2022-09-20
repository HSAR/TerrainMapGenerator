package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.model.CompartmentType.AIRLOCK
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE_ADVANCED
import io.hsar.mapgenerator.model.CompartmentType.CARGO_MAIN
import io.hsar.mapgenerator.model.CompartmentType.CARGO_SMALL
import io.hsar.mapgenerator.model.CompartmentType.CREW_QUARTERS
import io.hsar.mapgenerator.model.CompartmentType.ENGINE
import io.hsar.mapgenerator.model.CompartmentType.HANGAR
import io.hsar.mapgenerator.model.CompartmentType.WEAPONS
import java.lang.Integer.max
import kotlin.math.roundToInt

object CompartmentGenerator {

    fun generatePotentialCompartment(compartmentType: CompartmentType): PotentialCompartment {
        val (baseDecks, baseInternalArea) = BaseSizes.getCompartmentBaseSize(compartmentType)
        val sizeMultiplier = CompartmentScale.values().random().multiplier

        val decks = max(1, (baseDecks * sizeMultiplier).roundToInt())
        val internalAreaMin = baseInternalArea.first * sizeMultiplier
        val internalAreaMax = baseInternalArea.second * sizeMultiplier

        return PotentialCompartment(compartmentType, decks, internalAreaMin to internalAreaMax)
    }

    private data class BaseSize(val decks: Double, val sizeRange: Pair<Double, Double>)

    /**
     * This enum stores the base size of a compartment, subject to modification by multiplication.
     */
    private object BaseSizes {

        fun getCompartmentBaseSize(compartmentType: CompartmentType): BaseSize = when (compartmentType) {
            BRIDGE_ADVANCED -> BaseSize(0.6, 50.0 to 100.0)
            ENGINE -> BaseSize(1.0, 200.0 to 300.0)
            HANGAR -> BaseSize(1.0, 200.0 to 300.0)
            CARGO_MAIN -> BaseSize(1.0, 200.0 to 300.0)
            WEAPONS -> BaseSize(0.6, 30.0 to 50.0)
            CARGO_SMALL -> BaseSize(0.6, 50.0 to 100.0)
            CREW_QUARTERS -> BaseSize(0.2, 30.0 to 50.0)
            AIRLOCK -> BaseSize(0.2, 5.0 to 10.0)
            else -> throw IllegalStateException("Compartment type $compartmentType does not resize")
        }
    }
}