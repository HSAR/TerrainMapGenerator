package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationUnitPresentationTest {
    @Test
    fun `resolves instance and template metadata with legacy labels`() {
        val template = OperationUnitTemplate(
            OperationUnitTemplateId(UUID(0, 1)),
            "Template Name",
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
            0,
            affiliation = OperationUnitAffiliation.FRIEND,
            customProperties = mapOf("unitClass" to "ff", "mass" to "20"),
        )
        val unit = OperationUnit(
            OperationObjectId(UUID(0, 2)), template.id, "", false, null, MapCoordinate(0.0, 0.0), 0.1, 0.1,
        )
        val content = OperationContent(objects = listOf(unit), unitTemplates = listOf(template))

        assertThat(OperationUnitPresentation.displayName(unit, content), equalTo("Template Name"))
        assertThat(OperationUnitPresentation.unitClass(unit, content), equalTo("FF"))
        assertThat(OperationUnitPresentation.mass(unit, content), equalTo(20))
        assertThat(OperationUnitPresentation.affiliation(unit, content), equalTo(OperationUnitAffiliation.FRIEND))
        assertThat(OperationUnitPresentation.navrepMenuLabel(unit, content), equalTo("Template Name [FF]"))
        assertThat(OperationUnitPresentation.navrepReportLabel(unit, content), equalTo("FF Template Name"))
        assertThat(OperationUnitPresentation.sitrepValidationError(unit, content), equalTo(null))
    }

    @Test
    fun `reports the existing validation errors for missing and unknown class metadata`() {
        val template = OperationUnitTemplate(
            OperationUnitTemplateId(UUID(0, 3)), "Unknown", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0,
        )
        val unit = OperationUnit(
            OperationObjectId(UUID(0, 4)), template.id, "Unknown", false, null, MapCoordinate(0.0, 0.0), 0.1, 0.1,
        )
        val content = OperationContent(objects = listOf(unit), unitTemplates = listOf(template))

        assertThat(OperationUnitPresentation.sitrepValidationError(unit, content), equalTo("This unit has no unitClass setting."))
        assertThat(
            OperationUnitPresentation.sitrepValidationError(unit.copy(customProperties = mapOf("unitClass" to "invalid")), content),
            equalTo("The unitClass value \"INVALID\" is not recognised."),
        )
    }
}
