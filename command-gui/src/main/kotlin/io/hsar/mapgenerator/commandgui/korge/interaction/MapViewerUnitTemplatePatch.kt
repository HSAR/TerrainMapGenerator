package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.CustomPropertyJson
import java.util.*

internal data class UnitTemplateEditPatch(
    val forward: Map<String, JsonNode?>,
    val backward: Map<String, JsonNode?>,
)

internal object MapViewerUnitTemplatePatch {
    fun diff(before: UnitTemplateSnapshot, after: UnitTemplateSnapshot): UnitTemplateEditPatch =
        UnitTemplateEditPatch(
            forward = buildMap {
                if (before.template.name != after.template.name) {
                    put("name", TextNode(after.template.name))
                }
                if (before.template.affiliation != after.template.affiliation) {
                    put("affiliation", after.template.affiliation.toNode())
                }
                putAll(
                    CustomPropertyJson.diff(
                        before = before.template.customProperties,
                        after = after.template.customProperties,
                        reservedKeys = UNIT_TEMPLATE_PATCH_FIELDS,
                    ),
                )
            },
            backward = buildMap {
                if (before.template.name != after.template.name) {
                    put("name", TextNode(before.template.name))
                }
                if (before.template.affiliation != after.template.affiliation) {
                    put("affiliation", before.template.affiliation.toNode())
                }
                putAll(
                    CustomPropertyJson.diff(
                        before = after.template.customProperties,
                        after = before.template.customProperties,
                        reservedKeys = UNIT_TEMPLATE_PATCH_FIELDS,
                    ),
                )
            },
        )

    fun apply(
        snapshot: UnitTemplateSnapshot,
        changes: Map<String, JsonNode?>,
        resolveTemplateByAssetId: ((UUID) -> MapViewerUnitTemplate)? = null,
    ): UnitTemplateSnapshot {
        var template = snapshot.template
        val assetId = changes["assetId"]
            ?.takeUnless { node -> node.isNull }
            ?.asText()
            ?.let(UUID::fromString)
        if (assetId != null && resolveTemplateByAssetId != null) {
            val replacement = resolveTemplateByAssetId.invoke(assetId)
            template = template.copy(
                bitmap = replacement.bitmap,
                assetSource = replacement.assetSource,
            )
        }

        changes["name"]
            ?.takeUnless { node -> node.isNull }
            ?.asText()
            ?.let { template = template.copy(name = it) }

        changes["affiliation"]
            ?.let { node ->
                template = if (node.isNull) {
                    template.copy(affiliation = null)
                } else {
                    template.copy(affiliation = UnitAffiliation.valueOf(node.asText()))
                }
            }

        template = changes.entries
            .filterNot { (key, _) -> key in UNIT_TEMPLATE_PATCH_FIELDS }
            .fold(template) { current, (key, value) ->
                current.copy(customProperties = current.customProperties.applyCustomPropertyPatch(key, value))
            }

        return snapshot.copy(template = template)
    }

    private fun UnitAffiliation?.toNode(): JsonNode =
        this?.let { TextNode(it.name) } ?: NullNode.instance

    private fun Map<String, String>.applyCustomPropertyPatch(key: String, value: JsonNode?): Map<String, String> =
        if (value == null || value.isNull) {
            this - key
        } else {
            this + (key to CustomPropertyJson.toStringValue(value))
        }

    private val UNIT_TEMPLATE_PATCH_FIELDS = setOf("name", "affiliation", "assetId")
}
