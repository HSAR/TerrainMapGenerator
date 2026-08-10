package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationUnitAuthoringTest {
    private val image = BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB)
    private val template = OperationUnitTemplate(OperationUnitTemplateId(UUID(0, 1)), "Scout", image, 0)

    @Test
    fun `validates template image name and property keys`() {
        val draft = OperationUnitTemplateDraft(
            properties = listOf(OperationPropertyDraft("speed", "4"), OperationPropertyDraft("speed", "5")),
        )

        val validation = OperationUnitAuthoring.validateTemplateDraft(draft)

        assertThat(validation.errors.keys, equalTo(setOf("name", "image", "property:1")))
    }

    @Test
    fun `creates a template with embedded source and next stable index`() {
        val bytes = byteArrayOf(1, 2, 3)
        val draft = OperationUnitTemplateDraft(
            name = " Frigate ",
            affiliation = OperationUnitAffiliation.FRIEND,
            replacementImage = OperationImageDraft.fromBytes("frigate.png", bytes),
            properties = listOf(OperationPropertyDraft("Class", "FF")),
        )

        val created = OperationUnitAuthoring.createTemplate(OperationContent(unitTemplates = listOf(template.copy(index = 4))), draft, image)

        assertThat(created.name, equalTo("Frigate"))
        assertThat(created.index, equalTo(5))
        assertThat(created.assetSource!!.embeddedBytes!!.toList(), equalTo(bytes.toList()))
        assertThat(created.customProperties, equalTo(mapOf("Class" to "FF")))
    }

    @Test
    fun `places a unit with first unused name and aspect-correct dimensions`() {
        val existing = OperationUnit(OperationObjectId(UUID(0, 2)), template.id, "Scout 1", false, null, MapCoordinate(.1, .1), .1, .1)

        val placed = OperationUnitAuthoring.placeUnit(
            OperationContent(objects = listOf(existing), unitTemplates = listOf(template)), template,
            MapCoordinate(.5, .6), 64.0, 2.0, 1000, 500, OperationObjectId(UUID(0, 3)),
        )

        assertThat(placed.instanceName, equalTo("Scout 2"))
        assertThat(placed.normalizedWidth, closeTo(.032, .000001))
        assertThat(placed.normalizedHeight, closeTo(.032, .000001))
    }

    @Test
    fun `blocks deletion while a placed unit references the template`() {
        val unit = OperationUnit(OperationObjectId(UUID(0, 2)), template.id, "Scout 1", false, null, MapCoordinate(.1, .1), .1, .1)

        assertThat(OperationUnitAuthoring.canDeleteTemplate(OperationContent(objects = listOf(unit), unitTemplates = listOf(template)), template.id), equalTo(false))
        assertThat(OperationUnitAuthoring.canDeleteTemplate(OperationContent(unitTemplates = listOf(template)), template.id), equalTo(true))
    }

    @Test
    fun `authoring change sets retain exact before and after values and suppress no-ops`() {
        val edited = template.copy(name = "Renamed")

        val created = OperationUnitAuthoring.createTemplateChange(template)
        val changed = OperationUnitAuthoring.editTemplateChange(template, edited)
        val unchanged = OperationUnitAuthoring.editTemplateChange(template, template)

        assertThat(created.unitTemplates.single(), equalTo(OperationValueChange(after = template)))
        assertThat(changed.unitTemplates.single(), equalTo(OperationValueChange(template, edited)))
        assertThat(unchanged.isEmpty, equalTo(true))
    }

    @Test
    fun `round trips normalized positions without a grid`() {
        val renderData = MapViewerRenderData(BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB))
        val coordinate = MapCoordinate(.25, .75)

        val fields = OperationUnitAuthoring.formatPosition(renderData, coordinate)
        val parsed = OperationUnitAuthoring.parsePosition(renderData, fields.easting, fields.northing)

        assertThat(parsed, equalTo(coordinate))
    }
}
