package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.MapViewerSaveRepository
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.util.*

internal data class SavedOperationSummary(
    val path: Path,
    val operationId: UUID,
    val operationName: String,
    val updatedAt: Instant,
    val createdAt: Instant,
    val thumbnailAssetId: UUID?,
    val thumbnail: BufferedImage?,
)

internal class OperationSaveCatalogue {
    private val logger = LogManager.getLogger(OperationSaveCatalogue::class.java)

    fun list(directory: Path): List<SavedOperationSummary> {
        val startedAt = System.nanoTime()
        return MapViewerSaveRepository(directory).list()
            .map { summary ->
                SavedOperationSummary(
                    path = summary.path,
                    operationId = summary.operationId,
                    operationName = summary.operationName,
                    updatedAt = summary.updatedAt,
                    createdAt = summary.createdAt,
                    thumbnailAssetId = summary.thumbnailAssetId,
                    thumbnail = summary.thumbnail,
                )
            }
            .also { summaries ->
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                if (elapsedMs >= SLOW_LIST_THRESHOLD_MS) {
                    logger.info("Operation save catalogue listed {} saves in {}ms", summaries.size, elapsedMs)
                }
            }
    }

    private companion object {
        const val SLOW_LIST_THRESHOLD_MS = 100L
    }
}
