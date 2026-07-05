package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
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
        ).single() as DrawingSnapshot

        val resizedOval = resized.drawing as OvalDrawing
        assertPoint(resizedOval.center, 0.4, 0.35)
        assertThat(resizedOval.xRadius, closeTo(0.2, 0.0001))
        assertThat(resizedOval.yRadius, closeTo(0.15, 0.0001))
        assertThat(resizedOval.rotationDegrees, closeTo(30.0, 0.0001))
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

    private fun assertPoint(point: MapDrawingPoint, x: Double, y: Double) {
        assertThat(point.x, closeTo(x, 0.0001))
        assertThat(point.y, closeTo(y, 0.0001))
    }

    private fun id(value: String) =
        MapPlacedObjectId(UUID.fromString(value))
}
