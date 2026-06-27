package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.launch.GridConfiguration
import io.hsar.mapgenerator.commandgui.korge.launch.defaultGridConfiguration
import korlibs.image.bitmap.Bitmap32
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.util.*
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
) {
    fun write(request: OperationSaveRequest, targetPath: Path = defaultSavePath(request.operationName, request.operationId)): Path {
        targetPath.parent?.let(Files::createDirectories)

        val assets = OperationAssetRegistry(uuidGenerator)
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
        val ledgerMapper = LedgerEntrySaveMapper(assets)
        val ledger = request.ledgerEntries.map(ledgerMapper::toDto)
        val updatedAt = request.updatedAt.takeIf { !it.isBefore(request.createdAt) } ?: clock.instant()
        val geometry = geometryFor(request.mapImage)
        val gridConfiguration = request.settings.mapId
            ?.takeIf { it.isNotBlank() }
            ?.let { mapId ->
                val startX = request.settings.gridStartX
                val startY = request.settings.gridStartY
                if (startX != null && startY != null) {
                    GridConfiguration(mapId = mapId, startX = startX, startY = startY)
                } else {
                    null
                }
            } ?: defaultGridConfiguration(request.mapImage, request.settings.metresPerPixel, request.settings.seed)
        val game = GameSaveDto(
            formatVersion = FORMAT_VERSION,
            id = request.operationId.toString(),
            name = request.operationName,
            createdAt = request.createdAt.toString(),
            updatedAt = updatedAt.toString(),
            thumbnailAssetId = thumbnailAsset.id.toString(),
            camera = request.camera.toSaveCamera(geometry, request.mapImage.width, request.mapImage.height),
            map = MapSaveDto(
                metresPerPixel = request.settings.metresPerPixel,
                assetId = mapAsset.id.toString(),
                mapId = gridConfiguration.mapId,
                gridStartX = gridConfiguration.startX,
                gridStartY = gridConfiguration.startY,
            ),
        )
        val ledgerFile = LedgerFileDto(entries = ledger)

        ZipOutputStream(Files.newOutputStream(targetPath)).use { zip ->
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
        }

        return targetPath
    }

    fun defaultSavePath(operationName: String, operationId: UUID): Path =
        defaultSaveDirectory().resolve("${operationName.sanitizedFilename()}_${operationId.toString().take(8)}.oct.zip")

    fun defaultSaveDirectory(): Path =
        Path.of(System.getProperty("user.home"), "Documents", "My Games", "OperationalCommandTool")

    private fun String.sanitizedFilename(): String =
        trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "Operation" }

    private companion object {
        const val FORMAT_VERSION = 2
        const val GAME_JSON = "game.json"
        const val LEDGER_JSON = "ledger.json"
        const val ASSET_ROLE_MAP = "map"
    }
}

internal class OperationAssetRegistry(
    private val uuidGenerator: () -> UUID,
) {
    private val registeredAssets = mutableListOf<RegisteredAsset>()
    private val templateAssets = mutableMapOf<String, RegisteredAsset>()

    val assets: List<RegisteredAsset>
        get() = registeredAssets.toList()

    fun registerTemplate(template: MapViewerUnitTemplate): RegisteredAsset =
        templateAssets.getOrPut(template.assetKey()) {
            registerBitmap(
                bitmap = template.bitmap,
                role = "unit-icon",
                source = template.assetSource,
            )
        }

    private fun registerBitmap(bitmap: Bitmap32, role: String, source: AssetSource?): RegisteredAsset =
        source
            ?.existingFileAsset(role)
            ?: bitmap.toBufferedImage().let { image ->
                if (bitmap.hasTransparency()) {
                    image.fallbackAsset(role = role, originalFilename = source?.originalFilename, forcePng = true)
                } else {
                    image.fallbackAsset(role = role, originalFilename = source?.originalFilename)
                }
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
        val id = uuidGenerator()
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
        val id = uuidGenerator()
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

    private fun MapViewerUnitTemplate.assetKey(): String =
        assetSource?.path ?: bitmap.contentKey()

    private fun Bitmap32.contentKey(): String =
        buildString {
            append(width).append('x').append(height).append(':')
            for (y in 0 until height) {
                for (x in 0 until width) {
                    append(getRgba(x, y).toArgbInt()).append(',')
                }
            }
        }

    private fun Bitmap32.toBufferedImage(): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until height) {
                for (x in 0 until width) {
                    image.setRGB(x, y, getRgba(x, y).toArgbInt())
                }
            }
        }

    private fun Bitmap32.hasTransparency(): Boolean =
        (0 until height).any { y ->
            (0 until width).any { x ->
                getRgba(x, y).a != 255
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
        const val MAX_THUMBNAIL_WIDTH = 256
        const val JPEG_QUALITY = 0.72f
    }
}

private fun korlibs.image.color.RGBA.toArgbInt(): Int =
    (a shl 24) or (r shl 16) or (g shl 8) or b

internal data class RegisteredAsset(
    val id: UUID,
    val path: String,
    val mediaType: String,
    val role: String,
    val originalFilename: String?,
    val bytes: ByteArray,
) {
}
