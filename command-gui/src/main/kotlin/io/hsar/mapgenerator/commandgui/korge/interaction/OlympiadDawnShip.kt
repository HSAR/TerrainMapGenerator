package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlin.math.floor
import kotlin.random.Random

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
    val fighterSquadronIds: Set<String> = emptySet(),
    val capacity: Int? = null,
)

internal data class OlympiadDawnShipWeapon(
    val id: String?,
    val name: String,
    val arcs: List<String> = emptyList(),
    val beamClass: Int? = null,
    val magazineId: String? = null,
    val pointDefense: Boolean = false,
    val submunition: Boolean = false,
    val salvoRack: Boolean = false,
    val torpedo: Boolean = false,
    val interceptor: Boolean = false,
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

internal data class OlympiadDawnFighterLaunchStatus(
    val destroyed: Boolean,
)

internal data class OlympiadDawnSquadronStatus(
    val carrierId: MapPlacedObjectId,
    val squadronId: String,
    val name: String,
    val count: Int,
    val remaining: Int,
    val weapons: List<OlympiadDawnShipWeapon>,
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
    const val MAGAZINE_PROPERTY_PREFIX = "olympiadDawn.magazine."
    private const val RANDOM_FIGHTER_ID_CHARACTERS = "ABCDEFGHIJKLMNPQRSTUVWXYZ0123456789_"
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
        if (fighters.mapNotNull(OlympiadDawnFighterSquadron::hangar).toSet().size != fighters.count { it.hangar != null }) {
            throw IllegalArgumentException("Only one fighter squadron may be assigned to each hangar")
        }
        val fighterNamesByHangar = fighters.mapNotNull { fighter -> fighter.hangar?.let { it to "${fighter.hangarName} [${fighter.id}]" } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, names) -> names.distinct().joinToString(", ") }
        val fighterIdsByHangar = fighters.mapNotNull { fighter -> fighter.hangar?.let { it to fighter.id } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) -> ids.toSet() }
        val systems = root.path("systems").takeIf(JsonNode::isArray)
            ?.flatMap { system ->
                val id = system.requiredText("id")
                val rawSystemName = system.requiredText("name")
                val capacity = system.path("capacity").takeIf { !it.isMissingNode && !it.isNull }
                    ?.takeIf(JsonNode::isIntegralNumber)?.intValue()?.takeIf { it >= 0 }
                if (rawSystemName in CAPACITY_SYSTEM_NAMES && capacity == null) {
                    throw IllegalArgumentException("${rawSystemName.friendlySystemLabel()} capacity must be a non-negative integer")
                }
                val name = system.friendlySystemName(fighterNamesByHangar[id])
                val count = if (system.path("name").textValue() == "drive") {
                    system.path("thrust").intValue().coerceAtLeast(1)
                } else 1
                (1..count).map { index ->
                    OlympiadDawnShipSystem(
                        id = if (count == 1) id else "$id-$index",
                        name = name,
                        fighterSquadronIds = fighterIdsByHangar[id].orEmpty(),
                        capacity = capacity.takeIf { rawSystemName in CAPACITY_SYSTEM_NAMES },
                    )
                }
            }
            ?: throw IllegalArgumentException("Ship file must contain systems")
        if (systems.map(OlympiadDawnShipSystem::id).toSet().size != systems.size) {
            throw IllegalArgumentException("Ship systems must have unique IDs")
        }
        val magazineIds = systems.filter { it.name.startsWith("Magazine - ") }.map(OlympiadDawnShipSystem::id).toSet()
        val weapons = root.path("weapons").takeIf(JsonNode::isArray)?.map { it.parseWeapon(magazineIds) }.orEmpty()
        val ordnance = root.path("ordnance").takeIf(JsonNode::isArray)?.map { it.parseWeapon(magazineIds) }.orEmpty()
        return OlympiadDawnShipDefinition(
            name = name,
            shipClass = shipClass,
            mass = mass,
            hullPoints = hullPoints,
            hullRows = hullRows,
            armourPoints = armourPoints,
            systems = systems,
            weapons = weapons + ordnance,
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
            definition.systems.filter { it.capacity != null }.forEach { system ->
                put(magazinePropertyKey(system.id), system.capacity.toString())
            }
        }

    fun importedMoveLimit(definition: OlympiadDawnShipDefinition): Double =
        definition.systems.count { it.name == "Impulse Drive" } / 2.0

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
            ?.let { definition ->
                val instanceDefinition = parse(instanceDefinitionJson(definition))
                val instanceDamage = (defaultDamage(template.customProperties) ?: intactDamage()).let { damage ->
                    damage.copy(
                        remainingFighters = definition.fighters.mapIndexedNotNull { index, fighter ->
                            damage.remainingFighters[fighter.id]?.let { remaining -> instanceDefinition.fighters[index].id to remaining }
                        }.toMap(),
                    )
                }
                mapOf(
                    DEFINITION_KEY to instanceDefinition.sourceJson,
                    DAMAGE_KEY to encodeDamage(instanceDamage),
                ) +
                        definition.systems.filter { it.capacity != null }.associate { it.magazineKey() to it.capacity.toString() } +
                        definition.weapons.mapIndexedNotNull { index, weapon ->
                            weapon.takeIf { it.submunition }?.let { magazinePropertyKey(weaponKey(definition, index)) to "1" }
                        }
            }
            .orEmpty()

    fun magazinePropertyKey(id: String): String = "$MAGAZINE_PROPERTY_PREFIX$id"

    fun magazineRemaining(properties: Map<String, String>, system: OlympiadDawnShipSystem): Int =
        system.capacity?.let { capacityRemaining(properties, system.id, it) } ?: 0

    fun capacityRemaining(properties: Map<String, String>, id: String, capacity: Int): Int =
        properties[magazinePropertyKey(id)]?.toIntOrNull()?.coerceIn(0, capacity) ?: capacity

    private fun OlympiadDawnShipSystem.magazineKey(): String = magazinePropertyKey(id)

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
            "ecm" -> "Electronic Countermeasures"
            "sensors" -> "Advanced Sensors"
            "magazine" -> "Magazine - ${path("capacity").intValue()}"
            "mineLayer" -> "Mine Layer - ${path("capacity").intValue()}"
            "bay" -> listOfNotNull(
                path("type").textValue()?.titleCaseWords()?.takeIf(String::isNotEmpty),
                "Bay",
            ).joinToString(" ") + path("capacity").takeIf(JsonNode::isIntegralNumber)?.intValue()?.takeIf { it >= 0 }?.let { " - $it" }.orEmpty()
            else -> requiredText("name")
        }

    private fun String.titleCaseWords(): String =
        trim()
            .split(Regex("[\\s_-]+"))
            .filter(String::isNotEmpty)
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::titlecase) }

    private fun String.friendlySystemLabel(): String =
        when (this) {
            "mineLayer" -> "Mine layer"
            else -> replaceFirstChar(Char::titlecase)
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

    fun fighterLaunchStatuses(
        carrier: UnitSnapshot,
        units: Iterable<UnitSnapshot>,
    ): Map<String, OlympiadDawnFighterLaunchStatus> {
        val snapshots = units.toList()
        val squadronIds = definitionFor(carrier)?.fighters?.mapTo(linkedSetOf(), OlympiadDawnFighterSquadron::id).orEmpty()
        return snapshots.asSequence()
            .filter { it.unitClassLabel() == "F" }
            .mapNotNull { fighter ->
                fighter.customProperties[FIGHTER_UNIT_ID_KEY]?.trim()
                    ?.takeIf { requested -> requested in squadronIds }
                    ?.takeIf { linkedCarrier(fighter, snapshots)?.id == carrier.id }
                    ?.let { it to fighter.destroyed }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, destroyedStates) -> OlympiadDawnFighterLaunchStatus(destroyed = destroyedStates.all { it }) }
    }

    fun hasUniqueFighterLink(
        fighter: UnitSnapshot,
        units: Iterable<UnitSnapshot>,
    ): Boolean = linkedCarrier(fighter, units.toList()) != null

    fun validatedFighterProperties(
        fighter: UnitSnapshot,
        units: Iterable<UnitSnapshot>,
    ): Map<String, String> =
        fighter.customProperties[FIGHTER_UNIT_ID_KEY]?.trim()?.takeIf(String::isNotEmpty)
            ?.takeIf { fighter.unitClassLabel() == "F" }
            ?.takeUnless { hasUniqueFighterLink(fighter, units) }
            ?.let { fighter.customProperties - FIGHTER_UNIT_ID_KEY }
            ?: fighter.customProperties

    fun squadronStatus(
        fighter: UnitSnapshot,
        units: Iterable<UnitSnapshot>,
    ): OlympiadDawnSquadronStatus? {
        val snapshots = units.toList()
        val carrier = linkedCarrier(fighter, snapshots) ?: return null
        val definition = definitionFor(carrier) ?: return null
        val requestedId = fighter.customProperties[FIGHTER_UNIT_ID_KEY]?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val squadronId = definition.fighters.firstOrNull { requestedId == it.id }?.id ?: return null
        val index = definition.fighters.indexOfFirst { it.id == squadronId }.takeIf { it >= 0 } ?: return null
        val squadron = definition.fighters[index]
        return OlympiadDawnSquadronStatus(
            carrierId = carrier.id,
            squadronId = squadron.id,
            name = squadron.name,
            count = squadron.count,
            remaining = remainingFighters(definition, instanceDamage(carrier.customProperties, definition), index),
            weapons = squadron.weapons,
        )
    }

    fun updatedSquadronProperties(
        carrier: UnitSnapshot,
        squadronId: String,
        remaining: Int,
    ): Map<String, String>? {
        val definition = definitionFor(carrier) ?: return null
        val squadron = definition.fighters.firstOrNull { it.id == squadronId } ?: return null
        val damage = instanceDamage(carrier.customProperties, definition)
        return carrier.customProperties + (
                DAMAGE_KEY to encodeDamage(
                    damage.copy(
                        remainingFighters = damage.remainingFighters + (squadron.id to remaining.coerceIn(0, squadron.count)),
                    ),
                )
                )
    }

    fun shipDamage(unit: UnitSnapshot): OlympiadDawnShipDamage? =
        definitionFor(unit)?.let { definition -> instanceDamage(unit.customProperties, definition) }

    private fun linkedCarrier(
        fighter: UnitSnapshot,
        units: List<UnitSnapshot>,
    ): UnitSnapshot? {
        if (fighter.unitClassLabel() != "F") return null
        val squadronId = fighter.customProperties[FIGHTER_UNIT_ID_KEY]?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return units.filter { candidate ->
            definitionFor(candidate)?.fighters.orEmpty().any { squadron ->
                squadronId == squadron.id
            }
        }
            .singleOrNull()
    }

    private fun instanceDefinitionJson(definition: OlympiadDawnShipDefinition): String {
        val root = mapper.readTree(definition.sourceJson)
        root.path("fighters")
            .takeIf(JsonNode::isArray)
            ?.forEach { fighter ->
                fighter.path("hangar").textValue()?.trim()?.takeIf(String::isNotEmpty)
                    ?.let { (fighter as ObjectNode).put("id", randomFighterId()) }
            }
        return mapper.writeValueAsString(root)
    }

    // Persisted fighter IDs deliberately use a compact four-character token without O, never a placement UUID.
    private fun randomFighterId(): String = buildString(4) {
        repeat(4) { append(RANDOM_FIGHTER_ID_CHARACTERS[Random.nextInt(RANDOM_FIGHTER_ID_CHARACTERS.length)]) }
    }

    private fun definitionFor(unit: UnitSnapshot): OlympiadDawnShipDefinition? =
        definition(unit.customProperties) ?: definition(unit.template.customProperties)

    private fun JsonNode.parseWeapon(magazineIds: Set<String>): OlympiadDawnShipWeapon {
        val rawName = requiredText("name")
        val magazineId = path("magazine").textValue()?.trim()?.takeIf(String::isNotEmpty)
        if (rawName == "salvoLauncher" && magazineId !in magazineIds) {
            throw IllegalArgumentException("Salvo launcher magazine must reference a valid magazine")
        }
        return OlympiadDawnShipWeapon(
            id = path("id").textValue()?.trim()?.takeIf(String::isNotEmpty),
            name = when (rawName) {
                "pds" -> "Point Defence System"
                "beam" -> "Class ${path("class").intValue()} Beam Turret"
                "salvoLauncher" -> "Salvo Missile Launcher"
                "salvo" -> "Salvo Missile Rack"
                "submunition" -> "Submunition Rack"
                else -> rawName
            },
            arcs = if (rawName == "pds") ARC_ORDER else parseArcs(),
            beamClass = if (rawName == "beam") {
                path("class").intValue().takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Beam weapon class must be positive")
            } else null,
            magazineId = magazineId,
            pointDefense = rawName == "pds",
            submunition = rawName == "submunition",
            salvoRack = rawName == "salvo",
        )
    }

    private fun fighterProfile(type: String): OlympiadDawnFighterProfile =
        when (type) {
            "standard" -> OlympiadDawnFighterProfile("Fighter", "Fighters", 6, listOf(OlympiadDawnShipWeapon(null, "Light Multirole Beam", ARC_ORDER, 1)))
            "interceptor" -> OlympiadDawnFighterProfile(
                "Interceptor",
                "Interceptors",
                6,
                listOf(OlympiadDawnShipWeapon(null, "Light Interceptor Beam", ARC_ORDER, 1, interceptor = true))
            )

            "torpedo" -> OlympiadDawnFighterProfile(
                "Torpedo Fighter",
                "Torpedo Fighters",
                6,
                listOf(OlympiadDawnShipWeapon(null, "Light Torpedo Launcher", ARC_ORDER, torpedo = true))
            )
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
    private val CAPACITY_SYSTEM_NAMES = setOf("magazine", "mineLayer")

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
