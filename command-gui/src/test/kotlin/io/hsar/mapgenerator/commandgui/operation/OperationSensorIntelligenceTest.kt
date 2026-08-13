package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationSensorIntelligenceTest {
    @Test
    fun `sensor assessment includes the outer boundary and orders equal ranges by display name`() {
        val observer = unit("Observer", 0.5, unitClass = "FF", affiliation = OperationUnitAffiliation.FRIEND)
        val zulu = unit("Zulu", 0.5159, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)
        val alpha = unit("Alpha", 0.5159, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)
        val query = query(observer, zulu, alpha)

        val checks = OperationSensorService.visibilityChecks(observer, query, candidateFilter = { true })
        val boundary = OperationSensorService.visibilityCheck(observer, alpha, 16.0, query.content)

        assertThat(checks.map { it.unit.instanceName }, equalTo(listOf("Alpha", "Zulu")))
        assertThat(checks.map { it.detected }.distinct(), equalTo(listOf(true)))
        assertThat(checks.map { it.visible }.distinct(), equalTo(listOf(false)))
        assertThat(boundary?.detected, equalTo(true))
    }

    @Test
    fun `electronic warfare exposes faint and ghost transitions without KorGE models`() {
        val observer = unit("Observer", 0.5, unitClass = "DD")
        val target = unit("Target", 0.505, unitClass = "CL")
        val content = content(observer, target)

        val faint = OperationSensorService.visibilityCheck(observer, target, 5.0, content, ecmIntensity = 1)
        val ghost = OperationSensorService.visibilityCheck(observer, target, 5.0, content, ecmIntensity = 3)

        assertThat(faint?.ecmContact, equalTo(OperationEcmSensorContact.FAINT_TRACK))
        assertThat(ghost?.ecmContact, equalTo(OperationEcmSensorContact.SENSOR_GHOST))
    }

    @Test
    fun `contact refresh is idempotent and identification remains sticky while detected`() {
        val observer = unit("Observer", 0.5, unitClass = "FF", affiliation = OperationUnitAffiliation.FRIEND)
        val close = unit("Contact", 0.505, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)
        val first = report(observer, query(observer, close))
        val farther = close.copy(coordinate = MapCoordinate(0.51, 0.5))

        val second = report(observer, query(observer, farther), current = first.contactState, previous = first.contactState)
        val reopened = report(observer, query(observer, farther), current = second.contactState, previous = first.contactState)

        assertThat(first.contactState.identifiedIds, contains(close.id))
        assertThat(second.contactState.identifiedIds, contains(close.id))
        assertThat(reopened.contactState, equalTo(second.contactState))
        assertThat(second.assessment.copyText, containsString("was previously identified as"))
    }

    @Test
    fun `hidden and destroyed contacts are removed from active and identified state`() {
        val observer = unit("Observer", 0.5, unitClass = "FF", affiliation = OperationUnitAffiliation.FRIEND)
        val contact = unit("Contact", 0.505, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)
        val detected = report(observer, query(observer, contact))
        val hidden = contact.copy(coordinate = MapCoordinate(0.53, 0.5))
        val destroyed = contact.copy(destroyed = true)

        val hiddenResult = report(observer, query(observer, hidden), current = detected.contactState, previous = detected.contactState)
        val destroyedResult = report(observer, query(observer, destroyed), current = detected.contactState, previous = detected.contactState)

        assertThat(hiddenResult.contactState.sensorContactIds, empty())
        assertThat(hiddenResult.contactState.identifiedIds, empty())
        assertThat(destroyedResult.contactState.sensorContactIds, empty())
        assertThat(destroyedResult.contactState.identifiedIds, empty())
    }

    @Test
    fun `new contact within three units of a lost position inherits its Sierra number`() {
        val observer = unit("Observer", 0.5, unitClass = "FF", affiliation = OperationUnitAffiliation.FRIEND)
        val original = unit("Original", 0.51, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)
        val detected = report(observer, query(observer, original))
        val lost = report(
            observer,
            query(observer, original.copy(coordinate = MapCoordinate(0.53, 0.5))),
            current = detected.contactState,
            previous = detected.contactState,
        )
        val replacement = unit("Replacement", 0.512, unitClass = "DD", affiliation = OperationUnitAffiliation.HOSTILE)

        val reacquired = report(observer, query(observer, replacement), current = lost.contactState, previous = lost.contactState)

        assertThat(reacquired.contactState.sierraContactNumbers[replacement.id], equalTo(1))
        assertThat(reacquired.assessment.copyText, containsString("from last known position"))
    }

    @Test
    fun `neutral ECM geometry validates capability and includes cone boundaries`() {
        val emitter = unit("Emitter", 0.5)
        val query = query(emitter)
        val shape = OperationEcmRules.manual(moduleCount = 1, widthDegrees = 60.0, intensity = 1)!!
        val activation = OperationEcmActivation(1, emitter.id, setOf("ecm-1"), 0.0, shape.rangeU, shape.widthDegrees, shape.intensity)

        val contained = OperationEcmRules.contains(activation, emitter, MapCoordinate(0.5, 0.499), query)

        assertThat(OperationEcmRules.valid(1, shape.rangeU, shape.widthDegrees, shape.intensity), equalTo(true))
        assertThat(contained, equalTo(true))
    }

    private fun report(
        observer: OperationUnit,
        query: OperationUnitQueryService,
        current: OperationSitrepContactState = OperationSitrepContactState(),
        previous: OperationSitrepContactState = OperationSitrepContactState(),
    ) = OperationContactReportService.generate(
        OperationContactReportRequest(observer, query, OperationContactReportMode.SCIREP, current, previous),
    )

    private fun query(vararg units: OperationUnit) = OperationUnitQueryService(content(*units), 1_000, 1_000, 1.0)

    private fun content(vararg units: OperationUnit): OperationContent {
        val templates = units.distinctBy { it.unitTemplateId }.mapIndexed { index, unit ->
            OperationUnitTemplate(
                id = unit.unitTemplateId,
                name = "${unit.instanceName} Type",
                image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                index = index,
                affiliation = unit.affiliation,
                customProperties = mapOf("unitClass" to unit.customProperties.getValue("unitClass")),
            )
        }
        return OperationContent(objects = units.toList(), unitTemplates = templates)
    }

    private fun unit(
        name: String,
        x: Double,
        unitClass: String = "DD",
        affiliation: OperationUnitAffiliation? = null,
        properties: Map<String, String> = emptyMap(),
    ) = OperationUnit(
        id = OperationObjectId(UUID.randomUUID()),
        unitTemplateId = OperationUnitTemplateId(UUID.randomUUID()),
        instanceName = name,
        destroyed = false,
        facingDegrees = null,
        coordinate = MapCoordinate(x, 0.5),
        normalizedWidth = 0.01,
        normalizedHeight = 0.01,
        customProperties = properties + ("unitClass" to unitClass),
        affiliation = affiliation,
    )
}
