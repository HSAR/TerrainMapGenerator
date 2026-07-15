package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.AssetSource
import korlibs.image.bitmap.Bitmap32

internal data class MapViewerGraphicTemplate(
    val id: GraphicTemplateId,
    val name: String,
    val bitmap: Bitmap32,
    val assetSource: AssetSource? = null,
    val customProperties: Map<String, String> = emptyMap(),
)

internal data class GraphicTemplateEditPatch(
    val forward: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
    val backward: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
)
