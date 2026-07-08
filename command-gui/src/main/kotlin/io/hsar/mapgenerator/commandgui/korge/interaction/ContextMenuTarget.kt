package io.hsar.mapgenerator.commandgui.korge.interaction

internal data class MapPlacedObjectMenuAction(
    val label: String,
    val onSelected: () -> Unit,
    val submenuActions: List<MapPlacedObjectMenuAction> = emptyList(),
)

internal interface MapContextMenuTarget {
    fun contextMenuActions(): List<MapPlacedObjectMenuAction>
}
