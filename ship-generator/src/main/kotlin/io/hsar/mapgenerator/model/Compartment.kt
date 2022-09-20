package io.hsar.mapgenerator.model

/**
 * A compartment is a space that may span multiple decks, which are represented by compartment slices.
 */
data class Compartment(
    val id: String,
    val name: String,
    val compartmentType: CompartmentType,
    val slices: List<CompartmentSlice>
)
