package io.hsar.mapgenerator.commandgui.korge.drawing

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import kotlin.math.abs
import kotlin.math.hypot

internal object MapDrawingIconRenderer {
    fun render(drawing: MapDrawing, size: Int = DEFAULT_SIZE): Bitmap32 {
        val segments = drawing.iconSegments()
        val bounds = segments.bounds()
        if (segments.isEmpty() || bounds.isEmpty) {
            return transparent(size)
        }

        val strokeRadius = size * STROKE_WIDTH_RATIO
        val color = RGBA(drawing.color.red, drawing.color.green, drawing.color.blue, ICON_ALPHA)
        return Bitmap32(size, size) { x, y ->
            val point = IconPoint(x.toDouble(), y.toDouble())
            if (segments.any { segment -> segment.normalized(bounds, size).distanceTo(point) <= strokeRadius }) {
                color
            } else {
                RGBA(0, 0, 0, 0)
            }
        }.premultipliedIfRequired()
    }

    private fun MapDrawing.iconSegments(): List<IconSegment> =
        when (this) {
            is FreeformDrawing -> points.zipWithNext().map { (start, end) -> IconSegment(start.iconPoint(), end.iconPoint()) }
            is LineDrawing -> listOf(IconSegment(start.iconPoint(), end.iconPoint()))
            is RectangleDrawing -> MapDrawingGeometry.rectangleCorners(start, end, rotationDegrees, aspectRatio = 1.0)
                .closedIconSegments()

            is OvalDrawing -> MapDrawingGeometry.ovalPoints(center, xRadius, yRadius, rotationDegrees, segmentCount = 48, aspectRatio = 1.0)
                .closedIconSegments()
        }

    private fun List<MapDrawingPoint>.closedIconSegments(): List<IconSegment> =
        takeIf { it.size > 1 }
            ?.let { points -> (points + points.first()).zipWithNext().map { (start, end) -> IconSegment(start.iconPoint(), end.iconPoint()) } }
            .orEmpty()

    private fun MapDrawingPoint.iconPoint(): IconPoint =
        IconPoint(x, y)

    private fun List<IconSegment>.bounds(): IconBounds =
        flatMap { listOf(it.start, it.end) }
            .let { points ->
                IconBounds(
                    left = points.minOfOrNull { it.x } ?: 0.0,
                    top = points.minOfOrNull { it.y } ?: 0.0,
                    right = points.maxOfOrNull { it.x } ?: 0.0,
                    bottom = points.maxOfOrNull { it.y } ?: 0.0,
                )
            }

    private fun IconSegment.normalized(bounds: IconBounds, size: Int): IconSegment {
        val scale = ((size - ICON_PADDING * 2.0) / maxOf(bounds.width, bounds.height, 0.0001)).coerceAtLeast(0.0)
        val xOffset = (size - bounds.width * scale) * 0.5 - bounds.left * scale
        val yOffset = (size - bounds.height * scale) * 0.5 - bounds.top * scale
        return IconSegment(
            start = start.normalized(scale, xOffset, yOffset),
            end = end.normalized(scale, xOffset, yOffset),
        )
    }

    private fun IconPoint.normalized(scale: Double, xOffset: Double, yOffset: Double): IconPoint =
        IconPoint(x * scale + xOffset, y * scale + yOffset)

    private fun IconSegment.distanceTo(point: IconPoint): Double {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= 0.0) {
            return hypot(point.x - start.x, point.y - start.y)
        }
        val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val projection = IconPoint(start.x + t * dx, start.y + t * dy)
        return hypot(point.x - projection.x, point.y - projection.y)
    }

    private fun transparent(size: Int): Bitmap32 =
        Bitmap32(size, size) { _, _ -> RGBA(0, 0, 0, 0) }.premultipliedIfRequired()

    private data class IconPoint(
        val x: Double,
        val y: Double,
    )

    private data class IconSegment(
        val start: IconPoint,
        val end: IconPoint,
    )

    private data class IconBounds(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double,
    ) {
        val width: Double = abs(right - left)
        val height: Double = abs(bottom - top)
        val isEmpty: Boolean = width <= 0.0 && height <= 0.0
    }

    private const val DEFAULT_SIZE = 32
    private const val ICON_PADDING = 4.0
    private const val ICON_ALPHA = 170
    private const val STROKE_WIDTH_RATIO = 0.055
}
