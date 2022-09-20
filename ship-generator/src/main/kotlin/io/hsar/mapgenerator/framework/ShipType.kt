package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE_ADVANCED
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE_BASIC
import io.hsar.mapgenerator.model.CompartmentType.CARGO_MAIN
import io.hsar.mapgenerator.model.CompartmentType.CARGO_SMALL
import io.hsar.mapgenerator.model.CompartmentType.ENGINE

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
    val compartmentScale: CompartmentScale,
    val baseCompartments: List<CompartmentType>,
    val minCargoSpace: Double = 0.0,
    val minMilitarySpace: Double = 0.0,
) {
    LIGHT_FREIGHTER(
        deckRange = 1 to 2,
        mainCorridors = 1,
        compartmentScale = CompartmentScale.SMALL,
        baseCompartments = listOf(
            ENGINE,
            CARGO_MAIN,
            BRIDGE_BASIC
        ),
        minCargoSpace = 0.4
    ),
    CORVETTE(
        deckRange = 1 to 1,
        mainCorridors = 1,
        compartmentScale = CompartmentScale.SMALL,
        baseCompartments = listOf(
            ENGINE,
            CARGO_SMALL,
            BRIDGE_ADVANCED
        ),
        minCargoSpace = 0.2,
        minMilitarySpace = 0.3
    )
}