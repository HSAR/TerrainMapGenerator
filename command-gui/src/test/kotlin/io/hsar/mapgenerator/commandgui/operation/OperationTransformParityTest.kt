package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

class OperationTransformParityTest {
    @Test
    fun `move clamps the pointer and passes Shift to the unit constraint`() {
        val unit = unit(1, .4, .4)
        var bypass = false
        val gesture = OperationTransformGesture.Move(
            OperationContent(objects = listOf(unit)), setOf(unit.id), MapCoordinate(.4, .4),
            MapCoordinateBounds(0.0, 0.0, 1.0, 1.0), 1.0,
        ) { moved, shift -> bypass = shift; moved.copy(coordinate = moved.coordinate.copy(y = .75)) }

        val moved = gesture.preview(MapCoordinate(2.0, 2.0), OperationTransformModifiers(shiftHeld = true)).objects.single() as OperationUnit

        assertThat(moved.coordinate.x, closeTo(1.0, EPSILON))
        assertThat(moved.coordinate.y, closeTo(.75, EPSILON))
        assertThat(bypass, equalTo(true))
    }

    @Test
    fun `completion reports no changes for a click without a drag`() {
        val unit = unit(2, .4, .4)
        val gesture = OperationTransformGesture.Move(
            OperationContent(objects = listOf(unit)), setOf(unit.id), unit.coordinate,
            MapCoordinateBounds(-.3, -.3, 1.3, 1.3), 1.0,
        )

        assertThat(gesture.complete(unit.coordinate).changeSet.isEmpty, equalTo(true))
    }

    @Test
    fun `line resize moves the selected endpoint`() {
        val line = drawing(3, OperationLineDrawing(point(.2, .2), point(.4, .4), colour))
        val resized = OperationContent(objects = listOf(line)).resize(
            setOf(line.id), MapSelectionCorner.TOP_LEFT, MapCoordinate(.1, .3), 1.0,
        ).drawing<OperationLineDrawing>()

        assertPoint(resized.start, .1, .3)
        assertPoint(resized.end, .4, .4)
    }

    @Test
    fun `Alt line resize mirrors the opposite endpoint around its centre`() {
        val line = drawing(4, OperationLineDrawing(point(.2, .2), point(.4, .4), colour))
        val resized = OperationContent(objects = listOf(line)).resize(
            setOf(line.id), MapSelectionCorner.TOP_LEFT, MapCoordinate(.1, .2), 1.0, fromCentre = true,
        ).drawing<OperationLineDrawing>()

        assertPoint(resized.start, .1, .2)
        assertPoint(resized.end, .5, .4)
    }

    @Test
    fun `single and group Alt modifiers invert the resize anchor`() {
        val first = drawing(5, OperationRectangleDrawing(point(.2, .2), point(.4, .4), colour))
        val second = drawing(6, OperationRectangleDrawing(point(.6, .6), point(.8, .8), colour))
        val content = OperationContent(objects = listOf(first, second))

        val single = content.resize(setOf(first.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(.5, .5), 1.0, fromCentre = true)
        val group = content.resize(setOf(first.id, second.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(1.0, 1.0), 1.0)

        assertPoint(single.drawing<OperationRectangleDrawing>(first.id).start, .1, .1)
        assertPoint(group.drawing<OperationRectangleDrawing>(first.id).start, 0.0, 0.0)
    }

    @Test
    fun `Shift locks axis aligned rectangles and ovals`() {
        val rectangle = drawing(7, OperationRectangleDrawing(point(.2, .2), point(.4, .4), colour))
        val oval = drawing(8, OperationOvalDrawing(point(.3, .3), .1, .1, colour))

        val resizedRectangle = OperationContent(objects = listOf(rectangle)).resize(
            setOf(rectangle.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(.6, .5), 1.0, preserveAspect = true,
        ).drawing<OperationRectangleDrawing>()
        val resizedOval = OperationContent(objects = listOf(oval)).resize(
            setOf(oval.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(.6, .5), 1.0, preserveAspect = true,
        ).drawing<OperationOvalDrawing>()

        assertThat(abs(resizedRectangle.end.x - resizedRectangle.start.x), closeTo(abs(resizedRectangle.end.y - resizedRectangle.start.y), EPSILON))
        assertThat(resizedOval.xRadius, closeTo(resizedOval.yRadius, EPSILON))
    }

    @Test
    fun `rotated rectangle resize keeps its fixed corner and rotation`() {
        val rectangle = drawing(9, OperationRectangleDrawing(point(0.0, 0.0), point(2.0, 1.0), colour, 90.0))
        val content = OperationContent(objects = listOf(rectangle))
        val fixed = MapCoordinate(.5, -.5)

        val resized = content.resize(
            setOf(rectangle.id), MapSelectionCorner.TOP_RIGHT, MapCoordinate(1.5, 3.5), 1.0,
            originalFixedCorner = fixed,
        ).drawing<OperationRectangleDrawing>()

        assertThat(resized.rotationDegrees, closeTo(90.0, EPSILON))
        assertPoint(resized.start, -1.0, 1.0)
        assertPoint(resized.end, 3.0, 2.0)
    }

    @Test
    fun `rotated oval centre resize retains centre and Shift locks radii`() {
        val oval = drawing(10, OperationOvalDrawing(point(1.0, .5), 1.0, .5, colour, 30.0))
        val resized = OperationContent(objects = listOf(oval)).resize(
            setOf(oval.id), MapSelectionCorner.TOP_RIGHT, MapCoordinate(1.7, 2.0), 1.0,
            fromCentre = true, preserveAspect = true, originalFixedCorner = MapCoordinate(.38, -.43),
        ).drawing<OperationOvalDrawing>()

        assertPoint(resized.centre, 1.0, .5)
        assertThat(resized.xRadius, closeTo(resized.yRadius, EPSILON))
        assertThat(resized.rotationDegrees, closeTo(30.0, EPSILON))
    }

    @Test
    fun `mixed resize leaves units unchanged`() {
        val unit = unit(11, .7, .7)
        val line = drawing(12, OperationLineDrawing(point(.2, .2), point(.4, .4), colour))
        val resized = OperationContent(objects = listOf(unit, line)).resize(
            setOf(unit.id, line.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(1.0, 1.0), 1.0,
        )

        assertThat(resized.objectById(unit.id), equalTo(unit))
        assertThat(resized.objectById(line.id) == line, equalTo(false))
    }

    @Test
    fun `graphic resize preserves ratio and propagates template size`() {
        val first = graphic(13, .4, .3)
        val second = graphic(14, .8, .8)
        val template = OperationGraphicTemplate(graphicTemplateId, "Graphic", image(), 0, .4, .2)
        val resized = OperationContent(objects = listOf(first, second), graphicTemplates = listOf(template)).resize(
            setOf(first.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(1.0, .8), 1.0,
        )
        val graphics = resized.objects.filterIsInstance<OperationGraphic>()

        assertThat(graphics[0].normalizedWidth / graphics[0].normalizedHeight, closeTo(2.0, EPSILON))
        assertThat(graphics[1].normalizedWidth, equalTo(graphics[0].normalizedWidth))
        assertThat(resized.graphicTemplates.single().normalizedWidth, equalTo(graphics[0].normalizedWidth))
    }

    @Test
    fun `rotation updates group positions and unit facings around the shared pivot`() {
        val north = unit(15, .5, .4, 10)
        val south = unit(16, .5, .6, null)
        val rotated = OperationContent(objects = listOf(north, south)).rotate(setOf(north.id, south.id), PI / 2.0, 1.0)
            .objects.filterIsInstance<OperationUnit>()

        assertThat(rotated[0].coordinate.x, closeTo(.6, EPSILON))
        assertThat(rotated[0].facingDegrees, equalTo(100))
        assertThat(rotated[1].coordinate.x, closeTo(.4, EPSILON))
        assertThat(rotated[1].facingDegrees, equalTo(90))
    }

    @Test
    fun `Shift rotation snaps drawing delta and absolute drawing rotation`() {
        val freeform = drawing(17, OperationFreeformDrawing(listOf(point(.4, .5), point(.5, .4), point(.6, .5), point(.5, .6)), colour))
        val rectangle = drawing(18, OperationRectangleDrawing(point(.4, .45), point(.6, .55), colour, 10.0))

        val rotatedFreeform = OperationContent(objects = listOf(freeform)).rotate(setOf(freeform.id), Math.toRadians(40.0), 1.0, true)
            .drawing<OperationFreeformDrawing>()
        val rotatedRectangle = OperationContent(objects = listOf(rectangle)).rotate(setOf(rectangle.id), PI / 4.0, 1.0, true)
            .drawing<OperationRectangleDrawing>()

        assertPoint(rotatedFreeform.points.first(), .4293, .4293, .0002)
        assertThat(rotatedRectangle.rotationDegrees, equalTo(45.0))
    }

    @Test
    fun `rotation bearing follows grid north clockwise`() {
        val origin = MapCoordinate(.5, .5)

        assertThat(rotationDelta(origin, MapCoordinate(.5, .4), MapCoordinate(.6, .5), 1.0), closeTo(PI / 2.0, EPSILON))
    }

    @Test
    fun `unshifted drawing rotation retains fractional degrees`() {
        val rectangle = drawing(19, OperationRectangleDrawing(point(.4, .45), point(.6, .55), colour))

        val rotated = OperationContent(objects = listOf(rectangle)).rotate(
            setOf(rectangle.id), Math.toRadians(12.5), 1.0,
        ).drawing<OperationRectangleDrawing>()

        assertThat(rotated.rotationDegrees, closeTo(12.5, EPSILON))
    }

    @Test
    fun `Shift snaps unit facing absolutely while retaining raw position rotation`() {
        val north = unit(20, .5, .4, 10)
        val south = unit(21, .5, .6, null)

        val rotated = OperationContent(objects = listOf(north, south)).rotate(
            setOf(north.id, south.id), Math.toRadians(40.0), 1.0, true,
        ).objects.filterIsInstance<OperationUnit>()

        assertThat(rotated[0].facingDegrees, equalTo(45))
        assertThat(rotated[0].coordinate.x, closeTo(.5643, .0002))
    }

    @Test
    fun `oval resize updates centre radii and preserves rotation`() {
        val oval = drawing(22, OperationOvalDrawing(point(.3, .3), .1, .05, colour))

        val resized = OperationContent(objects = listOf(oval)).resize(
            setOf(oval.id), MapSelectionCorner.BOTTOM_RIGHT, MapCoordinate(.6, .5), 1.0,
        ).drawing<OperationOvalDrawing>()

        assertPoint(resized.centre, .4, .375)
        assertThat(resized.xRadius, closeTo(.2, EPSILON))
        assertThat(resized.yRadius, closeTo(.125, EPSILON))
        assertThat(resized.rotationDegrees, equalTo(0.0))
    }

    @Test
    fun `centre anchored rotated rectangle follows local axes`() {
        val rectangle = drawing(23, OperationRectangleDrawing(point(0.0, 0.0), point(2.0, 1.0), colour, 90.0))

        val resized = OperationContent(objects = listOf(rectangle)).resize(
            setOf(rectangle.id), MapSelectionCorner.TOP_RIGHT, MapCoordinate(1.5, 3.5), 1.0,
            fromCentre = true, originalFixedCorner = MapCoordinate(.5, -.5),
        ).drawing<OperationRectangleDrawing>()

        assertPoint(resized.start, -2.0, 0.0)
        assertPoint(resized.end, 4.0, 1.0)
        assertThat(resized.rotationDegrees, equalTo(90.0))
    }

    @Test
    fun `Shift rotated rectangle retains rotation and becomes square in local axes`() {
        val rectangle = drawing(24, OperationRectangleDrawing(point(0.0, 0.0), point(2.0, 1.0), colour, 30.0))

        val resized = OperationContent(objects = listOf(rectangle)).resize(
            setOf(rectangle.id), MapSelectionCorner.TOP_RIGHT, MapCoordinate(2.0, 3.0), 1.0,
            preserveAspect = true, originalFixedCorner = MapCoordinate(.384, -.433),
        ).drawing<OperationRectangleDrawing>()

        assertThat(abs(resized.end.x - resized.start.x), closeTo(abs(resized.end.y - resized.start.y), EPSILON))
        assertThat(resized.rotationDegrees, equalTo(30.0))
    }

    @Test
    fun `freeform rotation uses aspect ratio when rotating around selection pivot`() {
        val freeform = drawing(25, OperationFreeformDrawing(listOf(point(.4, .5), point(.5, .4), point(.6, .5), point(.5, .6)), colour))

        val rotated = OperationContent(objects = listOf(freeform)).rotate(setOf(freeform.id), PI / 2.0, 2.0)
            .drawing<OperationFreeformDrawing>()

        assertPoint(rotated.points.first(), .5, .3)
    }

    private inline fun <reified T : OperationDrawing> OperationContent.drawing(id: OperationObjectId? = null) =
        ((if (id == null) objects.single() else objectById(id)) as OperationDrawingObject).drawing as T

    private fun drawing(number: Int, value: OperationDrawing) = OperationDrawingObject(id(number), value)
    private fun unit(number: Int, x: Double, y: Double, facing: Int? = 0) = OperationUnit(id(number), unitTemplateId, "Unit", false, facing, MapCoordinate(x, y), .1, .1)
    private fun graphic(number: Int, x: Double, y: Double) = OperationGraphic(id(number), graphicTemplateId, "Graphic", MapCoordinate(x, y), .4, .2)
    private fun point(x: Double, y: Double) = OperationDrawingPoint(x, y)
    private fun id(number: Int) = OperationObjectId(UUID(0L, number.toLong()))
    private fun image() = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
    private fun assertPoint(point: OperationDrawingPoint, x: Double, y: Double, tolerance: Double = EPSILON) {
        assertThat(point.x, closeTo(x, tolerance))
        assertThat(point.y, closeTo(y, tolerance))
    }

    private companion object {
        val colour = OperationArgb(0xffff0000.toInt())
        val unitTemplateId = OperationUnitTemplateId(UUID(0L, 100L))
        val graphicTemplateId = OperationGraphicTemplateId(UUID(0L, 101L))
        const val EPSILON = .0001
    }
}
