package io.hsar.mapgenerator.commandgui.korge.interaction

internal data class MapPlacedObjectMenuAction(
    val label: String,
    val onSelected: () -> Unit,
)

internal interface MapContextMenuTarget {
    fun contextMenuActions(): List<MapPlacedObjectMenuAction>
}
