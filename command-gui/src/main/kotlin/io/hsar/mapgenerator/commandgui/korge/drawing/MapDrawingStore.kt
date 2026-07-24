package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId

internal class MapDrawingStore {
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

    fun upsert(snapshot: DrawingSnapshot) {
        val index = committedDrawings.indexOfFirst { it.id == snapshot.id }
        if (index >= 0) {
            committedDrawings[index] = PlacedDrawing(
                id = snapshot.id,
                drawing = snapshot.drawing,
                customProperties = snapshot.customProperties,
            )
        } else {
            committedDrawings += PlacedDrawing(
                id = snapshot.id,
                drawing = snapshot.drawing,
            )
        }
        activePreview = null
    }

    fun replaceAll(snapshots: List<DrawingSnapshot>) {
        committedDrawings.clear()
        committedDrawings += snapshots.map { PlacedDrawing(it.id, it.drawing, it.customProperties) }
        activePreview = null
    }

    fun remove(id: MapPlacedObjectId): DrawingSnapshot? =
        committedDrawings.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }
            ?.let { index ->
                committedDrawings.removeAt(index).snapshot()
            }

    fun snapshot(id: MapPlacedObjectId): DrawingSnapshot? =
        committedDrawings.firstOrNull { it.id == id }?.snapshot()

    fun snapshots(): List<DrawingSnapshot> =
        committedDrawings.map { it.snapshot() }

    fun findAt(mapCoordinate: MapCoordinate, tolerance: Double, coordinateSpace: MapCoordinateSpace): PlacedDrawing? =
        committedDrawings.lastOrNull { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }

    fun clearPreview() {
        activePreview = null
    }
}
