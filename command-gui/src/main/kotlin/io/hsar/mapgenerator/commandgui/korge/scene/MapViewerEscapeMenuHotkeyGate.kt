package io.hsar.mapgenerator.commandgui.korge.scene

internal object MapViewerEscapeMenuHotkeyGate {
    @Suppress("UNUSED_PARAMETER")
    fun canToggle(
        sidebarIsUnitDragging: Boolean,
        unitIsDragging: Boolean,
        drawingIsDragging: Boolean,
        selectionIsDragging: Boolean,
        anyDialogOpen: Boolean = false,
    ): Boolean =
        !sidebarIsUnitDragging &&
                !unitIsDragging &&
                !drawingIsDragging &&
                !selectionIsDragging
}
