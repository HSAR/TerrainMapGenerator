package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import java.util.UUID

const val OPERATION_START_TURN_NAME = "Operation Start"

sealed interface OperationLedgerTarget {
    data class Object(val id: OperationObjectId) : OperationLedgerTarget
    data class UnitTemplate(val id: OperationUnitTemplateId) : OperationLedgerTarget
    data class GraphicTemplate(val id: OperationGraphicTemplateId) : OperationLedgerTarget
    data class Note(val id: OperationNoteId) : OperationLedgerTarget
    data object Notes : OperationLedgerTarget
}

data class OperationTurnMarker(
    val entryIndex: Int,
    val name: String,
    val thumbnailAssetId: UUID?,
    val isFuture: Boolean,
) {
    val appliedCount get() = entryIndex + 1
}

data class OperationTimelineAction(
    val entryIndex: Int,
    val entry: OperationTimelineEntry,
    val isFuture: Boolean,
    val targets: List<OperationLedgerTarget>,
)

data class OperationTurnGroup(
    val marker: OperationTurnMarker?,
    val name: String,
    val actions: List<OperationTimelineAction>,
)

object OperationTimelineEntries {
    fun change(before: OperationContent, changeSet: OperationChangeSet): OperationTimelineEntry? =
        changes(before, changeSet.atomicComponents())

    fun changes(before: OperationContent, componentChangeSets: List<OperationChangeSet>): OperationTimelineEntry? {
        val components = componentChangeSets.filterNot(OperationChangeSet::isEmpty)
        if (components.isEmpty()) return null
        val after = components.fold(before) { content, change -> change.applyTo(content) }
        val combined = after.changesFrom(before)
        val (derivedKind, description) = operationEntryDescription(combined, before, componentChangeSets = components)
        return OperationTimelineEntry(
            changeSet = combined,
            kind = if (components.size > 1) MapViewerLedgerEntryKind.BATCH else derivedKind,
            description = description,
            componentChangeSets = components,
        )
    }

    fun turn(name: String, thumbnailAssetId: UUID? = null) = OperationTimelineEntry(
        changeSet = OperationChangeSet(),
        kind = MapViewerLedgerEntryKind.TURN,
        description = "added $name",
        turnName = name,
        turnThumbnailAssetId = thumbnailAssetId,
        componentChangeSets = emptyList(),
    )
}

fun OperationTimeline.recordChange(changeSet: OperationChangeSet): OperationTimeline =
    OperationTimelineEntries.change(contentAt(), changeSet)?.let(::record) ?: this

fun OperationTimeline.recordChanges(componentChangeSets: List<OperationChangeSet>): OperationTimeline =
    OperationTimelineEntries.changes(contentAt(), componentChangeSets)?.let(::record) ?: this

fun OperationTimeline.turnMarkers(): List<OperationTurnMarker> = entries.mapIndexedNotNull { index, entry ->
    entry.turnName?.let { OperationTurnMarker(index, it, entry.turnThumbnailAssetId, index >= appliedCount) }
}

fun OperationTimeline.nextTurnName(): String =
    "Turn ${entries.count { it.turnName != null && it.turnName != OPERATION_START_TURN_NAME } + 1}"

fun OperationTimeline.turnGroups(): List<OperationTurnGroup> {
    val groups = mutableListOf<OperationTurnGroup>()
    var marker: OperationTurnMarker? = null
    var actions = mutableListOf<OperationTimelineAction>()

    fun flush() {
        if (marker != null || actions.isNotEmpty()) {
            groups += OperationTurnGroup(marker, marker?.name ?: "Unmarked History", actions.toList())
        }
    }

    entries.forEachIndexed { index, entry ->
        if (entry.turnName != null) {
            flush()
            marker = OperationTurnMarker(index, entry.turnName, entry.turnThumbnailAssetId, index >= appliedCount)
            actions = mutableListOf()
        } else {
            actions += OperationTimelineAction(index, entry, index >= appliedCount, entry.affectedTargets())
        }
    }
    flush()
    return groups
}

fun OperationTimelineEntry.affectedTargets(): List<OperationLedgerTarget> = componentChangeSets
    .flatMap(OperationChangeSet::affectedTargets)
    .distinct()

private fun OperationChangeSet.affectedTargets() = buildList {
    unitTemplates.mapTo(this) { OperationLedgerTarget.UnitTemplate((it.after ?: it.before)!!.id) }
    graphicTemplates.mapTo(this) { OperationLedgerTarget.GraphicTemplate((it.after ?: it.before)!!.id) }
    objects.mapTo(this) { OperationLedgerTarget.Object((it.after ?: it.before)!!.id) }
    notes.mapTo(this) { OperationLedgerTarget.Note((it.after ?: it.before)!!.id) }
    if (noteOrderAfter != null) add(OperationLedgerTarget.Notes)
}

private fun OperationChangeSet.atomicComponents(): List<OperationChangeSet> = buildList {
    unitTemplates.mapTo(this) { OperationChangeSet(unitTemplates = listOf(it)) }
    graphicTemplates.mapTo(this) { OperationChangeSet(graphicTemplates = listOf(it)) }
    objects.mapTo(this) { OperationChangeSet(objects = listOf(it)) }
    notes.mapTo(this) { OperationChangeSet(notes = listOf(it)) }
    if (noteOrderAfter != null) add(OperationChangeSet(noteOrderBefore = noteOrderBefore, noteOrderAfter = noteOrderAfter))
}
