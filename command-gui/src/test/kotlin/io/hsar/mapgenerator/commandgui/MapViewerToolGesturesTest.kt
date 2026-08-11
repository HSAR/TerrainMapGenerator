package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerToolGesturesTest {
    private val bounds = MapCoordinateBounds(-.33, -.33, 1.33, 1.33)
    @Test
    fun `measure reports legacy east bearing and distance`() {
        val value = measureMapDrag(layer(), MapCoordinate(.1, .1), MapCoordinate(.2, .1), 100, 100)
        assertThat(value.bearingDegrees, equalTo(90)); assertThat(value.distanceText, equalTo("100m"))
    }

    @Test
    fun `freeform requires two sufficiently distant points`() {
        val start = MapViewerDrawingMode.FREEFORM.startedAt(MapCoordinate(.1, .1), MapViewerDrawingColour.RED.argb, bounds)!!
        assertThat(start.update(MapCoordinate(.101, .1), false, 1.0, .01).committedOrNull(), equalTo(null))
        assertThat(start.update(MapCoordinate(.2, .1), false, 1.0, .01).committedOrNull() != null, equalTo(true))
    }

    @Test
    fun `shift constrains rectangle and oval in physical map space`() {
        val rectangle = MapViewerDrawingMode.RECTANGLE.startedAt(MapCoordinate(.2, .2), MapViewerDrawingColour.RED.argb, bounds)!!.update(MapCoordinate(.4, .3), true, 2.0, .01)
        val oval = MapViewerDrawingMode.OVAL.startedAt(MapCoordinate(.2, .2), MapViewerDrawingColour.RED.argb, bounds)!!.update(MapCoordinate(.4, .3), true, 2.0, .01)
        assertThat((rectangle.preview as io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing).end.y, closeTo(.6, .000001))
        assertThat((oval.preview as io.hsar.mapgenerator.commandgui.operation.OperationOvalDrawing).yRadius, closeTo(.2, .000001))
    }

    @Test
    fun `line rectangle and oval reject zero extent`() {
        MapViewerDrawingMode.entries.filterNot { it == MapViewerDrawingMode.FREEFORM }.forEach { mode ->
            assertThat(mode.startedAt(MapCoordinate(.5, .5), MapViewerDrawingColour.RED.argb, bounds)!!.committedOrNull(), equalTo(null))
        }
    }

    @Test
    fun `drawing starts within padded background and clamps subsequent points`() {
        assertThat(MapViewerDrawingMode.LINE.startedAt(MapCoordinate(-.2, 1.2), MapViewerDrawingColour.BLUE.argb, bounds) != null, equalTo(true))
        assertThat(MapViewerDrawingMode.LINE.startedAt(MapCoordinate(-.4, .5), MapViewerDrawingColour.BLUE.argb, bounds), equalTo(null))

        val gesture = MapViewerDrawingMode.LINE.startedAt(MapCoordinate(0.0, 0.0), MapViewerDrawingColour.BLUE.argb, bounds)!!
            .update(MapCoordinate(2.0, -2.0), false, 1.0, .01)
        assertThat((gesture.preview as io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing).end, equalTo(
            io.hsar.mapgenerator.commandgui.operation.OperationDrawingPoint(1.33, -.33),
        ))
    }

    @Test
    fun `every mode commits when dragged in either direction`() {
        MapViewerDrawingMode.entries.forEach { mode ->
            listOf(MapCoordinate(.8, .7), MapCoordinate(-.2, -.1)).forEach { destination ->
                val gesture = mode.startedAt(MapCoordinate(.4, .4), MapViewerDrawingColour.GREEN.argb, bounds)!!
                    .update(destination, false, 1.5, .001)
                assertThat("$mode toward $destination", gesture.committedOrNull() != null, equalTo(true))
            }
        }
    }

    @Test
    fun `shift constraint is recalculated from the original start`() {
        val started = MapViewerDrawingMode.RECTANGLE.startedAt(
            MapCoordinate(.2, .2), MapViewerDrawingColour.WHITE.argb, bounds,
        )!!
        val constrained = started.update(MapCoordinate(.5, .3), true, 2.0, .001)
        val unconstrained = constrained.update(MapCoordinate(.5, .3), false, 2.0, .001)

        assertThat((constrained.preview as io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing).end.y, closeTo(.8, .000001))
        assertThat((unconstrained.preview as io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing).end.y, closeTo(.3, .000001))
    }

    @Test
    fun `shift constrained shapes retain physical aspect when fitted to background bounds`() {
        val rectangle = MapViewerDrawingMode.RECTANGLE.startedAt(
            MapCoordinate(.2, .2), MapViewerDrawingColour.WHITE.argb, bounds,
        )!!.update(MapCoordinate(2.0, .3), true, 2.0, .001).preview as io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing

        assertThat(rectangle.end.y, closeTo(bounds.bottom, .000001))
        assertThat((rectangle.end.x - rectangle.start.x) * 2.0, closeTo(rectangle.end.y - rectangle.start.y, .000001))
    }

    private fun layer() = GridLayer(
        "TEST", gridScale = 1000, unitsPerPixel = 10.0, detailLevel = 1, pixelsPerLine = 10.0,
        verticalLineNumberStart = MgrsCoord.fromMetres(0), horizontalLineNumberStart = MgrsCoord.fromMetres(0), verticalLineCount = 10, horizontalLineCount = 10
    )
}
