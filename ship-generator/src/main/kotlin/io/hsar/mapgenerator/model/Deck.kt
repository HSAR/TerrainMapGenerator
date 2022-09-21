package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.graph.GeomUtils.toAWTRectangle
import io.hsar.mapgenerator.graph.GeomUtils.toArea
import io.hsar.mapgenerator.graph.GeomUtils.toShape
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.model.CompartmentPriority.PRIMARY
import io.hsar.mapgenerator.model.CompartmentPriority.SECONDARY
import java.awt.geom.Area

data class Deck(val id: String, val rectangle: Rectangle, val compartmentSlices: List<CompartmentSlice>) {
    val area = rectangle.toArea()

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

    fun validPlacement(rectangle: Rectangle): Boolean {
        val inputRectangle = rectangle.toAWTRectangle()
        require(this.area.contains(inputRectangle))

        val areasToCheck = compartmentSlices.map { Area(it.shape.toShape()) }

        // Return true if all internal compartment slices do not intersect the input rectangle
        return areasToCheck.map { it.intersects(inputRectangle) }.all { !it }
    }
}
