package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import kotlin.math.abs

internal fun focusShadowPolygon(
    bounds: MapViewerDialogBounds,
    anchor: Vector2D?,
): List<Vector2D>? {
    anchor ?: return null
    if (bounds.width <= 0.0 || bounds.height <= 0.0) {
        return null
    }

    val centerX = bounds.left + bounds.width * 0.5
    val centerY = bounds.top + bounds.height * 0.5
    val dx = anchor.x - centerX
    val dy = anchor.y - centerY
    val halfWidth = bounds.width * 0.5
    val halfHeight = bounds.height * 0.5

    if (abs(dx) > halfWidth && abs(dy) > halfHeight) {
        return if (dx * dy < 0.0) {
            listOf(
                anchor,
                Vector2D(bounds.left, bounds.top),
                Vector2D(bounds.right, bounds.bottom),
            )
        } else {
            listOf(
                anchor,
                Vector2D(bounds.right, bounds.top),
                Vector2D(bounds.left, bounds.bottom),
            )
        }
    }

    val useHorizontalEdge = abs(dx) * bounds.height >= abs(dy) * bounds.width

    return when {
        useHorizontalEdge && dx < 0.0 -> listOf(
            anchor,
            Vector2D(bounds.left, bounds.top),
            Vector2D(bounds.left, bounds.bottom),
        )

        useHorizontalEdge -> listOf(
            anchor,
            Vector2D(bounds.right, bounds.top),
            Vector2D(bounds.right, bounds.bottom),
        )

        dy < 0.0 -> listOf(
            anchor,
            Vector2D(bounds.left, bounds.top),
            Vector2D(bounds.right, bounds.top),
        )

        else -> listOf(
            anchor,
            Vector2D(bounds.left, bounds.bottom),
            Vector2D(bounds.right, bounds.bottom),
        )
    }
}
