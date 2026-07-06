package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class MapDrawingIconRendererTest {
    @Test
    fun `drawing snapshot delegates name to drawing implementation`() {
        val snapshot = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.1, 0.1),
                end = MapDrawingPoint(0.3, 0.2),
                color = Color.RED,
            ),
        )

        assertThat(snapshot.name, equalTo("Rectangle Drawing"))
    }

    @Test
    fun `drawing icon renders translucent drawing pixels`() {
        val icon = MapDrawingIconRenderer.render(
            LineDrawing(
                start = MapDrawingPoint(0.1, 0.1),
                end = MapDrawingPoint(0.9, 0.9),
                color = Color.RED,
            ),
        )

        val paintedPixels = (0 until icon.width).sumOf { x ->
            (0 until icon.height).count { y -> icon.getRgba(x, y).a > 0 }
        }

        assertThat(paintedPixels, greaterThan(0))
        assertThat(icon.getRgba(16, 16).a, greaterThan(0))
    }
}
