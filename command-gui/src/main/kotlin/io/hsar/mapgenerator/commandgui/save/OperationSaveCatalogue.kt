package io.hsar.mapgenerator.commandgui.save

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

internal data class SavedOperationSummary(
    val path: Path,
    val operationId: UUID,
    val operationName: String,
    val updatedAt: Instant,
    val createdAt: Instant,
    val thumbnailAssetId: UUID?,
    val thumbnail: Bitmap32?,
)

internal class OperationSaveCatalogue {
    fun list(directory: Path): List<SavedOperationSummary> =
        Files.list(directory).use { paths ->
            paths.iterator().asSequence()
                .filter(Files::isRegularFile)
                .filter { it.fileName.toString().lowercase().endsWith(".zip") }
                .mapNotNull(::readSummaryOrNull)
                .sortedWith(compareByDescending<SavedOperationSummary> { it.updatedAt }.thenByDescending { it.createdAt })
                .toList()
        }

    private fun readSummaryOrNull(path: Path): SavedOperationSummary? =
        runCatching {
            OperationSaveLoader().load(path)
            ZipFile(path.toFile()).use { zip ->
                val game = zip.getEntry(GAME_JSON)
                    ?.let(zip::getInputStream)
                    ?.use { OperationSaveJson.objectMapper.readValue(it, GameSaveDto::class.java) }
                    ?: return null
                val thumbnailId = game.thumbnailAssetId?.let(UUID::fromString)
                SavedOperationSummary(
                    path = path,
                    operationId = UUID.fromString(game.id),
                    operationName = game.name,
                    updatedAt = Instant.parse(game.updatedAt),
                    createdAt = Instant.parse(game.createdAt),
                    thumbnailAssetId = thumbnailId,
                    thumbnail = thumbnailId?.let { zip.loadThumbnail(it) },
                )
            }
        }.getOrNull()

    private fun ZipFile.loadThumbnail(assetId: UUID): Bitmap32? =
        entries().asSequence()
            .filter { !it.isDirectory }
            .firstOrNull { entry ->
                val stem = entry.name.substringAfterLast('/').substringBeforeLast('.', missingDelimiterValue = "")
                stem == assetId.toString()
            }
            ?.let { entry ->
                getInputStream(entry).use(ImageIO::read)
                    ?.toThumbnailBitmap()
            }

    private fun BufferedImage.toThumbnailBitmap(): Bitmap32 =
        Bitmap32(width, height) { x, y ->
            val argb = getRGB(x, y)
            RGBA(
                (argb ushr 16) and 0xFF,
                (argb ushr 8) and 0xFF,
                argb and 0xFF,
                (argb ushr 24) and 0xFF,
            )
        }.premultipliedIfRequired()

    private companion object {
        const val GAME_JSON = "game.json"
    }
}
