package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationTemplate
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnit
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationUnitContent
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnFighterLaunchStatus
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnSquadronStatus

internal fun OlympiadDawnShip.instanceProperties(template: MapViewerUnitTemplate): Map<String, String> =
    instanceProperties(template.toOperationTemplate())

internal fun OlympiadDawnShip.fighterLaunchStatuses(
    carrier: UnitSnapshot,
    units: Iterable<UnitSnapshot>,
): Map<String, OlympiadDawnFighterLaunchStatus> = units.toOperationUnitContent().let { content ->
    fighterLaunchStatuses(carrier.toOperationUnit(), content)
}

internal fun OlympiadDawnShip.hasUniqueFighterLink(fighter: UnitSnapshot, units: Iterable<UnitSnapshot>): Boolean =
    units.toOperationUnitContent().let { content -> hasUniqueFighterLink(fighter.toOperationUnit(), content) }

internal fun OlympiadDawnShip.validatedFighterProperties(
    fighter: UnitSnapshot,
    units: Iterable<UnitSnapshot>,
): Map<String, String> = units.toOperationUnitContent().let { content ->
    validatedFighterProperties(fighter.toOperationUnit(), content)
}

internal fun OlympiadDawnShip.squadronStatus(
    fighter: UnitSnapshot,
    units: Iterable<UnitSnapshot>,
): OlympiadDawnSquadronStatus? = units.toOperationUnitContent().let { content ->
    squadronStatus(fighter.toOperationUnit(), content)
}

internal fun OlympiadDawnShip.updatedSquadronProperties(
    carrier: UnitSnapshot,
    units: Iterable<UnitSnapshot>,
    squadronId: String,
    remaining: Int,
): Map<String, String>? = units.toOperationUnitContent().let { content ->
    updatedSquadronProperties(carrier.toOperationUnit(), content, squadronId, remaining)
}

internal fun OlympiadDawnShip.shipDamage(unit: UnitSnapshot): OlympiadDawnShipDamage? =
    listOf(unit).toOperationUnitContent().let { content -> shipDamage(unit.toOperationUnit(), content) }
