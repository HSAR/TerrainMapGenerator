package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.TextNode
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerUnitTemplatePatchTest {
    @Test
    fun `unit template custom property diff can be applied and reversed`() {
        val before = UnitTemplateSnapshot(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                name = "Infantry",
                bitmap = Bitmap32(4, 4),
                affiliation = UnitAffiliation.Friend,
                customProperties = mapOf("role" to "rifle", "obsolete" to "yes"),
            ),
            index = 0,
        )
        val after = before.copy(
            template = before.template.copy(
                name = "Infantry Platoon",
                customProperties = mapOf("role" to "line", "strength" to "30"),
            ),
        )

        val patch = MapViewerUnitTemplatePatch.diff(before, after)

        assertThat(patch.forward["name"], equalTo(TextNode("Infantry Platoon")))
        assertThat(patch.forward["role"], equalTo(TextNode("line")))
        assertThat(patch.forward["strength"], equalTo(TextNode("30")))
        assertThat(patch.forward["obsolete"], equalTo(null))
        assertThat(MapViewerUnitTemplatePatch.apply(before, patch.forward), equalTo(after))
        assertThat(MapViewerUnitTemplatePatch.apply(after, patch.backward), equalTo(before))
    }

    @Test
    fun `unknown unit template patch keys update custom properties without shadowing known fields`() {
        val before = UnitTemplateSnapshot(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000011")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000011")),
                name = "Armour",
                bitmap = Bitmap32(4, 4),
                customProperties = mapOf("role" to "screen"),
            ),
            index = 0,
        )

        val after = MapViewerUnitTemplatePatch.apply(
            before,
            mapOf(
                "name" to TextNode("Armoured Troop"),
                "role" to BooleanNode.TRUE,
                "obsolete" to null,
            ),
        )

        assertThat(after.template.name, equalTo("Armoured Troop"))
        assertThat(after.template.customProperties, equalTo(mapOf("role" to "true")))
    }
}
