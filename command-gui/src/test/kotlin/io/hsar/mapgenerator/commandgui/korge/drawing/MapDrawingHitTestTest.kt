package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapDrawingHitTestTest {

    @Test
    fun `rectangle hit test follows the outline`() {
        val drawing = RectangleDrawing(
            start = MapDrawingPoint(10.0, 10.0),
            end = MapDrawingPoint(40.0, 30.0),
            color = Palette.Colours.RED,
        )

        assertThat(MapDrawingHitTest.contains(drawing, MapDrawingPoint(25.0, 10.0), tolerance = 2.0), equalTo(true))
        assertThat(MapDrawingHitTest.contains(drawing, MapDrawingPoint(25.0, 20.0), tolerance = 2.0), equalTo(false))
        assertThat(MapDrawingHitTest.contains(drawing, MapDrawingPoint(5.0, 5.0), tolerance = 2.0), equalTo(false))
    }

    @Test
    fun `oval hit test tracks the circumference`() {
        val coordinateSpace = MapCoordinateSpace(
            geometry = ViewerGeometry(
                backgroundWidth = 4000,
                backgroundHeight = 2000,
                mapOffsetX = 0,
                mapOffsetY = 0,
            ),
            mapWidth = 2000,
            mapHeight = 1000,
        )
        val drawing = OvalDrawing(
            center = MapDrawingPoint(0.5, 0.5),
            xRadius = 0.05,
            yRadius = 0.1,
            color = Palette.Colours.RED,
        )

        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.55, 0.5), tolerance = 0.001, coordinateSpace = coordinateSpace),
            equalTo(true),
        )
        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.6, 0.5), tolerance = 0.001, coordinateSpace = coordinateSpace),
            equalTo(false),
        )
        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.5, 0.6), tolerance = 0.001, coordinateSpace = coordinateSpace),
            equalTo(true),
        )
        assertThat(
            MapDrawingHitTest.contains(drawing, MapDrawingPoint(0.5, 0.5), tolerance = 0.001, coordinateSpace = coordinateSpace),
            equalTo(false),
        )
    }

    @Test
    fun `freeform hit test stays generous enough to grab thin strokes`() {
        val drawing = FreeformDrawing(
            points = listOf(
                MapDrawingPoint(10.0, 10.0),
                MapDrawingPoint(30.0, 10.0),
            ),
            color = Palette.Colours.RED,
        )

        assertThat(MapDrawingHitTest.contains(drawing, MapDrawingPoint(20.0, 12.0), tolerance = 0.5), equalTo(true))
        assertThat(MapDrawingHitTest.contains(drawing, MapDrawingPoint(20.0, 18.0), tolerance = 0.5), equalTo(false))
    }
}
