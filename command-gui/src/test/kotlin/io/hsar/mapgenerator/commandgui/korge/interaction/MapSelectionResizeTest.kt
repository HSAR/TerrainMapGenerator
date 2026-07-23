package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class MapSelectionResizeTest {
    @Test
    fun `single selection anchors opposite corner unless alt anchors center`() {
        val bounds = MapSelectionBounds(0.2, 0.3, 0.8, 0.9)

        assertThat(
            MapSelectionResize.anchorFor(bounds, ScreenSelectionCorner.TOP_LEFT, selectionSize = 1, altHeld = false),
            equalTo(MapCoordinate(0.8, 0.9)),
        )
        assertThat(
            MapSelectionResize.anchorFor(bounds, ScreenSelectionCorner.TOP_LEFT, selectionSize = 1, altHeld = true),
            equalTo(MapCoordinate(0.5, 0.6)),
        )
    }

    @Test
    fun `multi selection anchors center unless alt anchors opposite corner`() {
        val bounds = MapSelectionBounds(0.2, 0.3, 0.8, 0.9)

        assertThat(
            MapSelectionResize.anchorFor(bounds, ScreenSelectionCorner.TOP_RIGHT, selectionSize = 2, altHeld = false),
            equalTo(MapCoordinate(0.5, 0.6)),
        )
        assertThat(
            MapSelectionResize.anchorFor(bounds, ScreenSelectionCorner.TOP_RIGHT, selectionSize = 2, altHeld = true),
            equalTo(MapCoordinate(0.2, 0.9)),
        )
    }

    @Test
    fun `freeform line and rectangle points resize around the anchor`() {
        val freeform = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000101"),
            drawing = FreeformDrawing(
                points = listOf(MapDrawingPoint(0.2, 0.2), MapDrawingPoint(0.4, 0.4)),
                color = Color.RED,
            ),
        )
        val line = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000102"),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.2, 0.2),
                end = MapDrawingPoint(0.4, 0.4),
                color = Color.RED,
            ),
        )
        val rectangle = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000103"),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.2, 0.2),
                end = MapDrawingPoint(0.4, 0.4),
                color = Color.RED,
            ),
        )

        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(freeform, line, rectangle),
            bounds = MapSelectionBounds(0.2, 0.2, 0.4, 0.4),
            grabbedCorner = ScreenSelectionCorner.BOTTOM_RIGHT,
            anchor = MapCoordinate(0.2, 0.2),
            draggedCorner = MapCoordinate(0.6, 0.5),
        ).filterIsInstance<DrawingSnapshot>()

        val resizedFreeform = resized[0].drawing as FreeformDrawing
        val resizedLine = resized[1].drawing as LineDrawing
        val resizedRectangle = resized[2].drawing as RectangleDrawing
        assertPoint(resizedFreeform.points[1], 0.6, 0.5)
        assertPoint(resizedLine.end, 0.6, 0.5)
        assertPoint(resizedRectangle.end, 0.6, 0.5)
    }

    @Test
    fun `oval resize updates center and radii and preserves rotation`() {
        val oval = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000104"),
            drawing = OvalDrawing(
                center = MapDrawingPoint(0.3, 0.3),
                xRadius = 0.1,
                yRadius = 0.1,
                color = Color.RED,
                rotationDegrees = 30.0,
            ),
        )

        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(oval),
            bounds = MapSelectionBounds(0.2, 0.2, 0.4, 0.4),
            grabbedCorner = ScreenSelectionCorner.BOTTOM_RIGHT,
            anchor = MapCoordinate(0.2, 0.2),
            draggedCorner = MapCoordinate(0.6, 0.5),
            originalFixedCorner = oval.selectionCorner(ScreenSelectionCorner.TOP_LEFT),
        ).single() as DrawingSnapshot

        val resizedOval = resized.drawing as OvalDrawing
        val expected = expectedRotatedOvalResize(
            rotationDegrees = 30.0,
            originalFixedCorner = oval.selectionCorner(ScreenSelectionCorner.TOP_LEFT),
            draggedCorner = MapCoordinate(0.6, 0.5),
        )
        assertPoint(resizedOval.center, expected.center.x, expected.center.y)
        assertThat(resizedOval.xRadius, closeTo(expected.xRadius, 0.0001))
        assertThat(resizedOval.yRadius, closeTo(expected.yRadius, 0.0001))
        assertThat(resizedOval.rotationDegrees, closeTo(30.0, 0.0001))
        assertCoordinate(
            DrawingSnapshot(oval.id, resizedOval).selectionCorner(ScreenSelectionCorner.TOP_LEFT),
            oval.selectionCorner(ScreenSelectionCorner.TOP_LEFT),
        )
        assertCoordinate(
            DrawingSnapshot(oval.id, resizedOval).selectionCorner(ScreenSelectionCorner.BOTTOM_RIGHT),
            MapCoordinate(0.6, 0.5),
        )
    }

    @Test
    fun `single rotated rectangle resize follows the rectangle local axes`() {
        val rectangle = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000107"),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.0, 0.0),
                end = MapDrawingPoint(2.0, 1.0),
                color = Color.RED,
                rotationDegrees = 90.0,
            ),
        )

        val originalFixedCorner = rectangle.selectionCorner(ScreenSelectionCorner.BOTTOM_LEFT)
        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(rectangle),
            bounds = MapSelectionBounds(0.0, 0.0, 2.0, 1.0),
            grabbedCorner = ScreenSelectionCorner.TOP_RIGHT,
            anchor = MapCoordinate(0.0, 1.0),
            draggedCorner = MapCoordinate(1.5, 3.5),
            originalFixedCorner = originalFixedCorner,
            selectionSize = 1,
            aspectRatio = 1.0,
        )
            .single() as DrawingSnapshot

        val rotated = resized.drawing as RectangleDrawing
        val expected = expectedRotatedRectangleResize(
            rotationDegrees = 90.0,
            originalFixedCorner = originalFixedCorner,
            draggedCorner = MapCoordinate(1.5, 3.5),
        )
        assertPoint(rotated.start, expected.start.x, expected.start.y)
        assertPoint(rotated.end, expected.end.x, expected.end.y)
        assertThat(rotated.rotationDegrees, closeTo(90.0, 0.0001))
        assertCoordinate(
            DrawingSnapshot(rectangle.id, rotated).selectionCorner(ScreenSelectionCorner.BOTTOM_LEFT),
            originalFixedCorner,
        )
        assertCoordinate(
            DrawingSnapshot(rectangle.id, rotated).selectionCorner(ScreenSelectionCorner.TOP_RIGHT),
            MapCoordinate(1.5, 3.5),
        )
    }

    @Test
    fun `center anchored rotated rectangle resize keeps center fixed and follows local axes`() {
        val rectangle = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000108"),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.0, 0.0),
                end = MapDrawingPoint(2.0, 1.0),
                color = Color.RED,
                rotationDegrees = 90.0,
            ),
        )

        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(rectangle),
            bounds = MapSelectionBounds(0.0, 0.0, 2.0, 1.0),
            grabbedCorner = ScreenSelectionCorner.TOP_RIGHT,
            anchor = MapCoordinate(1.0, 0.5),
            draggedCorner = MapCoordinate(1.5, 3.5),
            originalFixedCorner = rectangle.selectionCorner(ScreenSelectionCorner.BOTTOM_LEFT),
            selectionSize = 1,
            aspectRatio = 1.0,
        )
            .single() as DrawingSnapshot

        val rotated = resized.drawing as RectangleDrawing
        assertPoint(MapDrawingGeometry.center(rotated.start, rotated.end), 1.0, 0.5)
        assertCoordinate(
            DrawingSnapshot(rectangle.id, rotated).selectionCorner(ScreenSelectionCorner.TOP_RIGHT),
            MapCoordinate(1.5, 3.5),
        )
        assertCoordinate(
            DrawingSnapshot(rectangle.id, rotated).selectionCorner(ScreenSelectionCorner.BOTTOM_LEFT),
            MapCoordinate(0.5, -2.5),
        )
    }

    @Test
    fun `unit snapshots remain unchanged during mixed selection resize`() {
        val unit = UnitSnapshot(
            id = id("00000000-0000-0000-0000-000000000105"),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000110")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000110")), "Unit", Bitmap32(4, 4)),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.7, 0.7),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )
        val drawing = DrawingSnapshot(
            id = id("00000000-0000-0000-0000-000000000106"),
            drawing = LineDrawing(MapDrawingPoint(0.2, 0.2), MapDrawingPoint(0.4, 0.4), Color.RED),
        )

        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(unit, drawing),
            bounds = MapSelectionBounds(0.2, 0.2, 0.75, 0.75),
            grabbedCorner = ScreenSelectionCorner.BOTTOM_RIGHT,
            anchor = MapCoordinate(0.2, 0.2),
            draggedCorner = MapCoordinate(1.0, 1.0),
        )

        assertThat(resized[0], equalTo(unit))
        assertThat((resized[1] as DrawingSnapshot).drawing == drawing.drawing, equalTo(false))
    }

    @Test
    fun `graphic resize keeps aspect ratio and uses one scale for position`() {
        val templateId = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000120"))
        val graphic = GraphicSnapshot(
            id = id("00000000-0000-0000-0000-000000000121"),
            graphicTemplateId = templateId,
            template = MapViewerGraphicTemplate(templateId, "Graphic", Bitmap32(200, 100)),
            instanceName = "Graphic",
            mapCoordinate = MapCoordinate(0.4, 0.3),
            normalizedWidth = 0.4,
            normalizedHeight = 0.2,
        )

        val resized = MapSelectionResize.resizeSnapshots(
            originalSnapshots = listOf(graphic),
            bounds = MapSelectionBounds(0.2, 0.2, 0.6, 0.4),
            grabbedCorner = ScreenSelectionCorner.BOTTOM_RIGHT,
            anchor = MapCoordinate(0.2, 0.2),
            draggedCorner = MapCoordinate(1.0, 0.8),
        ).single() as GraphicSnapshot

        assertThat(resized.normalizedWidth / resized.normalizedHeight, closeTo(2.0, 0.0001))
        assertThat(resized.mapCoordinate.x, closeTo(0.8, 0.0001))
        assertThat(resized.mapCoordinate.y, closeTo(0.5, 0.0001))
    }

    private fun assertPoint(point: MapDrawingPoint, x: Double, y: Double) {
        assertThat(point.x, closeTo(x, 0.0001))
        assertThat(point.y, closeTo(y, 0.0001))
    }

    private fun assertCoordinate(actual: MapCoordinate, expected: MapCoordinate) {
        assertThat(actual.x, closeTo(expected.x, 0.0001))
        assertThat(actual.y, closeTo(expected.y, 0.0001))
    }

    private fun expectedRotatedRectangleResize(
        rotationDegrees: Double,
        originalFixedCorner: MapCoordinate,
        draggedCorner: MapCoordinate,
    ): RectangleDrawing {
        val center = originalFixedCorner.midpoint(draggedCorner).toDrawingPoint()
        val fixedLocal = originalFixedCorner.rotateAround(center, -rotationDegrees, 1.0)
        val draggedLocal = draggedCorner.rotateAround(center, -rotationDegrees, 1.0)
        val localBounds = MapSelectionBounds.fromPoints(fixedLocal.toCoordinate(), draggedLocal.toCoordinate())
        return RectangleDrawing(
            start = MapDrawingPoint(localBounds.left, localBounds.top),
            end = MapDrawingPoint(localBounds.right, localBounds.bottom),
            color = Color.RED,
            rotationDegrees = rotationDegrees,
        )
    }

    private fun expectedRotatedOvalResize(
        rotationDegrees: Double,
        originalFixedCorner: MapCoordinate,
        draggedCorner: MapCoordinate,
    ): OvalDrawing {
        val center = originalFixedCorner.midpoint(draggedCorner).toDrawingPoint()
        val fixedLocal = originalFixedCorner.rotateAround(center, -rotationDegrees, 1.0)
        val draggedLocal = draggedCorner.rotateAround(center, -rotationDegrees, 1.0)
        val localBounds = MapSelectionBounds.fromPoints(fixedLocal.toCoordinate(), draggedLocal.toCoordinate())
        return OvalDrawing(
            center = MapDrawingPoint(
                x = (localBounds.left + localBounds.right) * 0.5,
                y = (localBounds.top + localBounds.bottom) * 0.5,
            ),
            xRadius = (localBounds.right - localBounds.left) * 0.5,
            yRadius = (localBounds.bottom - localBounds.top) * 0.5,
            color = Color.RED,
            rotationDegrees = rotationDegrees,
        )
    }

    private fun scale(current: Double, anchor: Double, original: Double): Double {
        val originalDelta = original - anchor
        return if (kotlin.math.abs(originalDelta) <= 0.0000001) {
            1.0
        } else {
            ((current - anchor) / originalDelta).coerceAtLeast(0.01)
        }
    }

    private fun MapDrawingPoint.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint {
        val radians = Math.toRadians(rotationDegrees)
        val dx = (x - pivot.x) * aspectRatio
        val dy = y - pivot.y
        val cos = kotlin.math.cos(radians)
        val sin = kotlin.math.sin(radians)
        return MapDrawingPoint(
            x = pivot.x + (dx * cos - dy * sin) / aspectRatio,
            y = pivot.y + dx * sin + dy * cos,
        )
    }

    private fun MapCoordinate.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint =
        MapDrawingPoint(x, y).rotateAround(pivot, rotationDegrees, aspectRatio)

    private fun MapCoordinate.midpoint(other: MapCoordinate): MapCoordinate =
        MapCoordinate(
            x = (x + other.x) * 0.5,
            y = (y + other.y) * 0.5,
        )

    private fun MapCoordinate.toDrawingPoint(): MapDrawingPoint =
        MapDrawingPoint(x, y)

    private fun MapDrawingPoint.resize(anchor: MapCoordinate, scaleX: Double, scaleY: Double): MapDrawingPoint =
        MapDrawingPoint(
            x = anchor.x + (x - anchor.x) * scaleX,
            y = anchor.y + (y - anchor.y) * scaleY,
        )

    private fun MapDrawingPoint.toCoordinate(): MapCoordinate =
        MapCoordinate(x, y)

    private fun DrawingSnapshot.selectionCorner(corner: ScreenSelectionCorner): MapCoordinate {
        val drawing = drawing
        return when (drawing) {
            is RectangleDrawing -> {
                val points = MapDrawingGeometry.rectangleCorners(drawing.start, drawing.end, drawing.rotationDegrees, 1.0)
                points[corner.index()].toCoordinate()
            }

            is OvalDrawing -> {
                val bounds = MapSelectionBounds(
                    left = drawing.center.x - drawing.xRadius,
                    top = drawing.center.y - drawing.yRadius,
                    right = drawing.center.x + drawing.xRadius,
                    bottom = drawing.center.y + drawing.yRadius,
                )
                val point = when (corner) {
                    ScreenSelectionCorner.TOP_LEFT -> MapDrawingPoint(bounds.left, bounds.top)
                    ScreenSelectionCorner.TOP_RIGHT -> MapDrawingPoint(bounds.right, bounds.top)
                    ScreenSelectionCorner.BOTTOM_RIGHT -> MapDrawingPoint(bounds.right, bounds.bottom)
                    ScreenSelectionCorner.BOTTOM_LEFT -> MapDrawingPoint(bounds.left, bounds.bottom)
                }
                point.rotateAround(drawing.center, drawing.rotationDegrees, 1.0).toCoordinate()
            }

            else -> error("Unsupported drawing")
        }
    }

    private fun ScreenSelectionCorner.index(): Int =
        when (this) {
            ScreenSelectionCorner.TOP_LEFT -> 0
            ScreenSelectionCorner.TOP_RIGHT -> 1
            ScreenSelectionCorner.BOTTOM_RIGHT -> 2
            ScreenSelectionCorner.BOTTOM_LEFT -> 3
        }

    private fun id(value: String) =
        MapPlacedObjectId(UUID.fromString(value))
}
