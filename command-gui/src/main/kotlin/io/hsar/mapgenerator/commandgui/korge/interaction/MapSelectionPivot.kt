package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
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
    ): List<MapPlacedObjectSnapshot> =
        originalSnapshots.map { original ->
            when (original) {
                is UnitSnapshot -> original.rotateUnit(pivotCenterWorld, deltaRadians, coordinateSpace)
                is DrawingSnapshot -> original.rotateDrawing(pivotCenterWorld, deltaRadians, coordinateSpace)
            }
        }

    private fun UnitSnapshot.rotateUnit(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
    ): UnitSnapshot {
        val deltaDegrees = Math.toDegrees(deltaRadians).roundToInt()
        return copy(
            facingDegrees = normalizeDegrees((facingDegrees ?: 0) + deltaDegrees),
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
    ): DrawingSnapshot =
        copy(
            drawing = drawing.rotate(
                pivotCenterWorld = pivotCenterWorld,
                deltaRadians = deltaRadians,
                coordinateSpace = coordinateSpace,
            ),
        )

    private fun MapDrawing.rotate(
        pivotCenterWorld: Vector2D,
        deltaRadians: Double,
        coordinateSpace: MapCoordinateSpace,
    ): MapDrawing =
        when (this) {
            is FreeformDrawing -> copy(points = points.map { it.rotate(pivotCenterWorld, deltaRadians, coordinateSpace) })
            is LineDrawing -> copy(
                start = start.rotate(pivotCenterWorld, deltaRadians, coordinateSpace),
                end = end.rotate(pivotCenterWorld, deltaRadians, coordinateSpace),
            )

            is RectangleDrawing -> copy(
                start = start.rotate(pivotCenterWorld, deltaRadians, coordinateSpace),
                end = end.rotate(pivotCenterWorld, deltaRadians, coordinateSpace),
            )

            is CircleDrawing -> copy(
                center = center.rotate(pivotCenterWorld, deltaRadians, coordinateSpace),
            )
        }

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

    private const val FULL_CIRCLE_DEGREES = 360
}
