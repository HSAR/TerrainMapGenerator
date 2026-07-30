package io.hsar.mapgenerator.commandgui.save

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.io.lang.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
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
    private val logger = LogManager.getLogger(OperationSaveCatalogue::class.java)

    fun list(directory: Path): List<SavedOperationSummary> {
        val startedAt = System.nanoTime()
        return runBlocking { listInParallel(directory) }
            .also { summaries ->
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                if (elapsedMs >= SLOW_LIST_THRESHOLD_MS) {
                    logger.info("Operation save catalogue listed {} saves in {}ms", summaries.size, elapsedMs)
                }
            }
    }

    private suspend fun listInParallel(directory: Path): List<SavedOperationSummary> =
        coroutineScope {
            Files.list(directory).use { paths ->
                paths.iterator().asSequence()
                    .filter(Files::isRegularFile)
                    .filter { it.fileName.toString().lowercase().endsWith(".zip") }
                    .map { path -> async(Dispatchers.IO) { readSummaryOrNull(path) } }
                    .toList()
            }.awaitAll()
                .filterNotNull()
                .sortedWith(compareByDescending<SavedOperationSummary> { it.updatedAt }.thenByDescending { it.createdAt })
        }

    private fun readSummaryOrNull(path: Path): SavedOperationSummary? =
        runCatching {
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
        }.onFailure { throwable ->
            logger.warn("Failed to read save summary from {}", path, throwable)
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
        const val SLOW_LIST_THRESHOLD_MS = 100L
    }
}
