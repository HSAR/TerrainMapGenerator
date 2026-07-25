package io.hsar.mapgenerator.commandgui.korge.interaction

internal enum class MapViewerTool {
    SELECT,
    MEASURE,
    DRAW,
}

internal fun MapViewerTool.canStartPlacementDrag(): Boolean =
    this == MapViewerTool.SELECT
