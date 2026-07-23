package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import korlibs.math.geom.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object MapSelectionPivot {
    fun pivotCenter(snapshots: List<MapPlacedObjectSnapshot>, coordinateSpace: MapCoordinateSpace): MapCoordinate =
        snapshots
            .combinedSelectionBounds(coordinateSpace)
            ?.let { bounds ->
                MapCoordinate(
                    x = (bounds.left + bounds.right) * 0.5,
                    y = (bounds.top + bounds.bottom) * 0.5,
                )
            }
            ?: error("Cannot calculate a pivot center without selected objects")

    fun bearingAngle(originWorld: Vector2D, targetWorld: Vector2D): Double =
        atan2(targetWorld.x - originWorld.x, originWorld.y - targetWorld.y)

    fun rotateSnapshots(
        originalSnapshots: List<MapPlacedObjectSnapshot>,
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
        shiftHeld: Boolean = false,
    ): List<MapPlacedObjectSnapshot> =
        originalSnapshots.map { original ->
            when (original) {
                is UnitSnapshot -> original.rotateUnit(pivotCenterWorld, deltaRadians, coordinateSpace, shiftHeld)
                is DrawingSnapshot -> original.rotateDrawing(pivotCenterWorld, deltaRadians, coordinateSpace, shiftHeld)
                is GraphicSnapshot -> original.copy(
                    mapCoordinate = coordinateSpace.toMapCoordinate(
                        rotateWorldPoint(coordinateSpace.toWorld(original.mapCoordinate), pivotCenterWorld, deltaRadians),
                    ),
                )
            }
        }

    private fun UnitSnapshot.rotateUnit(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
        shiftHeld: Boolean,
    ): UnitSnapshot {
        val totalDegrees = (facingDegrees ?: 0) + Math.toDegrees(deltaRadians)
        return copy(
            facingDegrees = if (shiftHeld) {
                snapDegrees(totalDegrees)
            } else {
                normalizeDegrees(totalDegrees.roundToInt())
            },
            mapCoordinate = coordinateSpace.toMapCoordinate(
                rotateWorldPoint(
                    world = coordinateSpace.toWorld(mapCoordinate),
                    pivotWorld = pivotCenterWorld,
                    deltaRadians = deltaRadians,
                ),
            ),
        )
    }

    private fun DrawingSnapshot.rotateDrawing(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
        shiftHeld: Boolean,
    ): DrawingSnapshot =
        copy(
            drawing = drawing.rotate(
                pivotCenterWorld = pivotCenterWorld,
                deltaRadians = deltaRadians,
                coordinateSpace = coordinateSpace,
                shiftHeld = shiftHeld,
            ),
        )

    private fun MapDrawing.rotate(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
        shiftHeld: Boolean,
    ): MapDrawing =
        rotationDelta(deltaRadians, shiftHeld).let { effectiveDeltaRadians ->
            when (this) {
                is FreeformDrawing -> copy(points = points.map { it.rotate(pivotCenterWorld, effectiveDeltaRadians, coordinateSpace) })
            is LineDrawing -> copy(
                start = start.rotate(pivotCenterWorld, effectiveDeltaRadians, coordinateSpace),
                end = end.rotate(pivotCenterWorld, effectiveDeltaRadians, coordinateSpace),
            )

            is RectangleDrawing -> {
                val originalCenter = MapDrawingGeometry.center(start, end)
                val rotatedCenter = originalCenter.rotate(pivotCenterWorld, effectiveDeltaRadians, coordinateSpace)
                copy(
                    start = start.translate(originalCenter, rotatedCenter),
                    end = end.translate(originalCenter, rotatedCenter),
                    rotationDegrees = rotatedDegrees(rotationDegrees, effectiveDeltaRadians, shiftHeld),
                )
            }

            is OvalDrawing -> copy(
                center = center.rotate(pivotCenterWorld, effectiveDeltaRadians, coordinateSpace),
                rotationDegrees = rotatedDegrees(rotationDegrees, effectiveDeltaRadians, shiftHeld),
            )
            }
        }

    private fun rotationDelta(deltaRadians: Double, shiftHeld: Boolean): Double =
        if (shiftHeld) Math.toRadians(snapDegrees(Math.toDegrees(deltaRadians).toDouble()).toDouble()) else deltaRadians

    private fun rotatedDegrees(rotationDegrees: Double, deltaRadians: Double, shiftHeld: Boolean): Double {
        val totalDegrees = rotationDegrees + Math.toDegrees(deltaRadians)
        return if (shiftHeld) {
            snapDegrees(totalDegrees).toDouble()
        } else {
            normalizeDegrees(totalDegrees)
        }
    }

    private fun MapDrawingPoint.translate(fromCenter: MapDrawingPoint, toCenter: MapDrawingPoint): MapDrawingPoint =
        MapDrawingPoint(
            x = x + toCenter.x - fromCenter.x,
            y = y + toCenter.y - fromCenter.y,
        )

    private fun MapDrawingPoint.rotate(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
    ): MapDrawingPoint =
        coordinateSpace
            .toMapCoordinate(
                rotateWorldPoint(
                    world = coordinateSpace.toWorld(MapCoordinate(x, y)),
                    pivotWorld = pivotCenterWorld,
                    deltaRadians = deltaRadians,
                ),
            )
            .let { MapDrawingPoint(it.x, it.y) }

    private fun rotateWorldPoint(
        world: Vector2D,
        pivotWorld: Vector2D,
        deltaRadians: Double,
    ): Vector2D {
        val dx = world.x - pivotWorld.x
        val dy = world.y - pivotWorld.y
        return Vector2D(
            x = pivotWorld.x + dx * cos(deltaRadians) - dy * sin(deltaRadians),
            y = pivotWorld.y + dx * sin(deltaRadians) + dy * cos(deltaRadians),
        )
    }

    private fun normalizeDegrees(degrees: Int): Int =
        ((degrees % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    private fun normalizeDegrees(degrees: Double): Double =
        ((degrees % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    private fun snapDegrees(degrees: Double): Int =
        normalizeDegrees((degrees / QUARTER_TURN_DEGREES).roundToInt() * QUARTER_TURN_DEGREES)

    private const val FULL_CIRCLE_DEGREES = 360
    private const val QUARTER_TURN_DEGREES = 45
}
