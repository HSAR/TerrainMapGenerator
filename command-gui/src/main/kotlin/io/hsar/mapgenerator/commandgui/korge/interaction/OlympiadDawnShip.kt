package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlin.math.floor

internal data class OlympiadDawnShipDefinition(
    val name: String,
    val shipClass: String,
    val mass: Int?,
    val hullPoints: Int,
    val hullRows: Int,
    val armourPoints: Int?,
    val systems: List<OlympiadDawnShipSystem>,
    val weapons: List<OlympiadDawnShipWeapon>,
    val fighters: List<OlympiadDawnFighterSquadron>,
    val sourceJson: String,
)

internal data class OlympiadDawnShipSystem(
    val id: String,
    val name: String,
)

internal data class OlympiadDawnShipWeapon(
    val id: String?,
    val name: String,
    val arcs: List<String> = emptyList(),
    val beamClass: Int? = null,
)

internal data class OlympiadDawnFighterSquadron(
    val id: String,
    val name: String,
    val hangarName: String,
    val hangar: String?,
    val count: Int,
    val weapons: List<OlympiadDawnShipWeapon>,
)

internal data class OlympiadDawnFighterWeaponTotal(
    val name: String,
    val arcs: List<String>,
    val count: Int,
)

internal object OlympiadDawnBeamRules {
    const val RANGE_BAND_SIZE_U = 6.0

    fun firepowerAtRange(beamClass: Int, rangeU: Double): Int {
        if (beamClass <= 0 || rangeU < 0.0) return 0
        val rangeBand = floor(rangeU / RANGE_BAND_SIZE_U).toInt() + 1
        return (beamClass - rangeBand + 1).coerceAtLeast(0)
    }

    fun maximumRangeU(beamClass: Int): Double =
        beamClass.coerceAtLeast(0) * RANGE_BAND_SIZE_U
}

internal data class OlympiadDawnShipDamage(
    val destroyedArmour: Set<Int> = emptySet(),
    val destroyedHull: Set<Int> = emptySet(),
    val destroyedSystems: Set<String> = emptySet(),
    val destroyedWeapons: Set<String> = emptySet(),
    val remainingFighters: Map<String, Int> = emptyMap(),
)

internal object OlympiadDawnShip {
    const val DEFINITION_KEY = "olympiadDawn.shipDefinition"
    const val DEFAULT_DAMAGE_KEY = "olympiadDawn.shipDefaultDamage"
    const val DAMAGE_KEY = "olympiadDawn.shipDamage"
    const val UNIT_CLASS_KEY = "unitClass"
    const val FIGHTER_UNIT_ID_KEY = "unitId"
    const val MASS_KEY = "mass"

    private val mapper = jacksonObjectMapper()

    fun import(file: File): OlympiadDawnShipDefinition =
        parse(file.readText())

    fun parse(source: String): OlympiadDawnShipDefinition {
        val root = mapper.readTree(source).takeIf(JsonNode::isObject)
            ?: throw IllegalArgumentException("Ship file must contain an object")
        val name = root.requiredText("name")
        val shipClass = root.requiredText("class")
        val mass = root.path("mass").takeUnless { it.isMissingNode || it.isNull }?.let {
            if (!it.isIntegralNumber || it.intValue() < 0) {
                throw IllegalArgumentException("Ship mass must be a non-negative integer")
            }
            it.intValue()
        }
        val hull = root.path("hull").takeIf(JsonNode::isObject)
            ?: throw IllegalArgumentException("Ship file must contain hull data")
        val hullPoints = hull.requiredPositiveInt("points")
        val hullRows = hull.requiredPositiveInt("rows")
        val armour = root.path("armour").takeIf(JsonNode::isArray)
            ?: throw IllegalArgumentException("Ship file must contain armour data")
        val armourPoints = if (armour.isEmpty()) null else armour.firstOrNull()
            ?.takeIf(JsonNode::isArray)
            ?.firstOrNull()
            ?.takeIf(JsonNode::isInt)
            ?.intValue()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Ship armour must begin with a non-negative point count")
        val fighters = root.path("fighters")
            .takeIf(JsonNode::isArray)
            ?.map { fighter ->
                val profile = fighterProfile(fighter.requiredText("type"))
                OlympiadDawnFighterSquadron(
                    id = fighter.requiredText("id"),
                    name = profile.name,
                    hangarName = profile.hangarName,
                    hangar = fighter.path("hangar").textValue()?.trim()?.takeIf(String::isNotEmpty),
                    count = profile.count,
                    weapons = profile.weapons,
                )
            }
            .orEmpty()
        if (fighters.map(OlympiadDawnFighterSquadron::id).toSet().size != fighters.size) {
            throw IllegalArgumentException("Fighter squadrons must have unique IDs")
        }
        val fighterNamesByHangar = fighters.mapNotNull { fighter -> fighter.hangar?.let { it to "${fighter.hangarName} [${fighter.id}]" } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, names) -> names.distinct().joinToString(", ") }
        val systems = root.path("systems").takeIf(JsonNode::isArray)
            ?.flatMap { system ->
                val id = system.requiredText("id")
                val name = system.friendlySystemName(fighterNamesByHangar[id])
                val count = if (system.path("name").textValue() == "drive") {
                    system.path("thrust").intValue().coerceAtLeast(1)
                } else 1
                (1..count).map { index ->
                    OlympiadDawnShipSystem(
                        id = if (count == 1) id else "$id-$index",
                        name = name,
                    )
                }
            }
            ?: throw IllegalArgumentException("Ship file must contain systems")
        if (systems.map(OlympiadDawnShipSystem::id).toSet().size != systems.size) {
            throw IllegalArgumentException("Ship systems must have unique IDs")
        }
        val weapons = root.path("weapons").takeIf(JsonNode::isArray)
            ?.map { it.parseWeapon() }
            .orEmpty()
        return OlympiadDawnShipDefinition(
            name = name,
            shipClass = shipClass,
            mass = mass,
            hullPoints = hullPoints,
            hullRows = hullRows,
            armourPoints = armourPoints,
            systems = systems,
            weapons = weapons,
            fighters = fighters,
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
        (properties - UNIT_CLASS_KEY - MASS_KEY) + mapOf(
            DEFINITION_KEY to definition.sourceJson,
            DEFAULT_DAMAGE_KEY to encodeDamage(intactDamage()),
        ) + buildMap {
            unitClassFor(definition.shipClass)?.let { put(UNIT_CLASS_KEY, it) }
            definition.mass?.let { put(MASS_KEY, it.toString()) }
        }

    private fun unitClassFor(shipClass: String): String? {
        val normalized = shipClass.trim().lowercase()
        return when {
            "cargo ship" in normalized -> "AK"
            normalized == "escort cruiser" -> "CA"
            else -> SENSOR_UNIT_CLASS_NAMES.entries
                .firstOrNull { (_, friendlyName) -> friendlyName.lowercase() == normalized }
                ?.key
        }
    }

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
                "destroyedWeapons" to damage.destroyedWeapons.sorted(),
                "remainingFighters" to damage.remainingFighters.toSortedMap(),
            ),
        )

    fun decodeDamage(source: String, definition: OlympiadDawnShipDefinition): OlympiadDawnShipDamage =
        runCatching {
            val root = mapper.readTree(source)
            val fighterCounts = definition.fighters.associate { fighter -> fighterKey(fighter) to fighter.count }
            OlympiadDawnShipDamage(
                destroyedArmour = root.indexSet("destroyedArmour", definition.armourPoints ?: 0),
                destroyedHull = root.indexSet("destroyedHull", definition.hullPoints),
                destroyedSystems = root.path("destroyedSystems")
                    .takeIf(JsonNode::isArray)
                    ?.mapNotNull(JsonNode::textValue)
                    ?.filter { it in definition.systems.map(OlympiadDawnShipSystem::id) }
                    ?.toSet()
                    .orEmpty(),
                destroyedWeapons = root.path("destroyedWeapons")
                    .takeIf(JsonNode::isArray)
                    ?.mapNotNull(JsonNode::textValue)
                    ?.filter { it in definition.weapons.indices.map { index -> weaponKey(definition, index) } }
                    ?.toSet()
                    .orEmpty(),
                remainingFighters = root.path("remainingFighters")
                    .takeIf(JsonNode::isObject)
                    ?.fields()
                    ?.asSequence()
                    ?.mapNotNull { (key, value) -> fighterCounts[key]?.let { count -> key to value.intValue().coerceIn(0, count) } }
                    ?.toMap()
                    .orEmpty(),
            )
        }.getOrDefault(intactDamage())

    private fun intactDamage(): OlympiadDawnShipDamage = OlympiadDawnShipDamage()

    private fun JsonNode.requiredText(field: String): String =
        path(field).textValue()?.trim()?.takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("Ship $field is required")

    private fun JsonNode.friendlySystemName(hangarFighterName: String?): String =
        when (path("name").textValue()) {
            "fireControl" -> "Gunnery Fire Control System"
            "adfc" -> "Area Defence Fire Control System"
            "ftl" -> "Warp Drive"
            "drive" -> "Impulse Drive"
            "pds" -> "Point Defence System"
            "screen" -> "Shields"
            "hangar" -> hangarFighterName?.let { "Hangar - $it" } ?: "Hangar"
            else -> requiredText("name")
        }

    private fun JsonNode.parseArcs(): List<String> {
        val leftArc = path("leftArc").textValue()?.trim()?.takeIf(String::isNotEmpty) ?: return emptyList()
        val start = ARC_ORDER.indexOf(leftArc)
        if (start < 0) throw IllegalArgumentException("Unknown weapon leftArc: $leftArc")
        val count = path("numArcs").intValue().takeIf { it in 1..ARC_ORDER.size }
            ?: throw IllegalArgumentException("Weapon numArcs must be between 1 and ${ARC_ORDER.size}")
        return List(count) { offset -> ARC_ORDER[(start + offset) % ARC_ORDER.size] }
    }

    fun weaponKey(definition: OlympiadDawnShipDefinition, index: Int): String =
        definition.weapons[index].id ?: "weapon-$index"

    fun fighterKey(fighter: OlympiadDawnFighterSquadron): String = fighter.id

    fun remainingFighters(definition: OlympiadDawnShipDefinition, damage: OlympiadDawnShipDamage, index: Int): Int {
        val fighter = definition.fighters[index]
        return damage.remainingFighters[fighterKey(fighter)]?.coerceIn(0, fighter.count) ?: fighter.count
    }

    fun fighterWeaponTotals(definition: OlympiadDawnShipDefinition, damage: OlympiadDawnShipDamage): List<OlympiadDawnFighterWeaponTotal> =
        definition.fighters.flatMapIndexed { index, fighter ->
            fighter.weapons.map { weapon -> Triple(weapon.name, weapon.arcs, remainingFighters(definition, damage, index)) }
        }
            .groupBy { (name, arcs, _) -> name to arcs }
            .map { (weapon, mounts) -> OlympiadDawnFighterWeaponTotal(weapon.first, weapon.second, mounts.sumOf { it.third }) }

    fun deployedFighterCounts(units: Iterable<UnitSnapshot>): Map<String, Int> =
        units.asSequence()
            .filter { it.unitClassLabel() == "F" }
            .mapNotNull { unit -> unit.customProperties[FIGHTER_UNIT_ID_KEY]?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()

    private fun JsonNode.parseWeapon(): OlympiadDawnShipWeapon {
        val rawName = requiredText("name")
        return OlympiadDawnShipWeapon(
            id = path("id").textValue()?.trim()?.takeIf(String::isNotEmpty),
            name = when (rawName) {
                "pds" -> "Point Defence System"
                "beam" -> "Beam Turret Class ${path("class").intValue()}"
                else -> rawName
            },
            arcs = parseArcs(),
            beamClass = if (rawName == "beam") {
                path("class").intValue().takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Beam weapon class must be positive")
            } else null,
        )
    }

    private fun fighterProfile(type: String): OlympiadDawnFighterProfile =
        when (type) {
            "standard" -> OlympiadDawnFighterProfile("Fighter", "Fighters", 6, listOf(OlympiadDawnShipWeapon(null, "Beam Turret Class 1", beamClass = 1)))
            "interceptor" -> OlympiadDawnFighterProfile("Interceptor", "Interceptors", 6, listOf(OlympiadDawnShipWeapon(null, "Interception Beam Class 1", beamClass = 1)))
            "torpedo" -> OlympiadDawnFighterProfile("Torpedo Fighter", "Torpedo Fighters", 6, listOf(OlympiadDawnShipWeapon(null, "Torpedo Launcher")))
            "assault" -> OlympiadDawnFighterProfile("Assault Shuttle", "Assault Shuttles", 3, emptyList())
            else -> throw IllegalArgumentException("Unknown fighter type: $type")
        }

    private data class OlympiadDawnFighterProfile(
        val name: String,
        val hangarName: String,
        val count: Int,
        val weapons: List<OlympiadDawnShipWeapon>,
    )

    fun beamFirepowerAtRange(weapon: OlympiadDawnShipWeapon, rangeU: Double): Int =
        weapon.beamClass?.let { OlympiadDawnBeamRules.firepowerAtRange(it, rangeU) } ?: 0

    fun totalBeamFirepowerAtRange(weapons: List<OlympiadDawnShipWeapon>, rangeU: Double): Int =
        weapons.sumOf { beamFirepowerAtRange(it, rangeU) }

    private val ARC_ORDER = listOf("F", "FS", "AS", "A", "AP", "FP")

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
