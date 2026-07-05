package io.hsar.mapgenerator.commandgui.korge.drawing

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal object MapDrawingGeometry {
    fun rectangleCorners(
        start: MapDrawingPoint,
        end: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): List<MapDrawingPoint> =
        axisAlignedCorners(start, end)
            .map { it.rotateAround(center(start, end), rotationDegrees, aspectRatio) }

    fun ovalPoints(
        center: MapDrawingPoint,
        xRadius: Double,
        yRadius: Double,
        rotationDegrees: Double,
        segmentCount: Int,
        aspectRatio: Double,
    ): List<MapDrawingPoint> {
        if (xRadius <= 0.0 || yRadius <= 0.0) {
            return emptyList()
        }
        val physicalXRadius = xRadius * aspectRatio
        return (0..segmentCount).map { index ->
            val angle = 2.0 * PI * index / segmentCount
            MapDrawingPoint(
                x = center.x + cos(angle) * physicalXRadius / aspectRatio,
                y = center.y + sin(angle) * yRadius,
            ).rotateAround(center, rotationDegrees, aspectRatio)
        }
    }

    fun ovalFromBounds(
        start: MapDrawingPoint,
        end: MapDrawingPoint,
        color: java.awt.Color,
        rotationDegrees: Double = 0.0,
    ): OvalDrawing =
        OvalDrawing(
            center = center(start, end),
            xRadius = abs(end.x - start.x) * 0.5,
            yRadius = abs(end.y - start.y) * 0.5,
            color = color,
            rotationDegrees = rotationDegrees,
        )

    fun center(start: MapDrawingPoint, end: MapDrawingPoint): MapDrawingPoint =
        MapDrawingPoint(
            x = (start.x + end.x) * 0.5,
            y = (start.y + end.y) * 0.5,
        )

    fun MapDrawingPoint.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint {
        val radians = Math.toRadians(rotationDegrees)
        val dx = (x - pivot.x) * aspectRatio
        val dy = y - pivot.y
        val cos = cos(radians)
        val sin = sin(radians)
        return MapDrawingPoint(
            x = pivot.x + (dx * cos - dy * sin) / aspectRatio,
            y = pivot.y + dx * sin + dy * cos,
        )
    }

    fun ovalSegmentCount(widthPixels: Double, heightPixels: Double, zoom: Double): Int =
        max(
            MIN_OVAL_SEGMENTS,
            (max(widthPixels, heightPixels) * zoom / OVAL_PIXELS_PER_SEGMENT).toInt(),
        )

    fun squareEnd(start: MapDrawingPoint, end: MapDrawingPoint, aspectRatio: Double): MapDrawingPoint {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0.0 || dy == 0.0) {
            return end
        }
        val widthPixels = abs(dx) * aspectRatio
        val heightPixels = abs(dy)
        return if (widthPixels >= heightPixels) {
            end.copy(y = start.y + kotlin.math.sign(dy) * abs(dx) * aspectRatio)
        } else {
            end.copy(x = start.x + kotlin.math.sign(dx) * abs(dy) / aspectRatio)
        }
    }

    private fun axisAlignedCorners(start: MapDrawingPoint, end: MapDrawingPoint): List<MapDrawingPoint> =
        listOf(
            MapDrawingPoint(start.x, start.y),
            MapDrawingPoint(end.x, start.y),
            MapDrawingPoint(end.x, end.y),
            MapDrawingPoint(start.x, end.y),
        )

    private const val MIN_OVAL_SEGMENTS = 24
    private const val OVAL_PIXELS_PER_SEGMENT = 12.0
}
