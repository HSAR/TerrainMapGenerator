package io.hsar.mapgenerator.commandgui

enum class MapViewerTool {
    SELECT,
    MEASURE,
    DRAW,
}

fun MapViewerTool.canStartPlacementDrag() = this == MapViewerTool.SELECT
