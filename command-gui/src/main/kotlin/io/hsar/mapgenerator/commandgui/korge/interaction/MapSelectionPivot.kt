package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object MapSelectionPivot {
    fun pivotCenter(units: List<UnitSnapshot>): MapCoordinate =
        requireNotNull(units.singleOrNull()?.mapCoordinate ?: units.takeIf { it.isNotEmpty() }?.let {
            MapCoordinate(
                x = it.map { unit -> unit.mapCoordinate.x }.average(),
                y = it.map { unit -> unit.mapCoordinate.y }.average(),
            )
        }) { "Cannot calculate a pivot center without selected units" }

    fun bearingAngle(originWorld: Vector2D, targetWorld: Vector2D): Double =
        atan2(targetWorld.x - originWorld.x, originWorld.y - targetWorld.y)

    fun rotateUnits(
        originalUnits: List<UnitSnapshot>,
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
    ): List<UnitSnapshot> {
        val deltaDegrees = Math.toDegrees(deltaRadians).roundToInt()
        val rotatePositions = originalUnits.size > 1
        return originalUnits.map { original ->
            original.copy(
                facingDegrees = normalizeDegrees((original.facingDegrees ?: 0) + deltaDegrees),
                mapCoordinate = if (rotatePositions) {
                    rotateAroundPivot(original.mapCoordinate, pivotCenterWorld, deltaRadians, coordinateSpace)
                } else {
                    original.mapCoordinate
                },
            )
        }
    }

    private fun rotateAroundPivot(
        mapCoordinate: MapCoordinate,
        pivotWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
    ): MapCoordinate {
        val world = coordinateSpace.toWorld(mapCoordinate)
        val dx = world.x - pivotWorld.x
        val dy = world.y - pivotWorld.y
        return coordinateSpace.toMapCoordinate(
            Vector2D(
                x = pivotWorld.x + dx * cos(deltaRadians) - dy * sin(deltaRadians),
                y = pivotWorld.y + dx * sin(deltaRadians) + dy * cos(deltaRadians),
            ),
        )
    }

    private fun normalizeDegrees(degrees: Int): Int =
        ((degrees % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    private const val FULL_CIRCLE_DEGREES = 360
}
