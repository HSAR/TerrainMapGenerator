package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing

internal data class MapPlacedObjectId(
    val value: Long,
)

internal sealed interface MapPlacedObjectSnapshot {
    val id: MapPlacedObjectId
}

internal data class UnitSnapshot(
    override val id: MapPlacedObjectId,
    val template: MapViewerUnitTemplate,
    val instanceName: String,
    val mapCoordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
) : MapPlacedObjectSnapshot

internal data class DrawingSnapshot(
    override val id: MapPlacedObjectId,
    val drawing: MapDrawing,
) : MapPlacedObjectSnapshot

internal interface MapAction {
    fun undo(controller: MapObjectController)

    fun redo(controller: MapObjectController)
}

internal data class CreateObjectAction(
    val snapshot: MapPlacedObjectSnapshot,
) : MapAction {
    override fun undo(controller: MapObjectController) {
        controller.deleteObject(snapshot.id)
    }

    override fun redo(controller: MapObjectController) {
        controller.createObject(snapshot)
    }
}

internal data class DeleteObjectAction(
    val snapshot: MapPlacedObjectSnapshot,
) : MapAction {
    override fun undo(controller: MapObjectController) {
        controller.createObject(snapshot)
    }

    override fun redo(controller: MapObjectController) {
        controller.deleteObject(snapshot.id)
    }
}

internal data class MoveObjectAction(
    val before: MapPlacedObjectSnapshot,
    val after: MapPlacedObjectSnapshot,
) : MapAction {
    override fun undo(controller: MapObjectController) {
        controller.updateObject(before)
    }

    override fun redo(controller: MapObjectController) {
        controller.updateObject(after)
    }
}

internal data class EditUnitAction(
    val before: UnitSnapshot,
    val after: UnitSnapshot,
) : MapAction {
    override fun undo(controller: MapObjectController) {
        controller.updateObject(before)
    }

    override fun redo(controller: MapObjectController) {
        controller.updateObject(after)
    }
}

internal class MapActionHistory {
    private val undoStack = ArrayDeque<MapAction>()
    private val redoStack = ArrayDeque<MapAction>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun record(action: MapAction) {
        undoStack.addLast(action)
        redoStack.clear()
    }

    fun undo(controller: MapObjectController): Boolean {
        val action = undoStack.removeLastOrNull() ?: return false
        action.undo(controller)
        redoStack.addLast(action)
        return true
    }

    fun redo(controller: MapObjectController): Boolean {
        val action = redoStack.removeLastOrNull() ?: return false
        action.redo(controller)
        undoStack.addLast(action)
        return true
    }
}
