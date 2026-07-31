package io.hsar.mapgenerator.commandgui

import java.util.Collections.emptySet
import kotlin.math.hypot

/** Renderer-neutral geometry for drawing and hit-testing a map selection frame. */
data class MapSelectionFrame(
    val topLeft: ViewerPoint,
    val topRight: ViewerPoint,
    val bottomRight: ViewerPoint,
    val bottomLeft: ViewerPoint,
) {
    val corners = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun corner(corner: MapSelectionCorner) = when (corner) {
        MapSelectionCorner.TOP_LEFT -> topLeft
        MapSelectionCorner.TOP_RIGHT -> topRight
        MapSelectionCorner.BOTTOM_RIGHT -> bottomRight
        MapSelectionCorner.BOTTOM_LEFT -> bottomLeft
    }

    fun rotationHandle(offset: Double = MAP_SELECTION_ROTATION_HANDLE_OFFSET_PX) =
        topRight + topRight.unitDirectionFrom(topLeft) * offset + topRight.unitDirectionFrom(bottomRight) * offset

    fun cornerBrackets(
        length: Double = MAP_SELECTION_BRACKET_LENGTH_PX,
        thickness: Double = MAP_SELECTION_BRACKET_THICKNESS_PX,
    ) = listOf(
        MapSelectionCornerBracket.fromCorner(MapSelectionCorner.TOP_LEFT, topLeft, topRight, bottomLeft, length, thickness),
        MapSelectionCornerBracket.fromCorner(MapSelectionCorner.TOP_RIGHT, topRight, topLeft, bottomRight, length, thickness),
        MapSelectionCornerBracket.fromCorner(MapSelectionCorner.BOTTOM_RIGHT, bottomRight, topRight, bottomLeft, length, thickness),
        MapSelectionCornerBracket.fromCorner(MapSelectionCorner.BOTTOM_LEFT, bottomLeft, bottomRight, topLeft, length, thickness),
    )

    companion object {
        fun fromBounds(bounds: MapCoordinateBounds, toScreen: (MapCoordinate) -> ViewerPoint) = MapSelectionFrame(
            topLeft = toScreen(MapCoordinate(bounds.left, bounds.top)),
            topRight = toScreen(MapCoordinate(bounds.right, bounds.top)),
            bottomRight = toScreen(MapCoordinate(bounds.right, bounds.bottom)),
            bottomLeft = toScreen(MapCoordinate(bounds.left, bounds.bottom)),
        )
    }
}

data class MapSelectionBox(
    val start: ViewerPoint,
    val current: ViewerPoint,
) {
    val left = minOf(start.x, current.x)
    val top = minOf(start.y, current.y)
    val right = maxOf(start.x, current.x)
    val bottom = maxOf(start.y, current.y)
    val isDrag = hypot(current.x - start.x, current.y - start.y) >= MAP_SELECTION_BOX_DRAG_THRESHOLD_PX

    fun overlaps(bounds: MapSelectionScreenBounds) =
        left <= bounds.right && right >= bounds.left && top <= bounds.bottom && bottom >= bounds.top
}

data class MapSelectionScreenBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    fun frame() = MapSelectionFrame(
        topLeft = ViewerPoint(left, top),
        topRight = ViewerPoint(right, top),
        bottomRight = ViewerPoint(right, bottom),
        bottomLeft = ViewerPoint(left, bottom),
    )
}

enum class MapSelectionCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

data class MapSelectionCornerBracket(
    val corner: MapSelectionCorner,
    val points: List<ViewerPoint>,
) {
    fun contains(point: ViewerPoint): Boolean = points.indices.count { index ->
        val start = points[index]
        val end = points[(index + 1) % points.size]
        (start.y > point.y) != (end.y > point.y) && point.x < (end.x - start.x) * (point.y - start.y) / (end.y - start.y) + start.x
    } % 2 == 1

    companion object {
        fun fromCorner(
            corner: MapSelectionCorner,
            point: ViewerPoint,
            firstEdge: ViewerPoint,
            secondEdge: ViewerPoint,
            length: Double,
            thickness: Double
        ): MapSelectionCornerBracket {
            val firstDirection = firstEdge.unitDirectionFrom(point)
            val secondDirection = secondEdge.unitDirectionFrom(point)
            return MapSelectionCornerBracket(
                corner,
                listOf(
                    point,
                    point + firstDirection * length,
                    point + firstDirection * length - secondDirection * thickness,
                    point - firstDirection * thickness - secondDirection * thickness,
                    point - firstDirection * thickness + secondDirection * length,
                    point + secondDirection * length,
                ),
            )
        }
    }
}

fun <Id> Set<Id>.withSelection(id: Id?, additive: Boolean): Set<Id> = when {
    id == null && !additive -> emptySet()
    id == null -> this
    !additive -> setOf(id)
    id in this -> this - id
    else -> this + id
}

fun <Id> Set<Id>.withBoxSelection(ids: Set<Id>, additive: Boolean): Set<Id> =
    if (additive) fold(ids) { selection, id -> if (id in selection) selection - id else selection + id } else ids

private operator fun ViewerPoint.plus(other: ViewerPoint) = ViewerPoint(x + other.x, y + other.y)
private operator fun ViewerPoint.minus(other: ViewerPoint) = ViewerPoint(x - other.x, y - other.y)
private operator fun ViewerPoint.times(amount: Double) = ViewerPoint(x * amount, y * amount)
private fun ViewerPoint.unitDirectionFrom(origin: ViewerPoint): ViewerPoint {
    val length = hypot(x - origin.x, y - origin.y)
    return if (length == 0.0) ViewerPoint(0.0, 0.0) else ViewerPoint((x - origin.x) / length, (y - origin.y) / length)
}

const val MAP_SELECTION_BOX_DRAG_THRESHOLD_PX = 4.0
const val MAP_SELECTION_BRACKET_LENGTH_PX = 18.0
const val MAP_SELECTION_BRACKET_THICKNESS_PX = 5.0
const val MAP_SELECTION_ROTATION_HANDLE_OFFSET_PX = 16.0
