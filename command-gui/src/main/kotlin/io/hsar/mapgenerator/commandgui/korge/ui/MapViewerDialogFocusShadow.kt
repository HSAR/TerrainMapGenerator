package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import korlibs.math.geom.vector.VectorPath

internal fun focusShadowShape(
    bounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds?,
): VectorPath? {
    targetBounds ?: return null
    if (bounds.width <= 0.0 || bounds.height <= 0.0 || targetBounds.width <= 0.0 || targetBounds.height <= 0.0) {
        return null
    }

    val hull = focusShadowHullPolygon(bounds, targetBounds)
    if (hull.size < 3) {
        return null
    }

    return VectorPath().apply {
        moveTo(hull.first())
        hull.drop(1).forEach(::lineTo)
        close()
        rectHole(bounds.left, bounds.top, bounds.width, bounds.height)
        rectHole(targetBounds.left, targetBounds.top, targetBounds.width, targetBounds.height)
    }
}

internal fun focusShadowHullPolygon(
    bounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> {
    return (bounds.corners() + targetBounds.corners()).convexHull()
}

private fun List<Vector2D>.convexHull(): List<Vector2D> {
    val sorted = distinct().sortedWith(compareBy<Vector2D> { it.x }.thenBy { it.y })
    if (sorted.size <= 1) {
        return sorted
    }

    fun appendHullPoint(hull: MutableList<Vector2D>, point: Vector2D) {
        while (hull.size >= 2 && cross(hull[hull.size - 2], hull[hull.size - 1], point) <= 0.0) {
            hull.removeAt(hull.lastIndex)
        }
        hull.add(point)
    }

    val lower = mutableListOf<Vector2D>()
    sorted.forEach { point -> appendHullPoint(lower, point) }
    val upper = mutableListOf<Vector2D>()
    sorted.asReversed().forEach { point -> appendHullPoint(upper, point) }
    return lower.dropLast(1) + upper.dropLast(1)
}

private fun cross(origin: Vector2D, first: Vector2D, second: Vector2D): Double =
    (first.x - origin.x) * (second.y - origin.y) - (first.y - origin.y) * (second.x - origin.x)

private fun MapViewerDialogBounds.corners(): List<Vector2D> =
    listOf(topLeft(), topRight(), bottomRight(), bottomLeft())

private fun ScreenBounds.corners(): List<Vector2D> =
    listOf(topLeft(), topRight(), bottomRight(), bottomLeft())

private fun MapViewerDialogBounds.topLeft(): Vector2D = Vector2D(left, top)

private fun MapViewerDialogBounds.topRight(): Vector2D = Vector2D(right, top)

private fun MapViewerDialogBounds.bottomLeft(): Vector2D = Vector2D(left, bottom)

private fun MapViewerDialogBounds.bottomRight(): Vector2D = Vector2D(right, bottom)

private fun ScreenBounds.topLeft(): Vector2D = Vector2D(left, top)

private fun ScreenBounds.topRight(): Vector2D = Vector2D(right, top)

private fun ScreenBounds.bottomLeft(): Vector2D = Vector2D(left, bottom)

private fun ScreenBounds.bottomRight(): Vector2D = Vector2D(right, bottom)
