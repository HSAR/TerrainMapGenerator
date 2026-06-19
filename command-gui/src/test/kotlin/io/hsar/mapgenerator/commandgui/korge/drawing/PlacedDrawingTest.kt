package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class PlacedDrawingTest {

    @Test
    fun `drawing context menu exposes delete only`() {
        val drawing = PlacedDrawing(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.0, 0.0),
                end = MapDrawingPoint(20.0, 10.0),
                color = Palette.Colours.RED,
            ),
        )

        assertThat(drawing.contextMenuActions().map { it.label }, equalTo(listOf("Delete")))
    }

    @Test
    fun `drawing hit testing uses normalized map coordinates`() {
        val drawing = PlacedDrawing(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.10, 0.10),
                end = MapDrawingPoint(0.14, 0.12),
                color = Palette.Colours.RED,
            ),
        )

        assertThat(drawing.containsMapCoordinate(MapCoordinate(0.12, 0.10), tolerance = 0.001), equalTo(true))
        assertThat(drawing.containsMapCoordinate(MapCoordinate(0.05, 0.05), tolerance = 0.001), equalTo(false))
    }
}
