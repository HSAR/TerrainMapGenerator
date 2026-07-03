package io.hsar.mapgenerator.grid

import kotlin.math.ceil
import kotlin.math.floor

interface Coord {
    val units: Int

    fun lineNumber(gridScale: Int): Double =
        units.toDouble() / gridScale

    fun lineNumberCeil(gridScale: Int): Double =
        ceil(lineNumber(gridScale))

    fun lineNumberFloor(gridScale: Int): Double =
        floor(lineNumber(gridScale))

    fun format(): String
}
