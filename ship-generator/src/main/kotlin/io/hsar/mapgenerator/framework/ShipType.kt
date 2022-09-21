package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.model.CompartmentSize
import io.hsar.mapgenerator.model.CompartmentSize.MEDIUM
import io.hsar.mapgenerator.model.CompartmentSize.SMALL
import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE
import io.hsar.mapgenerator.model.CompartmentType.CARGO
import io.hsar.mapgenerator.model.CompartmentType.ENGINE
import io.hsar.mapgenerator.model.CompartmentType.WEAPONS

/**
 * A classification of ship, including its essential statistics.
 * @param baseCompartments A number of compartments which must be generated. An engine room should always be present.
 * @param minCargoSpace A [Double] value from 0.0-1.0 describing the minimum proportional area that will be cargo bays.
 * @param minMilitarySpace A [Double] value from 0.0-1.0 describing the minimum proportional area that
 * will be militarised. This includes weapons bays, hangars and other military-purposed compartments.
 */
enum class ShipType(
    val deckRange: Pair<Int, Int>,
    val mainCorridors: Int,
    val sizeRange: Pair<Double, Double>,
    val compartmentScale: CompartmentScale,
    val baseCompartments: List<Pair<CompartmentType, CompartmentSize>>,
    val minCargoSpace: Double = 0.0,
    val minMilitarySpace: Double = 0.0,
) {
    LIGHT_FREIGHTER(
        deckRange = 1 to 2,
        mainCorridors = 1,
        sizeRange = 200.0 to 400.0,
        compartmentScale = CompartmentScale.SMALL,
        baseCompartments = listOf(
            ENGINE to MEDIUM,
            CARGO to MEDIUM,
            CARGO to MEDIUM,
            BRIDGE to SMALL
        ),
        minCargoSpace = 0.4
    ),
    CORVETTE(
        deckRange = 1 to 1,
        mainCorridors = 1,
        sizeRange = 100.0 to 250.0,
        compartmentScale = CompartmentScale.SMALL,
        baseCompartments = listOf(
            ENGINE to MEDIUM,
            BRIDGE to SMALL,
            WEAPONS to SMALL
        ),
        minCargoSpace = 0.2,
        minMilitarySpace = 0.3
    ),
}