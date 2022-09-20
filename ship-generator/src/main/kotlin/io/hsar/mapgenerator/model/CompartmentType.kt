package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.model.CompartmentPriority.PRIMARY
import io.hsar.mapgenerator.model.CompartmentPriority.SECONDARY
import io.hsar.mapgenerator.model.CompartmentPriority.TERTIARY

enum class CompartmentType(val friendlyName: String, val priority: CompartmentPriority) {
    BRIDGE_ADVANCED("Bridge", PRIMARY),
    BRIDGE_BASIC("Bridge", PRIMARY),
    ENGINE("Engineering", PRIMARY),
    HANGAR("Hangar", PRIMARY),
    CARGO_MAIN("Cargo", PRIMARY),
    WEAPONS("Weapons", SECONDARY),
    CARGO_SMALL("Cargo", SECONDARY),
    CREW_QUARTERS("Crew Quarters", SECONDARY),
    AIRLOCK("Airlock", TERTIARY),
    CORRIDOR("Corridor", TERTIARY),
    VERTICAL_HATCH("Hatch", TERTIARY),
}

