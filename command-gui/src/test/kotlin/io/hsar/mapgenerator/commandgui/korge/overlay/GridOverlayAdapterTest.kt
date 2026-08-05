package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.MapViewerGridLineSegment
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color

class GridOverlayAdapterTest {
    @Test
    fun `screen line converts to scaled map-local rectangle`() {
        val rect = MapViewerGridLineSegment(ViewerPoint(120.0, 150.0), ViewerPoint(120.0, 210.0), 2.0, Color.RED)
            .toKorgeGridRect(state())

        assertThat(rect, equalTo(KorgeGridRect(49.5, 60.0, 1.0, 30.0)))
    }

    private fun state() = GridOverlayState(
        Rectangle(Point.ORIGIN, Point(100.0, 100.0)), 0.0, 0.0, 20.0, 30.0, 2.0,
        100, 100, 200, 200, 0.0, 0.0, 200.0, 200.0, null, null,
        GridLayer(
            "GRID", gridScale = 1000, unitsPerPixel = 10.0, detailLevel = 2, pixelsPerLine = 50.0,
            verticalLineNumberStart = io.hsar.mapgenerator.grid.MgrsCoord.fromMetres(0), horizontalLineNumberStart = io.hsar.mapgenerator.grid.MgrsCoord.fromMetres(0),
            verticalLineCount = 2, horizontalLineCount = 2
        ),
    )
}
