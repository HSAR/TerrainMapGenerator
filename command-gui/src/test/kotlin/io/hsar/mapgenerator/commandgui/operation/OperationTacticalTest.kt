package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationTacticalTest {
    @Test
    fun `neutral targeting orders equal firepower by distance name and id`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":2,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc"}],"weapons":[{"id":"beam","name":"beam","class":1,"leftArc":"F","numArcs":1}]}""",
        )
        val source = unit(1, "Source", 0.5, 0.5, OperationUnitAffiliation.FRIEND, definition, facing = 0)
        val zulu = unit(3, "Zulu", 0.5, 0.497, OperationUnitAffiliation.HOSTILE, definition)
        val alphaHigh = unit(5, "Alpha", 0.5, 0.497, OperationUnitAffiliation.HOSTILE, definition)
        val alphaLow = unit(4, "Alpha", 0.5, 0.497, OperationUnitAffiliation.HOSTILE, definition)
        val farther = unit(2, "Alpha", 0.5, 0.496, OperationUnitAffiliation.HOSTILE, definition)
        val content = content(source, zulu, alphaHigh, farther, alphaLow)

        val report = OperationTacticalTargetingService.resolve(source, OperationUnitQueryService(content, 1000, 1000, 1.0))

        assertThat(report.targets.map { it.targetId }, equalTo(listOf(alphaLow.id, alphaHigh.id, zulu.id, farther.id)))
    }

    @Test
    fun `neutral combat validates dice and applies armour before hull`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":2,"rows":1},"armour":[[1,0]],"systems":[]}""",
        )
        val target = unit(1, "Target", 0.5, 0.5, OperationUnitAffiliation.HOSTILE, definition)
        val content = content(target)
        val weapon = OperationTacticalWeaponStatus("beam", "Beam", 1, arcs = listOf("F"), destroyed = false, fired = false)

        val result = OperationTacticalCombatService.resolve(target, content, listOf(weapon), listOf(OperationTacticalWeaponContribution("beam", "Beam", "F", 1)), rollD6 = { 6 })
        val damage = OperationTacticalCombatService.applyShipDamage(definition, OlympiadDawnShipDamage(), result.damage)

        assertThat(result.damage, equalTo(2))
        assertThat(damage.destroyedArmour, equalTo(setOf(0)))
        assertThat(damage.destroyedHull, equalTo(setOf(0)))
        val application = OperationTacticalEngagementService.applyDamage(target, content, result.damage)
        assertThat(application.revisedUnits.getValue(target.id).destroyed, equalTo(false))
        assertThat(OlympiadDawnShip.shipDamage(application.revisedUnits.getValue(target.id), content)?.destroyedArmour, equalTo(setOf(0)))
    }

    @Test
    fun `neutral tactical marker and GUNREP presentation preserve legacy text`() {
        val id = OperationObjectId(UUID(0, 9))
        val activation = OperationEcmActivation(2, id, setOf("ecm-1"), 0.0, 2.0, 60.0, 3)
        val properties = OperationTacticalTurnMarker.update(emptyMap(), 2, OperationTacticalFireUsage(setOf("beam"), 1), activation)
        val presentation = OperationGunrepPresentationService.present(
            OperationTacticalFireResolution("Attacker", "Target", listOf("Beam", "Beam"), 1, 2, ecmActivations = listOf(activation)),
        )

        assertThat(OperationTacticalTurnMarker.usage(properties, 2), equalTo(OperationTacticalFireUsage(setOf("beam"), 1)))
        assertThat(presentation.title, equalTo("GUNREP — Target"))
        assertThat(presentation.weaponsLabel, equalTo("Weapons: Beam x2"))
        assertThat(presentation.ecmLabel, equalTo("ECM: 2U × 60° | Level 3 | 1 module(s)"))
    }

    @Test
    fun `neutral fire preparation rejects unavailable fire control without changing the attacker`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[],"weapons":[{"id":"beam","name":"beam","class":2,"leftArc":"F","numArcs":1}]}""",
        )
        val attacker = unit(1, "Attacker", 0.5, 0.5, OperationUnitAffiliation.FRIEND, definition)
        val weapon = OperationTacticalWeaponStatus("beam", "Beam", 2, requiresFireControl = true, arcs = listOf("F"), destroyed = false, fired = false)
        val contribution = OperationTacticalWeaponContribution("beam", "Beam", "F", 2)

        val result = OperationTacticalEngagementService.prepareFire(
            attacker, definition, OperationTacticalTargetingReport(listOf(weapon), emptyList(), fireControlCapacity = 0),
            listOf(contribution), OperationTacticalFireUsage(), targetIsFighter = false,
        )

        assertThat(result, equalTo(OperationTacticalFirePreparationResult.Rejected("No fire control available")))
        assertThat(attacker.customProperties, equalTo(OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition)))
    }

    @Test
    fun `neutral fire arc selection distinguishes friendlies and hostiles`() {
        assertThat(
            OperationFireArcSelectionService.shouldDraw(
                OperationFireArcDisplayMode.HostilesOfSelected, setOf("selected"), listOf(OperationUnitAffiliation.FRIEND), "target", OperationUnitAffiliation.HOSTILE,
            ),
            equalTo(true),
        )
        assertThat(OperationTacticalTargetingService.arcForBearing(330.0, 0), equalTo("F"))
    }

    private fun content(vararg units: OperationUnit) = OperationContent(
        objects = units.toList(),
        unitTemplates = units.distinctBy { it.unitTemplateId }.mapIndexed { index, unit -> template(unit.unitTemplateId, index, unit.affiliation, unit.customProperties) },
    )

    private fun unit(id: Long, name: String, x: Double, y: Double, affiliation: OperationUnitAffiliation, definition: OlympiadDawnShipDefinition, facing: Int? = null): OperationUnit {
        val templateId = OperationUnitTemplateId(UUID(0, id + 100))
        return OperationUnit(OperationObjectId(UUID(0, id)), templateId, name, false, facing, MapCoordinate(x, y), 0.01, 0.01,
            OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition), affiliation)
    }

    private fun template(id: OperationUnitTemplateId, index: Int, affiliation: OperationUnitAffiliation?, properties: Map<String, String>) =
        OperationUnitTemplate(id, "Ship", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), index, affiliation = affiliation, customProperties = properties)
}
