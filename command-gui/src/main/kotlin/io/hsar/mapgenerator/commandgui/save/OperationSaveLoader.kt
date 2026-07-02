package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.GridColourSelector
import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplatePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.launch.GridConfiguration
import io.hsar.mapgenerator.commandgui.korge.launch.defaultGridConfiguration
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import io.hsar.mapgenerator.grid.MgrsCoord
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.imageio.ImageIO

internal class OperationSaveLoader {
    fun load(path: Path): LoadedOperation =
        ZipFile(path.toFile()).use { zip ->
            val save = OperationSaveReader().read(path)
            require(save.game.formatVersion == FORMAT_VERSION) {
                "Unsupported save format version ${save.game.formatVersion}"
            }

            val mapAsset = loadAsset(zip, save.game.map.assetId)
            val mapImage = decodeBufferedImage(mapAsset.bytes, mapAsset.entry.name)
                ?: error("Unable to decode map asset ${mapAsset.entry.name}")
            val mapAssetSource = AssetSource.fromEmbeddedBytes(
                fileName = mapAsset.entry.name.substringAfterLast('/'),
                bytes = mapAsset.bytes,
            )
            val operationUuid = UUID.fromString(save.game.id)
            val gridConfiguration = save.game.map.mapId
                ?.takeIf { it.isNotBlank() }
                ?.let { mapId ->
                    val startX = save.game.map.gridStartX
                    val startY = save.game.map.gridStartY
                    val parsedStartX = startX?.let(MgrsCoord::parse)
                    val parsedStartY = startY?.let(MgrsCoord::parse)
                    if (parsedStartX != null && parsedStartY != null) {
                        GridConfiguration(
                            mapId = mapId,
                            referenceSystem = save.game.map.referenceSystem,
                            startX = parsedStartX,
                            startY = parsedStartY,
                        )
                    } else {
                        null
                    }
                } ?: defaultGridConfiguration(
                mapImage = mapImage,
                metresPerPixel = save.game.map.metresPerPixel,
                seed = operationUuid.mostSignificantBits xor operationUuid.leastSignificantBits,
            )

            val settings = MapViewerSettings(
                operationName = save.game.name,
                availableLayers = listOf(
                    StaticImageLayer(id = save.game.map.assetId, displayName = "Map"),
                    MapLayer.GRID,
                ),
                enabledLayerIds = setOf(save.game.map.assetId, MapLayer.GRID.id),
                staticLayerImages = mapOf(save.game.map.assetId to mapImage),
                width = mapImage.width,
                height = mapImage.height,
                metresPerPixel = save.game.map.metresPerPixel,
                mapId = gridConfiguration.mapId,
                referenceSystem = gridConfiguration.referenceSystem,
                gridStartX = gridConfiguration.startX,
                gridStartY = gridConfiguration.startY,
                gridColour = GridColourSelector.resolve(save.game.map.gridColour, mapImage),
                seed = operationUuid.mostSignificantBits xor operationUuid.leastSignificantBits,
            )

            val replayed = RuntimeLedgerReplayer(zip).replay(save.ledger.entries)
            LoadedOperation(
                operationId = operationUuid,
                operationName = save.game.name,
                createdAt = Instant.parse(save.game.createdAt),
                updatedAt = Instant.parse(save.game.updatedAt),
                settings = settings,
                camera = save.game.camera,
                mapImage = mapImage,
                mapAssetSource = mapAssetSource,
                ledgerEntries = replayed.ledgerEntries,
                generatedImageAssets = loadTurnThumbnailAssets(zip, save.ledger.entries),
            )
        }

    private fun loadTurnThumbnailAssets(
        zip: ZipFile,
        entries: List<LedgerEntrySaveDto>,
    ): List<GeneratedImageAsset> =
        entries.turnThumbnailAssetIds()
            .mapNotNull { assetId ->
                runCatching {
                    val asset = loadAsset(zip, assetId.toString())
                    GeneratedImageAsset(
                        id = assetId,
                        role = ASSET_ROLE_TURN_THUMBNAIL,
                        image = decodeBufferedImage(asset.bytes, asset.entry.name)
                            ?: error("Unable to decode turn thumbnail ${asset.entry.name}"),
                    )
                }.onFailure { throwable ->
                    org.apache.logging.log4j.LogManager
                        .getLogger(OperationSaveLoader::class.java)
                        .warn("Unable to load turn thumbnail asset {}", assetId, throwable)
                }.getOrNull()
            }

    private fun List<LedgerEntrySaveDto>.turnThumbnailAssetIds(): Set<UUID> =
        flatMap { entry ->
            when (entry) {
                is AddTurnEntrySaveDto -> listOfNotNull(entry.thumbnailAssetId)
                is BatchLedgerEntrySaveDto -> entry.entries.turnThumbnailAssetIds().toList()
                else -> emptyList()
            }
        }.toSet()

    private fun loadAsset(zip: ZipFile, assetId: String): AssetBytes =
        resolveAssetEntry(zip, assetId).let { entry ->
            AssetBytes(
                entry = entry,
                bytes = zip.getInputStream(entry).use { it.readBytes() },
            )
        }

    private fun resolveAssetEntry(zip: ZipFile, assetId: String): ZipEntry {
        val prefix = "assets/$assetId."
        val matches = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .filter { it.name.startsWith(prefix) }
            .toList()

        return when (matches.size) {
            1 -> matches.single()
            0 -> error("Missing asset for id $assetId")
            else -> error("Multiple assets found for id $assetId")
        }
    }

    private fun decodeBufferedImage(bytes: ByteArray, fileName: String): BufferedImage? =
        when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "svg" -> MapViewerIconLoader.load(bytes, fileName)?.toBufferedImage()
            else -> ImageIO.read(bytes.inputStream())?.toArgbBufferedImage()
        }

    private fun korlibs.image.bitmap.Bitmap32.toBufferedImage() =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val rgba = getRgba(x, y)
                    image.setRGB(x, y, (rgba.a shl 24) or (rgba.r shl 16) or (rgba.g shl 8) or rgba.b)
                }
            }
        }

    private fun BufferedImage.toArgbBufferedImage(): BufferedImage =
        if (type == BufferedImage.TYPE_INT_ARGB) {
            this
        } else {
            BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        image.setRGB(x, y, getRGB(x, y))
                    }
                }
            }
        }

    private companion object {
        const val FORMAT_VERSION = 3
        const val ASSET_ROLE_TURN_THUMBNAIL = "turn-thumbnail"
    }
}

private data class AssetBytes(
    val entry: ZipEntry,
    val bytes: ByteArray,
)

private data class ReplayedLedger(
    val ledgerEntries: List<LedgerEntry>,
)

private class RuntimeLedgerReplayer(
    private val zip: ZipFile,
) {
    private val templatesById = mutableMapOf<UUID, MapViewerUnitTemplate>()
    private val templateOrder = mutableListOf<UUID>()
    private val templateAssetsByAssetId = mutableMapOf<String, LoadedTemplateAsset>()

    fun replay(entries: List<LedgerEntrySaveDto>): ReplayedLedger {
        val runtimeEntries = entries.mapNotNull(::toRuntimeEntry)
        return ReplayedLedger(ledgerEntries = runtimeEntries)
    }

    private fun toRuntimeEntry(entry: LedgerEntrySaveDto): LedgerEntry? =
        when (entry) {
            is CreateObjectEntrySaveDto -> entry.objectData.toRuntimeSnapshot(entry.objectId)
                .let(::CreateObjectAction)

            is DeleteObjectEntrySaveDto -> entry.objectData.toRuntimeSnapshot(entry.objectId)
                .let(::DeleteObjectAction)

            is EditObjectEntrySaveDto -> EditObjectAction(
                objectId = MapPlacedObjectId(entry.objectId),
                forward = entry.forward,
                backward = entry.backward,
            )

            is BatchLedgerEntrySaveDto -> BatchLedgerEntry(
                entries = entry.entries.mapNotNull(::toRuntimeEntry),
            )

            is AddTurnEntrySaveDto -> AddTurnLedgerEntry(entry.name, entry.thumbnailAssetId)

            is CreateUnitTemplateEntrySaveDto -> entry.unitTemplate.toRuntimeSnapshot()
                .also { registerTemplate(it) }
                .let(::CreateUnitTemplateAction)

            is DeleteUnitTemplateEntrySaveDto -> entry.unitTemplate.toRuntimeSnapshot()
                .also { unregisterTemplate(it.id) }
                .let(::DeleteUnitTemplateAction)

            is EditUnitTemplateEntrySaveDto -> {
                val before = snapshotFor(entry.id)
                val after = applyUnitTemplateEdit(entry.id, entry.forward)
                EditUnitTemplateAction(
                    id = UnitTemplateId(entry.id),
                    forward = entry.forward,
                    backward = entry.backward,
                    beforeSnapshot = before,
                    afterSnapshot = after,
                )
            }
        }

    private fun SavedMapObjectDto.toRuntimeSnapshot(id: UUID): MapPlacedObjectSnapshot =
        when (this) {
            is SavedUnitObjectDto -> {
                val template = templatesById[unitTemplateId]
                    ?: error("Missing unit template for id $unitTemplateId")
                UnitSnapshot(
                    id = MapPlacedObjectId(id),
                    unitTemplateId = UnitTemplateId(unitTemplateId),
                    template = template,
                    instanceName = instanceName,
                    destroyed = destroyed,
                    facingDegrees = facingDegrees,
                    mapCoordinate = mapCoordinate.toRuntime(),
                    normalizedWidth = normalizedWidth,
                    normalizedHeight = normalizedHeight,
                    customProperties = customProperties,
                )
            }

            is SavedDrawingObjectDto -> DrawingSnapshot(
                id = MapPlacedObjectId(id),
                drawing = drawing.toRuntimeDrawing(),
            )
        }

    private fun UnitTemplateSnapshotSaveDto.toRuntimeSnapshot() =
        UnitTemplateSnapshot(
            id = UnitTemplateId(id),
            template = template.toRuntimeTemplate(id),
            index = index,
        )

    private fun UnitTemplateSaveDto.toRuntimeTemplate(id: UUID) =
        templateAssetsByAssetId.getOrPut(assetId) {
            val entry = resolveAssetEntry(zip, assetId)
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            val bitmap = MapViewerIconLoader.load(bytes, entry.name)
                ?: error("Unable to load template asset ${entry.name}")
            LoadedTemplateAsset(
                bitmap = bitmap,
                assetSource = AssetSource.fromEmbeddedBytes(
                    fileName = entry.name.substringAfterLast('/'),
                    bytes = bytes,
                ),
            )
        }.let { asset ->
            MapViewerUnitTemplate(
                id = UnitTemplateId(id),
                name = name,
                bitmap = asset.bitmap,
                assetSource = asset.assetSource,
                affiliation = affiliation,
                customProperties = customProperties,
            )
        }

    private fun registerTemplate(snapshot: UnitTemplateSnapshot) {
        templateOrder.remove(snapshot.id.value)
        templateOrder.add(snapshot.index.coerceIn(0, templateOrder.size), snapshot.id.value)
        templatesById[snapshot.id.value] = snapshot.template
    }

    private fun unregisterTemplate(id: UnitTemplateId) {
        templateOrder.remove(id.value)
        templatesById.remove(id.value)
    }

    private fun snapshotFor(id: UUID): UnitTemplateSnapshot? {
        val template = templatesById[id] ?: return null
        val index = templateOrder.indexOf(id).takeIf { it >= 0 } ?: 0
        return UnitTemplateSnapshot(
            id = UnitTemplateId(id),
            template = template,
            index = index,
        )
    }

    private fun applyUnitTemplateEdit(id: UUID, changes: Map<String, com.fasterxml.jackson.databind.JsonNode?>): UnitTemplateSnapshot? {
        val current = snapshotFor(id) ?: return null
        val updated = MapViewerUnitTemplatePatch.apply(
            snapshot = current,
            changes = changes,
        ) { assetId ->
            loadTemplateAsset(assetId.toString()).let { loadedAsset ->
                current.template.copy(
                    bitmap = loadedAsset.bitmap,
                    assetSource = loadedAsset.assetSource,
                )
            }
        }
        templatesById[id] = updated.template
        return updated
    }

    private fun loadTemplateAsset(assetId: String): LoadedTemplateAsset =
        templateAssetsByAssetId.getOrPut(assetId) {
            val entry = resolveAssetEntry(zip, assetId)
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            val bitmap = MapViewerIconLoader.load(bytes, entry.name)
                ?: error("Unable to load template asset ${entry.name}")
            LoadedTemplateAsset(
                bitmap = bitmap,
                assetSource = AssetSource.fromEmbeddedBytes(
                    fileName = entry.name.substringAfterLast('/'),
                    bytes = bytes,
                ),
            )
        }

    private fun resolveAssetEntry(zip: ZipFile, assetId: String): ZipEntry {
        val prefix = "assets/$assetId."
        val matches = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .filter { it.name.startsWith(prefix) }
            .toList()

        return when (matches.size) {
            1 -> matches.single()
            0 -> error("Missing asset for id $assetId")
            else -> error("Multiple assets found for id $assetId")
        }
    }

    private fun DrawingSaveDto.toRuntimeDrawing(): MapDrawing =
        when (type) {
            "freeform" -> FreeformDrawing(points = points.orEmpty().map { it.toRuntime() }, color = color.toColor())
            "line" -> LineDrawing(start = start!!.toRuntime(), end = end!!.toRuntime(), color = color.toColor())
            "rectangle" -> RectangleDrawing(start = start!!.toRuntime(), end = end!!.toRuntime(), color = color.toColor())
            "circle" -> CircleDrawing(center = center!!.toRuntime(), radius = radius ?: 0.0, color = color.toColor())
            else -> error("Unsupported drawing type $type")
        }

    private fun MapCoordinateSaveDto.toRuntime() =
        MapCoordinate(x = x, y = y)

    private fun MapDrawingPointSaveDto.toRuntime() =
        MapDrawingPoint(x = x, y = y)

    private fun String.toColor(): Color {
        val hex = removePrefix("#")
        val alpha = hex.substring(0, 2).toInt(16)
        val red = hex.substring(2, 4).toInt(16)
        val green = hex.substring(4, 6).toInt(16)
        val blue = hex.substring(6, 8).toInt(16)
        return Color(red, green, blue, alpha)
    }
}

private data class LoadedTemplateAsset(
    val bitmap: korlibs.image.bitmap.Bitmap32,
    val assetSource: AssetSource,
)
