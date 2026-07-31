package io.hsar.mapgenerator.commandgui

import com.sun.org.apache.bcel.internal.util.Args.require
import io.hsar.mapgenerator.commandgui.korge.adapter.toLegacyEntries
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationTimeline
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationGeneratedImage
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.save.GameSaveDto
import io.hsar.mapgenerator.commandgui.save.GeneratedImageAsset
import io.hsar.mapgenerator.commandgui.save.LedgerFileDto
import io.hsar.mapgenerator.commandgui.save.OperationAdditionalData
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveRequest
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.commandgui.save.OperationToolStorage
import io.hsar.mapgenerator.commandgui.save.RuntimeLedgerReplayer
import io.hsar.mapgenerator.grid.CoordAxis
import io.hsar.mapgenerator.grid.GridLayerBuilder
import korlibs.io.lang.use
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

data class MapViewerSavedOperation(
    val path: Path,
    val operationId: UUID,
    val operationName: String,
    val updatedAt: Instant,
    val createdAt: Instant,
    val thumbnailAssetId: UUID?,
    val thumbnail: BufferedImage?,
)

data class MapViewerSavedCamera(
    /** Map-relative horizontal coordinate, where 0 is the left edge and 1 is the right edge. */
    val x: Double,
    /** Map-relative vertical coordinate, where 0 is the top edge and 1 is the bottom edge. */
    val y: Double,
    val zoom: Double,
)

data class MapViewerSavedMap(
    val operation: MapViewerSavedOperation,
    val settings: MapViewerSettings,
    val renderData: MapViewerRenderData,
    val camera: MapViewerSavedCamera?,
    val ledger: MapViewerLedgerReplay = MapViewerLedgerReplay.EMPTY,
)

/**
 * Engine-neutral, map-level access to Operational Command Tool saves.
 *
 * This deliberately reads only game metadata and raster assets. It does not replay the ledger,
 * construct unit models, or initialize either viewer backend.
 */
class MapViewerSaveRepository(
    val savesDirectory: Path = OperationToolStorage.defaultRootDirectory().resolve("saves"),
) {
    fun list(): List<MapViewerSavedOperation> {
        Files.createDirectories(savesDirectory)
        return Files.list(savesDirectory).use { paths ->
            paths.iterator().asSequence()
                .filter(Files::isRegularFile)
                .filter { it.fileName.toString().lowercase().endsWith(".zip") }
                .mapNotNull(::readSummaryOrNull)
                .sortedWith(compareByDescending<MapViewerSavedOperation> { it.updatedAt }.thenByDescending { it.createdAt })
                .toList()
        }
    }

    fun loadMap(path: Path): MapViewerSavedMap =
        ZipFile(path.toFile()).use { zip ->
            val game = zip.readGame()
            require(game.formatVersion in SUPPORTED_FORMAT_VERSIONS) {
                "Unsupported save format version ${game.formatVersion}"
            }
            val operationId = UUID.fromString(game.id)
            val mapImage = zip.readImageAsset(game.map.assetId)
            val seed = operationId.mostSignificantBits xor operationId.leastSignificantBits
            val parsedStartX = game.map.gridStartX?.let {
                game.map.referenceSystem.parseCoord(it, CoordAxis.X, game.map.referenceSystem.gridScale)
            }
            val parsedStartY = game.map.gridStartY?.let {
                game.map.referenceSystem.parseCoord(it, CoordAxis.Y, game.map.referenceSystem.gridScale)
            }
            val grid = GridLayerBuilder.build(
                width = mapImage.width,
                height = mapImage.height,
                gridScale = game.map.referenceSystem.gridScale,
                unitsPerPixel = game.map.unitsPerPixel,
                random = Random(seed),
                mapId = game.map.mapId,
                referenceSystem = game.map.referenceSystem,
                verticalLineNumberStart = parsedStartX,
                horizontalLineNumberStart = parsedStartY,
                colour = GridColourSelector.resolve(game.map.gridColour, mapImage),
            )
            val settings = MapViewerSettings(
                operationName = game.name,
                availableLayers = listOf(
                    StaticImageLayer(id = game.map.assetId, displayName = "Map"),
                    MapLayer.GRID,
                ),
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

            MapViewerSavedMap(
                operation = game.toSummary(path, zip),
                settings = settings,
                renderData = MapViewerRenderData(baseImage = mapImage, gridLayer = grid),
                camera = game.camera?.let { MapViewerSavedCamera(it.x, it.y, it.zoom) },
                ledger = zip.readLedgerEntries()
                    .takeIf { it.isNotEmpty() }
                    ?.let { RuntimeLedgerReplayer(zip).replay(it) }
                    ?.let(::replayForViewer)
                    ?: MapViewerLedgerReplay.EMPTY,
            )
        }

    fun loadOperation(path: Path): OperationDocument {
        val loaded = OperationSaveLoader().load(path)
        val map = loadMap(path)
        val additionalData = OperationAdditionalData.fromJson(loaded.additionalDataJson)
        return OperationDocument(
            operationId = loaded.operationId,
            operationName = loaded.operationName,
            createdAt = loaded.createdAt,
            updatedAt = loaded.updatedAt,
            settings = loaded.settings,
            renderData = map.renderData,
            camera = map.camera,
            mapAssetSource = loaded.mapAssetSource,
            timeline = loaded.ledgerEntries.toOperationTimeline(),
            generatedImageAssets = loaded.generatedImageAssets.map {
                OperationGeneratedImage(it.id, it.role, it.image)
            },
            additionalDataJson = loaded.additionalDataJson,
            sourcePath = path,
            uiState = additionalData.operationUiState(),
            persisted = true,
        )
    }

    fun createOperation(
        settings: MapViewerSettings,
        renderData: MapViewerRenderData,
        operationId: UUID = UUID.randomUUID(),
        now: Instant = Instant.now(),
    ): OperationDocument {
        val operationName = settings.operationName?.takeIf(String::isNotBlank) ?: "Operation"
        val writer = OperationSaveWriter()
        return OperationDocument(
            operationId = operationId,
            operationName = operationName,
            createdAt = now,
            updatedAt = now,
            settings = settings,
            renderData = renderData,
            timeline = OperationTimeline(),
            sourcePath = writer.defaultSavePath(operationName, operationId),
        )
    }

    fun saveOperation(
        document: OperationDocument,
        camera: MapViewerSavedCamera? = document.camera,
        uiState: OperationUiState = document.uiState,
    ): OperationDocument {
        val now = Instant.now()
        val geometry = geometryFor(document.renderData.baseImage)
        val savedCamera = camera ?: MapViewerSavedCamera(0.5, 0.5, 1.0)
        val viewport = CameraViewport(
            centerX = geometry.mapOffsetX + savedCamera.x * document.renderData.baseImage.width,
            centerY = geometry.mapOffsetY + savedCamera.y * document.renderData.baseImage.height,
            zoom = savedCamera.zoom,
            viewportWidth = 1,
            viewportHeight = 1,
            worldWidth = geometry.backgroundWidth,
            worldHeight = geometry.backgroundHeight,
        )
        val additionalData = OperationAdditionalData.fromJson(document.additionalDataJson)
            .also { it.updateOperationUiState(uiState) }
            .toJsonOrNull()
        val target = document.sourcePath ?: OperationSaveWriter().defaultSavePath(document.operationName, document.operationId)
        OperationSaveWriter().write(
            OperationSaveRequest(
                operationId = document.operationId,
                operationName = document.operationName,
                createdAt = document.createdAt,
                updatedAt = now,
                camera = viewport,
                settings = document.settings,
                mapImage = document.renderData.baseImage,
                mapAssetSource = document.mapAssetSource,
                ledgerEntries = document.timeline.toLegacyEntries(),
                generatedImageAssets = document.generatedImageAssets.map {
                    GeneratedImageAsset(it.id, it.role, it.image)
                },
                additionalDataJson = additionalData,
            ),
            target,
        )
        return document.copy(
            updatedAt = now,
            camera = savedCamera,
            additionalDataJson = additionalData,
            sourcePath = target,
            uiState = uiState,
            persisted = true,
        )
    }

    private fun readSummaryOrNull(path: Path): MapViewerSavedOperation? =
        runCatching {
            ZipFile(path.toFile()).use { zip -> zip.readGame().toSummary(path, zip) }
        }.getOrNull()

    private fun GameSaveDto.toSummary(path: Path, zip: ZipFile) =
        MapViewerSavedOperation(
            path = path,
            operationId = UUID.fromString(id),
            operationName = name,
            updatedAt = Instant.parse(updatedAt),
            createdAt = Instant.parse(createdAt),
            thumbnailAssetId = thumbnailAssetId?.let(UUID::fromString),
            thumbnail = thumbnailAssetId?.let { zip.readImageAssetOrNull(it) },
        )

    private fun ZipFile.readGame(): GameSaveDto =
        getEntry(GAME_JSON)
            ?.let(::getInputStream)
            ?.use { OperationSaveJson.objectMapper.readValue(it, GameSaveDto::class.java) }
            ?: error("Save does not contain $GAME_JSON")

    private fun ZipFile.readLedgerEntries() =
        getEntry(LEDGER_JSON)
            ?.let(::getInputStream)
            ?.use { OperationSaveJson.objectMapper.readValue(it, LedgerFileDto::class.java).entries }
            .orEmpty()

    private fun ZipFile.readImageAsset(assetId: String): BufferedImage =
        readImageAssetOrNull(assetId) ?: error("Unable to decode image asset $assetId")

    private fun ZipFile.readImageAssetOrNull(assetId: String): BufferedImage? =
        entries().asSequence()
            .filter { !it.isDirectory }
            .firstOrNull {
                it.name.substringAfterLast('/').substringBeforeLast('.', missingDelimiterValue = "") == assetId
            }
            ?.let { entry -> getInputStream(entry).use(ImageIO::read) }
            ?.toArgb()

    private fun BufferedImage.toArgb(): BufferedImage =
        takeIf { type == BufferedImage.TYPE_INT_ARGB }
            ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { target ->
                target.createGraphics().use { graphics -> graphics.drawImage(this, 0, 0, null) }
            }

    private fun java.awt.Graphics2D.use(block: (java.awt.Graphics2D) -> Unit) =
        try {
            block(this)
        } finally {
            dispose()
        }

    private companion object {
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
        val SUPPORTED_FORMAT_VERSIONS = setOf(5, 6)
    }
}
