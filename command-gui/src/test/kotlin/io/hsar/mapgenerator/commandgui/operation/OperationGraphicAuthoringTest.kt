package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationGraphicAuthoringTest {
    private val image = BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB)
    private val templateId = OperationGraphicTemplateId(UUID(0, 10))
    private val template = OperationGraphicTemplate(templateId, "Arrow", image, 0, .032, .032)

    @Test
    fun `validates required fields dimensions and reserved properties`() {
        val draft = OperationGraphicTemplateDraft(
            widthPercent = "0",
            heightPercent = "bad",
            properties = listOf(OperationPropertyDraft(GRAPHIC_TEMPLATE_WIDTH_PROPERTY, "4")),
        )

        val result = OperationGraphicAuthoring.validateTemplateDraft(draft)

        assertThat(result.errors.keys, equalTo(setOf("name", "image", "width", "height", "property:0")))
    }

    @Test
    fun `derives the legacy default height from image and map aspect ratios`() {
        val height = OperationGraphicAuthoring.defaultHeightPercent(image, 1000, 500)

        assertThat(height, closeTo(3.2, .000001))
    }

    @Test
    fun `template edit propagates shared dimensions to every instance in one change set`() {
        val first = graphic(1, .1, .1)
        val second = graphic(2, .2, .2)
        val otherTemplate = template.copy(id = OperationGraphicTemplateId(UUID(0, 11)), name = "Other")
        val other = first.copy(id = OperationObjectId(UUID(0, 3)), graphicTemplateId = otherTemplate.id)
        val content = OperationContent(listOf(first, second, other), graphicTemplates = listOf(template, otherTemplate))
        val edited = template.copy(normalizedWidth = .08, normalizedHeight = .04)

        val changes = OperationGraphicAuthoring.editTemplateChange(content, template, edited)
        val result = changes.applyTo(content)

        assertThat(changes.graphicTemplates.single(), equalTo(OperationValueChange(template, edited)))
        assertThat(changes.objects.size, equalTo(2))
        assertThat(result.objects.filterIsInstance<OperationGraphic>().take(2).map { it.normalizedWidth to it.normalizedHeight }, equalTo(listOf(.08 to .04, .08 to .04)))
        assertThat((result.objects.last() as OperationGraphic).normalizedWidth, equalTo(.032))
    }

    @Test
    fun `placement uses the shared template dimensions and rejects outside coordinates`() {
        val placed = OperationGraphicAuthoring.placeGraphic(template, MapCoordinate(.5, .6), OperationObjectId(UUID(0, 4)))

        assertThat(placed.instanceName, equalTo("Arrow"))
        assertThat(placed.normalizedWidth to placed.normalizedHeight, equalTo(.032 to .032))
        assertThat(runCatching { OperationGraphicAuthoring.placeGraphic(template, MapCoordinate(1.1, .5)) }.isFailure, equalTo(true))
    }

    @Test
    fun `retargeting adopts shared size and name while preserving display label`() {
        val before = graphic(1, .2, .3).copy(customProperties = mapOf(GRAPHIC_DISPLAY_NAME_PROPERTY to "Objective", "owner" to "Blue"))
        val replacement = template.copy(id = OperationGraphicTemplateId(UUID(0, 12)), name = "Circle", normalizedWidth = .1, normalizedHeight = .05)
        val content = OperationContent(objects = listOf(before), graphicTemplates = listOf(template, replacement))
        val renderData = MapViewerRenderData(BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB))
        val draft = OperationGraphicDraft(replacement.id, "Objective", ".7", ".8", listOf(OperationPropertyDraft("owner", "Blue")))

        val after = OperationGraphicAuthoring.editGraphic(before, draft, content, renderData)

        assertThat(after.graphicTemplateId, equalTo(replacement.id))
        assertThat(after.instanceName, equalTo("Circle"))
        assertThat(after.normalizedWidth to after.normalizedHeight, equalTo(.1 to .05))
        assertThat(after.customProperties, equalTo(mapOf("owner" to "Blue", GRAPHIC_DISPLAY_NAME_PROPERTY to "Objective")))
    }

    @Test
    fun `blocks template deletion while referenced`() {
        val content = OperationContent(objects = listOf(graphic(1, .1, .1)), graphicTemplates = listOf(template))

        assertThat(OperationGraphicAuthoring.canDeleteTemplate(content, template.id), equalTo(false))
        assertThat(OperationGraphicAuthoring.deleteTemplateChange(content, template.id), equalTo(null))
    }

    private fun graphic(id: Long, x: Double, y: Double) = OperationGraphic(
        OperationObjectId(UUID(0, id)), template.id, template.name, MapCoordinate(x, y), template.normalizedWidth, template.normalizedHeight,
    )
}
