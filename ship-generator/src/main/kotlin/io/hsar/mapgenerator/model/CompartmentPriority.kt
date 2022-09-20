package io.hsar.mapgenerator.model

/**
 * Defines presence of compartment on deck name
 */
enum class CompartmentPriority {
    PRIMARY, // Always show, pair up if more than one
    SECONDARY, // Show only if no primary compartments are on this deck
    TERTIARY // Do not show on deck name
}