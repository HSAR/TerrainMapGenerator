package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag

internal data class MapPlacedObjectMenuAction(
    val label: String,
    val onSelected: () -> Unit,
    val submenuActions: List<MapPlacedObjectMenuAction> = emptyList(),
    val requiredFeatureFlags: Set<MapViewerFeatureFlag> = emptySet(),
    val enabled: Boolean = true,
    val disabledTooltipText: String? = null,
)

internal interface MapContextMenuTarget {
    fun contextMenuActions(): List<MapPlacedObjectMenuAction>
}

internal fun MapPlacedObjectMenuAction.withRequiredFeatureFlags(vararg featureFlags: MapViewerFeatureFlag): MapPlacedObjectMenuAction =
    copy(requiredFeatureFlags = requiredFeatureFlags + featureFlags)

internal fun MapPlacedObjectMenuAction.isVisible(featureFlags: Set<MapViewerFeatureFlag>): Boolean =
    featureFlags.containsAll(requiredFeatureFlags)

internal fun List<MapPlacedObjectMenuAction>.visibleFor(featureFlags: Set<MapViewerFeatureFlag>): List<MapPlacedObjectMenuAction> =
    mapNotNull { it.visibleFor(featureFlags) }

private fun MapPlacedObjectMenuAction.visibleFor(featureFlags: Set<MapViewerFeatureFlag>): MapPlacedObjectMenuAction? {
    if (!isVisible(featureFlags)) {
        return null
    }

    val visibleSubmenuActions = submenuActions.visibleFor(featureFlags)
    if (submenuActions.isNotEmpty() && visibleSubmenuActions.isEmpty()) {
        return null
    }

    return if (visibleSubmenuActions == submenuActions) {
        this
    } else {
        copy(submenuActions = visibleSubmenuActions)
    }
}
