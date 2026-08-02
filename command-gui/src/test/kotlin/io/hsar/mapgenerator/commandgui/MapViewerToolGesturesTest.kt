package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerToolGesturesTest {
    @Test
    fun `measure reports legacy east bearing and distance`() {
        val value = measureMapDrag(layer(), MapCoordinate(.1, .1), MapCoordinate(.2, .1), 100, 100)
        assertThat(value.bearingDegrees, equalTo(90)); assertThat(value.distanceText, equalTo("100m"))
    }

    @Test
    fun `freeform requires two sufficiently distant points`() {
        val start = MapViewerDrawingMode.FREEFORM.startedAt(MapCoordinate(.1, .1), MapViewerDrawingColour.RED.argb)
        assertThat(start.update(MapCoordinate(.101, .1), false, 1.0, .01).committedOrNull(), equalTo(null))
        assertThat(start.update(MapCoordinate(.2, .1), false, 1.0, .01).committedOrNull() != null, equalTo(true))
    }

    @Test
    fun `shift constrains rectangle and oval in physical map space`() {
        val rectangle = MapViewerDrawingMode.RECTANGLE.startedAt(MapCoordinate(.2, .2), MapViewerDrawingColour.RED.argb).update(MapCoordinate(.4, .3), true, 2.0, .01)
        val oval = MapViewerDrawingMode.OVAL.startedAt(MapCoordinate(.2, .2), MapViewerDrawingColour.RED.argb).update(MapCoordinate(.4, .3), true, 2.0, .01)
        assertThat((rectangle.preview as io.hsar.mapgenerator.commandgui.operation.OperationRectangleDrawing).end.y, closeTo(.6, .000001))
        assertThat((oval.preview as io.hsar.mapgenerator.commandgui.operation.OperationOvalDrawing).yRadius, closeTo(.2, .000001))
    }

    @Test
    fun `line rectangle and oval reject zero extent`() {
        MapViewerDrawingMode.entries.filterNot { it == MapViewerDrawingMode.FREEFORM }.forEach { mode ->
            assertThat(mode.startedAt(MapCoordinate(.5, .5), MapViewerDrawingColour.RED.argb).committedOrNull(), equalTo(null))
        }
    }

    private fun layer() = GridLayer(
        "TEST", gridScale = 1000, unitsPerPixel = 10.0, detailLevel = 1, pixelsPerLine = 10.0,
        verticalLineNumberStart = MgrsCoord.fromMetres(0), horizontalLineNumberStart = MgrsCoord.fromMetres(0), verticalLineCount = 10, horizontalLineCount = 10
    )
}
