package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.operation.OlympiadDawnShipSystem
import io.hsar.mapgenerator.commandgui.operation.OperationUnitPresentation
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class KorgeOperationUnitParityTest {
    @Test
    fun `KorGE boundary and neutral query expose equivalent ship state`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Scout","class":"Frigate","hull":{"points":2,"rows":1},"armour":[],"systems":[{"name":"drive","id":"drive"},{"name":"sensors","id":"sensors"}]}""",
        )
        val template = MapViewerUnitTemplate(
            UnitTemplateId(UUID(0, 1)),
            "Scout Template",
            Bitmap32(1, 1),
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val snapshot = UnitSnapshot(
            MapPlacedObjectId(UUID(0, 2)), template.id, template, "", false, 0, MapCoordinate(0.5, 0.5), 0.1, 0.1,
            customProperties = mapOf(
                OlympiadDawnShip.DAMAGE_KEY to OlympiadDawnShip.encodeDamage(OlympiadDawnShipDamage(destroyedSystems = setOf("sensors"))),
            ),
        )
        val boundary = KorgeUnitQueryAdapter(listOf(snapshot), 1_000, 1_000, 1.0)

        val legacyMatch = boundary.unitsWithin(snapshot, 0.0).single()
        val neutralMatch = boundary.operation.unitsWithin(boundary.operation.allUnits().single(), 0.0).single()

        assertThat(legacyMatch.unit.id.value, equalTo(neutralMatch.unit.id.value))
        assertThat(
            legacyMatch.operationalSystems.map(OlympiadDawnShipSystem::id),
            equalTo(neutralMatch.operationalSystems.map(OlympiadDawnShipSystem::id)),
        )
        assertThat(snapshot.displayName(), equalTo(OperationUnitPresentation.displayName(neutralMatch.unit, boundary.operation.content)))
        assertThat(snapshot.unitClassLabel(), equalTo(OperationUnitPresentation.unitClass(neutralMatch.unit, boundary.operation.content)))
    }
}
