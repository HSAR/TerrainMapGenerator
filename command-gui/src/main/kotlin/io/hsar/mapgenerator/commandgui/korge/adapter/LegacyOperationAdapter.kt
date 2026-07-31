package io.hsar.mapgenerator.commandgui.korge.adapter

import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.*
import io.hsar.mapgenerator.commandgui.operation.*
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.io.util.buildList
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*
import java.util.Collections.emptyMap

internal fun List<LedgerEntry>.toOperationTimeline(): OperationTimeline {
    var before = OperationContent.EMPTY
    return foldIndexed(OperationTimeline()) { index, timeline, entry ->
        val after = LedgerReplay.replay(take(index + 1)).toOperationContent()
        val timelineEntry = OperationTimelineEntry(
            changeSet = after.changesFrom(before),
            kind = entry.toOperationKind(),
            description = entry.changeDescription(LedgerActionDirection.Redo) { id ->
                LedgerReplay.replay(take(index)).objects.firstOrNull { it.id == id }
            },
            turnName = (entry as? AddTurnLedgerEntry)?.name,
            turnThumbnailAssetId = (entry as? AddTurnLedgerEntry)?.thumbnailAssetId,
        )
        before = after
        timeline.record(timelineEntry)
    }
}

internal fun OperationTimeline.toLegacyEntries(): List<LedgerEntry> {
    var before = OperationContent.EMPTY
    return appliedEntries.mapNotNull { entry ->
        val after = entry.changeSet.applyTo(before)
        entry.toLegacyEntry(before, after).also { before = after }
    }
}

internal fun ReplayedLedger.toOperationContent(): OperationContent {
    val operationObjects = objects.map(MapPlacedObjectSnapshot::toOperationObject)
    val graphicSizes = operationObjects.filterIsInstance<OperationGraphic>()
        .groupBy(OperationGraphic::graphicTemplateId)
        .mapValues { (_, instances) -> instances.first().let { it.normalizedWidth to it.normalizedHeight } }
    return OperationContent(
        objects = operationObjects,
        unitTemplates = unitTemplates.map(UnitTemplateSnapshot::toOperationTemplate),
        graphicTemplates = graphicTemplates.map { snapshot ->
            snapshot.toOperationTemplate().let { template ->
                graphicSizes[template.id]?.let { size ->
                    template.copy(normalizedWidth = size.first, normalizedHeight = size.second)
                } ?: template
            }
        },
        notes = notes.map(MapNoteSnapshot::toOperationNote),
    )
}

private fun OperationTimelineEntry.toLegacyEntry(before: OperationContent, after: OperationContent): LedgerEntry? {
    if (turnName != null) return AddTurnLedgerEntry(turnName, turnThumbnailAssetId)
    val actions = buildList {
        changeSet.unitTemplates.forEach { change ->
            when {
                change.before == null -> add(CreateUnitTemplateAction(change.after!!.toLegacyTemplate()))
                change.after == null -> add(DeleteUnitTemplateAction(change.before.toLegacyTemplate()))
                else -> add(
                    EditUnitTemplateAction(
                        id = change.before.id.toLegacy(),
                        forward = emptyMap(),
                        backward = emptyMap(),
                        beforeSnapshot = change.before.toLegacyTemplate(),
                        afterSnapshot = change.after.toLegacyTemplate(),
                    )
                )
            }
        }
        changeSet.graphicTemplates.forEach { change ->
            when {
                change.before == null -> add(CreateGraphicTemplateAction(change.after!!.toLegacyTemplate()))
                change.after == null -> add(DeleteGraphicTemplateAction(change.before.toLegacyTemplate()))
                else -> add(
                    EditGraphicTemplateAction(
                        id = change.before.id.toLegacy(),
                        forward = emptyMap(),
                        backward = emptyMap(),
                        beforeSnapshot = change.before.toLegacyTemplate(),
                        afterSnapshot = change.after.toLegacyTemplate(),
                    )
                )
            }
        }
        changeSet.objects.forEach { change ->
            when {
                change.before == null -> add(CreateObjectAction(change.after!!.toLegacyObject(after)))
                change.after == null -> add(DeleteObjectAction(change.before.toLegacyObject(before)))
                else -> add(EditObjectAction.fromSnapshots(change.before.toLegacyObject(before), change.after.toLegacyObject(after)))
            }
        }
        changeSet.notes.forEach { change ->
            when {
                change.before == null -> add(CreateNoteAction(change.after!!.toLegacyNote()))
                change.after == null -> add(DeleteNoteAction(change.before.toLegacyNote()))
                else -> add(EditNoteAction.fromSnapshots(change.before.toLegacyNote(), change.after.toLegacyNote()))
            }
        }
        if (changeSet.noteOrderAfter != null && changeSet.noteOrderBefore != null) {
            add(
                ReorderNotesAction(
                    forward = changeSet.noteOrderAfter.map(OperationNoteId::toLegacy),
                    backward = changeSet.noteOrderBefore.map(OperationNoteId::toLegacy),
                )
            )
        }
    }
    return when (actions.size) {
        0 -> null
        1 -> actions.single()
        else -> BatchLedgerEntry(actions)
    }
}

private fun MapPlacedObjectSnapshot.toOperationObject(): OperationObject = when (this) {
    is UnitSnapshot -> OperationUnit(
        id = OperationObjectId(id.value),
        unitTemplateId = OperationUnitTemplateId(unitTemplateId.value),
        instanceName = instanceName,
        destroyed = destroyed,
        facingDegrees = facingDegrees,
        coordinate = mapCoordinate,
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is GraphicSnapshot -> OperationGraphic(
        id = OperationObjectId(id.value),
        graphicTemplateId = OperationGraphicTemplateId(graphicTemplateId.value),
        instanceName = instanceName,
        coordinate = mapCoordinate,
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is DrawingSnapshot -> OperationDrawingObject(
        id = OperationObjectId(id.value),
        drawing = drawing.toOperationDrawing(),
        customProperties = customProperties,
    )
}

private fun UnitTemplateSnapshot.toOperationTemplate() = OperationUnitTemplate(
    id = OperationUnitTemplateId(id.value),
    name = template.name,
    image = template.bitmap.toBufferedImage(),
    index = index,
    assetSource = template.assetSource,
    affiliation = template.affiliation?.let { OperationUnitAffiliation.valueOf(it.name.uppercase()) },
    customProperties = template.customProperties,
)

private fun GraphicTemplateSnapshot.toOperationTemplate() = OperationGraphicTemplate(
    id = OperationGraphicTemplateId(id.value),
    name = template.name,
    image = template.bitmap.toBufferedImage(),
    index = index,
    assetSource = template.assetSource,
    customProperties = template.customProperties,
)

private fun MapNoteSnapshot.toOperationNote() = OperationNote(
    id = OperationNoteId(id.value),
    title = title,
    body = body,
    referencedObjectIds = referencedObjectIds.map { OperationObjectId(it.value) },
)

private fun MapDrawing.toOperationDrawing(): OperationDrawing = when (this) {
    is FreeformDrawing -> OperationFreeformDrawing(points.map { OperationDrawingPoint(it.x, it.y) }, color.toOperationArgb())
    is LineDrawing -> OperationLineDrawing(start.toOperationPoint(), end.toOperationPoint(), color.toOperationArgb())
    is RectangleDrawing -> OperationRectangleDrawing(start.toOperationPoint(), end.toOperationPoint(), color.toOperationArgb(), rotationDegrees)
    is OvalDrawing -> OperationOvalDrawing(center.toOperationPoint(), xRadius, yRadius, color.toOperationArgb(), rotationDegrees)
}

private fun OperationObject.toLegacyObject(content: OperationContent): MapPlacedObjectSnapshot = when (this) {
    is OperationUnit -> UnitSnapshot(
        id = id.toLegacy(),
        unitTemplateId = unitTemplateId.toLegacy(),
        template = content.unitTemplate(unitTemplateId)?.toLegacyTemplate()?.template ?: error("Missing unit template ${unitTemplateId.value}"),
        instanceName = instanceName,
        destroyed = destroyed,
        facingDegrees = facingDegrees,
        mapCoordinate = coordinate,
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is OperationGraphic -> GraphicSnapshot(
        id = id.toLegacy(),
        graphicTemplateId = graphicTemplateId.toLegacy(),
        template = content.graphicTemplate(graphicTemplateId)?.toLegacyTemplate()?.template ?: error("Missing graphic template ${graphicTemplateId.value}"),
        instanceName = instanceName,
        mapCoordinate = coordinate,
        normalizedWidth = normalizedWidth,
        normalizedHeight = normalizedHeight,
        customProperties = customProperties,
    )

    is OperationDrawingObject -> DrawingSnapshot(id.toLegacy(), drawing.toLegacyDrawing(), customProperties)
}

private fun OperationUnitTemplate.toLegacyTemplate() = UnitTemplateSnapshot(
    id = id.toLegacy(),
    template = MapViewerUnitTemplate(
        id = id.toLegacy(),
        name = name,
        bitmap = image.toBitmap32(),
        assetSource = assetSource,
        affiliation = affiliation?.let {
            when (it) {
                OperationUnitAffiliation.FRIEND -> UnitAffiliation.Friend
                OperationUnitAffiliation.HOSTILE -> UnitAffiliation.Hostile
                OperationUnitAffiliation.NEUTRAL -> UnitAffiliation.Neutral
                OperationUnitAffiliation.UNKNOWN -> UnitAffiliation.Unknown
            }
        },
        customProperties = customProperties,
    ),
    index = index,
)

private fun OperationGraphicTemplate.toLegacyTemplate() = GraphicTemplateSnapshot(
    id = id.toLegacy(),
    template = MapViewerGraphicTemplate(
        id = id.toLegacy(),
        name = name,
        bitmap = image.toBitmap32(),
        assetSource = assetSource,
        customProperties = customProperties,
    ),
    index = index,
)

private fun OperationNote.toLegacyNote() = MapNoteSnapshot(
    id = id.toLegacy(),
    title = title,
    body = body,
    referencedObjectIds = referencedObjectIds.map(OperationObjectId::toLegacy),
)

private fun OperationDrawing.toLegacyDrawing(): MapDrawing = when (this) {
    is OperationFreeformDrawing -> FreeformDrawing(points.map(OperationDrawingPoint::toLegacy), colour.toAwt())
    is OperationLineDrawing -> LineDrawing(start.toLegacy(), end.toLegacy(), colour.toAwt())
    is OperationRectangleDrawing -> RectangleDrawing(start.toLegacy(), end.toLegacy(), colour.toAwt(), rotationDegrees)
    is OperationOvalDrawing -> OvalDrawing(centre.toLegacy(), xRadius, yRadius, colour.toAwt(), rotationDegrees)
}

private fun LedgerEntry.toOperationKind() = when (this) {
    is CreateObjectAction, is DeleteObjectAction, is EditObjectAction -> MapViewerLedgerEntryKind.OBJECT
    is CreateUnitTemplateAction, is DeleteUnitTemplateAction, is EditUnitTemplateAction -> MapViewerLedgerEntryKind.UNIT_TEMPLATE
    is CreateGraphicTemplateAction, is DeleteGraphicTemplateAction, is EditGraphicTemplateAction -> MapViewerLedgerEntryKind.GRAPHIC_TEMPLATE
    is CreateNoteAction, is DeleteNoteAction, is EditNoteAction, is ReorderNotesAction -> MapViewerLedgerEntryKind.NOTE
    is AddTurnLedgerEntry -> MapViewerLedgerEntryKind.TURN
    else -> MapViewerLedgerEntryKind.BATCH
}

private fun OperationObjectId.toLegacy() = MapPlacedObjectId(value)
private fun OperationUnitTemplateId.toLegacy() = UnitTemplateId(value)
private fun OperationGraphicTemplateId.toLegacy() = GraphicTemplateId(value)
private fun OperationNoteId.toLegacy() = MapNoteId(value)
private fun MapDrawingPoint.toOperationPoint() = OperationDrawingPoint(x, y)
private fun OperationDrawingPoint.toLegacy() = MapDrawingPoint(x, y)
private fun Color.toOperationArgb() = OperationArgb(rgb)
private fun OperationArgb.toAwt() = Color(value, true)

private object LegacyBitmapConversionCache {
    private val bufferedImages = WeakHashMap<Bitmap32, BufferedImage>()
    private val bitmaps = WeakHashMap<BufferedImage, Bitmap32>()

    @Synchronized
    fun bufferedImage(source: Bitmap32) = bufferedImages.getOrPut(source) {
        BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until source.height) for (x in 0 until source.width) source.getRgba(x, y).let { colour ->
                image.setRGB(x, y, (colour.a shl 24) or (colour.r shl 16) or (colour.g shl 8) or colour.b)
            }
        }
    }

    @Synchronized
    fun bitmap(source: BufferedImage) = bitmaps.getOrPut(source) {
        Bitmap32(source.width, source.height) { x, y ->
            source.getRGB(x, y).let { argb -> RGBA(argb ushr 16 and 0xff, argb ushr 8 and 0xff, argb and 0xff, argb ushr 24 and 0xff) }
        }
    }
}

private fun Bitmap32.toBufferedImage() = LegacyBitmapConversionCache.bufferedImage(this)
private fun BufferedImage.toBitmap32() = LegacyBitmapConversionCache.bitmap(this)
