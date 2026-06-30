package io.hsar.mapgenerator.commandgui

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal object CustomPropertyJson {
    private val objectMapper = jacksonObjectMapper()

    fun toStringValue(node: JsonNode?): String =
        when {
            node == null -> "null"
            node.isTextual -> node.asText()
            else -> objectMapper.writeValueAsString(node)
        }

    fun toNode(value: String): JsonNode =
        TextNode(value)

    fun diff(
        before: Map<String, String>,
        after: Map<String, String>,
        reservedKeys: Set<String>,
    ): Map<String, JsonNode?> =
        (before.keys + after.keys)
            .filterNot { it in reservedKeys }
            .sorted()
            .mapNotNull { key ->
                when {
                    before[key] == after[key] -> null
                    key in after -> key to toNode(after.getValue(key))
                    else -> key to null
                }
            }
            .toMap()
}
