package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OPERATION_START_TURN_NAME
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntries
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.save.GameSaveDto
import io.hsar.mapgenerator.commandgui.save.MapThumbnailRenderer
import io.hsar.mapgenerator.commandgui.save.OperationAdditionalData
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import io.hsar.mapgenerator.commandgui.save.OperationSaveLoader
import io.hsar.mapgenerator.commandgui.save.OperationSaveRequest
import io.hsar.mapgenerator.commandgui.save.OperationSaveWriter
import io.hsar.mapgenerator.commandgui.save.OperationToolStorage
import korlibs.io.lang.use
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile

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

/** Engine-neutral catalogue and complete [OperationDocument] archive boundary. */
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

    fun loadOperation(path: Path): OperationDocument {
        val loaded = OperationSaveLoader().load(path)
        val additionalData = OperationAdditionalData.fromJson(loaded.additionalDataJson)
        return OperationDocument(
            operationId = loaded.operationId,
            operationName = loaded.operationName,
            createdAt = loaded.createdAt,
            updatedAt = loaded.updatedAt,
            settings = loaded.settings,
            renderData = loaded.renderData,
            camera = loaded.camera,
            mapAssetSource = loaded.mapAssetSource,
            timeline = loaded.timeline,
            generatedImageAssets = loaded.generatedImageAssets,
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
            timeline = OperationTimeline().record(OperationTimelineEntries.turn(OPERATION_START_TURN_NAME)),
            sourcePath = writer.defaultSavePath(operationName, operationId),
        )
    }

    fun saveOperation(
        document: OperationDocument,
        camera: MapViewerSavedCamera? = document.camera,
        uiState: OperationUiState = document.uiState,
        thumbnailImage: BufferedImage? = null,
    ): OperationDocument {
        val now = Instant.now()
        val savedCamera = camera ?: MapViewerSavedCamera(0.5, 0.5, 1.0)
        val additionalData = OperationAdditionalData.fromJson(document.additionalDataJson)
            .also { it.updateOperationUiState(uiState) }
            .toJsonOrNull()
        val target = document.sourcePath ?: OperationSaveWriter().defaultSavePath(document.operationName, document.operationId)
        val mapImage = document.renderData.baseImage
        val geometry = geometryFor(mapImage)
        val thumbnail = thumbnailImage ?: MapThumbnailRenderer().renderOperationThumbnail(
            mapImage = mapImage,
            geometry = geometry,
            viewport = MapViewport(
                center = ViewerPoint(geometry.mapOffsetX + savedCamera.x * mapImage.width, geometry.mapOffsetY + savedCamera.y * mapImage.height),
                zoom = savedCamera.zoom,
                viewportWidth = mapImage.width,
                viewportHeight = mapImage.height,
                worldWidth = geometry.backgroundWidth.toDouble(),
                worldHeight = geometry.backgroundHeight.toDouble(),
            ),
            gridLayer = document.renderData.gridLayer,
            content = document.content,
        )
        OperationSaveWriter().write(
            OperationSaveRequest(
                operationId = document.operationId,
                operationName = document.operationName,
                createdAt = document.createdAt,
                updatedAt = now,
                camera = savedCamera,
                settings = document.settings,
                mapImage = mapImage,
                mapAssetSource = document.mapAssetSource,
                timeline = document.timeline,
                generatedImageAssets = document.generatedImageAssets.filter { asset ->
                    document.timeline.appliedEntries.any { it.turnThumbnailAssetId == asset.id }
                },
                thumbnailImage = thumbnail,
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

    private fun ZipFile.readImageAssetOrNull(assetId: String): BufferedImage? =
        entries().asSequence()
            .filter { !it.isDirectory }
            .firstOrNull {
                it.name.substringAfterLast('/').substringBeforeLast('.', missingDelimiterValue = "") == assetId
            }
            ?.let { entry -> getInputStream(entry).use { MapViewerImageLoader.load(it.readBytes(), entry.name) } }

    private companion object {
        const val GAME_JSON = "game.json"
    }
}
