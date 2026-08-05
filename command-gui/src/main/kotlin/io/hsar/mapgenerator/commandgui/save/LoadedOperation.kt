package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSavedCamera
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.operation.OperationGeneratedImage
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import java.time.Instant
import java.util.*
import java.util.Collections.emptyList

internal data class LoadedOperation(
    val operationId: UUID,
    val operationName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val settings: MapViewerSettings,
    val camera: MapViewerSavedCamera?,
    val renderData: MapViewerRenderData,
    val mapAssetSource: AssetSource? = null,
    val timeline: OperationTimeline,
    val generatedImageAssets: List<OperationGeneratedImage> = emptyList(),
    val additionalDataJson: ByteArray? = null,
)
