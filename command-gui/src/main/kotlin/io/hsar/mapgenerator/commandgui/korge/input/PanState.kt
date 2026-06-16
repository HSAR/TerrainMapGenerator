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
    private val panner: PanPanner,
    private val updateCursor: (GameWindow.ICursor) -> Unit,
) {
    private val activeTriggers = EnumSet.noneOf(PanTrigger::class.java)
    private var currentCursor: GameWindow.ICursor? = null

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

    fun onMouseDown(button: MouseButton, pointer: Vector2D, buttons: Int) {
        if (button == MouseButton.RIGHT || MouseButton.RIGHT.pressedFromFlags(buttons)) {
            setHeld(PanTrigger.RIGHT_CLICK, true, pointer)
        }
    }

    fun onMouseMove(pointer: Vector2D, buttons: Int) {
        if (MouseButton.RIGHT.pressedFromFlags(buttons)) {
            activeTriggers.add(PanTrigger.RIGHT_CLICK)
        } else {
            activeTriggers.remove(PanTrigger.RIGHT_CLICK)
        }

        if (activeTriggers.isNotEmpty() && !panner.isDragging) {
            panner.start(pointer)
        }

        if (panner.isDragging) {
            panner.drag(pointer)
        }

        refreshCursor()
    }

    fun onMouseUp(button: MouseButton, buttons: Int) {
        if (button == MouseButton.RIGHT || !MouseButton.RIGHT.pressedFromFlags(buttons)) {
            activeTriggers.remove(PanTrigger.RIGHT_CLICK)
            if (activeTriggers.isEmpty()) {
                panner.stop()
            }
            refreshCursor()
        }
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
