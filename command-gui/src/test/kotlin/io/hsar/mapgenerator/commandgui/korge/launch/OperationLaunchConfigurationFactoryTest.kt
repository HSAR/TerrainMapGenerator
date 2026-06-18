package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class OperationLaunchConfigurationFactoryTest {

    @Test
    fun `map image retains display name and drives the viewer size`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "same-path.png", image(200, 80)),
            pixelsPerMetre = 15.0,
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())
        val staticLayers = configuration.settings.availableLayers.filterIsInstance<StaticImageLayer>()

        assertThat(staticLayers.map { it.displayName }, equalTo(listOf("Satellite")))
        assertThat(staticLayers.map { it.id }.toSet().size, equalTo(1))
        assertThat(configuration.settings.staticLayerImages.values.map { it.width }.toSet(), equalTo(setOf(200)))
        assertThat(configuration.settings.staticLayerImages.values.map { it.height }.toSet(), equalTo(setOf(80)))
        assertThat(configuration.settings.width, equalTo(200))
        assertThat(configuration.settings.height, equalTo(80))
        assertThat(configuration.settings.metresPerPixel, equalTo(15.0))
    }

    @Test
    fun `operation map image is enabled by default`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50)),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())
        val layerId = configuration.settings.availableLayers.filterIsInstance<StaticImageLayer>().single().id

        assertThat(layerId in configuration.settings.enabledLayerIds, equalTo(true))
    }

    private fun image(width: Int, height: Int): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
}
