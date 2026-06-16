package io.hsar.mapgenerator.commandgui.korge.input

import korlibs.korge.view.View
import korlibs.math.geom.Vector2D

internal data class PanPanner(
    val view: View,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
) {
    var isDragging: Boolean = false
        private set

    private var lastPosition: Vector2D? = null

    fun start(position: Vector2D) {
        isDragging = true
        lastPosition = position
    }

    fun drag(position: Vector2D) {
        val previous = lastPosition ?: return
        val delta = position - previous
        lastPosition = position

        view.x = clamp(view.x + delta.x, 0.0, backgroundWidth.toDouble() - view.width)
        view.y = clamp(view.y + delta.y, 0.0, backgroundHeight.toDouble() - view.height)
    }

    fun stop() {
        isDragging = false
        lastPosition = null
    }

    private fun clamp(value: Double, min: Double, max: Double): Double =
        value.coerceIn(min, max)
}
