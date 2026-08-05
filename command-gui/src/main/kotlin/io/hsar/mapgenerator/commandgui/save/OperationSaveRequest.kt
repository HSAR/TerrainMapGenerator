package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.operation.OperationGeneratedImage
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*
import java.util.Collections.emptyList

internal data class OperationSaveRequest(
    val operationId: UUID,
    val operationName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val camera: MapViewerSavedCamera,
    val settings: MapViewerSettings,
    val mapImage: BufferedImage,
    val thumbnailImage: BufferedImage? = null,
    val mapAssetSource: AssetSource?,
    val timeline: OperationTimeline,
    val generatedImageAssets: List<OperationGeneratedImage> = emptyList(),
    val additionalDataJson: ByteArray? = null,
)
