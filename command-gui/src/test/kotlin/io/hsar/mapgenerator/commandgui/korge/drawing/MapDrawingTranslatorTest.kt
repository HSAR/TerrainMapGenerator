package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta
import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapDrawingTranslatorTest {

    @Test
    fun `translates freeform drawings by the supplied delta`() {
        val drawing = FreeformDrawing(
            points = listOf(MapDrawingPoint(10.0, 20.0), MapDrawingPoint(15.0, 25.0)),
            color = Palette.Colours.RED,
        )

        val translated = MapDrawingTranslator.translate(drawing, MapCoordinateDelta(5.0, -5.0))

        assertThat(
            translated,
            equalTo(
                FreeformDrawing(
                    points = listOf(MapDrawingPoint(15.0, 15.0), MapDrawingPoint(20.0, 20.0)),
                    color = Palette.Colours.RED,
                ),
            ),
        )
    }
}
