package io.hsar.mapgenerator.commandgui.operation

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationDrawingAuthoringTest {
    private val id = OperationObjectId(UUID(0, 41))
    private val drawing = OperationDrawingObject(
        id,
        OperationLineDrawing(OperationDrawingPoint(.1, .2), OperationDrawingPoint(.3, .4), OperationArgb.of(255, 1, 2, 3)),
        mapOf("owner" to "blue"),
    )

    @Test
    fun `rename trims the display name and preserves unrelated properties`() {
        val change = OperationDrawingAuthoring.rename(OperationContent(objects = listOf(drawing)), id, "  Objective  ")
        val after = change.objects.single().after as OperationDrawingObject

        assertThat(after.optionalDisplayName, equalTo("Objective"))
        assertThat(after.customProperties["owner"], equalTo("blue"))
        assertThat(after.displayName, equalTo("Objective"))
    }

    @Test
    fun `blank name removes the display name and unchanged name is suppressed`() {
        val named = drawing.copy(customProperties = drawing.customProperties + (OPERATION_DISPLAY_NAME_PROPERTY to "Objective"))
        val content = OperationContent(objects = listOf(named))

        assertThat(OperationDrawingAuthoring.rename(content, id, "Objective").isEmpty, equalTo(true))
        val cleared = OperationDrawingAuthoring.rename(content, id, "  ").objects.single().after as OperationDrawingObject
        assertThat(cleared.optionalDisplayName, equalTo(null))
        assertThat(cleared.displayName, equalTo("Line Drawing"))
    }
}
