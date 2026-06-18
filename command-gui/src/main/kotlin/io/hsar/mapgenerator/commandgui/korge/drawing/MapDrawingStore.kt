package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace

internal class MapDrawingStore {
    private var nextId = 1L
    private val committedDrawings = mutableListOf<PlacedDrawing>()
    private var activePreview: MapDrawing? = null

    val state: MapDrawingState
        get() = MapDrawingState(
            drawings = committedDrawings.toList(),
            preview = activePreview,
        )

    fun preview(drawing: MapDrawing?) {
        activePreview = drawing
    }

    fun commit(drawing: MapDrawing) {
        val id = nextId++
        committedDrawings += PlacedDrawing(
            id = id,
            drawing = drawing,
            onDelete = { remove(id) },
        )
        activePreview = null
    }

    fun remove(id: Long): Boolean =
        committedDrawings.removeAll { it.id == id }

    fun findAt(mapCoordinate: MapCoordinate, tolerance: Double, coordinateSpace: MapCoordinateSpace): PlacedDrawing? =
        committedDrawings.lastOrNull { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }

    fun clearPreview() {
        activePreview = null
    }
}
