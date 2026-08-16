package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.save.EcmPersistence
import io.hsar.mapgenerator.commandgui.save.SitrepContactKey
import io.hsar.mapgenerator.commandgui.save.SitrepContactPersistence

enum class OperationWorkflowReportMode { SCIREP, NAVREP }

data class OperationWorkflowReportResult(val text: String, val additionalDataJson: ByteArray?)
data class OperationWorkflowContribution(val weaponKey: String, val weaponName: String, val arc: String, val firepower: Int)
data class OperationWorkflowTarget(val id: OperationObjectId, val name: String, val rangeUnits: Double, val firepower: Int, val contributions: List<OperationWorkflowContribution>)
data class OperationWorkflowTacticalReport(val weaponsUsed: Int, val fireControlCapacity: Int, val targets: List<OperationWorkflowTarget>)
data class OperationWorkflowFireResult(val content: OperationContent, val additionalDataJson: ByteArray?, val gunrepText: String)
data class OperationWorkflowEcmField(
    val emitterId: OperationObjectId,
    val bearingDegrees: Double,
    val rangeUnits: Double,
    val widthDegrees: Double,
    val intensity: Int,
)
data class OperationWorkflowDamageOption(val token: String, val label: String, val damaged: Boolean)

object OperationWorkflowFacade {
    fun damageOptions(unit: OperationUnit, content: OperationContent): List<OperationWorkflowDamageOption> {
        val definition = OlympiadDawnShip.definitionFor(unit, content) ?: return emptyList()
        val damage = OlympiadDawnShip.instanceDamage(unit.customProperties, definition)
        return buildList {
            repeat(definition.hullPoints) { add(OperationWorkflowDamageOption("hull:$it", "Hull ${it + 1}", it in damage.destroyedHull)) }
            repeat(definition.armourPoints ?: 0) { add(OperationWorkflowDamageOption("armour:$it", "Armour ${it + 1}", it in damage.destroyedArmour)) }
            definition.systems.forEach { add(OperationWorkflowDamageOption("system:${it.id}", it.name, it.id in damage.destroyedSystems)) }
            definition.weapons.forEachIndexed { index, weapon -> OlympiadDawnShip.weaponKey(definition, index).let { key -> add(OperationWorkflowDamageOption("weapon:$key", weapon.name, key in damage.destroyedWeapons)) } }
        }
    }

    fun toggleDamage(unit: OperationUnit, content: OperationContent, token: String): OperationUnit? {
        val definition = OlympiadDawnShip.definitionFor(unit, content) ?: return null
        val damage = OlympiadDawnShip.instanceDamage(unit.customProperties, definition)
        fun <T> Set<T>.toggled(value: T) = if (value in this) this - value else this + value
        val updated = when {
            token.startsWith("hull:") -> token.substringAfter(':').toIntOrNull()?.let { damage.copy(destroyedHull = damage.destroyedHull.toggled(it)) }
            token.startsWith("armour:") -> token.substringAfter(':').toIntOrNull()?.let { damage.copy(destroyedArmour = damage.destroyedArmour.toggled(it)) }
            token.startsWith("system:") -> damage.copy(destroyedSystems = damage.destroyedSystems.toggled(token.substringAfter(':')))
            token.startsWith("weapon:") -> damage.copy(destroyedWeapons = damage.destroyedWeapons.toggled(token.substringAfter(':')))
            else -> null
        } ?: return null
        val destroyed = updated.destroyedHull.size >= definition.hullPoints
        return OlympiadDawnShip.withDamage(unit, updated).copy(destroyed = destroyed)
    }

    fun statusRows(unit: OperationUnit, content: OperationContent): List<String> {
        val definition = OlympiadDawnShip.definitionFor(unit, content)
        val squadron = OlympiadDawnShip.squadronStatus(unit, content)
        val damage = definition?.let { OlympiadDawnShip.instanceDamage(unit.customProperties, it) }
        return buildList {
            add(OperationUnitPresentation.displayName(unit, content))
            add("CLASS ${OperationUnitPresentation.unitClass(unit, content) ?: definition?.shipClass ?: "UNKNOWN"}")
            definition?.let { ship ->
                add("Hull: ${ship.hullPoints - damage!!.destroyedHull.size}/${ship.hullPoints}")
                ship.armourPoints?.let { add("Armour: ${it - damage.destroyedArmour.size}/$it") }
                val launches = OlympiadDawnShip.fighterLaunchStatuses(unit, content)
                add("SYSTEMS")
                ship.systems.forEach { system -> add("${if (system.id in damage.destroyedSystems) "DESTROYED — " else ""}${OlympiadDawnStatusPresentation.systemDisplayName(system, launches)}") }
                add("WEAPONS")
                ship.weapons.forEachIndexed { index, weapon ->
                    val key = OlympiadDawnShip.weaponKey(ship, index)
                    val magazine = weapon.magazineId?.let { id -> ship.systems.firstOrNull { it.id == id } }
                    val remaining = magazine?.let { OlympiadDawnShip.magazineRemaining(unit.customProperties, it) }
                    add(listOfNotNull(if (key in damage.destroyedWeapons) "DESTROYED" else null, weapon.name, remaining?.let { "$it remaining" }).joinToString(" — "))
                }
                ship.fighters.forEachIndexed { index, fighter -> add("${fighter.name}: ${OlympiadDawnShip.remainingFighters(ship, damage, index)}/${fighter.count}") }
            } ?: squadron?.let { add("Fighters remaining: ${it.remaining}/${it.count}") } ?: add("No Olympiad Dawn definition is attached.")
        }
    }

    fun sensorReport(
        observerId: OperationObjectId,
        mode: OperationWorkflowReportMode,
        turnNumber: Int,
        content: OperationContent,
        renderData: MapViewerRenderData,
        settings: MapViewerSettings,
        additionalDataJson: ByteArray?,
    ): OperationWorkflowReportResult? {
        val observer = content.objectById(observerId) as? OperationUnit ?: return null
        val persistence = SitrepContactPersistence.fromJson(additionalDataJson)
        val key = SitrepContactKey(turnNumber, observerId)
        val query = query(content, renderData, settings)
        val result = OperationContactReportService.generate(OperationContactReportRequest(
            observer, query, OperationContactReportMode.valueOf(mode.name), persistence.stateFor(key),
            persistence.stateFor(SitrepContactKey((turnNumber - 1).coerceAtLeast(1), observerId)),
            ecmIntensity = ecmIntensityAt(observer, query, turnNumber, additionalDataJson), gridLayer = renderData.gridLayer,
        ))
        persistence.update(key, result.contactState)
        return OperationWorkflowReportResult(result.assessment.displayText, persistence.toJson())
    }

    fun tacticalReport(
        attackerId: OperationObjectId,
        turnNumber: Int,
        content: OperationContent,
        renderData: MapViewerRenderData,
        settings: MapViewerSettings,
        additionalDataJson: ByteArray?,
    ): OperationWorkflowTacticalReport? {
        val attacker = content.objectById(attackerId) as? OperationUnit ?: return null
        val usage = OperationTacticalTurnMarker.usage(attacker.customProperties, turnNumber)
            ?: OperationTacticalFirePersistence.fromJson(additionalDataJson).usage(OperationTacticalFireKey(turnNumber, attackerId))
        val report = OperationTacticalTargetingService.resolve(attacker, query(content, renderData, settings), usage.firedWeapons, usage.fireControlUsed)
        return OperationWorkflowTacticalReport(report.fireControlUsed, report.fireControlCapacity, report.targets.map { target ->
            OperationWorkflowTarget(target.targetId, target.targetName, target.rangeU, target.firepower, target.contributions.map { OperationWorkflowContribution(it.weaponKey, it.weaponName, it.arc, it.firepower) })
        })
    }

    fun fire(
        attackerId: OperationObjectId,
        targetId: OperationObjectId,
        selectedWeaponKeys: Set<String>,
        turnNumber: Int,
        content: OperationContent,
        renderData: MapViewerRenderData,
        settings: MapViewerSettings,
        additionalDataJson: ByteArray?,
        rollD6: () -> Int = { kotlin.random.Random.nextInt(1, 7) },
    ): OperationWorkflowFireResult? {
        val attacker = content.objectById(attackerId) as? OperationUnit ?: return null
        val target = content.objectById(targetId) as? OperationUnit ?: return null
        val persistence = OperationTacticalFirePersistence.fromJson(additionalDataJson)
        val key = OperationTacticalFireKey(turnNumber, attackerId)
        val usage = OperationTacticalTurnMarker.usage(attacker.customProperties, turnNumber) ?: persistence.usage(key)
        val query = query(content, renderData, settings)
        val report = OperationTacticalTargetingService.resolve(attacker, query, usage.firedWeapons, usage.fireControlUsed)
        val selectedTarget = report.targets.firstOrNull { it.targetId == targetId } ?: return null
        val contributions = selectedTarget.contributions.filter { it.weaponKey in selectedWeaponKeys }
        val preparation = OperationTacticalEngagementService.prepareFire(
            attacker, OlympiadDawnShip.definitionFor(attacker, content), report, contributions, usage,
            OperationUnitPresentation.unitClass(target, content) == "F",
        ) as? OperationTacticalFirePreparationResult.Prepared ?: return null
        val ecmModules = report.weapons.filter { it.ecm && it.key in preparation.weaponKeys }
        val attackContributions = contributions.filter { it.firepower > 0 }
        val activation = ecmModules.takeIf { it.isNotEmpty() }?.let { modules -> OperationEcmRules.automatic(modules.size, selectedTarget.rangeU)?.let { shape ->
            OperationEcmActivation(turnNumber, attacker.id, modules.mapTo(linkedSetOf(), OperationTacticalWeaponStatus::key), OperationEcmRules.absoluteBearing(attacker, target.coordinate, query), shape.rangeU, shape.widthDegrees, shape.intensity)
        } }
        val resolution = if (attackContributions.isEmpty()) OperationTacticalFireResolution(
            OperationUnitPresentation.displayName(attacker, content), "ECM Field", ecmModules.map(OperationTacticalWeaponStatus::name), 0, 0,
            ecmActivations = listOfNotNull(activation), effectiveEcmLevel = activation?.intensity ?: 0,
        ) else OperationTacticalCombatService.resolve(
            target, content, report.weapons, attackContributions,
            ecmLevelsByWeaponKey = attackContributions.associate { it.weaponKey to ecmIntensityAt(target, query, turnNumber, additionalDataJson) }, rollD6 = rollD6,
        ).copy(attackerName = OperationUnitPresentation.displayName(attacker, content), ecmActivations = listOfNotNull(activation))
        val damage = OperationTacticalEngagementService.applyDamage(target, content, resolution.damage)
        val marked = preparation.attacker.copy(customProperties = OperationTacticalTurnMarker.update(preparation.attacker.customProperties, turnNumber, preparation.usage, activation))
        val revised = content.copy(objects = content.objects.map { value -> if (value.id == attacker.id) marked else damage.revisedUnits[value.id] ?: value })
        persistence.replace(key, preparation.usage)
        var additional = persistence.toJson()
        activation?.let { additional = EcmPersistence.fromJson(additional).also { state -> state.add(it) }.toJson() }
        val presentation = OperationGunrepPresentationService.present(resolution.copy(fightersDestroyed = damage.fightersDestroyed, targetDestroyed = damage.targetDestroyed))
        return OperationWorkflowFireResult(revised, additional, listOfNotNull(presentation.title, presentation.attackerLabel, presentation.weaponsLabel, presentation.damageLabel, presentation.ecmLabel, presentation.fightersDestroyedLabel, presentation.targetDestroyedLabel).joinToString("\n"))
    }

    fun ecmFields(turnNumber: Int, additionalDataJson: ByteArray?): List<OperationWorkflowEcmField> =
        EcmPersistence.fromJson(additionalDataJson).forTurn(turnNumber).map {
            OperationWorkflowEcmField(it.emitterId, it.relativeBearingDegrees, it.rangeU, it.widthDegrees, it.intensity)
        }

    private fun query(content: OperationContent, renderData: MapViewerRenderData, settings: MapViewerSettings) = OperationUnitQueryService(content, renderData.baseImage.width, renderData.baseImage.height, settings.unitsPerPixel)
    private fun ecmIntensityAt(unit: OperationUnit, query: OperationUnitQueryService, turn: Int, json: ByteArray?) = EcmPersistence.fromJson(json).forTurn(turn).filter { activation ->
        val emitter = query.content.objectById(activation.emitterId) as? OperationUnit ?: return@filter false
        activation.emitterId != unit.id && OperationEcmRules.contains(activation, emitter, unit.coordinate, query)
    }.sumOf(OperationEcmActivation::intensity).coerceAtMost(OperationEcmRules.MAXIMUM_INTENSITY)
}
