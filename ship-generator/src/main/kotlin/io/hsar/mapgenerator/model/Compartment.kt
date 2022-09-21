package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.graph.Rectangle

/**
 * A compartment is a space that may span multiple decks, which are represented by compartment slices.
 */
data class Compartment(
    val name: String,
    val type: CompartmentType,
    val decks: Collection<String>,
    val rectangle: Rectangle,
)

enum class CompartmentType(val friendlyName: String, val priority: CompartmentPriority) {
    BRIDGE("Bridge", CompartmentPriority.PRIMARY),
    ENGINE("Engineering", CompartmentPriority.PRIMARY),
    HANGAR("Hangar", CompartmentPriority.PRIMARY),
    CARGO("Cargo", CompartmentPriority.PRIMARY),
    WEAPONS("Weapons", CompartmentPriority.SECONDARY),
    CREW_QUARTERS("Crew Quarters", CompartmentPriority.SECONDARY),
    AIRLOCK("Airlock", CompartmentPriority.TERTIARY),
    CORRIDOR("Corridor", CompartmentPriority.TERTIARY),
    VERTICAL_HATCH("Hatch", CompartmentPriority.TERTIARY),
}

enum class CompartmentSize(val deckRange: Pair<Double, Double>, val areaRange: Pair<Double, Double>) {
    LARGE(0.8 to 1.2, 200.0 to 300.0), // Engine bays, main cargo, weapons
    MEDIUM(0.4 to 0.8, 50.0 to 100.0), // Small cargo, weapons, 20-person berths
    SMALL(0.3 to 0.8, 20.0 to 40.0), // Large berths, weapons, 10-person berths
    VERY_SMALL(0.2 to 0.4, 5.0 to 10.0), // Airlocks, storage cabinets, 4-person berths
    UNBOUNDED(0.0 to 0.0, 0.0 to Double.MAX_VALUE), // Corridors and hatches have their own size rules
}

/**
 * Defines presence of compartment on deck name
 */
enum class CompartmentPriority {
    PRIMARY, // Always show, pair up if more than one
    SECONDARY, // Show only if no primary compartments are on this deck
    TERTIARY // Do not show on deck name
}