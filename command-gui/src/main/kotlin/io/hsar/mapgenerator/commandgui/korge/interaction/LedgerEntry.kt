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
import kotlin.collections.asReversed
import kotlin.collections.forEachIndexed
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
    val color: java.awt.Color? = null,
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

    val redoEntriesInReplayOrder: List<LedgerEntry>
        get() = redoStack.asReversed().toList()

    val fullTimelineEntries: List<LedgerEntry>
        get() = appliedEntries + redoEntriesInReplayOrder

    val fullTimelineLastEntry: LedgerEntry?
        get() = fullTimelineEntries.lastOrNull()

    val liveHeadIndex: Int
        get() = fullTimelineEntries.lastIndex

    val turnMarkers: List<TurnMarker>
        get() = buildList {
            undoStack.forEachIndexed { index, entry ->
                (entry as? AddTurnLedgerEntry)?.let {
                    add(TurnMarker(name = it.name, appliedIndex = index, thumbnailAssetId = it.thumbnailAssetId))
                }
            }
            redoStack.asReversed().forEachIndexed { index, entry ->
                (entry as? AddTurnLedgerEntry)?.let {
                    add(TurnMarker(name = it.name, appliedIndex = undoStack.size + index, thumbnailAssetId = it.thumbnailAssetId, isFuture = true))
                }
            }
        }

    fun turnPanelState(mode: TurnPanelMode): TurnPanelState {
        val sourceEntries = if (mode is TurnPanelMode.ReadOnly) {
            fullTimelineEntries
        } else {
            appliedEntries
        }
        val markerRows = sourceEntries.mapIndexedNotNull { index, entry ->
            (entry as? AddTurnLedgerEntry)?.let {
                TurnPanelRow.Marker(
                    marker = TurnMarker(
                        name = it.name,
                        appliedIndex = index,
                        thumbnailAssetId = it.thumbnailAssetId,
                        isFuture = index >= undoStack.size,
                    ),
                    active = when (mode) {
                        TurnPanelMode.Edit -> appliedEntries.lastOrNull() is AddTurnLedgerEntry && index == undoStack.lastIndex
                        is TurnPanelMode.ReadOnly -> index == mode.selectedTurnAppliedIndex
                    },
                )
            }
        }
        val currentBoundaryEntry = when (mode) {
            TurnPanelMode.Edit -> appliedEntries.lastOrNull()
            is TurnPanelMode.ReadOnly -> fullTimelineLastEntry
        }
        val currentRow = if (currentBoundaryEntry is AddTurnLedgerEntry) {
            null
        } else {
            TurnPanelRow.Current(
                appliedIndex = when (mode) {
                    TurnPanelMode.Edit -> undoStack.size
                    is TurnPanelMode.ReadOnly -> fullTimelineEntries.size
                },
                active = mode == TurnPanelMode.Edit,
            )
        }
        return TurnPanelState(rows = currentRow?.let { markerRows + it } ?: markerRows)
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
    val thumbnailAssetId: UUID? = null,
    val isFuture: Boolean = false,
)

internal sealed interface TurnPanelMode {
    data object Edit : TurnPanelMode

    data class ReadOnly(
        val selectedTurnAppliedIndex: Int,
    ) : TurnPanelMode
}

internal data class TurnPanelState(
    val rows: List<TurnPanelRow>,
)

internal sealed interface TurnPanelRow {
    val appliedIndex: Int
    val active: Boolean

    data class Marker(
        val marker: TurnMarker,
        override val active: Boolean,
    ) : TurnPanelRow {
        override val appliedIndex: Int = marker.appliedIndex
    }

    data class Current(
        override val appliedIndex: Int,
        override val active: Boolean,
    ) : TurnPanelRow
}

internal fun List<TurnMarker>.nextTurnNumber(): Int =
    count { !it.isFuture && it.name != OPERATION_START_TURN_NAME } + 1

internal const val OPERATION_START_TURN_NAME = "Operation Start"
