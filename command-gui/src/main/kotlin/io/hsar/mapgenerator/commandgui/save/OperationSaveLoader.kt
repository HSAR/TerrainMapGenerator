package io.hsar.mapgenerator.commandgui.save

import com.sun.org.apache.bcel.internal.util.Args.require
import io.hsar.mapgenerator.commandgui.*
import io.hsar.mapgenerator.commandgui.operation.*
import io.hsar.mapgenerator.grid.CoordAxis
import io.hsar.mapgenerator.grid.GridLayerBuilder
import korlibs.io.lang.use
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.Collections.emptyList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal class OperationSaveLoader {
    fun load(path: Path): LoadedOperation = ZipFile(path.toFile()).use { zip ->
        val game = zip.readJson(GAME_JSON, GameSaveDto::class.java)
        val ledger = zip.readJson(LEDGER_JSON, LedgerFileDto::class.java)
        require(game.formatVersion in SUPPORTED_FORMAT_VERSIONS) { "Unsupported save format version ${game.formatVersion}" }
        val mapAsset = zip.loadAsset(game.map.assetId)
        val mapImage = MapViewerImageLoader.load(mapAsset.bytes, mapAsset.entry.name, trimTransparentBounds = false)
            ?: error("Unable to decode map asset ${mapAsset.entry.name}")
        val operationId = UUID.fromString(game.id)
        val seed = operationId.mostSignificantBits xor operationId.leastSignificantBits
        val gridConfiguration = game.map.mapId?.takeIf(String::isNotBlank)?.let { mapId ->
            val x = game.map.gridStartX?.let { game.map.referenceSystem.parseCoord(it, CoordAxis.X, game.map.referenceSystem.gridScale) }
            val y = game.map.gridStartY?.let { game.map.referenceSystem.parseCoord(it, CoordAxis.Y, game.map.referenceSystem.gridScale) }
            if (x != null && y != null) SaveGridConfiguration(mapId, game.map.referenceSystem, x, y) else null
        } ?: defaultSaveGridConfiguration(mapImage, game.map.unitsPerPixel, seed, game.map.referenceSystem)
        val grid = GridLayerBuilder.build(
            width = mapImage.width,
            height = mapImage.height,
            gridScale = gridConfiguration.referenceSystem.gridScale,
            unitsPerPixel = game.map.unitsPerPixel,
            random = Random(seed),
            mapId = gridConfiguration.mapId,
            referenceSystem = gridConfiguration.referenceSystem,
            verticalLineNumberStart = gridConfiguration.startX,
            horizontalLineNumberStart = gridConfiguration.startY,
            colour = GridColourSelector.resolve(game.map.gridColour, mapImage),
        )
        val settings = MapViewerSettings(
            operationName = game.name,
            availableLayers = listOf(StaticImageLayer(game.map.assetId, "Map"), MapLayer.GRID),
            enabledLayerIds = setOf(game.map.assetId, MapLayer.GRID.id),
            staticLayerImages = mapOf(game.map.assetId to mapImage),
            width = mapImage.width,
            height = mapImage.height,
            unitsPerPixel = game.map.unitsPerPixel,
            mapId = grid.mapId,
            referenceSystem = grid.referenceSystem,
            gridStartX = grid.verticalLineNumberStart,
            gridStartY = grid.horizontalLineNumberStart,
            gridColour = grid.colour,
            seed = seed,
        )
        val additionalData = zip.getEntry(ADDITIONAL_DATA_JSON)?.let { zip.getInputStream(it).use { input -> input.readBytes() } }
        LoadedOperation(
            operationId = operationId,
            operationName = game.name,
            createdAt = Instant.parse(game.createdAt),
            updatedAt = Instant.parse(game.updatedAt),
            settings = settings,
            camera = game.camera?.let { MapViewerSavedCamera(it.x, it.y, it.zoom) },
            renderData = MapViewerRenderData(mapImage, grid),
            mapAssetSource = AssetSource.fromEmbeddedBytes(mapAsset.entry.name.substringAfterLast('/'), mapAsset.bytes),
            timeline = OperationLedgerLoader(zip).load(ledger.entries),
            generatedImageAssets = loadTurnThumbnails(zip, ledger.entries),
            additionalDataJson = additionalData,
        )
    }

    private fun loadTurnThumbnails(zip: ZipFile, entries: List<LedgerEntrySaveDto>) = entries.thumbnailIds().mapNotNull { id ->
        runCatching {
            val asset = zip.loadAsset(id.toString())
            OperationGeneratedImage(id, TURN_THUMBNAIL_ROLE, MapViewerImageLoader.load(asset.bytes, asset.entry.name) ?: error("Unable to decode ${asset.entry.name}"))
        }.onFailure { org.apache.logging.log4j.LogManager.getLogger(OperationSaveLoader::class.java).warn("Unable to load turn thumbnail asset {}", id, it) }.getOrNull()
    }

    private companion object {
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
        const val ADDITIONAL_DATA_JSON = "additionalData.json"
        const val TURN_THUMBNAIL_ROLE = "turn-thumbnail"
        val SUPPORTED_FORMAT_VERSIONS = setOf(5, 6)
    }
}

private class OperationLedgerLoader(private val zip: ZipFile) {
    private val assets = mutableMapOf<UUID, Pair<java.awt.image.BufferedImage, AssetSource>>()

    fun load(entries: List<LedgerEntrySaveDto>): OperationTimeline {
        var timeline = OperationTimeline()
        var content = OperationContent.EMPTY
        entries.forEach { dto ->
            val decoded = decode(dto, content)
            val directlyApplied = decoded.changeSet.applyTo(content)
            val after = directlyApplied.withGraphicSizesFromInstances()
            val changes = if (after == directlyApplied) decoded.changeSet else after.changesFrom(content)
            val (derivedKind, description) = operationEntryDescription(changes, content, decoded.turnName, decoded.componentChangeSets)
            timeline = timeline.record(
                OperationTimelineEntry(
                    changeSet = changes,
                    kind = if (dto is BatchLedgerEntrySaveDto) MapViewerLedgerEntryKind.BATCH else derivedKind,
                    description = description,
                    turnName = decoded.turnName,
                    turnThumbnailAssetId = decoded.turnThumbnailAssetId,
                    componentChangeSets = decoded.componentChangeSets,
                ),
            )
            content = after
        }
        return timeline
    }

    private fun decode(dto: LedgerEntrySaveDto, content: OperationContent): DecodedLedgerEntry = when (dto) {
        is CreateObjectEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(objects = listOf(OperationValueChange(after = dto.objectData.toOperation(dto.objectId)))))
        is DeleteObjectEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(objects = listOf(OperationValueChange(before = dto.objectData.toOperation(dto.objectId)))))
        is EditObjectEntrySaveDto -> content.objectById(OperationObjectId(dto.objectId))?.let { before ->
            DecodedLedgerEntry(OperationChangeSet(objects = listOf(OperationValueChange(before, applyOperationObjectPatch(before, dto.forward)))))
        } ?: error("Missing object ${dto.objectId} for edit")

        is CreateUnitTemplateEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(unitTemplates = listOf(OperationValueChange(after = dto.unitTemplate.toOperation()))))
        is DeleteUnitTemplateEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(unitTemplates = listOf(OperationValueChange(before = dto.unitTemplate.toOperation()))))
        is EditUnitTemplateEntrySaveDto -> content.unitTemplate(OperationUnitTemplateId(dto.id))?.let { before ->
            DecodedLedgerEntry(OperationChangeSet(unitTemplates = listOf(OperationValueChange(before, applyOperationUnitTemplatePatch(before, dto.forward, ::asset)))))
        } ?: error("Missing unit template ${dto.id} for edit")

        is CreateGraphicTemplateEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(graphicTemplates = listOf(OperationValueChange(after = dto.graphicTemplate.toOperation()))))
        is DeleteGraphicTemplateEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(graphicTemplates = listOf(OperationValueChange(before = dto.graphicTemplate.toOperation()))))
        is EditGraphicTemplateEntrySaveDto -> content.graphicTemplate(OperationGraphicTemplateId(dto.id))?.let { before ->
            DecodedLedgerEntry(OperationChangeSet(graphicTemplates = listOf(OperationValueChange(before, applyOperationGraphicTemplatePatch(before, dto.forward, ::asset)))))
        } ?: error("Missing graphic template ${dto.id} for edit")

        is CreateNoteEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(notes = listOf(OperationValueChange(after = dto.note.toOperation()))))
        is DeleteNoteEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(notes = listOf(OperationValueChange(before = dto.note.toOperation()))))
        is EditNoteEntrySaveDto -> content.notes.firstOrNull { it.id.value == dto.id }?.let { before ->
            DecodedLedgerEntry(OperationChangeSet(notes = listOf(OperationValueChange(before, applyOperationNotePatch(before, dto.forward)))))
        } ?: error("Missing note ${dto.id} for edit")

        is ReorderNotesEntrySaveDto -> DecodedLedgerEntry(
            OperationChangeSet(
                noteOrderBefore = dto.backward.map(::OperationNoteId),
                noteOrderAfter = dto.forward.map(::OperationNoteId)
            )
        )

        is AddTurnEntrySaveDto -> DecodedLedgerEntry(OperationChangeSet(), dto.name, dto.thumbnailAssetId)
        is BatchLedgerEntrySaveDto -> {
            var after = content
            val components = dto.entries.map { nested ->
                decode(nested, after).changeSet.also { change ->
                    after = change.applyTo(after).withGraphicSizesFromInstances()
                }
            }
            DecodedLedgerEntry(after.changesFrom(content), componentChangeSets = components)
        }
    }

    private fun asset(id: UUID): Pair<java.awt.image.BufferedImage, AssetSource>? = assets.getOrPut(id) {
        val loaded = zip.loadAsset(id.toString())
        val image = MapViewerImageLoader.load(loaded.bytes, loaded.entry.name) ?: error("Unable to load template asset ${loaded.entry.name}")
        image to AssetSource.fromEmbeddedBytes(loaded.entry.name.substringAfterLast('/'), loaded.bytes)
    }

    private fun UnitTemplateSnapshotSaveDto.toOperation() = asset(UUID.fromString(template.assetId))!!.let { (image, source) ->
        OperationUnitTemplate(OperationUnitTemplateId(id), template.name, image, index, source, template.affiliation?.toOperation(), template.customProperties)
    }

    private fun GraphicTemplateSaveDto.toOperation() = asset(UUID.fromString(assetId))!!.let { (image, source) ->
        OperationGraphicTemplate(OperationGraphicTemplateId(id), name, image, index = 0, assetSource = source, customProperties = customProperties)
    }
}

private data class DecodedLedgerEntry(
    val changeSet: OperationChangeSet,
    val turnName: String? = null,
    val turnThumbnailAssetId: UUID? = null,
    val componentChangeSets: List<OperationChangeSet> = listOf(changeSet),
)

private fun SavedMapObjectDto.toOperation(id: UUID): OperationObject = when (this) {
    is SavedUnitObjectDto -> OperationUnit(
        OperationObjectId(id), OperationUnitTemplateId(unitTemplateId), instanceName, destroyed, facingDegrees,
        MapCoordinate(mapCoordinate.x, mapCoordinate.y), normalizedWidth, normalizedHeight, customProperties, affiliation?.toOperation(),
    )

    is SavedGraphicObjectDto -> OperationGraphic(
        OperationObjectId(id), OperationGraphicTemplateId(graphicTemplateId), instanceName,
        MapCoordinate(mapCoordinate.x, mapCoordinate.y), normalizedWidth, normalizedHeight, customProperties,
    )

    is SavedDrawingObjectDto -> OperationDrawingObject(OperationObjectId(id), drawing.toOperation(), customProperties)
}

private fun DrawingSaveDto.toOperation(): OperationDrawing = when (type) {
    "freeform" -> OperationFreeformDrawing(points.orEmpty().map { OperationDrawingPoint(it.x, it.y) }, color.toArgb())
    "line" -> OperationLineDrawing(OperationDrawingPoint(start!!.x, start.y), OperationDrawingPoint(end!!.x, end.y), color.toArgb())
    "rectangle" -> OperationRectangleDrawing(OperationDrawingPoint(start!!.x, start.y), OperationDrawingPoint(end!!.x, end.y), color.toArgb(), rotationDegrees ?: 0.0)
    "oval" -> OperationOvalDrawing(OperationDrawingPoint(center!!.x, center.y), xRadius!!, yRadius!!, color.toArgb(), rotationDegrees ?: 0.0)
    else -> error("Unsupported drawing type $type")
}

private fun NoteSaveDto.toOperation() = OperationNote(OperationNoteId(id), title, body, referencedObjectIds.map(::OperationObjectId).distinct())
private fun UnitAffiliationSaveDto.toOperation() = OperationUnitAffiliation.valueOf(name.uppercase())
private fun String.toArgb() =
    removePrefix("#").let { OperationArgb.of(it.substring(0, 2).toInt(16), it.substring(2, 4).toInt(16), it.substring(4, 6).toInt(16), it.substring(6, 8).toInt(16)) }

private fun OperationContent.withGraphicSizesFromInstances(): OperationContent {
    val sizes = objects.filterIsInstance<OperationGraphic>().groupBy(OperationGraphic::graphicTemplateId)
        .mapValues { it.value.first().let { graphic -> graphic.normalizedWidth to graphic.normalizedHeight } }
    return copy(graphicTemplates = graphicTemplates.map { template ->
        sizes[template.id]?.let { template.copy(normalizedWidth = it.first, normalizedHeight = it.second) } ?: template
    })
}

private fun List<LedgerEntrySaveDto>.thumbnailIds(): Set<UUID> = flatMap {
    when (it) {
        is AddTurnEntrySaveDto -> listOfNotNull(it.thumbnailAssetId)
        is BatchLedgerEntrySaveDto -> it.entries.thumbnailIds().toList()
        else -> emptyList()
    }
}.toSet()

private data class AssetBytes(val entry: ZipEntry, val bytes: ByteArray)

private fun ZipFile.loadAsset(id: String): AssetBytes {
    val matches = entries().asSequence().filterNot(ZipEntry::isDirectory).filter { it.name.startsWith("assets/$id.") }.toList()
    val entry = when (matches.size) {
        1 -> matches.single(); 0 -> error("Missing asset for id $id"); else -> error("Multiple assets found for id $id")
    }
    return AssetBytes(entry, getInputStream(entry).use { it.readBytes() })
}

private fun <T> ZipFile.readJson(path: String, type: Class<T>): T =
    getEntry(path)?.let { getInputStream(it).use { input -> OperationSaveJson.objectMapper.readValue(input, type) } }
        ?: error("Save does not contain $path")
