package io.hsar.mapgenerator.commandgui.korge.interaction

internal data class MapToolGestureState(
    val leftToolActive: Boolean,
)

internal class MapToolGestureGate {
    private var leftGestureBlocked = false
    private var previousLeftHeld = false

    fun update(leftHeld: Boolean, pointerBlocked: Boolean): MapToolGestureState {
        if (!leftHeld) {
            leftGestureBlocked = false
            previousLeftHeld = false
            return MapToolGestureState(leftToolActive = false)
        }

        if (!previousLeftHeld) {
            leftGestureBlocked = pointerBlocked
        }
        previousLeftHeld = true
        return MapToolGestureState(leftToolActive = !leftGestureBlocked)
    }
}
