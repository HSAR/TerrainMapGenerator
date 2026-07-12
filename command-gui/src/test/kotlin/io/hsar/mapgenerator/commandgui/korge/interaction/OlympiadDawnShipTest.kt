package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OlympiadDawnShipTest {
    @Test
    fun `parses a ship definition and preserves untracked source sections`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":24,"rows":4},"armour":[[5,0]],"systems":[{"name":"drive","id":"drive-1"}],"weapons":[{"name":"beam"}],"class":"Heavy Cruiser","name":"Vandenburg"}""",
        )

        assertThat(definition.name, equalTo("Vandenburg"))
        assertThat(definition.hullPoints, equalTo(24))
        assertThat(definition.armourPoints, equalTo(5))
        assertThat(definition.systems.single().id, equalTo("drive-1"))
        assertThat(definition.sourceJson.contains("weapons"), equalTo(true))
    }

    @Test
    fun `rejects duplicate system IDs`() {
        val result = runCatching {
            OlympiadDawnShip.parse(
                """{"hull":{"points":1,"rows":1},"armour":[[0,0]],"systems":[{"name":"drive","id":"a"},{"name":"ftl","id":"a"}],"class":"Scout","name":"Test"}""",
            )
        }

        assertThat(result.isFailure, equalTo(true))
    }

    @Test
    fun `creates independent default state for a placed unit`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":2,"rows":1},"armour":[[1,0]],"systems":[{"name":"drive","id":"drive"}],"class":"Scout","name":"Test"}""",
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(java.util.UUID.randomUUID()),
            name = "Test",
            bitmap = korlibs.image.bitmap.Bitmap32(2, 2),
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )

        val properties = OlympiadDawnShip.instanceProperties(template)

        assertThat(properties.containsKey(OlympiadDawnShip.DAMAGE_KEY), equalTo(true))
        assertThat(OlympiadDawnShip.instanceDamage(properties, definition), equalTo(OlympiadDawnShipDamage()))
    }
}
