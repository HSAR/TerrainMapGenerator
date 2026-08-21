package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingIconRenderer
import korlibs.datastructure.count
import korlibs.image.bitmap.Bitmap32
import korlibs.io.util.buildList
import java.util.Collections.emptyMap
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
import kotlin.plus
import kotlin.takeIf
import kotlin.takeUnless
import kotlin.text.Regex
import kotlin.text.format
import kotlin.text.ifBlank
import kotlin.text.isNotBlank
import kotlin.text.lowercase
import kotlin.text.take
import kotlin.text.trimEnd

internal data class MapPlacedObjectId(
    val value: UUID,
)

internal sealed interface MapPlacedObjectSnapshot {
    val id: MapPlacedObjectId

    @get:JsonIgnore
    val name: String

    @get:JsonIgnore
    val icon: Bitmap32
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
    val customProperties: Map<String, String> = emptyMap(),
) : MapPlacedObjectSnapshot {
    override val name: String
        get() = instanceName

    override val icon: Bitmap32
        get() = template.bitmap
}

internal data class DrawingSnapshot(
    override val id: MapPlacedObjectId,
    val drawing: MapDrawing,
    val customProperties: Map<String, String> = emptyMap(),
) : MapPlacedObjectSnapshot {
    override val name: String
        get() = drawing.name

    override val icon: Bitmap32
        get() = MapDrawingIconRenderer.render(drawing)
}

internal data class UnitTemplateId(
    val value: UUID,
)

internal data class GraphicTemplateId(
    val value: UUID,
)

internal data class GraphicTemplateSnapshot(
    val id: GraphicTemplateId,
    val template: MapViewerGraphicTemplate,
    val index: Int,
)

internal data class GraphicSnapshot(
    override val id: MapPlacedObjectId,
    val graphicTemplateId: GraphicTemplateId,
    val template: MapViewerGraphicTemplate,
    val instanceName: String,
    val mapCoordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
    val customProperties: Map<String, String> = emptyMap(),
) : MapPlacedObjectSnapshot {
    override val name: String
        get() = instanceName

    override val icon: Bitmap32
        get() = template.bitmap
}

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

    fun createGraphicTemplate(snapshot: GraphicTemplateSnapshot) {}

    fun replaceGraphicTemplate(snapshot: GraphicTemplateSnapshot) {}

    fun applyGraphicTemplateEdit(id: GraphicTemplateId, changes: Map<String, JsonNode?>): GraphicTemplateSnapshot? = null

    fun deleteGraphicTemplate(id: GraphicTemplateId): GraphicTemplateSnapshot? = null

    fun createNote(snapshot: MapNoteSnapshot)

    fun applyNoteEdit(id: MapNoteId, changes: Map<String, JsonNode?>): MapNoteSnapshot?

    fun deleteNote(id: MapNoteId): MapNoteSnapshot?

    fun reorderNotes(ids: List<MapNoteId>) {}
}

internal interface LedgerEntry {
    fun undo(controller: LedgerController)

    fun redo(controller: LedgerController)
}

internal enum class LedgerActionDirection {
    Undo,
    Redo,
}

internal fun LedgerEntry.changeDescription(
    direction: LedgerActionDirection,
    objectSnapshot: (MapPlacedObjectId) -> MapPlacedObjectSnapshot? = { null },
): String =
    when (this) {
        is CreateObjectAction -> if (direction == LedgerActionDirection.Undo) "removed ${snapshot.objectDescription()}" else "created ${snapshot.objectDescription()}"
        is DeleteObjectAction -> if (direction == LedgerActionDirection.Undo) "restored ${snapshot.objectDescription()}" else "deleted ${snapshot.objectDescription()}"
        is EditObjectAction -> objectSnapshot(objectId)
            ?.editDescription(if (direction == LedgerActionDirection.Undo) backward else forward)
            ?: "edited an item"

        is BatchLedgerEntry -> entries.firstOrNull()
            ?.changeDescription(direction, objectSnapshot)
            ?.let { first -> if (entries.size == 1) first else "$first and ${entries.size - 1} other item${if (entries.size == 2) "" else "s"}" }
            ?: "changed no items"

        is AddTurnLedgerEntry -> if (direction == LedgerActionDirection.Undo) "removed $name" else "added $name"
        is CreateUnitTemplateAction -> if (direction == LedgerActionDirection.Undo) "removed unit template ${snapshot.template.name}" else "created unit template ${snapshot.template.name}"
        is DeleteUnitTemplateAction -> if (direction == LedgerActionDirection.Undo) "restored unit template ${snapshot.template.name}" else "deleted unit template ${snapshot.template.name}"
        is EditUnitTemplateAction -> editDescription(
            item = "unit template ${afterSnapshot?.template?.name ?: beforeSnapshot?.template?.name ?: ""}".trimEnd(),
            changes = if (direction == LedgerActionDirection.Undo) backward else forward,
        )

        is CreateGraphicTemplateAction -> if (direction == LedgerActionDirection.Undo) "removed graphic template ${snapshot.template.name}" else "created graphic template ${snapshot.template.name}"
        is DeleteGraphicTemplateAction -> if (direction == LedgerActionDirection.Undo) "restored graphic template ${snapshot.template.name}" else "deleted graphic template ${snapshot.template.name}"
        is EditGraphicTemplateAction -> editDescription(
            item = "graphic template ${afterSnapshot?.template?.name ?: beforeSnapshot?.template?.name ?: ""}".trimEnd(),
            changes = if (direction == LedgerActionDirection.Undo) backward else forward,
        )

        is CreateNoteAction -> if (direction == LedgerActionDirection.Undo) "removed note ${snapshot.title}" else "created note ${snapshot.title}"
        is EditNoteAction -> editDescription("note", if (direction == LedgerActionDirection.Undo) backward else forward)
        is DeleteNoteAction -> if (direction == LedgerActionDirection.Undo) "restored note ${snapshot.title}" else "deleted note ${snapshot.title}"
        is ReorderNotesAction -> "reordered notes"
        else -> "changed an item"
    }

private fun editDescription(item: String, changes: Map<String, JsonNode?>): String =
    changes.entries
        .sortedBy { it.key }
        .mapNotNull { (key, value) -> value?.takeUnless(JsonNode::isNull)?.let { "$key to ${it.safeDisplayValue()}" } ?: "removed $key" }
        .joinToString(", ")
        .takeIf(String::isNotBlank)
        ?.let { "$item: $it" }
        ?: "edited $item"

private fun MapPlacedObjectSnapshot.objectDescription(): String =
    when (this) {
        is UnitSnapshot -> "unit ${instanceName.ifBlank { template.name }} (${template.name})"
        is GraphicSnapshot -> "graphic ${displayName() ?: instanceName.ifBlank { template.name }} (${template.name})"
        is DrawingSnapshot -> drawing.name
    }

private fun MapPlacedObjectSnapshot.editDescription(changes: Map<String, JsonNode?>): String =
    when (this) {
        is UnitSnapshot -> objectDescription() + ": " + unitChangeDescriptions(changes).joinToString(", ")
        is GraphicSnapshot -> objectDescription() + ": " + graphicChangeDescriptions(changes).joinToString(", ")
        is DrawingSnapshot -> objectDescription() + ": " + drawingChangeDescriptions(changes).joinToString(", ")
    }

private fun UnitSnapshot.unitChangeDescriptions(changes: Map<String, JsonNode?>): List<String> =
    buildList {
        if ("instanceName" in changes) add("renamed to $instanceName")
        if ("unitTemplateId" in changes) add("type set to ${template.name}")
        if ("destroyed" in changes) add(if (destroyed) "marked destroyed" else "marked active")
        if ("facingDegrees" in changes) add("facing ${facingDegrees?.let { "$it°" } ?: "unset"}")
        if ("affiliation" in changes) add("affiliation set to ${template.affiliation?.name?.lowercase() ?: "unset"}")
        if ("mapCoordinate" in changes) add("moved to ${mapCoordinate.formatNoticeCoordinate()}")
        if ("normalizedWidth" in changes || "normalizedHeight" in changes) add("size set to ${normalizedWidth.formatNoticeNumber()} × ${normalizedHeight.formatNoticeNumber()}")
        addAll(customPropertyChangeDescriptions(changes, UNIT_PATCH_FIELDS))
    }.ifEmpty { listOf("updated") }

private fun GraphicSnapshot.graphicChangeDescriptions(changes: Map<String, JsonNode?>): List<String> =
    buildList {
        if ("instanceName" in changes) add("renamed to $instanceName")
        if ("graphicTemplateId" in changes) add("type set to ${template.name}")
        if ("mapCoordinate" in changes) add("moved to ${mapCoordinate.formatNoticeCoordinate()}")
        if ("normalizedWidth" in changes || "normalizedHeight" in changes) add("size set to ${normalizedWidth.formatNoticeNumber()} × ${normalizedHeight.formatNoticeNumber()}")
        addAll(customPropertyChangeDescriptions(changes, GRAPHIC_PATCH_FIELDS))
    }.ifEmpty { listOf("updated") }

private fun DrawingSnapshot.drawingChangeDescriptions(changes: Map<String, JsonNode?>): List<String> =
    buildList {
        when (drawing) {
            is io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing ->
                if ("points" in changes) add("points set to ${drawing.points.size}")

            is io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing -> {
                if ("start" in changes) add("start set to ${drawing.start.formatNoticeCoordinate()}")
                if ("end" in changes) add("end set to ${drawing.end.formatNoticeCoordinate()}")
            }

            is io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing -> {
                if ("start" in changes || "end" in changes) add("bounds set to ${drawing.start.formatNoticeCoordinate()}–${drawing.end.formatNoticeCoordinate()}")
                if ("rotationDegrees" in changes) add("rotation set to ${drawing.rotationDegrees.formatNoticeNumber()}°")
            }

            is io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing -> {
                if ("center" in changes) add("centre set to ${drawing.center.formatNoticeCoordinate()}")
                if ("xRadius" in changes || "yRadius" in changes) add("radii set to ${drawing.xRadius.formatNoticeNumber()} × ${drawing.yRadius.formatNoticeNumber()}")
                if ("rotationDegrees" in changes) add("rotation set to ${drawing.rotationDegrees.formatNoticeNumber()}°")
            }
        }
        if ("color" in changes) add("colour set to ${drawing.color.toNoticeColour()}")
        if ("drawing" in changes) add("shape set to ${drawing.name}")
        addAll(customPropertyChangeDescriptions(changes, DRAWING_PATCH_FIELDS))
    }.ifEmpty { listOf("updated") }

private fun customPropertyChangeDescriptions(changes: Map<String, JsonNode?>, reservedKeys: Set<String>): List<String> =
    changes.entries
        .filterNot { (key, _) -> key in reservedKeys }
        .sortedBy { it.key }
        .map { (key, value) ->
            value?.takeUnless(JsonNode::isNull)
                ?.safeDisplayValue()
                ?.let { "$key set to $it" }
                ?: "removed $key"
        }

private fun MapCoordinate.formatNoticeCoordinate() =
    "(${x.formatNoticeNumber()}, ${y.formatNoticeNumber()})"

private fun io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint.formatNoticeCoordinate() =
    "(${x.formatNoticeNumber()}, ${y.formatNoticeNumber()})"

private fun Double.formatNoticeNumber() = "%.3f".format(java.util.Locale.ROOT, this).trimEnd('0').trimEnd('.')

private fun java.awt.Color.toNoticeColour() = "#%02X%02X%02X%02X".format(alpha, red, green, blue)

private fun JsonNode.safeDisplayValue(): String =
    asText()
        .let { value -> if (UUID_PATTERN.containsMatchIn(value)) "redacted" else value }
        .take(MAX_NOTICE_VALUE_LENGTH)
        .let { value -> if (value.length == MAX_NOTICE_VALUE_LENGTH) "$value…" else value }

private val UUID_PATTERN = Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")
private const val MAX_NOTICE_VALUE_LENGTH = 80
private val UNIT_PATCH_FIELDS = setOf("unitTemplateId", "instanceName", "destroyed", "facingDegrees", "affiliation", "mapCoordinate", "normalizedWidth", "normalizedHeight")
private val DRAWING_PATCH_FIELDS = setOf("drawing", "points", "color", "start", "end", "rotationDegrees", "center", "xRadius", "yRadius")
private val GRAPHIC_PATCH_FIELDS = setOf("graphicTemplateId", "instanceName", "mapCoordinate", "normalizedWidth", "normalizedHeight")

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

    companion object {
        fun fromSnapshots(before: MapPlacedObjectSnapshot, after: MapPlacedObjectSnapshot): EditObjectAction {
            val forward = MapPlacedObjectPatch.diff(before, after)
            val backward = MapPlacedObjectPatch.diff(after, before)
            return EditObjectAction(
                objectId = before.id,
                forward = forward,
                backward = backward,
            )
        }
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

internal data class CreateGraphicTemplateAction(
    val snapshot: GraphicTemplateSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.deleteGraphicTemplate(snapshot.id)
    }

    override fun redo(controller: LedgerController) {
        controller.createGraphicTemplate(snapshot)
    }
}

internal data class DeleteGraphicTemplateAction(
    val snapshot: GraphicTemplateSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.createGraphicTemplate(snapshot)
    }

    override fun redo(controller: LedgerController) {
        controller.deleteGraphicTemplate(snapshot.id)
    }
}

internal data class EditGraphicTemplateAction(
    val id: GraphicTemplateId,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
    val beforeSnapshot: GraphicTemplateSnapshot? = null,
    val afterSnapshot: GraphicTemplateSnapshot? = null,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        beforeSnapshot?.let(controller::replaceGraphicTemplate)
            ?: controller.applyGraphicTemplateEdit(id, backward)
    }

    override fun redo(controller: LedgerController) {
        afterSnapshot?.let(controller::replaceGraphicTemplate)
            ?: controller.applyGraphicTemplateEdit(id, forward)
    }
}

internal data class CreateNoteAction(
    val snapshot: MapNoteSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.deleteNote(snapshot.id)
    }

    override fun redo(controller: LedgerController) {
        controller.createNote(snapshot)
    }
}

internal data class EditNoteAction(
    val id: MapNoteId,
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.applyNoteEdit(id, backward)
    }

    override fun redo(controller: LedgerController) {
        controller.applyNoteEdit(id, forward)
    }

    companion object {
        fun fromSnapshots(before: MapNoteSnapshot, after: MapNoteSnapshot): EditNoteAction {
            val forward = MapNotePatch.diff(before, after)
            val backward = MapNotePatch.diff(after, before)
            return EditNoteAction(
                id = before.id,
                forward = forward,
                backward = backward,
            )
        }
    }
}

internal data class DeleteNoteAction(
    val snapshot: MapNoteSnapshot,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.createNote(snapshot)
    }

    override fun redo(controller: LedgerController) {
        controller.deleteNote(snapshot.id)
    }
}

internal data class ReorderNotesAction(
    val forward: List<MapNoteId>,
    val backward: List<MapNoteId>,
) : LedgerEntry {
    override fun undo(controller: LedgerController) {
        controller.reorderNotes(backward)
    }

    override fun redo(controller: LedgerController) {
        controller.reorderNotes(forward)
    }
}

internal class MapActionHistory {
    private val undoStack = ArrayDeque<LedgerEntry>()
    private val redoStack = ArrayDeque<LedgerEntry>()
    private val overriddenTurnOrigins = mutableMapOf<MapPlacedObjectId, MapCoordinate>()

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
        if (action is AddTurnLedgerEntry) overriddenTurnOrigins.clear()
    }

    fun resetTurnOrigin(unitId: MapPlacedObjectId, coordinate: MapCoordinate) {
        overriddenTurnOrigins[unitId] = coordinate
    }

    internal fun overriddenTurnOriginFor(unitId: MapPlacedObjectId): MapCoordinate? =
        overriddenTurnOrigins[unitId]

    fun undo(controller: LedgerController): Boolean {
        val action = undoStack.removeLastOrNull() ?: return false
        action.undo(controller)
        redoStack.addLast(action)
        overriddenTurnOrigins.clear()
        return true
    }

    fun redo(controller: LedgerController): Boolean {
        val action = redoStack.removeLastOrNull() ?: return false
        action.redo(controller)
        undoStack.addLast(action)
        overriddenTurnOrigins.clear()
        return true
    }

    fun undoEntry(controller: LedgerController): LedgerEntry? =
        undoStack.removeLastOrNull()?.also { action ->
            action.undo(controller)
            redoStack.addLast(action)
            overriddenTurnOrigins.clear()
        }

    fun redoEntry(controller: LedgerController): LedgerEntry? =
        redoStack.removeLastOrNull()?.also { action ->
            action.redo(controller)
            undoStack.addLast(action)
            overriddenTurnOrigins.clear()
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

    fun advanceToLiveHeadForAutosave(controller: LedgerController): List<LedgerEntry> =
        revertToCurrent(controller)

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
