package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.model.CompartmentPriority.PRIMARY
import io.hsar.mapgenerator.model.CompartmentPriority.SECONDARY

data class Deck(val id: String, val compartmentSlices: List<CompartmentSlice>) {
    val name: String
        get() = compartmentSlices.sortedBy { it.type }
            .let { sortedCompartments ->
                when {
                    sortedCompartments.any { it.type.priority == PRIMARY } -> {
                        sortedCompartments.filter { it.type.priority == PRIMARY }
                            .distinctBy { it.type }
                            .joinToString(separator = "/") { it.type.friendlyName }
                    }
                    sortedCompartments.any { it.type.priority == SECONDARY } -> {
                        sortedCompartments.filter { it.type.priority == SECONDARY }
                            .distinctBy { it.type }
                            .joinToString(separator = "/") { it.type.friendlyName }
                    }
                    else -> {
                        "Utility"
                    }
                }
            }
            .let { compartmentIds ->
                "$id ($compartmentIds)"
            }
}
