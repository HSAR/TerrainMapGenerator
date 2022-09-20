package model

import io.hsar.mapgenerator.graph.Point

data class Room(
    val name: String,
    val shape: List<Point>
)
