package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
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
            },
            backward = buildMap {
                if (before.template.name != after.template.name) {
                    put("name", TextNode(before.template.name))
                }
                if (before.template.affiliation != after.template.affiliation) {
                    put("affiliation", before.template.affiliation.toNode())
                }
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

        return snapshot.copy(template = template)
    }

    private fun UnitAffiliation?.toNode(): JsonNode =
        this?.let { TextNode(it.name) } ?: NullNode.instance
}
