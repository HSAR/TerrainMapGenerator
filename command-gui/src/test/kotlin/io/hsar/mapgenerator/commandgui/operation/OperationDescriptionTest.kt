package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.*

class OperationDescriptionTest {
    @Test
    fun `unit edit description retains established legacy wording`() {
        val templateId = OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val objectId = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val template = OperationUnitTemplate(templateId, "Alpha", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0)
        val beforeUnit = OperationUnit(objectId, templateId, "Alpha 1", false, null, MapCoordinate(0.25, 0.75), 0.08, 0.08)
        val afterUnit = beforeUnit.copy(instanceName = "Alpha 2", destroyed = true, facingDegrees = 135, coordinate = MapCoordinate(0.5, 0.4))
        val content = OperationContent(objects = listOf(beforeUnit), unitTemplates = listOf(template))
        val changes = OperationChangeSet(objects = listOf(OperationValueChange(beforeUnit, afterUnit)))

        val (_, description) = operationEntryDescription(changes, content)

        assertThat(description, equalTo("unit Alpha 2 (Alpha): renamed to Alpha 2, marked destroyed, facing 135°, moved to (0.5, 0.4)"))
    }
}
