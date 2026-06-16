package io.hsar.mapgenerator.commandgui.korge.interaction

internal data class MapPointerIntent(
    val leftActive: Boolean,
    val rightPanHeld: Boolean,
    val spacePanHeld: Boolean,
    val allowZoom: Boolean,
)

internal object MapPointerIntentResolver {
    fun resolve(
        pointerBlocked: Boolean,
        dragging: Boolean,
        leftHeld: Boolean,
        rightHeld: Boolean,
        spaceHeld: Boolean,
    ) = MapPointerIntent(
        leftActive = leftHeld && !pointerBlocked,
        rightPanHeld = rightHeld && (!pointerBlocked || dragging),
        spacePanHeld = spaceHeld && (!pointerBlocked || dragging),
        allowZoom = !pointerBlocked,
    )
}
