package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

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

    @Test
    fun `operation map image only enables static map and grid by default`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50)),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())
        val layerId = configuration.settings.availableLayers.filterIsInstance<StaticImageLayer>().single().id

        assertThat(configuration.settings.enabledLayerIds, equalTo(setOf(layerId, MapLayer.GRID.id)))
    }

    @Test
    fun `setup grid configuration is preserved into runtime settings`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50)),
            gridConfiguration = GridConfiguration(
                mapId = "29UPA",
                startX = 123.0,
                startY = 456.0,
            ),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())

        assertThat(configuration.settings.mapId, equalTo("29UPA"))
        assertThat(configuration.settings.gridStartX, equalTo(123.0))
        assertThat(configuration.settings.gridStartY, equalTo(456.0))
    }

    @Test
    fun `operation map image drives automatic grid colour`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50, Color.WHITE)),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())

        assertThat(configuration.settings.gridColour, equalTo(io.hsar.mapgenerator.image.Palette.Colours.DARK))
    }

    @Test
    fun `unit templates from setup are seeded into the initial ledger`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50)),
            unitTemplates = listOf(
                template("Alpha"),
                template("Bravo"),
            ),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())

        assertThat(configuration.initialLedgerEntries.size, equalTo(3))
        assertThat(configuration.initialLedgerEntries[0] is CreateUnitTemplateAction, equalTo(true))
        assertThat((configuration.initialLedgerEntries[0] as CreateUnitTemplateAction).snapshot.id, equalTo(template("Alpha").id))
        assertThat((configuration.initialLedgerEntries[1] as CreateUnitTemplateAction).snapshot.id, equalTo(template("Bravo").id))
        assertThat(configuration.initialLedgerEntries[2], equalTo(AddTurnLedgerEntry("Operation Start")))
    }

    private fun image(width: Int, height: Int, color: Color = Color.BLACK): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until height) {
                for (x in 0 until width) {
                    image.setRGB(x, y, color.rgb)
                }
            }
        }

    private fun template(name: String) =
        io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.nameUUIDFromBytes(name.toByteArray())),
            name = name,
            bitmap = korlibs.image.bitmap.Bitmap32(4, 4),
        )
}
