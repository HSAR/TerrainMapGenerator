package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.model.Compartment
import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.model.CompartmentType.AIRLOCK
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE
import io.hsar.mapgenerator.model.CompartmentType.CARGO
import io.hsar.mapgenerator.model.CompartmentType.CREW_QUARTERS
import io.hsar.mapgenerator.model.CompartmentType.ENGINE
import io.hsar.mapgenerator.model.CompartmentType.HANGAR
import io.hsar.mapgenerator.model.CompartmentType.WEAPONS
import java.lang.Integer.max
import kotlin.math.roundToInt

object CompartmentGenerator {

    fun generateCompartment(compartmentType: CompartmentType): Compartment {
        val (baseDecks, baseInternalArea) = BaseSizes.getCompartmentBaseSize(compartmentType)
        val sizeMultiplier = CompartmentSize.values().random().multiplier

        val decks = max(1, (baseDecks * sizeMultiplier).roundToInt())
        val internalAreaMin = baseInternalArea.first * sizeMultiplier
        val internalAreaMax = baseInternalArea.second * sizeMultiplier

        TODO()
    }

    private enum class CompartmentSize(val multiplier: Double) {
        SMALL(1.0),
        MEDIUM(2.0),
        LARGE(3.0),
    }

    private data class BaseSize(val decks: Double, val sizeRange: Pair<Double, Double>)

    /**
     * This enum stores the base size of a compartment, subject to modification by multiplication.
     */
    private object BaseSizes {

        fun getCompartmentBaseSize(compartmentType: CompartmentType): BaseSize = when (compartmentType) {
            BRIDGE -> BaseSize(0.6, 50.0 to 100.0)
            ENGINE -> BaseSize(1.0, 200.0 to 300.0)
            HANGAR -> BaseSize(1.0, 200.0 to 300.0)
            WEAPONS -> BaseSize(0.6, 30.0 to 50.0)
            CARGO -> BaseSize(0.6, 50.0 to 100.0)
            CREW_QUARTERS -> BaseSize(0.2, 30.0 to 50.0)
            AIRLOCK -> BaseSize(0.2, 5.0 to 10.0)
            else -> throw IllegalStateException("Compartment type $compartmentType does not resize")
        }
    }
}