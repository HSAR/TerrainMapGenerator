package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuAction as SharedContextMenuAction
import io.hsar.mapgenerator.commandgui.MapViewerContextMenuTarget as SharedContextMenuTarget
import io.hsar.mapgenerator.commandgui.visibleFor as sharedVisibleFor
import io.hsar.mapgenerator.commandgui.withRequiredFeatureFlags as sharedWithRequiredFeatureFlags

internal typealias MapPlacedObjectMenuAction = SharedContextMenuAction
internal typealias MapContextMenuTarget = SharedContextMenuTarget

internal fun MapPlacedObjectMenuAction.withRequiredFeatureFlags(vararg featureFlags: MapViewerFeatureFlag) =
    sharedWithRequiredFeatureFlags(*featureFlags)

internal fun List<MapPlacedObjectMenuAction>.visibleFor(featureFlags: Set<MapViewerFeatureFlag>) =
    sharedVisibleFor(featureFlags)
