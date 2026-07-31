package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditGraphicTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerActionDirection
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerReplay
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.ReorderNotesAction
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.changeDescription
import korlibs.image.bitmap.Bitmap32
import java.awt.image.BufferedImage
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptyMap

enum class MapViewerLedgerEntryKind {
    OBJECT,
    UNIT_TEMPLATE,
    GRAPHIC_TEMPLATE,
    NOTE,
    TURN,
    BATCH,
}

data class MapViewerLedgerEntrySummary(
    val index: Int,
    val kind: MapViewerLedgerEntryKind,
    val description: String,
    val turnName: String? = null,
)

enum class MapViewerReplayObjectKind {
    UNIT,
    DRAWING,
    GRAPHIC,
}

data class MapViewerReplayObject(
    val id: UUID,
    val kind: MapViewerReplayObjectKind,
    val name: String,
    val templateName: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    val destroyed: Boolean? = null,
    val customProperties: Map<String, String> = emptyMap(),
    val icon: BufferedImage? = null,
    val normalizedWidth: Double? = null,
    val normalizedHeight: Double? = null,
) {
    val displayName: String?
        get() = customProperties[DISPLAY_NAME_PROPERTY]?.trim()?.takeIf(String::isNotEmpty)
}

data class MapViewerReplayTemplate(
    val id: UUID,
    val name: String,
)

data class MapViewerReplayNote(
    val id: UUID,
    val title: String,
    val body: String,
    val referencedObjectIds: List<UUID>,
)

data class MapViewerLedgerReplay(
    val entries: List<MapViewerLedgerEntrySummary>,
    val objects: List<MapViewerReplayObject>,
    val unitTemplates: List<MapViewerReplayTemplate>,
    val graphicTemplates: List<MapViewerReplayTemplate>,
    val notes: List<MapViewerReplayNote>,
) {
    companion object {
        val EMPTY = MapViewerLedgerReplay(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

internal fun replayForViewer(entries: List<LedgerEntry>): MapViewerLedgerReplay {
    val state = LedgerReplay.replay(entries)
    val objectsById = state.objects.associateBy { it.id }
    return MapViewerLedgerReplay(
        entries = entries.mapIndexed { index, entry ->
            MapViewerLedgerEntrySummary(
                index = index,
                kind = entry.toViewerKind(),
                description = entry.changeDescription(LedgerActionDirection.Redo, objectsById::get),
                turnName = (entry as? AddTurnLedgerEntry)?.name,
            )
        },
        objects = state.objects.map(MapPlacedObjectSnapshot::toViewerObject),
        unitTemplates = state.unitTemplates.map {
            MapViewerReplayTemplate(id = it.id.value, name = it.template.name)
        },
        graphicTemplates = state.graphicTemplates.map {
            MapViewerReplayTemplate(id = it.id.value, name = it.template.name)
        },
        notes = state.notes.map {
            MapViewerReplayNote(
                id = it.id.value,
                title = it.title,
                body = it.body,
                referencedObjectIds = it.referencedObjectIds.map { id -> id.value },
            )
        },
    )
}

private fun LedgerEntry.toViewerKind() =
    when (this) {
        is CreateObjectAction, is DeleteObjectAction, is EditObjectAction -> MapViewerLedgerEntryKind.OBJECT
        is CreateUnitTemplateAction, is DeleteUnitTemplateAction, is EditUnitTemplateAction -> MapViewerLedgerEntryKind.UNIT_TEMPLATE
        is CreateGraphicTemplateAction, is DeleteGraphicTemplateAction, is EditGraphicTemplateAction -> MapViewerLedgerEntryKind.GRAPHIC_TEMPLATE
        is CreateNoteAction, is DeleteNoteAction, is EditNoteAction, is ReorderNotesAction -> MapViewerLedgerEntryKind.NOTE
        is AddTurnLedgerEntry -> MapViewerLedgerEntryKind.TURN
        is BatchLedgerEntry -> MapViewerLedgerEntryKind.BATCH
        else -> MapViewerLedgerEntryKind.BATCH
    }

private fun MapPlacedObjectSnapshot.toViewerObject() =
    when (this) {
        is UnitSnapshot -> MapViewerReplayObject(
            id = id.value,
            kind = MapViewerReplayObjectKind.UNIT,
            name = instanceName.ifBlank { template.name },
            templateName = template.name,
            x = mapCoordinate.x,
            y = mapCoordinate.y,
            destroyed = destroyed,
            customProperties = customProperties,
            icon = template.bitmap.toBufferedImage(),
        )

        is DrawingSnapshot -> MapViewerReplayObject(
            id = id.value,
            kind = MapViewerReplayObjectKind.DRAWING,
            name = drawing.name,
            customProperties = customProperties,
        )

        is GraphicSnapshot -> MapViewerReplayObject(
            id = id.value,
            kind = MapViewerReplayObjectKind.GRAPHIC,
            name = instanceName.ifBlank { template.name },
            templateName = template.name,
            x = mapCoordinate.x,
            y = mapCoordinate.y,
            customProperties = customProperties,
            icon = template.bitmap.toBufferedImage(),
            normalizedWidth = normalizedWidth,
            normalizedHeight = normalizedHeight,
        )
    }

private fun Bitmap32.toBufferedImage() =
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
        for (y in 0 until height) {
            for (x in 0 until width) {
                getRgba(x, y).let { rgba ->
                    image.setRGB(x, y, (rgba.a shl 24) or (rgba.r shl 16) or (rgba.g shl 8) or rgba.b)
                }
            }
        }
    }

private const val DISPLAY_NAME_PROPERTY = "displayName"
