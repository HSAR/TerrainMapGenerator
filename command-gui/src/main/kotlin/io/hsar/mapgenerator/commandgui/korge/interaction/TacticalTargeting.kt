package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.math.atan2
import kotlin.math.floor

internal data class TacticalWeaponStatus(
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

internal data class TacticalWeaponContribution(
    val weaponKey: String,
    val weaponName: String,
    val arc: String,
    val firepower: Int,
)

internal data class TacticalTarget(
    val targetId: MapPlacedObjectId,
    val targetName: String,
    val rangeU: Double,
    val arcs: List<String>,
    val firepower: Int,
    val contributions: List<TacticalWeaponContribution>,
    val manualEcm: Boolean = false,
    val shipClass: String? = null,
)

internal data class TacticalTargetingReport(
    val weapons: List<TacticalWeaponStatus>,
    val targets: List<TacticalTarget>,
    val fireControlCapacity: Int = 0,
    val fireControlUsed: Int = 0,
)

internal object TacticalTargetingResolver {
    private val arcOrder = listOf("F", "FS", "AS", "A", "AP", "FP")

    fun arcForBearing(bearingDegrees: Double, facingDegrees: Int): String {
        val relative = (bearingDegrees - facingDegrees).mod(360.0)
        return arcOrder[floor((relative + 30.0) / 60.0).toInt().mod(arcOrder.size)]
    }

    fun resolve(
        source: UnitSnapshot,
        unitQueryService: UnitQueryService,
        firedWeaponKeys: Set<String> = emptySet(),
        fireControlUsed: Int = 0,
    ): TacticalTargetingReport {
        val definition = source.shipDefinition()
        val squadron = if (definition == null) OlympiadDawnShip.squadronStatus(source, unitQueryService.allUnits()) else null
        if (definition == null && squadron == null) return TacticalTargetingReport(emptyList(), emptyList())
        val damage = definition?.let { OlympiadDawnShip.instanceDamage(source.customProperties, it) }
        val fireControlCapacity = definition?.systems?.count { it.name == "Gunnery Fire Control System" && it.id !in damage!!.destroyedSystems } ?: 0
        val shipWeapons = definition?.weapons.orEmpty().mapIndexedNotNull { index, weapon ->
            weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                val key = OlympiadDawnShip.weaponKey(definition!!, index)
                val magazine = weapon.magazineId?.let { id -> definition!!.systems.firstOrNull { it.id == id } }
                TacticalWeaponStatus(
                    key, weapon.name, weapon.beamClass, weapon.magazineId != null, weapon.magazineId,
                    when {
                        magazine != null -> OlympiadDawnShip.magazineRemaining(source.customProperties, magazine)
                        weapon.submunition || weapon.salvoRack -> if (key in firedWeaponKeys) 0 else 1
                        else -> null
                    }, 1, weapon.pointDefense, weapon.submunition, weapon.salvoRack, weapon.torpedo, weapon.interceptor, !weapon.pointDefense && !weapon.submunition, weapon.arcs,
                    key in damage!!.destroyedWeapons, key in firedWeaponKeys
                )
            }
        }
        val ecmSystems = definition?.systems.orEmpty()
            .filter { it.name == "Electronic Countermeasures" }
            .map { system ->
                val key = "ecm-${system.id}"
                TacticalWeaponStatus(
                    key = key,
                    name = system.name,
                    beamClass = null,
                    arcs = arcOrder,
                    destroyed = system.id in damage!!.destroyedSystems,
                    fired = key in firedWeaponKeys,
                    ecm = true,
                )
            }
        val fighterWeapons = definition?.fighters?.flatMapIndexed { fighterIndex, fighter ->
            val quantity = OlympiadDawnShip.remainingFighters(definition, damage!!, fighterIndex)
            fighter.weapons.mapIndexedNotNull { weaponIndex, weapon ->
                weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                    TacticalWeaponStatus(
                        "fighter-${fighter.id}-$weaponIndex", weapon.name, weapon.beamClass, false, null, null, quantity,
                        false, false, false, weapon.torpedo, weapon.interceptor, false, weapon.arcs, quantity <= 0, "fighter-${fighter.id}-$weaponIndex" in firedWeaponKeys,
                        fighterWeapon = true,
                    )
                }
            }
        }.orEmpty() + squadron?.let { status ->
            status.weapons.mapIndexedNotNull { weaponIndex, weapon ->
                weapon.takeIf { it.arcs.isNotEmpty() }?.let {
                    TacticalWeaponStatus(
                        "fighter-${status.squadronId}-$weaponIndex", it.name, it.beamClass, false, null, null,
                        status.remaining,
                        false,
                        false,
                        false,
                        it.torpedo,
                        it.interceptor,
                        false,
                        it.arcs,
                        status.remaining <= 0,
                        "fighter-${status.squadronId}-$weaponIndex" in firedWeaponKeys,
                        fighterWeapon = true,
                    )
                }
            }
        }.orEmpty()
        val weapons = shipWeapons + ecmSystems + fighterWeapons
        val facing = source.facingDegrees ?: squadron?.let { 0 } ?: return TacticalTargetingReport(weapons, emptyList())
        val remainingFireControl = (fireControlCapacity - fireControlUsed).coerceAtLeast(0)
        val intactWeapons = weapons.filter {
            !it.destroyed && !it.fired && (it.ecm || it.pointDefense || it.beamClass != null || it.torpedo || it.salvoRack || (it.salvo && (it.magazineRemaining
                ?: 0) > 0) || (it.submunition && (it.magazineRemaining ?: 0) > 0))
        }
        val unitTargets = unitQueryService.allUnits().asSequence()
            .filter { it.id != source.id && !it.destroyed && (it.shipDefinition() != null || it.unitClassLabel() == "F") && it.template.affiliation != source.template.affiliation }
            .mapNotNull { target -> targetFor(source, target, facing, intactWeapons, remainingFireControl, unitQueryService) }
            .sortedWith(
                compareByDescending<TacticalTarget>(TacticalTarget::firepower)
                    .thenBy(TacticalTarget::rangeU)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, TacticalTarget::targetName)
                    .thenBy { it.targetId.value },
            )
            .toList()
        val readyEcm = intactWeapons.filter(TacticalWeaponStatus::ecm)
        val manualTarget = readyEcm.takeIf(List<TacticalWeaponStatus>::isNotEmpty)?.let { modules ->
            val shape = EcmRules.manual(modules.size)!!
            TacticalTarget(
                targetId = source.id,
                targetName = "Manual Targeting",
                rangeU = shape.rangeU,
                arcs = arcOrder,
                firepower = 0,
                contributions = modules.map { TacticalWeaponContribution(it.key, it.name, "F", 0) },
                manualEcm = true,
            )
        }
        val targets = unitTargets + listOfNotNull(manualTarget)
        return TacticalTargetingReport(weapons, targets, fireControlCapacity, fireControlUsed)
    }

    fun resolve(
        source: UnitSnapshot,
        candidates: List<UnitSnapshot>,
        mapWidth: Int,
        mapHeight: Int,
        mapUnitsPerPixel: Double,
        firedWeaponKeys: Set<String> = emptySet(),
        fireControlUsed: Int = 0,
    ): TacticalTargetingReport =
        resolve(source, UnitQueryService(candidates, mapWidth, mapHeight, mapUnitsPerPixel), firedWeaponKeys, fireControlUsed)

    private fun targetFor(
        source: UnitSnapshot,
        target: UnitSnapshot,
        facing: Int,
        weapons: List<TacticalWeaponStatus>,
        remainingFireControl: Int,
        unitQueryService: UnitQueryService,
    ): TacticalTarget? {
        val range = unitQueryService.distanceBetween(source, target)
        val arc = targetArc(source, target, facing)
        val weaponContributions = weapons
            .filterNot(TacticalWeaponStatus::ecm)
            .filter { arc in it.arcs && weaponEffectiveAgainst(it, target) && fireControlAvailableFor(it, target, remainingFireControl) }
            .mapNotNull { weapon ->
                weaponFirepower(weapon, range)
                    .takeIf { it > 0 }
                    ?.let { TacticalWeaponContribution(weapon.key, weapon.name, arc, it) }
            }
        val ecmModules = weapons.filter(TacticalWeaponStatus::ecm)
        val ecmContributions = ecmModules
            .takeIf { EcmRules.automatic(it.size, range) != null }
            ?.map { TacticalWeaponContribution(it.key, it.name, arc, 0) }
            .orEmpty()
        val contributions = weaponContributions + ecmContributions
        return contributions.takeIf(List<TacticalWeaponContribution>::isNotEmpty)?.let {
            TacticalTarget(
                targetId = target.id,
                targetName = target.displayName(),
                rangeU = range,
                arcs = it.map(TacticalWeaponContribution::arc).distinct(),
                firepower = it.sumOf(TacticalWeaponContribution::firepower),
                contributions = it,
                shipClass = target.unitClassLabel() ?: target.shipDefinition()?.shipClass,
            )
        }
    }

    private fun weaponFirepower(weapon: TacticalWeaponStatus, range: Double): Int = singleWeaponFirepower(weapon, range) * weapon.quantity

    private fun singleWeaponFirepower(weapon: TacticalWeaponStatus, range: Double): Int = when {
        weapon.fighterWeapon && range > FIGHTER_WEAPON_RANGE_U + RANGE_EPSILON -> 0
        weapon.pointDefense -> if (range < OlympiadDawnBeamRules.RANGE_BAND_SIZE_U) 1 else 0
        weapon.submunition -> OlympiadDawnBeamRules.firepowerAtRange(3, range)
        weapon.beamClass != null -> OlympiadDawnBeamRules.firepowerAtRange(weapon.beamClass, range)
        weapon.torpedo && range <= 12.0 -> 1
        (weapon.salvo || weapon.salvoRack) && range <= 12.0 -> 6
        else -> 0
    }

    private fun weaponEffectiveAgainst(weapon: TacticalWeaponStatus, target: UnitSnapshot): Boolean =
        when {
            weapon.fighterWeapon && weapon.torpedo -> target.unitClassLabel() != "F"
            weapon.fighterWeapon && weapon.beamClass == 1 -> true
            target.unitClassLabel() == "F" -> weapon.pointDefense || weapon.beamClass == 1
            else -> !weapon.pointDefense
        }

    private fun fireControlAvailableFor(weapon: TacticalWeaponStatus, target: UnitSnapshot, remainingFireControl: Int): Boolean =
        !weapon.requiresFireControl || remainingFireControl > 0 || (target.unitClassLabel() == "F" && weapon.beamClass == 1)

    private fun targetArc(source: UnitSnapshot, target: UnitSnapshot, facing: Int): String {
        val bearing = Math.toDegrees(
            atan2(
                (target.mapCoordinate.x - source.mapCoordinate.x),
                -(target.mapCoordinate.y - source.mapCoordinate.y),
            ),
        )
        return arcForBearing(bearing, facing)
    }

    private fun UnitSnapshot.shipDefinition(): OlympiadDawnShipDefinition? =
        OlympiadDawnShip.definition(customProperties) ?: OlympiadDawnShip.definition(template.customProperties)

    private const val FIGHTER_WEAPON_RANGE_U = 1.0
    private const val RANGE_EPSILON = 0.000001
}
