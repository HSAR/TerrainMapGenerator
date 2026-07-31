package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerTool as SharedMapViewerTool
import io.hsar.mapgenerator.commandgui.canStartPlacementDrag as sharedCanStartPlacementDrag

internal typealias MapViewerTool = SharedMapViewerTool

internal fun MapViewerTool.canStartPlacementDrag() =
    sharedCanStartPlacementDrag()
