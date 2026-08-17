package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.GridColourSelector
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.grid.CoordAxis
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.util.*
import java.util.Collections.emptySet
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.math.roundToInt

internal class OperationSaveWriter(
    private val clock: Clock = Clock.systemUTC(),
    private val uuidGenerator: () -> UUID = UUID::randomUUID,
    private val storage: OperationToolStorage = OperationToolStorage(),
    private val archiveCommitter: OperationArchiveCommitter = OperationArchiveCommitter(),
) {
    fun write(request: OperationSaveRequest, targetPath: Path = defaultSavePath(request.operationName, request.operationId)): Path {
        if (targetPath.parent == defaultSaveDirectory()) {
            storage.ensureExists()
        } else {
            targetPath.parent?.let(Files::createDirectories)
        }

        val assets = OperationAssetRegistry(uuidGenerator, setOf(request.operationId))
        val mapAsset = assets.registerBufferedImage(
            image = request.mapImage,
            role = ASSET_ROLE_MAP,
            source = request.mapAssetSource,
        )
        val thumbnailAsset = assets.registerThumbnail(
            operationId = request.operationId,
            image = request.thumbnailImage ?: request.mapImage,
        )
        request.generatedImageAssets.forEach { generatedAsset ->
            assets.registerGeneratedJpeg(
                id = generatedAsset.id,
                role = generatedAsset.role,
                image = generatedAsset.image,
            )
        }
        val ledger = LedgerEntrySaveMapper(assets).toDtos(request.timeline)
        val updatedAt = request.updatedAt.takeIf { !it.isBefore(request.createdAt) } ?: clock.instant()
        val gridConfiguration = request.settings.mapId
            ?.takeIf { it.isNotBlank() }
            ?.let { mapId ->
                val startX = request.settings.gridStartX
                val startY = request.settings.gridStartY
                if (startX != null && startY != null) {
                    SaveGridConfiguration(
                        mapId = mapId,
                        referenceSystem = request.settings.referenceSystem,
                        startX = startX,
                        startY = startY,
                    )
                } else {
                    null
                }
            } ?: defaultSaveGridConfiguration(
            mapImage = request.mapImage,
            unitsPerPixel = request.settings.unitsPerPixel,
            seed = request.settings.seed,
            referenceSystem = request.settings.referenceSystem,
        )
        val game = GameSaveDto(
            formatVersion = FORMAT_VERSION,
            id = request.operationId.toString(),
            name = request.operationName,
            createdAt = request.createdAt.toString(),
            updatedAt = updatedAt.toString(),
            thumbnailAssetId = thumbnailAsset.id.toString(),
            camera = CameraSaveDto(request.camera.x, request.camera.y, request.camera.zoom),
            map = MapSaveDto(
                unitsPerPixel = request.settings.unitsPerPixel,
                assetId = mapAsset.id.toString(),
                referenceSystem = gridConfiguration.referenceSystem,
                mapId = gridConfiguration.mapId,
                gridStartX = gridConfiguration.referenceSystem.formatCoord(
                    gridConfiguration.startX,
                    CoordAxis.X,
                    gridConfiguration.referenceSystem.gridScale,
                ),
                gridStartY = gridConfiguration.referenceSystem.formatCoord(
                    gridConfiguration.startY,
                    CoordAxis.Y,
                    gridConfiguration.referenceSystem.gridScale,
                ),
                gridColour = GridColourSelector.toHex(
                    request.settings.gridColour ?: GridColourSelector.bestPresetFor(request.mapImage),
                ),
            ),
        )
        val ledgerFile = LedgerFileDto(entries = ledger)

        archiveCommitter.write(targetPath) { stagedPath ->
            ZipOutputStream(Files.newOutputStream(stagedPath)).use { zip ->
                assets.assets.forEach { asset ->
                    zip.putNextEntry(ZipEntry(asset.path))
                    zip.write(asset.bytes)
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry(GAME_JSON))
                zip.write(OperationSaveJson.objectMapper.writeValueAsBytes(game))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(LEDGER_JSON))
                zip.write(OperationSaveJson.objectMapper.writeValueAsBytes(ledgerFile))
                zip.closeEntry()
                request.additionalDataJson?.let { additionalData ->
                    zip.putNextEntry(ZipEntry(ADDITIONAL_DATA_JSON))
                    zip.write(additionalData)
                    zip.closeEntry()
                }
            }
        }

        return targetPath
    }

    fun defaultSavePath(operationName: String, operationId: UUID): Path =
        defaultSaveDirectory().resolve("${operationName.sanitizedFilename()}_${operationId.toString().take(8)}.oct.zip")

    fun defaultSaveDirectory(): Path =
        storage.savesDirectory

    fun defaultStorageDirectory(): Path =
        storage.rootDirectory

    fun ensureDefaultStorage() =
        storage.ensureExists()

    private fun String.sanitizedFilename(): String =
        trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "Operation" }

    private companion object {
        const val FORMAT_VERSION = 6
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
        const val ADDITIONAL_DATA_JSON = "additionalData.json"
        const val ASSET_ROLE_MAP = "map"
    }
}

internal class OperationArchiveCommitter(
    private val move: (Path, Path, Boolean) -> Path = { source, target, atomic ->
        if (atomic) Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        else Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    },
) {
    fun write(targetPath: Path, writeArchive: (Path) -> Unit) {
        val absoluteTarget = targetPath.toAbsolutePath()
        val parent = absoluteTarget.parent ?: error("Save target must have a parent directory")
        Files.createDirectories(parent)
        val staged = Files.createTempFile(parent, ".${absoluteTarget.fileName}.", ".tmp")
        try {
            writeArchive(staged)
            try {
                move(staged, absoluteTarget, true)
            } catch (_: AtomicMoveNotSupportedException) {
                replaceWithBackup(staged, absoluteTarget)
            }
        } finally {
            Files.deleteIfExists(staged)
        }
    }

    private fun replaceWithBackup(staged: Path, target: Path) {
        val backup = target.takeIf(Files::exists)?.let {
            Files.createTempFile(target.parent, ".${target.fileName}.", ".backup").also(Files::deleteIfExists)
        }
        try {
            backup?.let { move(target, it, false) }
            try {
                move(staged, target, false)
            } catch (failure: Throwable) {
                backup?.takeIf(Files::exists)?.let { move(it, target, false) }
                throw failure
            }
        } finally {
            backup?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }
}

internal class OperationAssetRegistry(
    private val uuidGenerator: () -> UUID,
    reservedIds: Set<UUID> = emptySet(),
) {
    private val registeredAssets = mutableListOf<RegisteredAsset>()
    private val templateAssets = mutableMapOf<String, RegisteredAsset>()
    private val allocatedIds = reservedIds.toMutableSet()

    val assets: List<RegisteredAsset>
        get() = registeredAssets.toList()

    fun registerTemplate(template: OperationUnitTemplate): RegisteredAsset =
        templateAssets.getOrPut(template.assetKey()) {
            registerBufferedImage(template.image, "unit-icon", template.assetSource)
        }

    fun registerGraphicTemplate(template: OperationGraphicTemplate): RegisteredAsset =
        templateAssets.getOrPut("graphic:${template.assetSource?.path ?: template.image.contentKey()}") {
            registerBufferedImage(template.image, "graphic-icon", template.assetSource)
        }

    fun registerBufferedImage(image: BufferedImage, role: String, source: AssetSource?): RegisteredAsset =
        source
            ?.existingFileAsset(role)
            ?: image.fallbackAsset(role = role, originalFilename = source?.originalFilename)

    fun registerThumbnail(operationId: UUID, image: BufferedImage): RegisteredAsset =
        RegisteredAsset(
            id = operationId,
            path = "assets/$operationId.jpg",
            mediaType = "image/jpeg",
            role = "thumbnail",
            originalFilename = null,
            bytes = image.thumbnail(MAX_THUMBNAIL_WIDTH).encodeJpeg(),
        ).also(registeredAssets::add)

    fun registerGeneratedJpeg(id: UUID, role: String, image: BufferedImage): RegisteredAsset =
        registeredAssets
            .firstOrNull { it.id == id }
            ?: RegisteredAsset(
                id = id,
                path = "assets/$id.jpg",
                mediaType = "image/jpeg",
                role = role,
                originalFilename = null,
                bytes = image.thumbnail(MAX_THUMBNAIL_WIDTH).encodeJpeg(),
            ).also(registeredAssets::add)

    private fun AssetSource.existingFileAsset(role: String): RegisteredAsset? {
        val file = file().takeIf { path.isNotBlank() && it.isFile }
        val extension = extension.ifBlank { file?.extension?.lowercase().orEmpty() }.ifBlank { "bin" }
        val bytes = embeddedBytes ?: file?.toPath()?.let(Files::readAllBytes) ?: return null
        val id = nextAssetId()
        return RegisteredAsset(
            id = id,
            path = "assets/$id.$extension",
            mediaType = mediaType,
            role = role,
            originalFilename = originalFilename,
            bytes = bytes,
        ).also(registeredAssets::add)
    }

    private fun BufferedImage.fallbackAsset(
        role: String,
        originalFilename: String?,
        forcePng: Boolean = false,
    ): RegisteredAsset {
        val id = nextAssetId()
        val usePng = forcePng || colorModel.hasAlpha()
        val extension = if (usePng) "png" else "jpg"
        return RegisteredAsset(
            id = id,
            path = "assets/$id.$extension",
            mediaType = if (usePng) "image/png" else "image/jpeg",
            role = role,
            originalFilename = originalFilename,
            bytes = if (usePng) toArgbBufferedImage().encodePng() else toRgb().encodeJpeg(),
        ).also(registeredAssets::add)
    }

    private fun OperationUnitTemplate.assetKey(): String =
        assetSource?.path ?: image.contentKey()

    private fun nextAssetId(): UUID = generateSequence(uuidGenerator).first(allocatedIds::add)

    private fun BufferedImage.contentKey(): String =
        buildString {
            append(width).append('x').append(height).append(':')
            for (y in 0 until height) {
                for (x in 0 until width) {
                    append(getRGB(x, y)).append(',')
                }
            }
        }

    private fun BufferedImage.thumbnail(maxWidth: Int): BufferedImage {
        if (width <= maxWidth) {
            return this.toRgb()
        }

        val scale = maxWidth.toDouble() / width
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val output = BufferedImage(maxWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        return output.also { target ->
            target.createGraphics().useHighQuality {
                drawImage(this@thumbnail.toRgb(), 0, 0, maxWidth, targetHeight, null)
            }
        }
    }

    private fun BufferedImage.toRgb(): BufferedImage {
        if (type == BufferedImage.TYPE_INT_RGB && !colorModel.hasAlpha()) {
            return this
        }

        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        return output.also { target ->
            target.createGraphics().useHighQuality {
                fillRect(0, 0, width, height)
                drawImage(this@toRgb, 0, 0, null)
            }
        }
    }

    private fun Graphics2D.useHighQuality(block: Graphics2D.() -> Unit) {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        try {
            block()
        } finally {
            dispose()
        }
    }

    private fun BufferedImage.encodePng(): ByteArray =
        ByteArrayOutputStream().use { output ->
            ImageIO.write(this, "png", output)
            output.toByteArray()
        }

    private fun BufferedImage.encodeJpeg(): ByteArray =
        ByteArrayOutputStream().use { output ->
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            MemoryCacheImageOutputStream(output).use { imageOutput ->
                writer.output = imageOutput
                writer.write(
                    null,
                    IIOImage(toRgb(), null, null),
                    writer.defaultWriteParam.apply {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = JPEG_QUALITY
                    },
                )
                writer.dispose()
            }
            output.toByteArray()
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
        const val MAX_THUMBNAIL_WIDTH = 512
        const val JPEG_QUALITY = 0.72f
    }
}

internal data class RegisteredAsset(
    val id: UUID,
    val path: String,
    val mediaType: String,
    val role: String,
    val originalFilename: String?,
    val bytes: ByteArray,
) {
}
