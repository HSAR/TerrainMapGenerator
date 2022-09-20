package io.hsar.mapgenerator.model

data class Ship(
    val name: String = "Unknown Contact",
    val compartments: List<Compartment>,
    val decks: List<Deck>
)
