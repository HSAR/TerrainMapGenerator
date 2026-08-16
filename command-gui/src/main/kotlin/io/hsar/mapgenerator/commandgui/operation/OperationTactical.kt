package io.hsar.mapgenerator.commandgui.operation

import kotlin.math.atan2
import kotlin.math.floor
import kotlin.random.Random

internal data class OperationTacticalWeaponStatus(
    val key: String,
    val name: String,
    val beamClass: Int?,
    val salvo: Boolean = false,
    val magazineId: String? = null,
    val magazineRemaining: Int? = null,
    val quantity: Int = 1,
    val pointDefense: Boolean = false,
    val submunition: Boolean = false,
    val salvoRack: Boolean = false,
    val torpedo: Boolean = false,
    val interceptor: Boolean = false,
    val requiresFireControl: Boolean = false,
    val arcs: List<String>,
    val destroyed: Boolean,
    val fired: Boolean,
    val fighterWeapon: Boolean = false,
    val ecm: Boolean = false,
)

internal data class OperationTacticalWeaponContribution(val weaponKey: String, val weaponName: String, val arc: String, val firepower: Int)

internal data class OperationTacticalTarget(
    val targetId: OperationObjectId,
    val targetName: String,
    val rangeU: Double,
    val arcs: List<String>,
    val firepower: Int,
    val contributions: List<OperationTacticalWeaponContribution>,
    val manualEcm: Boolean = false,
    val shipClass: String? = null,
)

internal data class OperationTacticalTargetingReport(
    val weapons: List<OperationTacticalWeaponStatus>,
    val targets: List<OperationTacticalTarget>,
    val fireControlCapacity: Int = 0,
    val fireControlUsed: Int = 0,
)

internal object OperationTacticalTargetingService {
    val arcOrder = listOf("F", "FS", "AS", "A", "AP", "FP")

    fun arcForBearing(bearingDegrees: Double, facingDegrees: Int): String =
        arcOrder[floor((bearingDegrees - facingDegrees).mod(360.0).plus(30.0) / 60.0).toInt().mod(arcOrder.size)]

    fun resolve(source: OperationUnit, query: OperationUnitQueryService, firedWeaponKeys: Set<String> = emptySet(), fireControlUsed: Int = 0): OperationTacticalTargetingReport {
        val content = query.content
        val definition = OlympiadDawnShip.definitionFor(source, content)
        val squadron = if (definition == null) OlympiadDawnShip.squadronStatus(source, content) else null
        if (definition == null && squadron == null) return OperationTacticalTargetingReport(emptyList(), emptyList())
        val damage = definition?.let { OlympiadDawnShip.instanceDamage(source.customProperties, it) }
        val fireControlCapacity = definition?.systems?.count { it.name == "Gunnery Fire Control System" && it.id !in damage!!.destroyedSystems } ?: 0
        val shipWeapons = definition?.weapons.orEmpty().mapIndexedNotNull { index, weapon ->
            weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                val key = OlympiadDawnShip.weaponKey(definition!!, index)
                val magazine = weapon.magazineId?.let { id -> definition.systems.firstOrNull { it.id == id } }
                OperationTacticalWeaponStatus(
                    key, weapon.name, weapon.beamClass, weapon.magazineId != null, weapon.magazineId,
                    when {
                        magazine != null -> OlympiadDawnShip.magazineRemaining(source.customProperties, magazine)
                        weapon.submunition || weapon.salvoRack -> if (key in firedWeaponKeys) 0 else 1
                        else -> null
                    }, 1, weapon.pointDefense, weapon.submunition, weapon.salvoRack, weapon.torpedo, weapon.interceptor,
                    !weapon.pointDefense && !weapon.submunition, weapon.arcs, key in damage!!.destroyedWeapons, key in firedWeaponKeys,
                )
            }
        }
        val ecmSystems = definition?.systems.orEmpty().filter { it.name == "Electronic Countermeasures" }.map { system ->
            val key = "ecm-${system.id}"
            OperationTacticalWeaponStatus(key, system.name, null, arcs = arcOrder, destroyed = system.id in damage!!.destroyedSystems, fired = key in firedWeaponKeys, ecm = true)
        }
        val fighterWeapons = definition?.fighters?.flatMapIndexed { fighterIndex, fighter ->
            val quantity = OlympiadDawnShip.remainingFighters(definition, damage!!, fighterIndex)
            fighter.weapons.mapIndexedNotNull { weaponIndex, weapon ->
                weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                    OperationTacticalWeaponStatus("fighter-${fighter.id}-$weaponIndex", it.name, it.beamClass, quantity = quantity,
                        torpedo = it.torpedo, interceptor = it.interceptor, arcs = it.arcs, destroyed = quantity <= 0,
                        fired = "fighter-${fighter.id}-$weaponIndex" in firedWeaponKeys, fighterWeapon = true)
                }
            }
        }.orEmpty() + squadron?.let { status ->
            status.weapons.mapIndexedNotNull { weaponIndex, weapon -> weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                OperationTacticalWeaponStatus("fighter-${status.squadronId}-$weaponIndex", it.name, it.beamClass, quantity = status.remaining,
                    torpedo = it.torpedo, interceptor = it.interceptor, arcs = it.arcs, destroyed = status.remaining <= 0,
                    fired = "fighter-${status.squadronId}-$weaponIndex" in firedWeaponKeys, fighterWeapon = true)
            } }
        }.orEmpty()
        val weapons = shipWeapons + ecmSystems + fighterWeapons
        val facing = source.facingDegrees ?: squadron?.let { 0 } ?: return OperationTacticalTargetingReport(weapons, emptyList())
        val remainingFireControl = (fireControlCapacity - fireControlUsed).coerceAtLeast(0)
        val intact = weapons.filter { !it.destroyed && !it.fired && (it.ecm || it.pointDefense || it.beamClass != null || it.torpedo || it.salvoRack || (it.salvo && (it.magazineRemaining ?: 0) > 0) || (it.submunition && (it.magazineRemaining ?: 0) > 0)) }
        val targets = query.allUnits().asSequence()
            .filter { it.id != source.id && !it.destroyed && (OlympiadDawnShip.definitionFor(it, content) != null || OperationUnitPresentation.unitClass(it, content) == "F") && OperationUnitPresentation.affiliation(it, content) != OperationUnitPresentation.affiliation(source, content) }
            .mapNotNull { targetFor(source, it, facing, intact, remainingFireControl, query) }
            .sortedWith(compareByDescending<OperationTacticalTarget>(OperationTacticalTarget::firepower).thenBy(OperationTacticalTarget::rangeU).thenBy(String.CASE_INSENSITIVE_ORDER, OperationTacticalTarget::targetName).thenBy { it.targetId.value })
            .toList()
        val manual = intact.filter(OperationTacticalWeaponStatus::ecm).takeIf { it.isNotEmpty() }?.let { modules ->
            val shape = OperationEcmRules.manual(modules.size)!!
            OperationTacticalTarget(source.id, "Manual Targeting", shape.rangeU, arcOrder, 0, modules.map { OperationTacticalWeaponContribution(it.key, it.name, "F", 0) }, manualEcm = true)
        }
        return OperationTacticalTargetingReport(weapons, targets + listOfNotNull(manual), fireControlCapacity, fireControlUsed)
    }

    private fun targetFor(source: OperationUnit, target: OperationUnit, facing: Int, weapons: List<OperationTacticalWeaponStatus>, remainingFireControl: Int, query: OperationUnitQueryService): OperationTacticalTarget? {
        val range = query.distanceBetween(source, target)
        val bearing = Math.toDegrees(atan2((target.coordinate.x - source.coordinate.x), -(target.coordinate.y - source.coordinate.y)))
        val arc = arcForBearing(bearing, facing)
        val content = query.content
        val contributions = weapons.filterNot(OperationTacticalWeaponStatus::ecm)
            .filter { arc in it.arcs && effectiveAgainst(it, target, content) && (!it.requiresFireControl || remainingFireControl > 0 || (OperationUnitPresentation.unitClass(target, content) == "F" && it.beamClass == 1)) }
            .mapNotNull { weapon -> firepower(weapon, range).takeIf { it > 0 }?.let { OperationTacticalWeaponContribution(weapon.key, weapon.name, arc, it) } } +
            weapons.filter(OperationTacticalWeaponStatus::ecm).takeIf { OperationEcmRules.automatic(it.size, range) != null }.orEmpty().map { OperationTacticalWeaponContribution(it.key, it.name, arc, 0) }
        return contributions.takeIf { it.isNotEmpty() }?.let {
            OperationTacticalTarget(target.id, OperationUnitPresentation.displayName(target, content), range, it.map(OperationTacticalWeaponContribution::arc).distinct(), it.sumOf(OperationTacticalWeaponContribution::firepower), it,
                shipClass = OperationUnitPresentation.unitClass(target, content) ?: OlympiadDawnShip.definitionFor(target, content)?.shipClass)
        }
    }

    private fun firepower(weapon: OperationTacticalWeaponStatus, range: Double): Int = (when {
        weapon.fighterWeapon && range > 1.000001 -> 0
        weapon.pointDefense -> if (range < OlympiadDawnBeamRules.RANGE_BAND_SIZE_U) 1 else 0
        weapon.submunition -> OlympiadDawnBeamRules.firepowerAtRange(3, range)
        weapon.beamClass != null -> OlympiadDawnBeamRules.firepowerAtRange(weapon.beamClass, range)
        weapon.torpedo && range <= 12.0 -> 1
        (weapon.salvo || weapon.salvoRack) && range <= 12.0 -> 6
        else -> 0
    }) * weapon.quantity

    private fun effectiveAgainst(weapon: OperationTacticalWeaponStatus, target: OperationUnit, content: OperationContent): Boolean = when {
        weapon.fighterWeapon && weapon.torpedo -> OperationUnitPresentation.unitClass(target, content) != "F"
        weapon.fighterWeapon && weapon.beamClass == 1 -> true
        OperationUnitPresentation.unitClass(target, content) == "F" -> weapon.pointDefense || weapon.beamClass == 1
        else -> !weapon.pointDefense
    }
}

internal data class OperationTacticalDieResult(val weaponName: String, val rolledValue: Int, val resolvedValue: Int = rolledValue, val damage: Int) {
    val label get() = if (rolledValue == resolvedValue) rolledValue.toString() else "$rolledValue→$resolvedValue"
}

internal data class OperationTacticalFireResolution(
    val attackerName: String = "", val targetName: String, val weaponNames: List<String>, val diceRolled: Int, val damage: Int,
    val dieResults: List<OperationTacticalDieResult> = emptyList(), val fightersDestroyed: Int = 0, val targetDestroyed: Boolean = false,
    val interceptPending: Boolean = false, val ecmActivations: List<OperationEcmActivation> = emptyList(), val effectiveEcmLevel: Int = 0,
)

internal object OperationTacticalCombatService {
    fun removeInterceptedAttackers(weapons: List<OperationTacticalWeaponStatus>, contributions: List<OperationTacticalWeaponContribution>, intercepted: Int): List<OperationTacticalWeaponContribution> {
        val byKey = weapons.associateBy(OperationTacticalWeaponStatus::key)
        var remaining = intercepted.coerceAtLeast(0)
        return contributions.mapNotNull { contribution ->
            val weapon = byKey[contribution.weaponKey]
            val interceptable = weapon?.salvo == true || weapon?.salvoRack == true || weapon?.torpedo == true || weapon?.fighterWeapon == true
            val surviving = if (interceptable) (contribution.firepower - remaining).coerceAtLeast(0).also { remaining = (remaining - contribution.firepower).coerceAtLeast(0) } else contribution.firepower
            contribution.copy(firepower = surviving).takeIf { surviving > 0 }
        }
    }

    fun resolve(target: OperationUnit, content: OperationContent, weapons: List<OperationTacticalWeaponStatus>, contributions: List<OperationTacticalWeaponContribution>, ecmLevel: Int = 0, ecmLevelsByWeaponKey: Map<String, Int> = emptyMap(), rollD6: () -> Int = { Random.nextInt(1, 7) }): OperationTacticalFireResolution {
        val byKey = weapons.associateBy(OperationTacticalWeaponStatus::key)
        val results = mutableListOf<OperationTacticalDieResult>()
        fun roll() = rollD6().also { require(it in 1..6) { "d6 rolls must be between 1 and 6" } }
        contributions.forEach { contribution ->
            val weapon = byKey[contribution.weaponKey] ?: return@forEach
            val level = (operationalShieldCount(target, content) + (ecmLevelsByWeaponKey[weapon.key] ?: ecmLevel).coerceAtLeast(0)).coerceAtMost(4)
            val modifier = if (level >= 3) -1 else 0
            fun reduce(damage: Int) = if (level >= 4 && damage > 0) (damage - 1).coerceAtLeast(1) else damage
            when {
                weapon.salvo || weapon.salvoRack -> roll().also { value -> val resolved = (value + modifier).coerceIn(1, 6); results += OperationTacticalDieResult(weapon.name, value, resolved, reduce(resolved.coerceAtMost(contribution.firepower))) }
                weapon.torpedo -> repeat(contribution.firepower.coerceAtLeast(0)) { val value = roll(); val resolved = (value + modifier).coerceIn(1, 6); results += OperationTacticalDieResult(weapon.name, value, resolved, reduce(resolved.takeIf { it >= 4 } ?: 0)) }
                weapon.submunition -> repeat(contribution.firepower.coerceAtLeast(0)) { exploding(weapon.name, modifier, ::reduce, ::roll, results) }
                else -> repeat(contribution.firepower.coerceAtLeast(0)) { val value = roll(); val resolved = (value + interceptorModifier(weapon, target, content) + modifier).coerceIn(1, 6); val damage = when (resolved) { 6 -> if (level.coerceAtMost(2) == 0) 2 else 1; 5 -> 1; 4 -> if (level.coerceAtMost(2) >= 2) 0 else 1; else -> 0 }; results += OperationTacticalDieResult(weapon.name, value, resolved, reduce(damage)) }
            }
        }
        return OperationTacticalFireResolution(targetName = OperationUnitPresentation.displayName(target, content), weaponNames = contributions.map(OperationTacticalWeaponContribution::weaponName), diceRolled = results.size, damage = results.sumOf(OperationTacticalDieResult::damage), dieResults = results, effectiveEcmLevel = (ecmLevelsByWeaponKey.values + ecmLevel).maxOrNull()?.coerceAtMost(4) ?: 0)
    }

    fun applyShipDamage(definition: OlympiadDawnShipDefinition, damage: OlympiadDawnShipDamage, hits: Int): OlympiadDawnShipDamage {
        val armour = damage.destroyedArmour.toMutableSet(); val hull = damage.destroyedHull.toMutableSet()
        repeat(hits.coerceAtLeast(0)) { (0 until (definition.armourPoints ?: 0)).firstOrNull { it !in armour }?.let(armour::add) ?: (0 until definition.hullPoints).firstOrNull { it !in hull }?.let(hull::add) }
        return damage.copy(destroyedArmour = armour, destroyedHull = hull)
    }

    fun remainingFightersAfterDamage(remaining: Int, hits: Int) = (remaining - hits.coerceAtLeast(0)).coerceAtLeast(0)

    private fun exploding(name: String, modifier: Int, reduce: (Int) -> Int, roll: () -> Int, results: MutableList<OperationTacticalDieResult>) {
        val value = roll(); val resolved = (value + modifier).coerceIn(1, 6); val damage = when (resolved) { 6 -> 2; 4, 5 -> 1; else -> 0 }
        results += OperationTacticalDieResult(name, value, resolved, reduce(damage)); if (resolved == 6) exploding(name, modifier, reduce, roll, results)
    }

    private fun interceptorModifier(weapon: OperationTacticalWeaponStatus, target: OperationUnit, content: OperationContent) = when {
        !weapon.interceptor -> 0
        OperationUnitPresentation.unitClass(target, content) == "F" -> 2
        OlympiadDawnShip.definitionFor(target, content) != null -> -1
        else -> 0
    }

    private fun operationalShieldCount(target: OperationUnit, content: OperationContent) = (OlympiadDawnShip.definitionFor(target, content)?.let { definition ->
        val damage = OlympiadDawnShip.instanceDamage(target.customProperties, definition)
        definition.systems.count { it.name == "Shields" && it.id !in damage.destroyedSystems }
    } ?: 0).coerceAtMost(2)
}

internal enum class OperationFireArcDisplayMode(val label: String) {
    AllUnits("All Units"), FriendliesOfSelected("Friendlies of Selected"), HostilesOfSelected("Hostiles of Selected"), SelectedOnly("Selected Only"), None("None");
    companion object { fun fromSettingsValue(value: String?) = entries.firstOrNull { it.label.equals(value, true) || it.name.equals(value, true) } ?: AllUnits }
}

internal object OperationFireArcSelectionService {
    fun <T> shouldDraw(mode: OperationFireArcDisplayMode, selectedUnitIds: Set<T>, selectedAffiliations: List<OperationUnitAffiliation?>, targetId: T, targetAffiliation: OperationUnitAffiliation?): Boolean = when (mode) {
        OperationFireArcDisplayMode.AllUnits -> true
        OperationFireArcDisplayMode.None -> false
        else -> if (selectedAffiliations.size != 1) false else when (mode) {
            OperationFireArcDisplayMode.SelectedOnly -> targetId in selectedUnitIds
            OperationFireArcDisplayMode.FriendliesOfSelected -> targetAffiliation == selectedAffiliations.single()
            OperationFireArcDisplayMode.HostilesOfSelected -> when (selectedAffiliations.single()) { OperationUnitAffiliation.FRIEND -> targetAffiliation == OperationUnitAffiliation.HOSTILE; OperationUnitAffiliation.HOSTILE -> targetAffiliation == OperationUnitAffiliation.FRIEND; else -> false }
            else -> false
        }
    }
}

internal data class OperationTacticalFireKey(val turnNumber: Int, val unitId: OperationObjectId)
internal data class OperationTacticalFireUsage(val firedWeapons: Set<String> = emptySet(), val fireControlUsed: Int = 0)

internal sealed interface OperationTacticalFirePreparationResult {
    data class Prepared(
        val attacker: OperationUnit,
        val usage: OperationTacticalFireUsage,
        val weaponKeys: Set<String>,
        val consumesFireControl: Boolean,
    ) : OperationTacticalFirePreparationResult
    data class Rejected(val reason: String) : OperationTacticalFirePreparationResult
}

internal data class OperationPendingInterception(
    val attackerId: OperationObjectId,
    val defenderId: OperationObjectId,
    val incomingWeapons: List<OperationTacticalWeaponStatus>,
    val incomingContributions: List<OperationTacticalWeaponContribution>,
    val defensiveWeapons: List<OperationTacticalWeaponStatus>,
    val defensiveContributions: List<OperationTacticalWeaponContribution>,
    val interceptedDamage: Int = 0,
)

internal data class OperationTacticalDamageApplication(
    val revisedUnits: Map<OperationObjectId, OperationUnit>,
    val fightersDestroyed: Int = 0,
    val targetDestroyed: Boolean = false,
)

/** Atomically validates and consumes the persisted attacker-side state for a selected solution. */
internal object OperationTacticalEngagementService {
    fun prepareFire(
        attacker: OperationUnit,
        definition: OlympiadDawnShipDefinition?,
        report: OperationTacticalTargetingReport,
        contributions: List<OperationTacticalWeaponContribution>,
        currentUsage: OperationTacticalFireUsage,
        targetIsFighter: Boolean,
    ): OperationTacticalFirePreparationResult {
        val weaponKeys = contributions.mapTo(linkedSetOf(), OperationTacticalWeaponContribution::weaponKey)
        if (weaponKeys.isEmpty()) return OperationTacticalFirePreparationResult.Rejected("No weapons selected")
        val consumesFireControl = report.weapons.any { it.key in weaponKeys && it.requiresFireControl && !(targetIsFighter && it.beamClass == 1) }
        if (consumesFireControl && currentUsage.fireControlUsed >= report.fireControlCapacity) {
            return OperationTacticalFirePreparationResult.Rejected("No fire control available")
        }
        val properties = attacker.customProperties.toMutableMap()
        definition?.let { sourceDefinition ->
            contributions.mapNotNull { contribution -> sourceDefinition.weapons.firstOrNull { weapon ->
                OlympiadDawnShip.weaponKey(sourceDefinition, sourceDefinition.weapons.indexOf(weapon)) == contribution.weaponKey
            } }.forEach { weapon ->
                val magazine = weapon.magazineId?.let { id -> sourceDefinition.systems.firstOrNull { it.id == id } }
                val capacityId = magazine?.id ?: weapon.takeIf { it.submunition }?.let { OlympiadDawnShip.weaponKey(sourceDefinition, sourceDefinition.weapons.indexOf(it)) }
                val capacity = magazine?.capacity ?: weapon.takeIf { it.submunition }?.let { 1 }
                if (capacityId != null && capacity != null) {
                    val remaining = OlympiadDawnShip.capacityRemaining(properties, capacityId, capacity)
                    if (remaining <= 0) return OperationTacticalFirePreparationResult.Rejected("A selected weapon has no ammunition")
                    properties[OlympiadDawnShip.magazinePropertyKey(capacityId)] = (remaining - 1).toString()
                }
            }
        }
        return OperationTacticalFirePreparationResult.Prepared(
            attacker.copy(customProperties = properties),
            currentUsage.copy(firedWeapons = currentUsage.firedWeapons + weaponKeys, fireControlUsed = currentUsage.fireControlUsed + if (consumesFireControl) 1 else 0),
            weaponKeys,
            consumesFireControl,
        )
    }

    fun applyDamage(target: OperationUnit, content: OperationContent, hits: Int): OperationTacticalDamageApplication {
        if (hits <= 0) return OperationTacticalDamageApplication(emptyMap())
        OlympiadDawnShip.definitionFor(target, content)?.let { definition ->
            val damage = OperationTacticalCombatService.applyShipDamage(definition, OlympiadDawnShip.instanceDamage(target.customProperties, definition), hits)
            val destroyed = damage.destroyedHull.size == definition.hullPoints
            val revised = OlympiadDawnShip.withDamage(target, damage).copy(destroyed = target.destroyed || destroyed)
            return OperationTacticalDamageApplication(mapOf(target.id to revised), targetDestroyed = destroyed)
        }
        val status = OlympiadDawnShip.squadronStatus(target, content) ?: return OperationTacticalDamageApplication(emptyMap())
        val remaining = OperationTacticalCombatService.remainingFightersAfterDamage(status.remaining, hits)
        val carrier = content.objectById(status.carrierId) as? OperationUnit ?: return OperationTacticalDamageApplication(emptyMap())
        val revisedCarrier = OlympiadDawnShip.withSquadronRemaining(carrier, content, status.squadronId, remaining)
            ?: return OperationTacticalDamageApplication(emptyMap())
        val destroyed = remaining == 0
        return OperationTacticalDamageApplication(
            mapOf(carrier.id to revisedCarrier, target.id to target.copy(destroyed = target.destroyed || destroyed)),
            status.remaining - remaining,
            destroyed,
        )
    }
}

internal data class OperationGunrepPresentation(
    val title: String,
    val attackerLabel: String,
    val weaponsLabel: String,
    val damageLabel: String,
    val ecmLabel: String? = null,
    val fightersDestroyedLabel: String? = null,
    val targetDestroyedLabel: String? = null,
)

internal object OperationGunrepPresentationService {
    fun present(resolution: OperationTacticalFireResolution): OperationGunrepPresentation = OperationGunrepPresentation(
        title = "GUNREP — ${resolution.targetName}",
        attackerLabel = "Attacker: ${resolution.attackerName}",
        weaponsLabel = "Weapons: " + resolution.weaponNames.distinct().joinToString { name ->
            resolution.weaponNames.count { it == name }.let { count -> if (count == 1) name else "$name x$count" }
        },
        damageLabel = "Damage: ${resolution.damage}",
        ecmLabel = resolution.ecmActivations.firstOrNull()?.let { activation ->
            "ECM: ${format(activation.rangeU)}U × ${format(activation.widthDegrees)}° | Level ${activation.intensity} | ${activation.moduleKeys.size} module(s)"
        } ?: resolution.effectiveEcmLevel.takeIf { it > 0 }?.let { "Affected by ECM level $it" },
        fightersDestroyedLabel = resolution.fightersDestroyed.takeIf { it > 0 }?.let { "Fighters destroyed: $it" },
        targetDestroyedLabel = resolution.targetDestroyed.takeIf { it }?.let { "Target destroyed" },
    )

    private fun format(value: Double) = "%.1f".format(value).trimEnd('0').trimEnd('.')
}
