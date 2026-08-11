package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test

class ComposeDrawingPresentationTest {
    @Test
    fun `world stroke keeps a constant three pixel screen width`() {
        listOf(.25, 1.0, 4.0).forEach { zoom ->
            assertThat(
                (ComposeDrawingPresentation.worldStrokeWidth(zoom) * zoom).toDouble(),
                closeTo(ComposeDrawingPresentation.screenStrokeWidth.toDouble(), .0001),
            )
        }
    }

    @Test
    fun `label is centred above oriented screen bounds with a fixed gap`() {
        val position = ComposeDrawingPresentation.labelTopLeft(MapSelectionScreenBounds(100.0, 80.0, 220.0, 160.0), 60, 20)

        assertThat(position.x, closeTo(130.0, .0001))
        assertThat(position.y, closeTo(56.0, .0001))
    }
}
