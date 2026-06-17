package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

class MapDrawingOutlineBuilderTest {

    @Test
    fun `freeform drawing emits one segment between each pair of points`() {
        val segments = MapDrawingOutlineBuilder.build(
            drawing = FreeformDrawing(
                points = listOf(
                    MapDrawingPoint(0.0, 0.0),
                    MapDrawingPoint(10.0, 0.0),
                    MapDrawingPoint(10.0, 10.0),
                ),
                color = Palette.Colours.RED,
            ),
            zoom = 1.0,
        )

        assertThat(segments.size, equalTo(2))
        assertThat(segments.first().start, equalTo(MapDrawingPoint(0.0, 0.0)))
        assertThat(segments.first().end, equalTo(MapDrawingPoint(10.0, 0.0)))
    }

    @Test
    fun `rectangle drawing emits four outline segments in map coordinates`() {
        val segments = MapDrawingOutlineBuilder.build(
            drawing = RectangleDrawing(
                start = MapDrawingPoint(10.0, 20.0),
                end = MapDrawingPoint(30.0, 50.0),
                color = Palette.Colours.RED,
            ),
            zoom = 1.0,
        )

        assertThat(segments.size, equalTo(4))
        assertThat(
            segments.map { it.start }, equalTo(
                listOf(
                    MapDrawingPoint(10.0, 20.0),
                    MapDrawingPoint(30.0, 20.0),
                    MapDrawingPoint(30.0, 50.0),
                    MapDrawingPoint(10.0, 50.0),
                ),
            )
        )
    }

    @Test
    fun `circle drawing emits a smooth outline with the drawing colour`() {
        val segments = MapDrawingOutlineBuilder.build(
            drawing = CircleDrawing(
                center = MapDrawingPoint(50.0, 50.0),
                edge = MapDrawingPoint(90.0, 50.0),
                color = Palette.Colours.RED,
            ),
            zoom = 4.0,
        )

        assertThat(segments.size, greaterThanOrEqualTo(24))
        assertThat(segments.all { it.color == Palette.Colours.RED }, equalTo(true))
    }
}
