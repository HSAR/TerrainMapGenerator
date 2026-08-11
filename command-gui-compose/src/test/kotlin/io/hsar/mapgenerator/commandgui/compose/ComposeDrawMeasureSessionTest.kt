package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapCoordinateBounds
import io.hsar.mapgenerator.commandgui.MapViewerDrawingColour
import io.hsar.mapgenerator.commandgui.MapViewerDrawingMode
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.MgrsCoord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ComposeDrawMeasureSessionTest {
    private val padded = MapCoordinateBounds(-.33, -.33, 1.33, 1.33)

    @Test
    fun `blocked press never owns a drawing gesture`() {
        assertThat(
            ComposeDrawMeasureSession.startDrawing(MapViewerDrawingMode.LINE, MapViewerDrawingColour.RED.argb, MapCoordinate(.2, .2), padded, blocked = true),
            equalTo(null),
        )
    }

    @Test
    fun `drawing release commits the last padded-background-clamped preview`() {
        val session = ComposeDrawMeasureSession.startDrawing(
            MapViewerDrawingMode.LINE, MapViewerDrawingColour.RED.argb, MapCoordinate(-.2, .2), padded,
        )!!.update(MapCoordinate(2.0, .4), false, 1.0, .01, null, 100, 100)

        val drawing = session.finishDrawing() as io.hsar.mapgenerator.commandgui.operation.OperationLineDrawing
        assertThat(drawing.end.x, equalTo(1.33))
    }

    @Test
    fun `measurement requires map origin and grid then clamps its destination`() {
        assertThat(ComposeDrawMeasureSession.startMeasurement(MapCoordinate(-.1, .2), grid()), equalTo(null))
        assertThat(ComposeDrawMeasureSession.startMeasurement(MapCoordinate(.1, .2), null), equalTo(null))

        val session = ComposeDrawMeasureSession.startMeasurement(MapCoordinate(.1, .2), grid())!!
            .update(MapCoordinate(2.0, -.5), false, 1.0, .01, grid(), 100, 100)
        assertThat(session.measurement!!.destination, equalTo(MapCoordinate(1.0, 0.0)))
    }

    @Test
    fun `release commits a drawing while cancellation and inactive release do not`() {
        val started = ComposeDrawMeasureSession.startDrawing(
            MapViewerDrawingMode.LINE, MapViewerDrawingColour.RED.argb, MapCoordinate(.1, .1), padded,
        )!!
        val updated = started.update(MapCoordinate(.2, .2), false, 1.0, .01, null, 100, 100)

        assertThat(updated.release() is ComposeDrawMeasureOutcome.DrawingCommitted, equalTo(true))
        assertThat(updated.cancel(), equalTo(ComposeDrawMeasureOutcome.Cleared))
        assertThat(ComposeDrawMeasureSession().release(), equalTo(ComposeDrawMeasureOutcome.NoOp))
    }

    private fun grid() = GridLayer(
        "TEST", gridScale = 1000, unitsPerPixel = 1.0, detailLevel = 1, pixelsPerLine = 10.0,
        verticalLineNumberStart = MgrsCoord.fromMetres(0), horizontalLineNumberStart = MgrsCoord.fromMetres(0),
        verticalLineCount = 10, horizontalLineCount = 10,
    )
}
