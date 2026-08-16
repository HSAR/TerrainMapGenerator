package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationWorkflowFacadeTest {
    @Test
    fun `status and tactical facade expose neutral Compose presentation`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":2,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc"}],"weapons":[{"id":"beam","name":"beam","class":1,"leftArc":"F","numArcs":1}]}""",
        )
        val attacker = unit(1, "Attacker", .5, .5, OperationUnitAffiliation.FRIEND, definition, 0)
        val target = unit(2, "Target", .5, .499, OperationUnitAffiliation.HOSTILE, definition, 180)
        val content = content(attacker, target)

        val status = OperationWorkflowFacade.statusRows(attacker, content)
        val tactical = OperationWorkflowFacade.tacticalReport(attacker.id, 1, content, renderData(), settings(), null)!!

        assertThat(status.joinToString("\n"), containsString("Hull: 2/2"))
        assertThat(tactical.targets.single().name, equalTo("Target"))
    }

    @Test
    fun `fire facade returns one revised content and compatible GUNREP`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":2,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc"}],"weapons":[{"id":"beam","name":"beam","class":1,"leftArc":"F","numArcs":1}]}""",
        )
        val attacker = unit(1, "Attacker", .5, .5, OperationUnitAffiliation.FRIEND, definition, 0)
        val target = unit(2, "Target", .5, .499, OperationUnitAffiliation.HOSTILE, definition, 180)
        val content = content(attacker, target)
        val report = OperationWorkflowFacade.tacticalReport(attacker.id, 1, content, renderData(), settings(), null)!!
        val weapon = report.targets.single().contributions.single().weaponKey

        val result = OperationWorkflowFacade.fire(attacker.id, target.id, setOf(weapon), 1, content, renderData(), settings(), null) { 6 }!!

        assertThat(result.gunrepText, containsString("GUNREP — Target"))
        assertThat((result.content.objectById(target.id) as OperationUnit).customProperties != target.customProperties, equalTo(true))
        assertThat(result.additionalDataJson?.toString(Charsets.UTF_8), containsString("tacticalFire"))
    }

    private fun settings() = MapViewerSettings(unitsPerPixel = 1.0, width = 1000, height = 1000)
    private fun renderData() = MapViewerRenderData(BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB))
    private fun content(vararg units: OperationUnit) = OperationContent(units.toList(), units.mapIndexed { index, unit ->
        OperationUnitTemplate(unit.unitTemplateId, "Ship", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), index, affiliation = unit.affiliation, customProperties = unit.customProperties)
    })
    private fun unit(id: Long, name: String, x: Double, y: Double, affiliation: OperationUnitAffiliation, definition: OlympiadDawnShipDefinition, facing: Int) = OperationUnit(
        OperationObjectId(UUID(0, id)), OperationUnitTemplateId(UUID(0, 100 + id)), name, false, facing, MapCoordinate(x, y), .02, .02,
        OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition), affiliation,
    )
}
