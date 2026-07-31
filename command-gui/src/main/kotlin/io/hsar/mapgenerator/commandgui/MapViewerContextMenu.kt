package io.hsar.mapgenerator.commandgui

import java.util.Collections.emptyList
import java.util.Collections.emptySet

/** Declarative context-menu actions derived from an engine-neutral replayed map object. */
enum class MapViewerContextMenuCommand {
    EDIT_UNIT,
    DELETE_UNIT,
    SITREP,
    EDIT_GRAPHIC,
    DELETE_GRAPHIC,
    DELETE_DRAWING,
}

data class MapViewerContextMenuItem(
    val action: MapViewerContextMenuCommand,
    val label: String,
)

fun MapViewerReplayObject.contextMenuItems(): List<MapViewerContextMenuItem> =
    when (kind) {
        MapViewerReplayObjectKind.UNIT -> listOf(
            MapViewerContextMenuItem(MapViewerContextMenuCommand.EDIT_UNIT, "Edit Unit"),
            MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_UNIT, "Delete Unit"),
            MapViewerContextMenuItem(MapViewerContextMenuCommand.SITREP, "SITREP"),
        )

        MapViewerReplayObjectKind.GRAPHIC -> listOf(
            MapViewerContextMenuItem(MapViewerContextMenuCommand.EDIT_GRAPHIC, "Edit Graphic"),
            MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_GRAPHIC, "Delete Graphic"),
        )

        MapViewerReplayObjectKind.DRAWING -> listOf(
            MapViewerContextMenuItem(MapViewerContextMenuCommand.DELETE_DRAWING, "Delete Drawing"),
        )
    }

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
