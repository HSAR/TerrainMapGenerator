package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationArgb
import io.hsar.mapgenerator.commandgui.operation.OperationDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint
import io.hsar.mapgenerator.commandgui.operation.OperationFreeformDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationOvalDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing
import io.hsar.mapgenerator.grid.GridLayer
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class MapViewerDrawingMode(val label: String) { FREEFORM("Freeform"), LINE("Line"), RECTANGLE("Rectangle"), OVAL("Oval") }

enum class MapViewerDrawingColour(val label: String, val argb: OperationArgb) {
    RED("Red", OperationArgb.of(255, 190, 62, 60)), BLUE("Blue", OperationArgb.of(255, 39, 106, 136)),
    GREEN("Green", OperationArgb.of(255, 61, 118, 82)), YELLOW("Yellow", OperationArgb.of(255, 128, 110, 41)),
    WHITE("White", OperationArgb.of(255, 238, 238, 238)), LIGHT("Light", OperationArgb.of(255, 180, 180, 180)),
    MAIN("Main", OperationArgb.of(255, 62, 58, 58)), DARK("Dark", OperationArgb.of(255, 24, 12, 12));
}

data class MapViewerMeasurement(
    val origin: MapCoordinate,
    val destination: MapCoordinate,
    val distanceUnits: Double,
    val unitLabel: String,
    val bearingDegrees: Int,
) {
    val distanceText
        get() = "${
            BigDecimal.valueOf(distanceUnits).round(MathContext(4, RoundingMode.HALF_UP)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }$unitLabel"
    val bearingText get() = "${bearingDegrees.toString().padStart(3, '0')}°"
}

fun measureMapDrag(layer: GridLayer, origin: MapCoordinate, destination: MapCoordinate, mapWidth: Int, mapHeight: Int) =
    MapViewerMeasurement(
        origin = origin,
        destination = destination,
        distanceUnits = hypot((destination.x - origin.x) * mapWidth, (destination.y - origin.y) * mapHeight) * layer.unitsPerPixel,
        unitLabel = layer.referenceSystem.displayUnit,
        bearingDegrees = Math.toDegrees(atan2(destination.x - origin.x, -(destination.y - origin.y))).roundToInt().mod(360),
    )

sealed interface MapViewerDrawingGesture {
    val bounds: MapCoordinateBounds
    val preview: OperationDrawing
    fun update(point: MapCoordinate, shiftHeld: Boolean, aspectRatio: Double, minimumFreeformDistance: Double): MapViewerDrawingGesture
    fun committedOrNull(): OperationDrawing?

    data class Freeform(
        val points: List<OperationDrawingPoint>,
        val colour: OperationArgb,
        override val bounds: MapCoordinateBounds,
    ) : MapViewerDrawingGesture {
        override val preview get() = OperationFreeformDrawing(points, colour)
        override fun update(point: MapCoordinate, shiftHeld: Boolean, aspectRatio: Double, minimumFreeformDistance: Double) =
            bounds.clamp(point).let { clamped -> OperationDrawingPoint(clamped.x, clamped.y) }.let { next ->
                if (hypot(
                        next.x - points.last().x,
                        next.y - points.last().y
                    ) >= minimumFreeformDistance
                ) copy(points = points + next) else this
            }

        override fun committedOrNull() = preview.takeIf { points.size >= 2 }
    }

    data class Line(
        val start: OperationDrawingPoint,
        val end: OperationDrawingPoint,
        val colour: OperationArgb,
        override val bounds: MapCoordinateBounds,
    ) : MapViewerDrawingGesture {
        override val preview get() = OperationLineDrawing(start, end, colour)
        override fun update(point: MapCoordinate, shiftHeld: Boolean, aspectRatio: Double, minimumFreeformDistance: Double) =
            bounds.clamp(point).let { copy(end = OperationDrawingPoint(it.x, it.y)) }
        override fun committedOrNull() = preview.takeIf { start != end }
    }

    data class Rectangle(
        val start: OperationDrawingPoint,
        val end: OperationDrawingPoint,
        val colour: OperationArgb,
        override val bounds: MapCoordinateBounds,
    ) : MapViewerDrawingGesture {
        override val preview get() = OperationRectangleDrawing(start, end, colour)
        override fun update(point: MapCoordinate, shiftHeld: Boolean, aspectRatio: Double, minimumFreeformDistance: Double) =
            copy(end = constrainedEnd(start, bounds.clamp(point), shiftHeld, aspectRatio).fitFrom(start, bounds))

        override fun committedOrNull() = preview.takeIf { start != end }
    }

    data class Oval(
        val start: OperationDrawingPoint,
        val end: OperationDrawingPoint,
        val colour: OperationArgb,
        override val bounds: MapCoordinateBounds,
    ) : MapViewerDrawingGesture {
        override val preview get() = OperationOvalDrawing(centre(start, end), abs(end.x - start.x) * .5, abs(end.y - start.y) * .5, colour)
        override fun update(point: MapCoordinate, shiftHeld: Boolean, aspectRatio: Double, minimumFreeformDistance: Double) =
            copy(end = constrainedEnd(start, bounds.clamp(point), shiftHeld, aspectRatio).fitFrom(start, bounds))

        override fun committedOrNull() = preview.takeIf { start != end }
    }
}

fun MapViewerDrawingMode.startedAt(
    point: MapCoordinate,
    colour: OperationArgb,
    bounds: MapCoordinateBounds,
): MapViewerDrawingGesture? {
    if (!bounds.contains(point)) return null
    val start = OperationDrawingPoint(point.x, point.y)
    return when (this) {
        MapViewerDrawingMode.FREEFORM -> MapViewerDrawingGesture.Freeform(listOf(start), colour, bounds)
        MapViewerDrawingMode.LINE -> MapViewerDrawingGesture.Line(start, start, colour, bounds)
        MapViewerDrawingMode.RECTANGLE -> MapViewerDrawingGesture.Rectangle(start, start, colour, bounds)
        MapViewerDrawingMode.OVAL -> MapViewerDrawingGesture.Oval(start, start, colour, bounds)
    }
}

private fun OperationDrawingPoint.fitFrom(start: OperationDrawingPoint, bounds: MapCoordinateBounds): OperationDrawingPoint {
    val dx = x - start.x
    val dy = y - start.y
    val xScale = when {
        dx > 0.0 -> (bounds.right - start.x) / dx
        dx < 0.0 -> (bounds.left - start.x) / dx
        else -> 1.0
    }
    val yScale = when {
        dy > 0.0 -> (bounds.bottom - start.y) / dy
        dy < 0.0 -> (bounds.top - start.y) / dy
        else -> 1.0
    }
    val scale = minOf(1.0, xScale, yScale).coerceAtLeast(0.0)
    return OperationDrawingPoint(start.x + dx * scale, start.y + dy * scale)
}

private fun constrainedEnd(start: OperationDrawingPoint, point: MapCoordinate, shift: Boolean, aspect: Double): OperationDrawingPoint {
    val end = OperationDrawingPoint(point.x, point.y)
    if (!shift || end.x == start.x || end.y == start.y) return end
    val dx = end.x - start.x
    val dy = end.y - start.y
    return if (abs(dx) * aspect >= abs(dy)) end.copy(y = start.y + kotlin.math.sign(dy) * abs(dx) * aspect)
    else end.copy(x = start.x + kotlin.math.sign(dx) * abs(dy) / aspect)
}

private fun centre(start: OperationDrawingPoint, end: OperationDrawingPoint) = OperationDrawingPoint((start.x + end.x) * .5, (start.y + end.y) * .5)
