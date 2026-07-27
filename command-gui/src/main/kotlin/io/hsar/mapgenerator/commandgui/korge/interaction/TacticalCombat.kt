package io.hsar.mapgenerator.commandgui.korge.interaction

import kotlin.random.Random

internal data class TacticalDieResult(
    val weaponName: String,
    val rolledValue: Int,
    val resolvedValue: Int = rolledValue,
    val damage: Int,
) {
    val label: String
        get() = if (rolledValue == resolvedValue) rolledValue.toString() else "$rolledValue→$resolvedValue"
}

internal data class TacticalFireResolution(
    val attackerName: String = "",
    val targetName: String,
    val weaponNames: List<String>,
    val diceRolled: Int,
    val damage: Int,
    val dieResults: List<TacticalDieResult> = emptyList(),
    val fightersDestroyed: Int = 0,
    val targetDestroyed: Boolean = false,
    val interceptPending: Boolean = false,
    val ecmActivations: List<EcmActivation> = emptyList(),
    val effectiveEcmLevel: Int = 0,
)

internal object TacticalCombatResolver {
    fun removeInterceptedAttackers(
        weapons: List<TacticalWeaponStatus>,
        contributions: List<TacticalWeaponContribution>,
        intercepted: Int,
    ): List<TacticalWeaponContribution> {
        val weaponsByKey = weapons.associateBy(TacticalWeaponStatus::key)
        var remainingInterceptions = intercepted.coerceAtLeast(0)
        return contributions.mapNotNull { contribution ->
            val weapon = weaponsByKey[contribution.weaponKey]
            val interceptable = weapon?.salvo == true || weapon?.salvoRack == true || weapon?.torpedo == true || weapon?.fighterWeapon == true
            val survivingFirepower = if (interceptable) {
                (contribution.firepower - remainingInterceptions).coerceAtLeast(0)
                    .also { remainingInterceptions = (remainingInterceptions - contribution.firepower).coerceAtLeast(0) }
            } else contribution.firepower
            contribution.copy(firepower = survivingFirepower).takeIf { it.firepower > 0 }
        }
    }

    fun resolve(
        target: UnitSnapshot,
        weapons: List<TacticalWeaponStatus>,
        contributions: List<TacticalWeaponContribution>,
        ecmLevel: Int = 0,
        ecmLevelsByWeaponKey: Map<String, Int> = emptyMap(),
        rollD6: () -> Int = { Random.nextInt(1, 7) },
    ): TacticalFireResolution {
        val weaponByKey = weapons.associateBy(TacticalWeaponStatus::key)
        val dieResults = mutableListOf<TacticalDieResult>()
        fun roll(): Int = rollD6().also { require(it in 1..6) { "d6 rolls must be between 1 and 6" } }
        contributions.forEach { contribution ->
            val weapon = weaponByKey[contribution.weaponKey] ?: return@forEach
            val contributionEcmLevel = (ecmLevelsByWeaponKey[contribution.weaponKey] ?: ecmLevel).coerceAtLeast(0)
            val effectiveLevel = (target.operationalShieldCount() + contributionEcmLevel).coerceAtMost(4)
            val attackModifier = if (effectiveLevel >= 3) -1 else 0
            fun reducedDamage(damage: Int): Int = if (effectiveLevel >= 4 && damage > 0) (damage - 1).coerceAtLeast(1) else damage
            when {
                weapon.salvo || weapon.salvoRack -> roll().also { value ->
                    val resolved = (value + attackModifier).coerceIn(1, 6)
                    dieResults += TacticalDieResult(weapon.name, value, resolved, reducedDamage(resolved.coerceAtMost(contribution.firepower)))
                }

                weapon.torpedo -> repeat(contribution.firepower.coerceAtLeast(0)) {
                    val value = roll()
                    val resolved = (value + attackModifier).coerceIn(1, 6)
                    val inflicted = resolved.takeIf { it >= 4 } ?: 0
                    dieResults += TacticalDieResult(weapon.name, value, resolved, reducedDamage(inflicted))
                }

                weapon.submunition -> explodingSubmunitionDamage(contribution.firepower, weapon.name, attackModifier, ::reducedDamage, ::roll, dieResults)
                else -> standardDamage(
                    firepower = contribution.firepower,
                    shieldCount = effectiveLevel.coerceAtMost(2),
                    modifier = weapon.interceptorModifier(target) + attackModifier,
                    reduceDamage = ::reducedDamage,
                    roll = ::roll,
                    weaponName = weapon.name,
                    dieResults = dieResults,
                )
            }
        }
        return TacticalFireResolution(
            targetName = target.displayName(),
            weaponNames = contributions.map(TacticalWeaponContribution::weaponName),
            diceRolled = dieResults.size,
            damage = dieResults.sumOf(TacticalDieResult::damage),
            dieResults = dieResults,
            effectiveEcmLevel = (ecmLevelsByWeaponKey.values + ecmLevel).maxOrNull()?.coerceAtMost(4) ?: 0,
        )
    }

    fun applyShipDamage(
        definition: OlympiadDawnShipDefinition,
        damage: OlympiadDawnShipDamage,
        hits: Int,
    ): OlympiadDawnShipDamage {
        val armour = damage.destroyedArmour.toMutableSet()
        val hull = damage.destroyedHull.toMutableSet()
        repeat(hits.coerceAtLeast(0)) {
            (0 until (definition.armourPoints ?: 0)).firstOrNull { it !in armour }
                ?.let(armour::add)
                ?: (0 until definition.hullPoints).firstOrNull { it !in hull }?.let(hull::add)
        }
        return damage.copy(destroyedArmour = armour, destroyedHull = hull)
    }

    fun remainingFightersAfterDamage(remaining: Int, hits: Int): Int =
        (remaining - hits.coerceAtLeast(0)).coerceAtLeast(0)

    private fun explodingSubmunitionDamage(
        firepower: Int,
        weaponName: String,
        modifier: Int,
        reduceDamage: (Int) -> Int,
        roll: () -> Int,
        dieResults: MutableList<TacticalDieResult>,
    ) {
        fun recordRoll(): Unit = roll().let { value ->
            val resolved = (value + modifier).coerceIn(1, 6)
            val damage = when (resolved) {
                6 -> 2
                4, 5 -> 1
                else -> 0
            }
            dieResults += TacticalDieResult(weaponName, value, resolved, reduceDamage(damage))
            if (resolved == 6) recordRoll()
        }
        repeat(firepower.coerceAtLeast(0)) { recordRoll() }
    }

    private fun standardDamage(
        firepower: Int,
        shieldCount: Int,
        modifier: Int,
        reduceDamage: (Int) -> Int,
        roll: () -> Int,
        weaponName: String,
        dieResults: MutableList<TacticalDieResult>,
    ) {
        repeat(firepower.coerceAtLeast(0)) {
            val rolledValue = roll()
            val resolvedValue = (rolledValue + modifier).coerceIn(1, 6)
            val damage = when (resolvedValue) {
                6 -> if (shieldCount == 0) 2 else 1
                5 -> 1
                4 -> if (shieldCount >= 2) 0 else 1
                else -> 0
            }
            dieResults += TacticalDieResult(weaponName, rolledValue, resolvedValue, reduceDamage(damage))
        }
    }

    private fun TacticalWeaponStatus.interceptorModifier(target: UnitSnapshot): Int = when {
        !interceptor -> 0
        target.unitClassLabel() == "F" -> 2
        target.shipDefinition() != null -> -1
        else -> 0
    }

    private fun UnitSnapshot.operationalShieldCount(): Int = (shipDefinition()
        ?.let { definition ->
            OlympiadDawnShip.instanceDamage(customProperties, definition)
                .let { damage -> definition.systems.count { it.name == "Shields" && it.id !in damage.destroyedSystems } }
        }
        ?: 0).coerceAtMost(2)

    private fun UnitSnapshot.shipDefinition(): OlympiadDawnShipDefinition? =
        OlympiadDawnShip.definition(customProperties) ?: OlympiadDawnShip.definition(template.customProperties)
}
