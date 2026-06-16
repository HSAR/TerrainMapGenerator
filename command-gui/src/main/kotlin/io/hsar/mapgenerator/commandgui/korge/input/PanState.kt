package io.hsar.mapgenerator.commandgui.korge.input

import korlibs.event.MouseButton
import korlibs.math.geom.Vector2D
import korlibs.render.GameWindow
import java.util.EnumSet

internal enum class PanTrigger {
    RIGHT_CLICK,
    SPACE,
}

internal class PanState(
    private val panner: PanMotion,
    private val updateCursor: (GameWindow.ICursor) -> Unit,
) {
    private val activeTriggers = EnumSet.noneOf(PanTrigger::class.java)
    private var currentCursor: GameWindow.ICursor? = null

    val isActive: Boolean
        get() = activeTriggers.isNotEmpty()

    val isDragging: Boolean
        get() = panner.isDragging

    fun setHeld(trigger: PanTrigger, held: Boolean, pointer: Vector2D) {
        if (held) {
            activeTriggers.add(trigger)
            if (!panner.isDragging) {
                panner.start(pointer)
            }
        } else {
            activeTriggers.remove(trigger)
            if (activeTriggers.isEmpty()) {
                panner.stop()
            }
        }

        refreshCursor()
    }

    fun update(pointer: Vector2D, rightHeld: Boolean, spaceHeld: Boolean): Boolean {
        if (rightHeld) {
            activeTriggers.add(PanTrigger.RIGHT_CLICK)
        } else {
            activeTriggers.remove(PanTrigger.RIGHT_CLICK)
        }

        if (spaceHeld) {
            activeTriggers.add(PanTrigger.SPACE)
        } else {
            activeTriggers.remove(PanTrigger.SPACE)
        }

        if (activeTriggers.isEmpty()) {
            if (panner.isDragging) {
                panner.stop()
            }
            refreshCursor()
            return false
        }

        if (!panner.isDragging) {
            panner.start(pointer)
        }

        val moved = panner.drag(pointer)
        refreshCursor()
        return moved
    }

    fun refreshCursor() {
        val desiredCursor = if (activeTriggers.isNotEmpty() || panner.isDragging) {
            GameWindow.Cursor.MOVE
        } else {
            GameWindow.Cursor.DEFAULT
        }

        if (currentCursor != desiredCursor) {
            updateCursor(desiredCursor)
            currentCursor = desiredCursor
        }
    }
}
