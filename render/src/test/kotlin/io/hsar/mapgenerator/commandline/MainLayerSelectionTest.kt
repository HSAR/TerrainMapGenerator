package io.hsar.mapgenerator.commandline

import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class MainLayerSelectionTest {

    @Test
    fun `defaultEnabledLayerIds enables all built in layers when none are selected`() {
        val staticLayer = loadedStaticLayer("static-layer", "Static")

        val enabledIds = defaultEnabledLayerIds(
            selectedLayers = emptySet(),
            staticLayers = listOf(staticLayer),
        )

        assertThat(enabledIds, equalTo(MapLayer.values().map { it.id }.toSet() + staticLayer.layer.id))
    }

    @Test
    fun `selectedViewerLayers preserves static layers alongside explicit built in selection`() {
        val staticLayer = loadedStaticLayer("static-layer", "Static")

        val layers = selectedViewerLayers(
            selectedLayers = setOf(MapLayer.CONTOUR),
            staticLayers = listOf(staticLayer),
        )

        assertThat(layers, equalTo(listOf(MapLayer.CONTOUR, staticLayer.layer)))
    }

    @Test
    fun `renderSelectedLayers only invokes the requested layers`() {
        val calls = mutableListOf<String>()
        val staticLayer = StaticImageLayer(id = "static-layer", displayName = "Static")

        val rendered = renderSelectedLayers(
            selectedLayers = listOf(MapLayer.GRAPH, staticLayer, MapLayer.CONTOUR),
            renderers = mapOf(
                MapLayer.GRAPH.id to { calls += MapLayer.GRAPH.id; imageFor(MapLayer.GRAPH) },
                MapLayer.HEIGHT.id to { calls += MapLayer.HEIGHT.id; imageFor(MapLayer.HEIGHT) },
                MapLayer.CONTOUR.id to { calls += MapLayer.CONTOUR.id; imageFor(MapLayer.CONTOUR) },
                MapLayer.GRID.id to { calls += MapLayer.GRID.id; imageFor(MapLayer.GRID) },
                staticLayer.id to { calls += staticLayer.id; BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB) },
            ),
        )

        assertThat(calls, equalTo(listOf(MapLayer.GRAPH.id, staticLayer.id, MapLayer.CONTOUR.id)))
        assertThat(rendered.size, equalTo(3))
    }

    private fun imageFor(layer: MapLayer): BufferedImage =
        BufferedImage(layer.ordinal + 1, layer.ordinal + 1, BufferedImage.TYPE_INT_ARGB)

    private fun loadedStaticLayer(id: String, displayName: String) =
        LoadedStaticImageLayer(
            layer = StaticImageLayer(id = id, displayName = displayName),
            image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        )
}
