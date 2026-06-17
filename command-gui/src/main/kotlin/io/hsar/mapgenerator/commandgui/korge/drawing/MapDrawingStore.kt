package io.hsar.mapgenerator.commandgui.korge.drawing

internal class MapDrawingStore {
    private val committedDrawings = mutableListOf<MapDrawing>()
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
        committedDrawings += drawing
        activePreview = null
    }

    fun clearPreview() {
        activePreview = null
    }
}
