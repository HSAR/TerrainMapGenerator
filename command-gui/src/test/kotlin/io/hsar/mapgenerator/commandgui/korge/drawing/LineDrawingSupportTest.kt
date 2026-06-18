package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color

class LineDrawingSupportTest {

    @Test
    fun `outline builder returns a single segment for line drawings`() {
        val coordinateSpace = MapCoordinateSpace(
            geometry = ViewerGeometry(1000, 800, 0, 0),
            mapWidth = 1000,
            mapHeight = 800,
        )
        val drawing = LineDrawing(
            start = MapDrawingPoint(0.1, 0.2),
            end = MapDrawingPoint(0.4, 0.6),
            color = Color.RED,
        )

        val segments = MapDrawingOutlineBuilder.build(drawing, zoom = 1.0, coordinateSpace = coordinateSpace)

        assertThat(
            segments,
            equalTo(listOf(MapDrawingSegment(drawing.start, drawing.end, Color.RED))),
        )
    }

    @Test
    fun `hit testing uses the line segment geometry`() {
        val drawing = LineDrawing(
            start = MapDrawingPoint(0.0, 0.0),
            end = MapDrawingPoint(1.0, 1.0),
            color = Color.RED,
        )

        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.5, 0.52), tolerance = 0.01),
            equalTo(true),
        )
        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.5, 0.7), tolerance = 0.01),
            equalTo(false),
        )
    }

    @Test
    fun `translator moves both endpoints of line drawings`() {
        val drawing = LineDrawing(
            start = MapDrawingPoint(0.2, 0.3),
            end = MapDrawingPoint(0.5, 0.7),
            color = Color.RED,
        )

        val translated = MapDrawingTranslator.translate(drawing, MapCoordinateDelta(0.1, -0.2))

        translated as LineDrawing
        assertThat(translated.start.x, closeTo(0.3, 1e-9))
        assertThat(translated.start.y, closeTo(0.1, 1e-9))
        assertThat(translated.end.x, closeTo(0.6, 1e-9))
        assertThat(translated.end.y, closeTo(0.5, 1e-9))
        assertThat(translated.color, equalTo(Color.RED))
    }
}
