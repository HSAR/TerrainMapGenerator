package io.hsar.mapgenerator.grid

enum class ReferenceSystem {
    MGRS,
    EPSILON,
    ;

    val displayUnit: String
        get() = when (this) {
            MGRS -> "m"
            EPSILON -> "U"
        }

    val gridScale: Int
        get() = when (this) {
            MGRS -> 1_000
            EPSILON -> 20
        }

    fun scaleInputToUnitsPerPixel(value: Double): Double =
        when (this) {
            MGRS -> value
            EPSILON -> 1.0 / value
        }

    fun parseCoord(value: String): Coord? =
        when (this) {
            MGRS -> MgrsCoord.parse(value)
            EPSILON -> EpsilonCoord.parse(value)
        }

    fun parseCoord(value: String, axis: CoordAxis, gridScale: Int): Coord? =
        when (this) {
            MGRS -> MgrsCoord.parse(value)
            EPSILON -> EpsilonCoord.parseDesignator(value, axis, gridScale)
        }

    fun formatCoord(coord: Coord, axis: CoordAxis, gridScale: Int): String =
        when (this) {
            MGRS -> coord.format()
            EPSILON -> EpsilonCoord.formatDesignator(coord, axis, gridScale)
        }

    fun coordFromUnits(units: Int): Coord =
        when (this) {
            MGRS -> MgrsCoord.fromUnits(units)
            EPSILON -> EpsilonCoord.fromUnits(units)
        }

    fun coordFromLineNumber(lineNumber: Double, gridScale: Int): Coord =
        when (this) {
            MGRS -> MgrsCoord.fromLineNumber(lineNumber, gridScale)
            EPSILON -> EpsilonCoord.fromLineNumber(lineNumber, gridScale)
        }
}
