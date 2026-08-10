package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.MapViewerTool
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import java.util.Collections.emptySet

@JvmInline
value class OperationArgb(val value: Int) {
    val alpha: Int get() = value ushr 24 and 0xff
    val red: Int get() = value ushr 16 and 0xff
    val green: Int get() = value ushr 8 and 0xff
    val blue: Int get() = value and 0xff

    companion object {
        fun of(alpha: Int, red: Int, green: Int, blue: Int) = OperationArgb(
            ((alpha and 0xff) shl 24) or ((red and 0xff) shl 16) or
                    ((green and 0xff) shl 8) or (blue and 0xff),
        )
    }
}

@JvmInline
value class OperationObjectId(val value: UUID)
@JvmInline
value class OperationUnitTemplateId(val value: UUID)
@JvmInline
value class OperationGraphicTemplateId(val value: UUID)
@JvmInline
value class OperationNoteId(val value: UUID)

enum class OperationUnitAffiliation { FRIEND, HOSTILE, NEUTRAL, UNKNOWN }

data class OperationDrawingPoint(val x: Double, val y: Double)

sealed interface OperationDrawing {
    val colour: OperationArgb
    val displayName: String
}

data class OperationFreeformDrawing(
    val points: List<OperationDrawingPoint>,
    override val colour: OperationArgb,
) : OperationDrawing {
    override val displayName = "Freeform Drawing"
}

data class OperationLineDrawing(
    val start: OperationDrawingPoint,
    val end: OperationDrawingPoint,
    override val colour: OperationArgb,
) : OperationDrawing {
    override val displayName = "Line Drawing"
}

data class OperationRectangleDrawing(
    val start: OperationDrawingPoint,
    val end: OperationDrawingPoint,
    override val colour: OperationArgb,
    val rotationDegrees: Double = 0.0,
) : OperationDrawing {
    override val displayName = "Rectangle Drawing"
}

data class OperationOvalDrawing(
    val centre: OperationDrawingPoint,
    val xRadius: Double,
    val yRadius: Double,
    override val colour: OperationArgb,
    val rotationDegrees: Double = 0.0,
) : OperationDrawing {
    override val displayName = "Oval Drawing"
}

data class OperationUnitTemplate(
    val id: OperationUnitTemplateId,
    val name: String,
    val image: BufferedImage,
    val index: Int,
    val assetSource: AssetSource? = null,
    val affiliation: OperationUnitAffiliation? = null,
    val customProperties: Map<String, String> = emptyMap(),
)

data class OperationGraphicTemplate(
    val id: OperationGraphicTemplateId,
    val name: String,
    val image: BufferedImage,
    val index: Int,
    val normalizedWidth: Double = 0.1,
    val normalizedHeight: Double = 0.1,
    val assetSource: AssetSource? = null,
    val customProperties: Map<String, String> = emptyMap(),
)

sealed interface OperationObject {
    val id: OperationObjectId
    val customProperties: Map<String, String>
    val name: String
}

data class OperationUnit(
    override val id: OperationObjectId,
    val unitTemplateId: OperationUnitTemplateId,
    val instanceName: String,
    val destroyed: Boolean,
    val facingDegrees: Int?,
    val coordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
    override val customProperties: Map<String, String> = emptyMap(),
    /** Legacy saves may override the template affiliation for one placed unit. */
    val affiliation: OperationUnitAffiliation? = null,
) : OperationObject {
    override val name = instanceName
}

data class OperationGraphic(
    override val id: OperationObjectId,
    val graphicTemplateId: OperationGraphicTemplateId,
    val instanceName: String,
    val coordinate: MapCoordinate,
    val normalizedWidth: Double,
    val normalizedHeight: Double,
    override val customProperties: Map<String, String> = emptyMap(),
) : OperationObject {
    override val name = instanceName
}

data class OperationDrawingObject(
    override val id: OperationObjectId,
    val drawing: OperationDrawing,
    override val customProperties: Map<String, String> = emptyMap(),
) : OperationObject {
    override val name = drawing.displayName
}

data class OperationNote(
    val id: OperationNoteId,
    val title: String,
    val body: String,
    val referencedObjectIds: List<OperationObjectId> = emptyList(),
)

data class OperationContent(
    val objects: List<OperationObject> = emptyList(),
    val unitTemplates: List<OperationUnitTemplate> = emptyList(),
    val graphicTemplates: List<OperationGraphicTemplate> = emptyList(),
    val notes: List<OperationNote> = emptyList(),
) {
    fun objectById(id: OperationObjectId) = objects.firstOrNull { it.id == id }
    fun unitTemplate(id: OperationUnitTemplateId) = unitTemplates.firstOrNull { it.id == id }
    fun graphicTemplate(id: OperationGraphicTemplateId) = graphicTemplates.firstOrNull { it.id == id }

    companion object {
        val EMPTY = OperationContent()
    }
}

data class OperationValueChange<T>(val before: T? = null, val after: T? = null) {
    init {
        require(before != null || after != null) { "An operation change requires a before or after value" }
    }
}

data class OperationChangeSet(
    val objects: List<OperationValueChange<OperationObject>> = emptyList(),
    val unitTemplates: List<OperationValueChange<OperationUnitTemplate>> = emptyList(),
    val graphicTemplates: List<OperationValueChange<OperationGraphicTemplate>> = emptyList(),
    val notes: List<OperationValueChange<OperationNote>> = emptyList(),
    val noteOrderBefore: List<OperationNoteId>? = null,
    val noteOrderAfter: List<OperationNoteId>? = null,
) {
    val isEmpty get() = objects.isEmpty() && unitTemplates.isEmpty() && graphicTemplates.isEmpty() && notes.isEmpty() && noteOrderAfter == null

    fun applyTo(content: OperationContent) = content.copy(
        objects = content.objects.applyChanges(objects, OperationObject::id),
        unitTemplates = content.unitTemplates.applyChanges(unitTemplates, OperationUnitTemplate::id).sortedBy(OperationUnitTemplate::index),
        graphicTemplates = content.graphicTemplates.applyChanges(graphicTemplates, OperationGraphicTemplate::id).sortedBy(OperationGraphicTemplate::index),
        notes = content.notes.applyChanges(notes, OperationNote::id).let { updated ->
            noteOrderAfter?.let { order ->
                val byId = updated.associateBy(OperationNote::id)
                order.mapNotNull(byId::get) + updated.filter { it.id !in order }
            } ?: updated
        },
    )

    fun reversed() = OperationChangeSet(
        objects = objects.map { OperationValueChange(it.after, it.before) },
        unitTemplates = unitTemplates.map { OperationValueChange(it.after, it.before) },
        graphicTemplates = graphicTemplates.map { OperationValueChange(it.after, it.before) },
        notes = notes.map { OperationValueChange(it.after, it.before) },
        noteOrderBefore = noteOrderAfter,
        noteOrderAfter = noteOrderBefore,
    )
}

data class OperationTimelineEntry(
    val changeSet: OperationChangeSet,
    val kind: MapViewerLedgerEntryKind,
    val description: String,
    val turnName: String? = null,
    val turnThumbnailAssetId: UUID? = null,
    /** Sequential neutral actions retained when one persisted top-level entry is a batch. */
    val componentChangeSets: List<OperationChangeSet> = listOf(changeSet),
)

data class OperationTimeline(
    val entries: List<OperationTimelineEntry> = emptyList(),
    val appliedCount: Int = entries.size,
) {
    init {
        require(appliedCount in 0..entries.size) { "Applied timeline count must be within the entry list" }
    }

    val canUndo get() = appliedCount > 0
    val canRedo get() = appliedCount < entries.size
    val appliedEntries get() = entries.take(appliedCount)

    fun contentAt(count: Int = appliedCount) = entries.take(count.coerceIn(0, entries.size))
        .fold(OperationContent.EMPTY) { content, entry -> entry.changeSet.applyTo(content) }

    fun record(entry: OperationTimelineEntry) = copy(
        entries = entries.take(appliedCount) + entry,
        appliedCount = appliedCount + 1,
    )

    fun recordBatch(
        changes: List<OperationChangeSet>,
        description: String,
        kind: MapViewerLedgerEntryKind = MapViewerLedgerEntryKind.BATCH,
    ): OperationTimeline {
        val before = contentAt()
        val after = changes.fold(before) { content, change -> change.applyTo(content) }
        val combined = after.changesFrom(before)
        return if (combined.isEmpty) this else record(OperationTimelineEntry(combined, kind, description))
    }

    fun undo() = if (canUndo) copy(appliedCount = appliedCount - 1) else this
    fun redo() = if (canRedo) copy(appliedCount = appliedCount + 1) else this
}

data class OperationGeneratedImage(
    val id: UUID,
    val role: String,
    val image: BufferedImage,
)

data class OperationUiState(
    val selectedTool: MapViewerTool = MapViewerTool.SELECT,
    val selectedObjectIds: Set<OperationObjectId> = emptySet(),
    val enabledLayerIds: Set<String> = emptySet(),
    val workspace: OperationWorkspaceState = OperationWorkspaceState.default(),
)

data class OperationDocument(
    val operationId: UUID,
    val operationName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val settings: MapViewerSettings,
    val renderData: MapViewerRenderData,
    val camera: MapViewerSavedCamera? = null,
    val mapAssetSource: AssetSource? = null,
    val timeline: OperationTimeline = OperationTimeline(),
    val generatedImageAssets: List<OperationGeneratedImage> = emptyList(),
    val additionalDataJson: ByteArray? = null,
    val sourcePath: Path? = null,
    val uiState: OperationUiState = OperationUiState(),
    val persisted: Boolean = false,
) {
    val content get() = timeline.contentAt()
}

private fun <T, I> List<T>.applyChanges(changes: List<OperationValueChange<T>>, id: (T) -> I): List<T> =
    changes.fold(this) { values, change ->
        val targetId = change.before?.let(id) ?: change.after?.let(id) ?: return@fold values
        val index = values.indexOfFirst { id(it) == targetId }
        when {
            change.after == null -> values.filterNot { id(it) == targetId }
            index < 0 -> values + change.after
            else -> values.toMutableList().also { it[index] = change.after }.toList()
        }
    }
