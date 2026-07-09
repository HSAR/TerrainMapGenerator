package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.MapLayer
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.StaticImageLayer
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.grid.CoordAxis
import io.hsar.mapgenerator.grid.MgrsCoord
import io.hsar.mapgenerator.grid.ReferenceSystem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

class OperationLaunchConfigurationFactoryTest {

    @Test
    fun `operation name generator creates a two word name`() {
        val name = OperationNameGenerator.generate(Random(1234L))
        val words = name.split(" ")

        assertThat(words.size, equalTo(2))
        assertThat(words.all { it.first().isUpperCase() && it.drop(1).all(Char::isLowerCase) }, equalTo(true))
    }

    @Test
    fun `default grid configuration can be generated before map image selection`() {
        val configuration = defaultGridConfiguration(
            width = 1_000,
            height = 1_000,
            unitsPerPixel = 20.0,
            seed = 1234L,
        )

        assertThat(configuration.mapId.isNotBlank(), equalTo(true))
        assertThat(configuration.startX.lineNumber(1000) >= 1.0, equalTo(true))
        assertThat(configuration.startY.lineNumber(1000) >= 1.0, equalTo(true))
    }

    @Test
    fun `default epsilon grid configuration creates displayable axis starts`() {
        val configuration = defaultGridConfiguration(
            width = 1_000,
            height = 1_000,
            unitsPerPixel = ReferenceSystem.EPSILON.scaleInputToUnitsPerPixel(20.0),
            seed = 1234L,
            referenceSystem = ReferenceSystem.EPSILON,
        )

        assertThat(configuration.referenceSystem, equalTo(ReferenceSystem.EPSILON))
        assertThat(configuration.referenceSystem.formatCoord(configuration.startX, CoordAxis.X, ReferenceSystem.EPSILON.gridScale).toInt() in 1..100, equalTo(true))
        assertThat(configuration.referenceSystem.formatCoord(configuration.startY, CoordAxis.Y, ReferenceSystem.EPSILON.gridScale).matches(Regex("[A-Z]{1,2}")), equalTo(true))
    }

    @Test
    fun `epsilon grid configuration uses twenty unit squares and pixels per unit input`() {
        val gridLayer = io.hsar.mapgenerator.grid.GridLayerBuilder.build(
            width = 1_000,
            height = 1_000,
            gridScale = ReferenceSystem.EPSILON.gridScale,
            unitsPerPixel = ReferenceSystem.EPSILON.scaleInputToUnitsPerPixel(20.0),
            referenceSystem = ReferenceSystem.EPSILON,
            random = Random(1234L),
        )

        assertThat(gridLayer.gridScale, equalTo(20))
        assertThat(gridLayer.pixelsPerLine, equalTo(400.0))
    }

    @Test
    fun `grid configuration translation preserves line numbers for epsilon input display`() {
        val translated = translateGridConfiguration(
            gridConfiguration = GridConfiguration(
                mapId = "25F XK",
                referenceSystem = ReferenceSystem.MGRS,
                startX = MgrsCoord.fromLineNumber(39.0, ReferenceSystem.MGRS.gridScale),
                startY = MgrsCoord.fromLineNumber(45.0, ReferenceSystem.MGRS.gridScale),
            ),
            mapId = "EPSILON",
            referenceSystem = ReferenceSystem.EPSILON,
        )

        assertThat(translated.referenceSystem.formatCoord(translated.startX, CoordAxis.X, ReferenceSystem.EPSILON.gridScale), equalTo("40"))
        assertThat(translated.referenceSystem.formatCoord(translated.startY, CoordAxis.Y, ReferenceSystem.EPSILON.gridScale), equalTo("AT"))
    }

    @Test
    fun `map image retains display name and drives the viewer size`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "same-path.png", image(200, 80)),
            unitsPerPixel = 15.0,
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())
        val staticLayers = configuration.settings.availableLayers.filterIsInstance<StaticImageLayer>()

        assertThat(staticLayers.map { it.displayName }, equalTo(listOf("Satellite")))
        assertThat(staticLayers.map { it.id }.toSet().size, equalTo(1))
        assertThat(configuration.settings.staticLayerImages.values.map { it.width }.toSet(), equalTo(setOf(200)))
        assertThat(configuration.settings.staticLayerImages.values.map { it.height }.toSet(), equalTo(setOf(80)))
        assertThat(configuration.settings.width, equalTo(200))
        assertThat(configuration.settings.height, equalTo(80))
        assertThat(configuration.settings.unitsPerPixel, equalTo(15.0))
    }

    @Test
    fun `feature flags are preserved into runtime settings`() {
        val draft = NewOperationDraft(
            operationName = "Operation Copperhead",
            mapImage = OperationMapImage("Satellite", "satellite.png", image(100, 50)),
        )

        val configuration = OperationLaunchConfigurationFactory.create(
            draft,
            MapViewerSettings(featureFlags = setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)),
        )

        assertThat(configuration.settings.featureFlags, equalTo(setOf(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)))
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
                startX = MgrsCoord.fromMetres(12_300),
                startY = MgrsCoord.fromMetres(45_600),
            ),
        )

        val configuration = OperationLaunchConfigurationFactory.create(draft, MapViewerSettings())

        assertThat(configuration.settings.mapId, equalTo("29UPA"))
        assertThat(configuration.settings.gridStartX, equalTo(MgrsCoord.fromMetres(12_300)))
        assertThat(configuration.settings.gridStartY, equalTo(MgrsCoord.fromMetres(45_600)))
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
    fun `preview units per pixel preserves map scale when image is fitted smaller`() {
        val unitsPerPixel = previewUnitsPerPixel(
            unitsPerPixel = 20.0,
            previewScale = 0.25,
        )

        assertThat(unitsPerPixel, equalTo(80.0))
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
