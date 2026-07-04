package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D

internal data class MapSelectionState(
    val selected: List<MapPlacedObjectSnapshot> = emptyList(),
    val box: ScreenSelectionBox? = null,
    val groupFrame: ScreenSelectionFrame? = null,
    val pivotHandle: ScreenPivotHandle? = null,
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
