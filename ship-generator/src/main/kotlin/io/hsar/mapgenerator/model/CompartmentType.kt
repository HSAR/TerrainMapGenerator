package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.model.CompartmentPriority.PRIMARY
import io.hsar.mapgenerator.model.CompartmentPriority.SECONDARY
import io.hsar.mapgenerator.model.CompartmentPriority.TERTIARY

enum class CompartmentType(val friendlyName: String, val priority: CompartmentPriority) {
    BRIDGE("Bridge", PRIMARY),
    ENGINE("Engineering", PRIMARY),
    HANGAR("Hangar", PRIMARY),
    WEAPONS("Weapons", SECONDARY),
    CARGO("Cargo", SECONDARY),
    CREW_QUARTERS("Crew Quarters", SECONDARY),
    AIRLOCK("Airlock", TERTIARY),
    CORRIDOR("Corridor", TERTIARY),
    VERTICHAL_HATCH("Hatch", TERTIARY),
}

