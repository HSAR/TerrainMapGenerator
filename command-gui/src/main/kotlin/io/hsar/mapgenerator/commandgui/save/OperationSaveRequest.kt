package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*

internal data class OperationSaveRequest(
    val operationId: UUID,
    val operationName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val camera: CameraViewport,
    val settings: MapViewerSettings,
    val mapImage: BufferedImage,
    val thumbnailImage: BufferedImage? = null,
    val mapAssetSource: AssetSource?,
    val ledgerEntries: List<LedgerEntry>,
    val generatedImageAssets: List<GeneratedImageAsset> = emptyList(),
)

internal data class GeneratedImageAsset(
    val id: UUID,
    val role: String,
    val image: BufferedImage,
)
