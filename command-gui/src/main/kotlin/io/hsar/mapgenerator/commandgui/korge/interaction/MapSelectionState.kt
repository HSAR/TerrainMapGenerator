package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D
import java.awt.Color
import kotlin.math.hypot

internal data class MapSelectionState(
    val selected: List<MapPlacedObjectSnapshot> = emptyList(),
    val box: ScreenSelectionBox? = null,
    val groupFrame: ScreenSelectionFrame? = null,
    val pivotHandle: ScreenPivotHandle? = null,
    val gridColor: Color? = null,
)

internal data class ScreenSelectionBox(
    val start: Vector2D,
    val current: Vector2D,
) {
    val left = minOf(start.x, current.x)
    val top = minOf(start.y, current.y)
    val right = maxOf(start.x, current.x)
    val bottom = maxOf(start.y, current.y)
    val width = right - left
    val height = bottom - top
}

internal data class ScreenSelectionFrame(
    val topLeft: Vector2D,
    val topRight: Vector2D,
    val bottomRight: Vector2D,
    val bottomLeft: Vector2D,
) {
    val corners: List<Vector2D> =
        listOf(topLeft, topRight, bottomRight, bottomLeft)

    val topRightCorner: Vector2D
        get() = topRight

    fun topRightOffsetCorner(amount: Double): Vector2D =
        topRight + topRight.unitDirectionFrom(topLeft) * amount + topRight.unitDirectionFrom(bottomRight) * amount

    fun cornerBrackets(length: Double, thickness: Double): List<ScreenCornerBracket> =
        listOf(
            ScreenCornerBracket.fromCorner(ScreenSelectionCorner.TOP_LEFT, topLeft, topRight, bottomLeft, length, thickness),
            ScreenCornerBracket.fromCorner(ScreenSelectionCorner.TOP_RIGHT, topRight, topLeft, bottomRight, length, thickness),
            ScreenCornerBracket.fromCorner(ScreenSelectionCorner.BOTTOM_RIGHT, bottomRight, topRight, bottomLeft, length, thickness),
            ScreenCornerBracket.fromCorner(ScreenSelectionCorner.BOTTOM_LEFT, bottomLeft, bottomRight, topLeft, length, thickness),
        )

    fun corner(corner: ScreenSelectionCorner): Vector2D =
        when (corner) {
            ScreenSelectionCorner.TOP_LEFT -> topLeft
            ScreenSelectionCorner.TOP_RIGHT -> topRight
            ScreenSelectionCorner.BOTTOM_RIGHT -> bottomRight
            ScreenSelectionCorner.BOTTOM_LEFT -> bottomLeft
        }

    fun rotatedAround(pivot: Vector2D, deltaRadians: Double): ScreenSelectionFrame =
        copy(
            topLeft = topLeft.rotateAround(pivot, deltaRadians),
            topRight = topRight.rotateAround(pivot, deltaRadians),
            bottomRight = bottomRight.rotateAround(pivot, deltaRadians),
            bottomLeft = bottomLeft.rotateAround(pivot, deltaRadians),
        )

    fun expanded(amount: Double): ScreenSelectionFrame {
        val left = corners.minOf { it.x } - amount
        val top = corners.minOf { it.y } - amount
        val right = corners.maxOf { it.x } + amount
        val bottom = corners.maxOf { it.y } + amount
        return ScreenSelectionFrame(
            topLeft = Vector2D(left, top),
            topRight = Vector2D(right, top),
            bottomRight = Vector2D(right, bottom),
            bottomLeft = Vector2D(left, bottom),
        )
    }

    companion object {
        fun fromBounds(bounds: MapSelectionBounds, coordinateSpace: MapCoordinateSpace, viewport: CameraViewport): ScreenSelectionFrame {
            val topLeft = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.left, bounds.top)), viewport)
            val topRight = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.right, bounds.top)), viewport)
            val bottomRight = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.right, bounds.bottom)), viewport)
            val bottomLeft = coordinateSpace.worldToScreen(coordinateSpace.toWorld(MapCoordinate(bounds.left, bounds.bottom)), viewport)
            return ScreenSelectionFrame(
                topLeft = topLeft,
                topRight = topRight,
                bottomRight = bottomRight,
                bottomLeft = bottomLeft,
            )
        }

        fun fromSegment(start: Vector2D, end: Vector2D, padding: Double): ScreenSelectionFrame {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = hypot(dx, dy)
            if (length <= 0.0) {
                return ScreenSelectionFrame(
                    topLeft = Vector2D(start.x - padding, start.y - padding),
                    topRight = Vector2D(start.x + padding, start.y - padding),
                    bottomRight = Vector2D(start.x + padding, start.y + padding),
                    bottomLeft = Vector2D(start.x - padding, start.y + padding),
                )
            }

            val unitX = dx / length
            val unitY = dy / length
            val normalX = -unitY * padding
            val normalY = unitX * padding
            return ScreenSelectionFrame(
                topLeft = Vector2D(start.x + normalX, start.y + normalY),
                topRight = Vector2D(end.x + normalX, end.y + normalY),
                bottomRight = Vector2D(end.x - normalX, end.y - normalY),
                bottomLeft = Vector2D(start.x - normalX, start.y - normalY),
            )
        }
    }
}

internal data class ScreenPivotHandle(
    val center: Vector2D,
    val radius: Double,
) {
    fun contains(point: Vector2D): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        return dx * dx + dy * dy <= radius * radius
    }
}

internal enum class ScreenSelectionCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
}

internal fun ScreenSelectionCorner.opposite(): ScreenSelectionCorner =
    when (this) {
        ScreenSelectionCorner.TOP_LEFT -> ScreenSelectionCorner.BOTTOM_RIGHT
        ScreenSelectionCorner.TOP_RIGHT -> ScreenSelectionCorner.BOTTOM_LEFT
        ScreenSelectionCorner.BOTTOM_RIGHT -> ScreenSelectionCorner.TOP_LEFT
        ScreenSelectionCorner.BOTTOM_LEFT -> ScreenSelectionCorner.TOP_RIGHT
    }

internal data class ScreenCornerBracket(
    val corner: ScreenSelectionCorner,
    val points: List<Vector2D>,
) {
    fun contains(point: Vector2D): Boolean {
        val crossings = points.indices.count { index ->
            val start = points[index]
            val end = points[(index + 1) % points.size]
            (start.y > point.y) != (end.y > point.y) &&
                    point.x < (end.x - start.x) * (point.y - start.y) / (end.y - start.y) + start.x
        }
        return crossings % 2 == 1
    }

    companion object {
        fun fromCorner(
            selectionCorner: ScreenSelectionCorner,
            corner: Vector2D,
            firstEdge: Vector2D,
            secondEdge: Vector2D,
            length: Double,
            thickness: Double,
        ): ScreenCornerBracket {
            val firstDirection = firstEdge.unitDirectionFrom(corner)
            val secondDirection = secondEdge.unitDirectionFrom(corner)
            return ScreenCornerBracket(
                corner = selectionCorner,
                points = listOf(
                    corner,
                    corner + firstDirection * length,
                    corner + firstDirection * length - secondDirection * thickness,
                    corner - firstDirection * thickness - secondDirection * thickness,
                    corner - firstDirection * thickness + secondDirection * length,
                    corner + secondDirection * length,
                ),
            )
        }
    }
}

private operator fun Vector2D.plus(other: Vector2D): Vector2D =
    Vector2D(x + other.x, y + other.y)

private operator fun Vector2D.minus(other: Vector2D): Vector2D =
    Vector2D(x - other.x, y - other.y)

private fun Vector2D.unitDirectionFrom(origin: Vector2D): Vector2D {
    val dx = x - origin.x
    val dy = y - origin.y
    val length = hypot(dx, dy)
    return if (length == 0.0) Vector2D(0.0, 0.0) else Vector2D(dx / length, dy / length)
}

private fun Vector2D.rotateAround(pivot: Vector2D, deltaRadians: Double): Vector2D {
    val dx = x - pivot.x
    val dy = y - pivot.y
    val cos = kotlin.math.cos(deltaRadians)
    val sin = kotlin.math.sin(deltaRadians)
    return Vector2D(
        x = pivot.x + dx * cos - dy * sin,
        y = pivot.y + dx * sin + dy * cos,
    )
}

internal const val SELECTION_FRAME_SCREEN_PADDING = 4.0
internal const val SELECTION_BRACKET_LENGTH_PX = 18.0
internal const val SELECTION_BRACKET_THICKNESS_PX = 5.0
