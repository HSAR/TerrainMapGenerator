package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import korlibs.math.geom.vector.VectorPath
import korlibs.math.geom.vector.Winding
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator

internal fun focusShadowShape(
    bounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds?,
): VectorPath? =
    targetBounds?.let { focusShadowShape(bounds, listOf(it)) }

internal fun focusShadowShape(
    bounds: MapViewerDialogBounds,
    targetBounds: Collection<ScreenBounds>,
): VectorPath? {
    val validTargets = targetBounds.filter { it.width > 0.0 && it.height > 0.0 }
    if (bounds.width <= 0.0 || bounds.height <= 0.0 || validTargets.isEmpty()) {
        return null
    }

    // TODO: Swap this AWT boolean fallback for Korlibs shape ops once the matching artifact is available;
    // this will avoid the extra conversion cost on every dialog refresh.
    val shadowArea = validTargets
        .mapNotNull { target ->
            focusShadowHullPolygon(bounds, target)
                .takeIf { it.size >= 3 }
                ?.toArea()
        }
        .fold(Area()) { accumulated, hull ->
            accumulated.apply { add(hull) }
        }

    if (shadowArea.isEmpty) {
        return null
    }

    shadowArea.subtract(bounds.toArea())
    validTargets.forEach { target ->
        shadowArea.subtract(target.toArea())
    }

    return shadowArea
        .takeUnless { it.isEmpty }
        ?.toVectorPath()
}

internal fun focusShadowHullPolygon(
    bounds: MapViewerDialogBounds,
    targetBounds: ScreenBounds,
): List<Vector2D> =
    (bounds.corners() + targetBounds.corners()).convexHull()

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

private fun List<Vector2D>.toArea(): Area =
    Area(
        Path2D.Double(Path2D.WIND_NON_ZERO).apply {
            moveTo(first().x, first().y)
            drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
            closePath()
        },
    )

private fun MapViewerDialogBounds.toArea(): Area =
    listOf(
        topLeft(),
        topRight(),
        bottomRight(),
        bottomLeft(),
    ).toArea()

private fun ScreenBounds.toArea(): Area =
    listOf(
        topLeft(),
        topRight(),
        bottomRight(),
        bottomLeft(),
    ).toArea()

private fun Area.toVectorPath(): VectorPath =
    VectorPath(winding = Winding.EVEN_ODD).also { path ->
        val iterator = getPathIterator(null)
        val coords = DoubleArray(6)
        while (!iterator.isDone) {
            when (iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> path.moveTo(coords[0], coords[1])
                PathIterator.SEG_LINETO -> path.lineTo(coords[0], coords[1])
                PathIterator.SEG_QUADTO -> path.quadTo(coords[0], coords[1], coords[2], coords[3])
                PathIterator.SEG_CUBICTO -> path.cubicTo(
                    coords[0],
                    coords[1],
                    coords[2],
                    coords[3],
                    coords[4],
                    coords[5],
                )

                PathIterator.SEG_CLOSE -> path.close()
            }
            iterator.next()
        }
    }

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
