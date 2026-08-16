package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.ViewerPoint
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationSelectionHandlePolicyTest {
    @Test
    fun `units do not expose resize handles while drawings do`() {
        val unit = OperationUnit(
            OperationObjectId(UUID(0, 1)), OperationUnitTemplateId(UUID(0, 2)), "Unit", false, null,
            MapCoordinate(.5, .5), .1, .1,
        )
        val drawing = OperationDrawingObject(
            OperationObjectId(UUID(0, 3)),
            OperationLineDrawing(
                OperationDrawingPoint(.1, .1), OperationDrawingPoint(.2, .2), OperationArgb.of(255, 1, 2, 3),
            ),
        )

        assertThat(unit.supportsResizeHandles(), equalTo(false))
        assertThat(drawing.supportsResizeHandles(), equalTo(true))
        assertThat(listOf(unit).showsSelectionBrackets(), equalTo(false))
        assertThat(listOf(drawing).showsSelectionBrackets(), equalTo(true))
    }

    @Test
    fun `legacy all-unit multi selections show group brackets without resize hit targets`() {
        val first = unit(1)
        val second = unit(2)

        assertThat(listOf(first, second).showsSelectionBrackets(), equalTo(true))
        assertThat(listOf(first, second).supportsSelectionResize(), equalTo(false))
    }

    @Test
    fun `multi selection frame encloses every selected object`() {
        val first = unit(1).copy(coordinate = MapCoordinate(.2, .3))
        val second = unit(2).copy(coordinate = MapCoordinate(.8, .7))
        val content = OperationContent(objects = listOf(first, second))

        val frame = content.selectionFrame(setOf(first.id, second.id), projection)

        assertThat(frame?.topLeft, equalTo(ViewerPoint(15.0, 25.0)))
        assertThat(frame?.bottomRight, equalTo(ViewerPoint(85.0, 75.0)))
    }

    private fun unit(id: Long) = OperationUnit(
        OperationObjectId(UUID(0, id)), OperationUnitTemplateId(UUID(0, 10)), "Unit $id", false, null,
        MapCoordinate(.5, .5), .1, .1,
    )

    private val projection = object : OperationSelectionProjection {
        override val aspectRatio = 1.0
        override fun toScreen(point: MapCoordinate) = ViewerPoint(point.x * 100.0, point.y * 100.0)
        override fun toMap(point: ViewerPoint) = MapCoordinate(point.x / 100.0, point.y / 100.0)
        override fun unitScreenBounds(unit: OperationUnit) = MapSelectionScreenBounds(
            unit.coordinate.x * 100.0 - 5.0,
            unit.coordinate.y * 100.0 - 5.0,
            unit.coordinate.x * 100.0 + 5.0,
            unit.coordinate.y * 100.0 + 5.0,
        )
    }
}
