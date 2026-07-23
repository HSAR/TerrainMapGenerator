package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.AssetSource
import korlibs.image.bitmap.Bitmap32

internal data class MapViewerGraphicTemplate(
    val id: GraphicTemplateId,
    val name: String,
    val bitmap: Bitmap32,
    val assetSource: AssetSource? = null,
    val customProperties: Map<String, String> = emptyMap(),
) {
    fun normalizedSize(): GraphicTemplateSize? =
        customProperties[GRAPHIC_WIDTH_PROPERTY]?.toDoubleOrNull()
            ?.let { width ->
                customProperties[GRAPHIC_HEIGHT_PROPERTY]?.toDoubleOrNull()
                    ?.let { height -> GraphicTemplateSize(width, height) }
            }

    fun withNormalizedSize(width: Double, height: Double): MapViewerGraphicTemplate =
        copy(
            customProperties = customProperties + mapOf(
                GRAPHIC_WIDTH_PROPERTY to width.toString(),
                GRAPHIC_HEIGHT_PROPERTY to height.toString(),
            ),
        )
}

internal data class GraphicTemplateSize(
    val width: Double,
    val height: Double,
)

internal data class GraphicTemplateEditPatch(
    val forward: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
    val backward: Map<String, com.fasterxml.jackson.databind.JsonNode?>,
)

private const val GRAPHIC_WIDTH_PROPERTY = "normalizedWidth"
private const val GRAPHIC_HEIGHT_PROPERTY = "normalizedHeight"
