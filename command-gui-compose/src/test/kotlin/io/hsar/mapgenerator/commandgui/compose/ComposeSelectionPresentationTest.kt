package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.Color
import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.operation.OperationArgb
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject
import io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint
import io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplate
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class ComposeSelectionPresentationTest {
    private val templateId = OperationUnitTemplateId(UUID(0, 1))

    @Test
    fun `unit selection colour resolves instance then template affiliation`() {
        val template = OperationUnitTemplate(
            templateId, "Patrol", BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), 0,
            affiliation = OperationUnitAffiliation.FRIEND,
        )
        val content = OperationContent(unitTemplates = listOf(template))

        assertThat(unit().composeSelectionColour(content), equalTo(Color(0xFF276A88)))
        assertThat(
            unit(OperationUnitAffiliation.HOSTILE).composeSelectionColour(content),
            equalTo(Color(0xFFC23E3C)),
        )
    }

    @Test
    fun `drawing selection colour uses the drawing colour`() {
        val colour = OperationArgb.of(255, 12, 34, 56)
        val drawing = OperationDrawingObject(
            OperationObjectId(UUID(0, 3)),
            OperationLineDrawing(OperationDrawingPoint(.1, .2), OperationDrawingPoint(.3, .4), colour),
        )

        assertThat(drawing.composeSelectionColour(OperationContent()), equalTo(Color(0xFF0C2238)))
    }

    @Test
    fun `group selection follows KorGE homogeneous affiliation and fallback rules`() {
        val friend = unit(OperationUnitAffiliation.FRIEND, 4)
        val secondFriend = unit(OperationUnitAffiliation.FRIEND, 5)
        val hostile = unit(OperationUnitAffiliation.HOSTILE, 6)
        val fallback = Color(0xFF998877)

        assertThat(
            listOf(friend, secondFriend).composeSelectionColour(OperationContent(), fallback),
            equalTo(Color(0xFF276A88)),
        )
        assertThat(
            listOf(friend, hostile).composeSelectionColour(OperationContent(), fallback),
            equalTo(fallback),
        )
        assertThat(emptyList<io.hsar.mapgenerator.commandgui.operation.OperationObject>()
            .composeSelectionColour(OperationContent(), fallback), equalTo(fallback))
    }

    private fun unit(affiliation: OperationUnitAffiliation? = null, id: Long = 2) = OperationUnit(
        OperationObjectId(UUID(0, id)), templateId, "Patrol $id", false, null,
        MapCoordinate(.5, .5), .1, .1, affiliation = affiliation,
    )
}
