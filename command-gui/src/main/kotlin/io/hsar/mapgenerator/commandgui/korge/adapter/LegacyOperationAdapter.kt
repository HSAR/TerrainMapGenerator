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
import java.util.Collections.emptyList
import java.util.Collections.emptyMap

internal fun List<LedgerEntry>.toOperationTimeline(): OperationTimeline {
    var before = OperationContent.EMPTY
    return fold(OperationTimeline()) { timeline, entry ->
        val changeSet = entry.toOperationChangeSet(before)
        val after = changeSet.applyTo(before)
        val timelineEntry = OperationTimelineEntry(
            changeSet = changeSet,
            kind = entry.toOperationKind(),
            description = entry.changeDescription(LedgerActionDirection.Redo) { id ->
                before.objects.firstOrNull { it.id.value == id.value }?.toLegacyObject(before)
            },
            turnName = (entry as? AddTurnLedgerEntry)?.name,
            turnThumbnailAssetId = (entry as? AddTurnLedgerEntry)?.thumbnailAssetId,
            componentChangeSets = entry.toOperationComponentChangeSets(before),
        )
        before = after
        timeline.record(timelineEntry)
    }
}

private fun LedgerEntry.toOperationComponentChangeSets(content: OperationContent): List<OperationChangeSet> =
    if (this !is BatchLedgerEntry) {
        listOf(toOperationChangeSet(content))
    } else {
        var current = content
        entries.map { nested ->
            nested.toOperationChangeSet(current).also { change -> current = change.applyTo(current) }
        }
    }

internal fun MapActionHistory.toOperationTimeline(): OperationTimeline =
    fullTimelineEntries.toOperationTimeline().copy(appliedCount = appliedEntries.size)

private fun LedgerEntry.toOperationChangeSet(content: OperationContent): OperationChangeSet = when (this) {
    is CreateObjectAction -> OperationChangeSet(objects = listOf(OperationValueChange(after = snapshot.toOperationObject())))
    is DeleteObjectAction -> OperationChangeSet(objects = listOf(OperationValueChange(before = snapshot.toOperationObject())))
    is EditObjectAction -> content.objectById(objectId.toOperation())?.let { before ->
        OperationChangeSet(objects = listOf(OperationValueChange(before, applyOperationObjectPatch(before, forward))))
    } ?: OperationChangeSet()

    is CreateUnitTemplateAction -> OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = snapshot.toOperationTemplate())))
    is DeleteUnitTemplateAction -> OperationChangeSet(unitTemplates = listOf(OperationValueChange(before = snapshot.toOperationTemplate())))
    is EditUnitTemplateAction -> {
        val before = beforeSnapshot?.toOperationTemplate() ?: content.unitTemplate(OperationUnitTemplateId(id.value))
        val after = afterSnapshot?.toOperationTemplate() ?: before?.let { applyOperationUnitTemplatePatch(it, forward) { null } }
        if (before == null || after == null) OperationChangeSet() else OperationChangeSet(unitTemplates = listOf(OperationValueChange(before, after)))
    }

    is CreateGraphicTemplateAction -> OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = snapshot.toOperationTemplate())))
    is DeleteGraphicTemplateAction -> OperationChangeSet(graphicTemplates = listOf(OperationValueChange(before = snapshot.toOperationTemplate())))
    is EditGraphicTemplateAction -> {
        val before = beforeSnapshot?.toOperationTemplate() ?: content.graphicTemplate(OperationGraphicTemplateId(id.value))
        val after = afterSnapshot?.toOperationTemplate() ?: before?.let { applyOperationGraphicTemplatePatch(it, forward) { null } }
        if (before == null || after == null) OperationChangeSet() else OperationChangeSet(graphicTemplates = listOf(OperationValueChange(before, after)))
    }

    is CreateNoteAction -> OperationChangeSet(notes = listOf(OperationValueChange(after = snapshot.toOperationNote())))
    is DeleteNoteAction -> OperationChangeSet(notes = listOf(OperationValueChange(before = snapshot.toOperationNote())))
    is EditNoteAction -> content.notes.firstOrNull { it.id.value == id.value }?.let { before ->
        OperationChangeSet(notes = listOf(OperationValueChange(before, applyOperationNotePatch(before, forward))))
    } ?: OperationChangeSet()

    is ReorderNotesAction -> OperationChangeSet(noteOrderBefore = backward.map { OperationNoteId(it.value) }, noteOrderAfter = forward.map { OperationNoteId(it.value) })
    is AddTurnLedgerEntry -> OperationChangeSet()
    is BatchLedgerEntry -> toOperationComponentChangeSets(content).fold(content) { current, change -> change.applyTo(current) }.changesFrom(content)
    else -> OperationChangeSet()
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
    val unitTemplateSnapshots = unitTemplates + objects.filterIsInstance<UnitSnapshot>()
        .filter { unit -> unitTemplates.none { it.id == unit.unitTemplateId } }
        .distinctBy(UnitSnapshot::unitTemplateId)
        .mapIndexed { index, unit -> UnitTemplateSnapshot(unit.unitTemplateId, unit.template, unitTemplates.size + index) }
    val graphicTemplateSnapshots = graphicTemplates + objects.filterIsInstance<GraphicSnapshot>()
        .filter { graphic -> graphicTemplates.none { it.id == graphic.graphicTemplateId } }
        .distinctBy(GraphicSnapshot::graphicTemplateId)
        .mapIndexed { index, graphic -> GraphicTemplateSnapshot(graphic.graphicTemplateId, graphic.template, graphicTemplates.size + index) }
    val graphicSizes = operationObjects.filterIsInstance<OperationGraphic>()
        .groupBy(OperationGraphic::graphicTemplateId)
        .mapValues { (_, instances) -> instances.first().let { it.normalizedWidth to it.normalizedHeight } }
    return OperationContent(
        objects = operationObjects,
        unitTemplates = unitTemplateSnapshots.map(UnitTemplateSnapshot::toOperationTemplate),
        graphicTemplates = graphicTemplateSnapshots.map { snapshot ->
            snapshot.toOperationTemplate().let { template ->
                graphicSizes[template.id]?.let { size ->
                    template.copy(normalizedWidth = size.first, normalizedHeight = size.second)
                } ?: template
            }
        },
        notes = notes.map(MapNoteSnapshot::toOperationNote),
    )
}

internal fun List<MapPlacedObjectSnapshot>.toOperationContent() =
    ReplayedLedger(this, emptyList(), emptyList(), emptyList()).toOperationContent()

private fun OperationTimelineEntry.toLegacyEntry(before: OperationContent, after: OperationContent): LedgerEntry? {
    if (turnName != null) return AddTurnLedgerEntry(turnName, turnThumbnailAssetId)
    if (kind == MapViewerLedgerEntryKind.BATCH) {
        var current = before
        val actions = componentChangeSets.flatMap { component ->
            val componentAfter = component.applyTo(current)
            component.toLegacyActions(current, componentAfter).also { current = componentAfter }
        }
        return BatchLedgerEntry(actions)
    }
    val actions = changeSet.toLegacyActions(before, after)
    return when (actions.size) {
        0 -> null
        1 -> actions.single()
        else -> BatchLedgerEntry(actions)
    }
}

private fun OperationChangeSet.toLegacyActions(before: OperationContent, after: OperationContent): List<LedgerEntry> = buildList {
    unitTemplates.forEach { change ->
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
    graphicTemplates.forEach { change ->
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
    objects.forEach { change ->
            when {
                change.before == null -> add(CreateObjectAction(change.after!!.toLegacyObject(after)))
                change.after == null -> add(DeleteObjectAction(change.before.toLegacyObject(before)))
                else -> add(EditObjectAction.fromSnapshots(change.before.toLegacyObject(before), change.after.toLegacyObject(after)))
            }
        }
    notes.forEach { change ->
            when {
                change.before == null -> add(CreateNoteAction(change.after!!.toLegacyNote()))
                change.after == null -> add(DeleteNoteAction(change.before.toLegacyNote()))
                else -> add(EditNoteAction.fromSnapshots(change.before.toLegacyNote(), change.after.toLegacyNote()))
            }
        }
    if (noteOrderAfter != null && noteOrderBefore != null) {
            add(
                ReorderNotesAction(
                    forward = noteOrderAfter.map(OperationNoteId::toLegacy),
                    backward = noteOrderBefore.map(OperationNoteId::toLegacy),
                )
            )
        }
}

internal fun MapPlacedObjectSnapshot.toOperationObject(): OperationObject = when (this) {
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
        affiliation = template.affiliation?.let { OperationUnitAffiliation.valueOf(it.name.uppercase()) },
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

internal fun UnitTemplateSnapshot.toOperationTemplate() = OperationUnitTemplate(
    id = OperationUnitTemplateId(id.value),
    name = template.name,
    image = template.bitmap.toBufferedImage(),
    index = index,
    assetSource = template.assetSource,
    affiliation = template.affiliation?.let { OperationUnitAffiliation.valueOf(it.name.uppercase()) },
    customProperties = template.customProperties,
)

internal fun GraphicTemplateSnapshot.toOperationTemplate() = OperationGraphicTemplate(
    id = OperationGraphicTemplateId(id.value),
    name = template.name,
    image = template.bitmap.toBufferedImage(),
    index = index,
    assetSource = template.assetSource,
    customProperties = template.customProperties,
)

internal fun MapNoteSnapshot.toOperationNote() = OperationNote(
    id = OperationNoteId(id.value),
    title = title,
    body = body,
    referencedObjectIds = referencedObjectIds.map { OperationObjectId(it.value) },
)

internal fun MapDrawing.toOperationDrawing(): OperationDrawing = when (this) {
    is FreeformDrawing -> OperationFreeformDrawing(points.map { OperationDrawingPoint(it.x, it.y) }, color.toOperationArgb())
    is LineDrawing -> OperationLineDrawing(start.toOperationPoint(), end.toOperationPoint(), color.toOperationArgb())
    is RectangleDrawing -> OperationRectangleDrawing(start.toOperationPoint(), end.toOperationPoint(), color.toOperationArgb(), rotationDegrees)
    is OvalDrawing -> OperationOvalDrawing(center.toOperationPoint(), xRadius, yRadius, color.toOperationArgb(), rotationDegrees)
}

internal fun OperationObject.toLegacyObject(content: OperationContent): MapPlacedObjectSnapshot = when (this) {
    is OperationUnit -> UnitSnapshot(
        id = id.toLegacy(),
        unitTemplateId = unitTemplateId.toLegacy(),
        template = content.unitTemplate(unitTemplateId)?.toLegacyTemplate()?.template
            ?.let { template -> affiliation?.let { template.copy(affiliation = it.toLegacyAffiliation()) } ?: template }
            ?: error("Missing unit template ${unitTemplateId.value}"),
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

internal fun OperationUnitTemplate.toLegacyTemplate() = UnitTemplateSnapshot(
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

internal fun OperationGraphicTemplate.toLegacyTemplate() = GraphicTemplateSnapshot(
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

internal fun OperationObjectId.toLegacy() = MapPlacedObjectId(value)
internal fun MapPlacedObjectId.toOperation() = OperationObjectId(value)
private fun OperationUnitTemplateId.toLegacy() = UnitTemplateId(value)
private fun OperationGraphicTemplateId.toLegacy() = GraphicTemplateId(value)
private fun OperationNoteId.toLegacy() = MapNoteId(value)
private fun MapDrawingPoint.toOperationPoint() = OperationDrawingPoint(x, y)
private fun OperationDrawingPoint.toLegacy() = MapDrawingPoint(x, y)
private fun Color.toOperationArgb() = OperationArgb(rgb)
private fun OperationArgb.toAwt() = Color(value, true)
private fun OperationUnitAffiliation.toLegacyAffiliation() = when (this) {
    OperationUnitAffiliation.FRIEND -> UnitAffiliation.Friend
    OperationUnitAffiliation.HOSTILE -> UnitAffiliation.Hostile
    OperationUnitAffiliation.NEUTRAL -> UnitAffiliation.Neutral
    OperationUnitAffiliation.UNKNOWN -> UnitAffiliation.Unknown
}

private object LegacyBitmapConversionCache {
    private val bufferedImages = WeakHashMap<Bitmap32, BufferedImage>()
    private val bitmaps = WeakHashMap<BufferedImage, Bitmap32>()

    @Synchronized
    fun bufferedImage(source: Bitmap32) = bufferedImages.getOrPut(source) {
        BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until source.height) for (x in 0 until source.width) source.getRgba(x, y).let { colour ->
                image.setRGB(x, y, (colour.a shl 24) or (colour.r shl 16) or (colour.g shl 8) or colour.b)
            }
            bitmaps[image] = source
        }
    }

    @Synchronized
    fun bitmap(source: BufferedImage) = bitmaps.getOrPut(source) {
        Bitmap32(source.width, source.height) { x, y ->
            source.getRGB(x, y).let { argb -> RGBA(argb ushr 16 and 0xff, argb ushr 8 and 0xff, argb and 0xff, argb ushr 24 and 0xff) }
        }.also { bufferedImages[it] = source }
    }
}

private fun Bitmap32.toBufferedImage() = LegacyBitmapConversionCache.bufferedImage(this)
private fun BufferedImage.toBitmap32() = LegacyBitmapConversionCache.bitmap(this)
internal fun BufferedImage.toKorgeBitmap() = toBitmap32()
