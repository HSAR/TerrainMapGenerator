package io.hsar.mapgenerator.grid

import kotlin.math.ceil
import kotlin.math.floor

interface Coord {
    val preciseUnits: Double

    val units: Int
        get() = floor(preciseUnits).toInt()

    fun lineNumber(gridScale: Int): Double =
        preciseUnits / gridScale

    fun lineNumberCeil(gridScale: Int): Double =
        ceil(lineNumber(gridScale))

    fun lineNumberFloor(gridScale: Int): Double =
        floor(lineNumber(gridScale))

    fun format(): String
}
