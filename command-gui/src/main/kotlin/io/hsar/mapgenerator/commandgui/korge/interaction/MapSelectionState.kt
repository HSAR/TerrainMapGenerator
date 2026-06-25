package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D

internal data class MapSelectionState(
    val selected: List<MapPlacedObjectSnapshot> = emptyList(),
    val box: ScreenSelectionBox? = null,
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
