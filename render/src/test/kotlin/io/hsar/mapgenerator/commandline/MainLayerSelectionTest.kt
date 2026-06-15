package io.hsar.mapgenerator.commandline

import io.hsar.mapgenerator.commandgui.MapLayer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class MainLayerSelectionTest {

    @Test
    fun `renderSelectedLayers only invokes the requested layers`() {
        val calls = mutableListOf<MapLayer>()

        val rendered = renderSelectedLayers(
            selectedLayers = setOf(MapLayer.CONTOUR, MapLayer.GRAPH),
            renderers = mapOf(
                MapLayer.GRAPH to { calls += MapLayer.GRAPH; imageFor(MapLayer.GRAPH) },
                MapLayer.HEIGHT to { calls += MapLayer.HEIGHT; imageFor(MapLayer.HEIGHT) },
                MapLayer.CONTOUR to { calls += MapLayer.CONTOUR; imageFor(MapLayer.CONTOUR) },
                MapLayer.GRID to { calls += MapLayer.GRID; imageFor(MapLayer.GRID) },
            ),
        )

        assertThat(calls, equalTo(listOf(MapLayer.GRAPH, MapLayer.CONTOUR)))
        assertThat(rendered.size, equalTo(2))
    }

    private fun imageFor(layer: MapLayer): BufferedImage =
        BufferedImage(layer.ordinal + 1, layer.ordinal + 1, BufferedImage.TYPE_INT_ARGB)
}
