package io.hsar.mapgenerator.commandgui

import java.util.Collections.emptyList
import java.util.Collections.emptySet

enum class MapViewerContextMenuCommand {
    EDIT_UNIT,
    DELETE_UNIT,
    SITREP,
    SCIREP,
    NAVREP,
    TACTICAL,
    OLYMPIAD_DAWN_STATUS,
    EDIT_GRAPHIC,
    DELETE_GRAPHIC,
    EDIT_DRAWING_NAME,
    DELETE_DRAWING,
}

data class MapViewerContextMenuItem(
    val action: MapViewerContextMenuCommand,
    val label: String,
)

data class MapViewerContextMenuAction(
    val label: String,
    val onSelected: () -> Unit,
    val submenuActions: List<MapViewerContextMenuAction> = emptyList(),
    val requiredFeatureFlags: Set<MapViewerFeatureFlag> = emptySet(),
    val enabled: Boolean = true,
    val disabledTooltipText: String? = null,
)

interface MapViewerContextMenuTarget {
    fun contextMenuActions(): List<MapViewerContextMenuAction>
}

fun MapViewerContextMenuAction.withRequiredFeatureFlags(vararg featureFlags: MapViewerFeatureFlag) =
    copy(requiredFeatureFlags = requiredFeatureFlags + featureFlags)

fun List<MapViewerContextMenuAction>.visibleFor(featureFlags: Set<MapViewerFeatureFlag>): List<MapViewerContextMenuAction> =
    mapNotNull { action ->
        action.takeIf { featureFlags.containsAll(it.requiredFeatureFlags) }?.let { visibleAction ->
            visibleAction.submenuActions.visibleFor(featureFlags).let { visibleSubmenuActions ->
                when {
                    visibleAction.submenuActions.isNotEmpty() && visibleSubmenuActions.isEmpty() -> null
                    visibleSubmenuActions == visibleAction.submenuActions -> visibleAction
                    else -> visibleAction.copy(submenuActions = visibleSubmenuActions)
                }
            }
        }
    }
