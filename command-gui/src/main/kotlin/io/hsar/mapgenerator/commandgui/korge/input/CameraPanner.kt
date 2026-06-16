package io.hsar.mapgenerator.commandgui.korge.input

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import korlibs.math.geom.Vector2D

internal class CameraPanner(
    initialViewport: CameraViewport,
    private val updateViewport: (CameraViewport) -> Unit,
) : PanMotion {
    override var isDragging: Boolean = false
        private set

    var viewport: CameraViewport = initialViewport
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

        return setViewport(viewport.pan(delta))
    }

    fun zoomAt(position: Vector2D, factor: Double): Boolean =
        setViewport(viewport.zoomAt(position, factor))

    override fun stop() {
        isDragging = false
        lastPosition = null
    }

    private fun setViewport(nextViewport: CameraViewport): Boolean {
        if (nextViewport == viewport) {
            return false
        }

        viewport = nextViewport
        updateViewport(nextViewport)
        return true
    }
}
