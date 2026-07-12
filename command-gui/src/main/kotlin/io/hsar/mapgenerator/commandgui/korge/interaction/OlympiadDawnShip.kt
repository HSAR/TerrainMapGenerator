package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

internal data class OlympiadDawnShipDefinition(
    val name: String,
    val shipClass: String,
    val hullPoints: Int,
    val hullRows: Int,
    val armourPoints: Int,
    val systems: List<OlympiadDawnShipSystem>,
    val weapons: List<OlympiadDawnShipWeapon>,
    val sourceJson: String,
)

internal data class OlympiadDawnShipSystem(
    val id: String,
    val name: String,
)

internal data class OlympiadDawnShipWeapon(
    val id: String?,
    val name: String,
)

internal data class OlympiadDawnShipDamage(
    val destroyedArmour: Set<Int> = emptySet(),
    val destroyedHull: Set<Int> = emptySet(),
    val destroyedSystems: Set<String> = emptySet(),
)

internal object OlympiadDawnShip {
    const val DEFINITION_KEY = "olympiadDawn.shipDefinition"
    const val DEFAULT_DAMAGE_KEY = "olympiadDawn.shipDefaultDamage"
    const val DAMAGE_KEY = "olympiadDawn.shipDamage"

    private val mapper = jacksonObjectMapper()

    fun import(file: File): OlympiadDawnShipDefinition =
        parse(file.readText())

    fun parse(source: String): OlympiadDawnShipDefinition {
        val root = mapper.readTree(source).takeIf(JsonNode::isObject)
            ?: throw IllegalArgumentException("Ship file must contain an object")
        val name = root.requiredText("name")
        val shipClass = root.requiredText("class")
        val hull = root.path("hull").takeIf(JsonNode::isObject)
            ?: throw IllegalArgumentException("Ship file must contain hull data")
        val hullPoints = hull.requiredPositiveInt("points")
        val hullRows = hull.requiredPositiveInt("rows")
        val armour = root.path("armour").takeIf(JsonNode::isArray)
            ?: throw IllegalArgumentException("Ship file must contain armour data")
        val armourPoints = armour.firstOrNull()
            ?.takeIf(JsonNode::isArray)
            ?.firstOrNull()
            ?.takeIf(JsonNode::isInt)
            ?.intValue()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Ship armour must begin with a non-negative point count")
        val systems = root.path("systems").takeIf(JsonNode::isArray)
            ?.map { system ->
                OlympiadDawnShipSystem(
                    id = system.requiredText("id"),
                    name = system.requiredText("name"),
                )
            }
            ?: throw IllegalArgumentException("Ship file must contain systems")
        if (systems.map(OlympiadDawnShipSystem::id).toSet().size != systems.size) {
            throw IllegalArgumentException("Ship systems must have unique IDs")
        }
        val weapons = root.path("weapons").takeIf(JsonNode::isArray)
            ?.map { weapon ->
                OlympiadDawnShipWeapon(
                    id = weapon.path("id").textValue()?.trim()?.takeIf(String::isNotEmpty),
                    name = weapon.requiredText("name"),
                )
            }
            .orEmpty()
        return OlympiadDawnShipDefinition(
            name = name,
            shipClass = shipClass,
            hullPoints = hullPoints,
            hullRows = hullRows,
            armourPoints = armourPoints,
            systems = systems,
            weapons = weapons,
            sourceJson = mapper.writeValueAsString(root),
        )
    }

    fun definition(properties: Map<String, String>): OlympiadDawnShipDefinition? =
        properties[DEFINITION_KEY]?.let { source -> runCatching { parse(source) }.getOrNull() }

    fun defaultDamage(properties: Map<String, String>): OlympiadDawnShipDamage? =
        definition(properties)?.let { definition ->
            properties[DEFAULT_DAMAGE_KEY]
                ?.let { source -> decodeDamage(source, definition) }
                ?: intactDamage()
        }

    fun instanceDamage(properties: Map<String, String>, definition: OlympiadDawnShipDefinition): OlympiadDawnShipDamage =
        properties[DAMAGE_KEY]?.let { source -> decodeDamage(source, definition) } ?: intactDamage()

    fun importedTemplateProperties(
        properties: Map<String, String>,
        definition: OlympiadDawnShipDefinition,
    ): Map<String, String> =
        properties + mapOf(
            DEFINITION_KEY to definition.sourceJson,
            DEFAULT_DAMAGE_KEY to encodeDamage(intactDamage()),
        )

    fun instanceProperties(template: MapViewerUnitTemplate): Map<String, String> =
        template.customProperties
            .let(::definition)
            ?.let { definition -> mapOf(DAMAGE_KEY to encodeDamage(defaultDamage(template.customProperties) ?: intactDamage())) }
            .orEmpty()

    fun encodeDamage(damage: OlympiadDawnShipDamage): String =
        mapper.writeValueAsString(
            mapOf(
                "destroyedArmour" to damage.destroyedArmour.sorted(),
                "destroyedHull" to damage.destroyedHull.sorted(),
                "destroyedSystems" to damage.destroyedSystems.sorted(),
            ),
        )

    fun decodeDamage(source: String, definition: OlympiadDawnShipDefinition): OlympiadDawnShipDamage =
        runCatching {
            val root = mapper.readTree(source)
            OlympiadDawnShipDamage(
                destroyedArmour = root.indexSet("destroyedArmour", definition.armourPoints),
                destroyedHull = root.indexSet("destroyedHull", definition.hullPoints),
                destroyedSystems = root.path("destroyedSystems")
                    .takeIf(JsonNode::isArray)
                    ?.mapNotNull(JsonNode::textValue)
                    ?.filter { it in definition.systems.map(OlympiadDawnShipSystem::id) }
                    ?.toSet()
                    .orEmpty(),
            )
        }.getOrDefault(intactDamage())

    private fun intactDamage(): OlympiadDawnShipDamage = OlympiadDawnShipDamage()

    private fun JsonNode.requiredText(field: String): String =
        path(field).textValue()?.trim()?.takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("Ship $field is required")

    private fun JsonNode.requiredPositiveInt(field: String): Int =
        path(field).takeIf(JsonNode::isInt)?.intValue()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("Ship hull $field must be positive")

    private fun JsonNode.indexSet(field: String, limit: Int): Set<Int> =
        path(field)
            .takeIf(JsonNode::isArray)
            ?.mapNotNull { it.takeIf(JsonNode::isInt)?.intValue() }
            ?.filter { it in 0 until limit }
            ?.toSet()
            .orEmpty()
}
