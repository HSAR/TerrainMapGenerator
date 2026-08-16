package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnitContent
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipDefinition
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalCombatService
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalFireResolution

internal typealias TacticalDieResult = io.hsar.mapgenerator.commandgui.operation.OperationTacticalDieResult

internal data class TacticalFireResolution(
    val attackerName: String = "", val targetName: String, val weaponNames: List<String>, val diceRolled: Int, val damage: Int,
    val dieResults: List<TacticalDieResult> = emptyList(), val fightersDestroyed: Int = 0, val targetDestroyed: Boolean = false,
    val interceptPending: Boolean = false, val ecmActivations: List<EcmActivation> = emptyList(), val effectiveEcmLevel: Int = 0,
)

internal object TacticalCombatResolver {
    fun removeInterceptedAttackers(weapons: List<TacticalWeaponStatus>, contributions: List<TacticalWeaponContribution>, intercepted: Int) =
        OperationTacticalCombatService.removeInterceptedAttackers(weapons, contributions, intercepted)

    fun resolve(target: UnitSnapshot, weapons: List<TacticalWeaponStatus>, contributions: List<TacticalWeaponContribution>, ecmLevel: Int = 0, ecmLevelsByWeaponKey: Map<String, Int> = emptyMap(), rollD6: () -> Int = { kotlin.random.Random.nextInt(1, 7) }): TacticalFireResolution {
        val content = listOf(target).toOperationUnitContent()
        return OperationTacticalCombatService.resolve(target.toOperationUnit(), content, weapons, contributions, ecmLevel, ecmLevelsByWeaponKey, rollD6).let { result ->
            TacticalFireResolution(result.attackerName, result.targetName, result.weaponNames, result.diceRolled, result.damage, result.dieResults, result.fightersDestroyed,
                result.targetDestroyed, result.interceptPending, result.ecmActivations.map { it.korge() }, result.effectiveEcmLevel)
        }
    }

    fun applyShipDamage(definition: OlympiadDawnShipDefinition, damage: OlympiadDawnShipDamage, hits: Int) = OperationTacticalCombatService.applyShipDamage(definition, damage, hits)
    fun remainingFightersAfterDamage(remaining: Int, hits: Int) = OperationTacticalCombatService.remainingFightersAfterDamage(remaining, hits)
}

internal fun TacticalFireResolution.operation() = OperationTacticalFireResolution(
    attackerName, targetName, weaponNames, diceRolled, damage, dieResults, fightersDestroyed, targetDestroyed,
    interceptPending, ecmActivations.map(EcmActivation::operation), effectiveEcmLevel,
)
