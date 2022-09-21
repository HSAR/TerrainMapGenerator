package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.graph.Point

data class CompartmentSlice(
    val id: String,
    val name: String,
    val type: CompartmentType,
    val shape: List<Point>
) {
    companion object {
        fun generateName(baseName: String, totalDeckSpan: Int, currDeck: Int): String {
            require(totalDeckSpan > 0)
            return if (totalDeckSpan == 1) {
                baseName
            } else {
                when (currDeck) {
                    0 -> "Upper"
                    totalDeckSpan - 1 -> "Lower"
                    else -> "Mid"
                }
                    .let { descText -> "$baseName ($descText)" }
            }
        }
    }
}
