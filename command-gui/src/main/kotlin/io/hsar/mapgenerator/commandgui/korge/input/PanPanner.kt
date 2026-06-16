package io.hsar.mapgenerator.commandgui.korge.input

import korlibs.korge.view.View
import korlibs.math.geom.Vector2D

internal data class PanPanner(
    val view: View,
    val contentWidth: Int,
    val contentHeight: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
) : PanMotion {
    override var isDragging: Boolean = false
        private set

    private var lastPosition: Vector2D? = null

    override fun start(position: Vector2D) {
        isDragging = true
        lastPosition = position
    }

    override fun drag(position: Vector2D): Boolean {
        val previous = lastPosition ?: return false
        val delta = position - previous
        lastPosition = position

        if (delta.x == 0.0 && delta.y == 0.0) {
            return false
        }

        val zoom = view.scaleX.coerceAtLeast(1e-6)
        val scaledWidth = contentWidth / zoom
        val scaledHeight = contentHeight / zoom
        val maxX = if (scaledWidth <= backgroundWidth) backgroundWidth - scaledWidth else 0.0
        val maxY = if (scaledHeight <= backgroundHeight) backgroundHeight - scaledHeight else 0.0
        val minX = if (scaledWidth <= backgroundWidth) 0.0 else backgroundWidth - scaledWidth
        val minY = if (scaledHeight <= backgroundHeight) 0.0 else backgroundHeight - scaledHeight
        val previousX = view.x
        val previousY = view.y
        view.x = clamp(view.x + delta.x, minX, maxX)
        view.y = clamp(view.y + delta.y, minY, maxY)
        return previousX != view.x || previousY != view.y
    }

    override fun stop() {
        isDragging = false
        lastPosition = null
    }

    private fun clamp(value: Double, min: Double, max: Double): Double =
        value.coerceIn(min, max)
}

internal interface PanMotion {
    val isDragging: Boolean

    fun start(position: Vector2D)

    fun drag(position: Vector2D): Boolean

    fun stop()
}
