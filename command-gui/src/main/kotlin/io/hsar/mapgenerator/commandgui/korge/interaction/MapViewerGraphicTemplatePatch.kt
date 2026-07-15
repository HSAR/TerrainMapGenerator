package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import java.util.*

internal object MapViewerGraphicTemplatePatch {
    fun diff(before: GraphicTemplateSnapshot, after: GraphicTemplateSnapshot): GraphicTemplateEditPatch =
        GraphicTemplateEditPatch(
            forward = patch(before, after),
            backward = patch(after, before),
        )

    fun apply(
        snapshot: GraphicTemplateSnapshot,
        changes: Map<String, JsonNode?>,
        resolveTemplateByAssetId: ((UUID) -> MapViewerGraphicTemplate)? = null,
    ): GraphicTemplateSnapshot {
        var template = snapshot.template
        changes["assetId"]
            ?.takeUnless(JsonNode::isNull)
            ?.asText()
            ?.let(UUID::fromString)
            ?.let { resolveTemplateByAssetId?.invoke(it) }
            ?.let { replacement -> template = template.copy(bitmap = replacement.bitmap, assetSource = replacement.assetSource) }
        changes["name"]?.takeUnless(JsonNode::isNull)?.asText()?.let { template = template.copy(name = it) }
        template = changes.entries
            .filterNot { (key, _) -> key in GRAPHIC_TEMPLATE_FIELDS }
            .fold(template) { current, (key, value) ->
                current.copy(customProperties = current.customProperties.applyCustomPropertyPatch(key, value))
            }
        return snapshot.copy(template = template)
    }

    private fun patch(before: GraphicTemplateSnapshot, after: GraphicTemplateSnapshot): Map<String, JsonNode?> = buildMap {
        if (before.template.name != after.template.name) put("name", TextNode(after.template.name))
        putAll(CustomPropertyJson.diff(before.template.customProperties, after.template.customProperties, GRAPHIC_TEMPLATE_FIELDS))
    }

    private fun Map<String, String>.applyCustomPropertyPatch(key: String, value: JsonNode?): Map<String, String> =
        if (value == null || value.isNull) this - key else this + (key to CustomPropertyJson.toStringValue(value))

    private val GRAPHIC_TEMPLATE_FIELDS = setOf("name", "assetId")
}
