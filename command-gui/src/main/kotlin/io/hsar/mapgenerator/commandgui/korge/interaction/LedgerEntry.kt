package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import java.util.UUID
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.also
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.asReversed
import kotlin.collections.buildList
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.lastIndex
import kotlin.collections.toList
import kotlin.let

internal data class MapPlacedObjectId(
    val value: UUID,
)

internal sealed interface MapPlacedObjectSnapshot {
    val id: MapPlacedObjectId
}

internal data class MapObjectMovePreview(
    val origin: MapCoordinate,
    val destination: MapCoordinate,
    val referenceText: String? = null,
)

internal data class UnitSnapshot(
    override val id: MapPlacedObjectId,
    val unitTemplateId: UnitTemplateId,
    val template: MapViewerUnitTemplate,
    val instanceName: String,
    val destroyed: Boolean,
    val facingDegrees: Int?,
    val mapCoordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
) : MapPlacedObjectSnapshot

internal data class DrawingSnapshot(
    override val id: MapPlacedObjectId,
    val drawing: MapDrawing,
) : MapPlacedObjectSnapshot

internal data class UnitTemplateId(
    val value: UUID,
)

internal data class UnitTemplateSnapshot(
    val id: UnitTemplateId,
    val template: MapViewerUnitTemplate,
    val index: Int,
)

internal interface LedgerController {
    fun createObject(snapshot: MapPlacedObjectSnapshot)

    fun applyObjectEdit(id: MapPlacedObjectId, changes: Map<String, JsonNode?>)

    fun deleteObject(id: MapPlacedObjectId): MapPlacedObjectSnapshot?

    fun createUnitTemplate(snapshot: UnitTemplateSnapshot)

    fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot)

    fun applyUnitTemplateEdit(id: UnitTemplateId, changes: Map<String, JsonNode?>): UnitTemplateSnapshot?

    fun deleteUnitTemplate(id: UnitTemplateId): UnitTemplateSnapshot?
}

internal interface LedgerEntry {
    fun undo(controller: LedgerController)

    fun redo(controller: LedgerController)
}

internal data class CreateObjectAction(
    val snapshot: MapPlacedObjectSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.deleteObject(snapshot.id)
    }

    override fun redo(controller: LedgerController) {
        controller.createObject(snapshot)
    }
}

internal data class DeleteObjectAction(
    val snapshot: MapPlacedObjectSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.createObject(snapshot)
    }

    override fun redo(controller: LedgerController) {
        controller.deleteObject(snapshot.id)
    }
}

internal data class EditObjectAction(
    val objectId: MapPlacedObjectId,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.applyObjectEdit(objectId, backward)
    }

    override fun redo(controller: LedgerController) {
        controller.applyObjectEdit(objectId, forward)
    }
}

internal data class BatchLedgerEntry(
    val entries: List<LedgerEntry>,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        entries.asReversed().forEach { it.undo(controller) }
    }

    override fun redo(controller: LedgerController) {
        entries.forEach { it.redo(controller) }
    }
}

internal data class AddTurnLedgerEntry(
    val name: String,
    val thumbnailAssetId: UUID? = null,
) : LedgerEntry {
    override fun undo(controller: LedgerController) = Unit

    override fun redo(controller: LedgerController) = Unit
}

internal data class CreateUnitTemplateAction(
    val snapshot: UnitTemplateSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.deleteUnitTemplate(snapshot.id)
    }

    override fun redo(controller: LedgerController) {
        controller.createUnitTemplate(snapshot)
    }
}

internal data class DeleteUnitTemplateAction(
    val snapshot: UnitTemplateSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.createUnitTemplate(snapshot)
    }

    override fun redo(controller: LedgerController) {
        controller.deleteUnitTemplate(snapshot.id)
    }
}

internal data class EditUnitTemplateAction(
    val id: UnitTemplateId,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
    val beforeSnapshot: UnitTemplateSnapshot? = null,
    val afterSnapshot: UnitTemplateSnapshot? = null,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        beforeSnapshot?.let(controller::replaceUnitTemplate)
            ?: controller.applyUnitTemplateEdit(id, backward)
    }

    override fun redo(controller: LedgerController) {
        afterSnapshot?.let(controller::replaceUnitTemplate)
            ?: controller.applyUnitTemplateEdit(id, forward)
    }
}

internal class MapActionHistory {
    private val undoStack = ArrayDeque<LedgerEntry>()
    private val redoStack = ArrayDeque<LedgerEntry>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val appliedEntries: List<LedgerEntry>
        get() = undoStack.toList()

    val turnMarkers: List<TurnMarker>
        get() = buildList {
            undoStack.forEachIndexed { index, entry ->
                (entry as? AddTurnLedgerEntry)?.let {
                    add(TurnMarker(name = it.name, appliedIndex = index))
                }
            }
            redoStack.asReversed().forEachIndexed { index, entry ->
                (entry as? AddTurnLedgerEntry)?.let {
                    add(TurnMarker(name = it.name, appliedIndex = undoStack.size + index, isFuture = true))
                }
            }
        }

    val currentTurn: CurrentTurn
        get() = when {
            redoStack.isNotEmpty() -> CurrentTurn(
                label = "Current",
                appliedIndex = if (undoStack.lastOrNull() is AddTurnLedgerEntry) undoStack.lastIndex else undoStack.size,
                selected = false,
                showInjectedCurrentRow = true,
                isLiveHead = false,
            )
            undoStack.lastOrNull() is AddTurnLedgerEntry -> CurrentTurn(
                label = (undoStack.lastOrNull() as AddTurnLedgerEntry).name,
                appliedIndex = undoStack.lastIndex,
                selected = true,
                showInjectedCurrentRow = false,
                isLiveHead = true,
            )

            else -> CurrentTurn(
                label = "Current",
                appliedIndex = undoStack.size,
                selected = true,
                showInjectedCurrentRow = true,
                isLiveHead = true,
            )
        }

    fun record(action: LedgerEntry) {
        undoStack.addLast(action)
        redoStack.clear()
    }

    fun undo(controller: LedgerController): Boolean {
        val action = undoStack.removeLastOrNull() ?: return false
        action.undo(controller)
        redoStack.addLast(action)
        return true
    }

    fun redo(controller: LedgerController): Boolean {
        val action = redoStack.removeLastOrNull() ?: return false
        action.redo(controller)
        undoStack.addLast(action)
        return true
    }

    fun undoEntry(controller: LedgerController): LedgerEntry? =
        undoStack.removeLastOrNull()?.also { action ->
            action.undo(controller)
            redoStack.addLast(action)
        }

    fun redoEntry(controller: LedgerController): LedgerEntry? =
        redoStack.removeLastOrNull()?.also { action ->
            action.redo(controller)
            undoStack.addLast(action)
        }

    fun revertToLedgerIndex(index: Int, controller: LedgerController): List<LedgerEntry> =
        buildList {
            while (undoStack.lastIndex > index) {
                undoEntry(controller)?.let(::add) ?: return@buildList
            }
            while (undoStack.lastIndex < index) {
                redoEntry(controller)?.let(::add) ?: return@buildList
            }
        }

    fun revertToCurrent(controller: LedgerController): List<LedgerEntry> =
        buildList {
            while (redoStack.isNotEmpty()) {
                redoEntry(controller)?.let(::add) ?: return@buildList
            }
        }

    fun revertToAppliedIndex(index: Int, controller: LedgerController): List<LedgerEntry> =
        revertToLedgerIndex(index, controller)
}

internal data class TurnMarker(
    val name: String,
    val appliedIndex: Int,
    val isFuture: Boolean = false,
)

internal data class CurrentTurn(
    val label: String,
    val appliedIndex: Int,
    val selected: Boolean,
    val showInjectedCurrentRow: Boolean,
    val isLiveHead: Boolean,
)
