package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*

internal data class LoadedOperation(
    val operationId: UUID,
    val operationName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val settings: MapViewerSettings,
    val camera: CameraSaveDto?,
    val mapImage: BufferedImage,
    val mapAssetSource: AssetSource? = null,
    val ledgerEntries: List<LedgerEntry>,
    val generatedImageAssets: List<GeneratedImageAsset> = emptyList(),
    val additionalDataJson: ByteArray? = null,
)
