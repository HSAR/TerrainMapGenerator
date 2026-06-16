package io.hsar.mapgenerator.commandgui.korge.input

import korlibs.math.geom.Vector2D

internal interface PanMotion {
    val isDragging: Boolean

    fun start(position: Vector2D)

    fun drag(position: Vector2D): Boolean

    fun stop()
}
