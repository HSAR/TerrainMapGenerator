package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.*

class OperationTransformsTest {
    private val unitTemplateId = OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val graphicTemplateId = OperationGraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000002"))

    @Test
    fun `moves units graphics and drawings as one bounded selection`() {
        val unit = unit(3, 0.2, 0.2)
        val graphic = graphic(4, 0.4, 0.4)
        val drawing = OperationDrawingObject(id(5), OperationLineDrawing(OperationDrawingPoint(0.1, 0.1), OperationDrawingPoint(0.2, 0.2), OperationArgb(0xffabcdef.toInt())))
        val content = OperationContent(objects = listOf(unit, graphic, drawing))

        val moved = content.move(content.objects.mapTo(hashSetOf(), OperationObject::id), MapCoordinateDelta(2.0, 2.0), MapCoordinateBounds(0.0, 0.0, 1.0, 1.0), 1.0)

        assertThat(moved.objects.maxOf { it.selectionBounds(1.0).right }, closeTo(1.0, 0.000001))
        assertThat((moved.objects[2] as OperationDrawingObject).drawing is OperationLineDrawing, equalTo(true))
    }

    @Test
    fun `resizing a graphic updates every instance and its template size`() {
        val first = graphic(6, 0.3, 0.3)
        val second = graphic(7, 0.7, 0.7)
        val template = OperationGraphicTemplate(graphicTemplateId, "Graphic", image(), 0)
        val content = OperationContent(objects = listOf(first, second), graphicTemplates = listOf(template))

        val resized = content.resize(setOf(first.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(0.5, 0.5), 1.0)

        val graphics = resized.objects.filterIsInstance<OperationGraphic>()
        assertThat(graphics[0].normalizedWidth, equalTo(graphics[1].normalizedWidth))
        assertThat(resized.graphicTemplates.single().normalizedWidth, equalTo(graphics[0].normalizedWidth))
    }

    @Test
    fun `rotation updates unit facing and every drawing shape`() {
        val unit = unit(8, 0.4, 0.5)
        val drawings = listOf(
            OperationDrawingObject(id(9), OperationFreeformDrawing(listOf(OperationDrawingPoint(0.2, 0.2), OperationDrawingPoint(0.3, 0.3)), OperationArgb(0xffffffff.toInt()))),
            OperationDrawingObject(id(10), OperationLineDrawing(OperationDrawingPoint(0.2, 0.3), OperationDrawingPoint(0.4, 0.3), OperationArgb(0xffffffff.toInt()))),
            OperationDrawingObject(id(11), OperationRectangleDrawing(OperationDrawingPoint(0.2, 0.2), OperationDrawingPoint(0.4, 0.4), OperationArgb(0xffffffff.toInt()))),
            OperationDrawingObject(id(12), OperationOvalDrawing(OperationDrawingPoint(0.6, 0.6), 0.1, 0.05, OperationArgb(0xffffffff.toInt()))),
        )
        val content = OperationContent(objects = listOf(unit) + drawings)

        val rotated = content.rotate(content.objects.mapTo(hashSetOf(), OperationObject::id), Math.PI / 2.0, 1.0, snap = true)

        assertThat((rotated.objects.first() as OperationUnit).facingDegrees, equalTo(90))
        assertThat(((rotated.objects[3] as OperationDrawingObject).drawing as OperationRectangleDrawing).rotationDegrees, equalTo(90.0))
        assertThat(((rotated.objects[4] as OperationDrawingObject).drawing as OperationOvalDrawing).rotationDegrees, equalTo(90.0))
    }

    private fun unit(number: Int, x: Double, y: Double) = OperationUnit(id(number), unitTemplateId, "Unit", false, 0, MapCoordinate(x, y), 0.04, 0.04)
    private fun graphic(number: Int, x: Double, y: Double) = OperationGraphic(id(number), graphicTemplateId, "Graphic", MapCoordinate(x, y), 0.1, 0.1)
    private fun id(number: Int) = OperationObjectId(UUID(0L, number.toLong()))
    private fun image() = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
}
