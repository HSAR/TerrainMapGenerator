package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.graph.Point

data class Compartment(
    val name: String,
    val type: CompartmentType,
    val shape: List<Point>
)
