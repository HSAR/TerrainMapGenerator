package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.launch.defaultGridConfiguration
import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.ReferenceSystem
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class OperationSaveWriterTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save archive contains game json ledger json copied assets and jpg thumbnail`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
        val mapFile = tempDir.resolve("map.png")
        val unitFile = tempDir.resolve("unit.svg")
        val mapImage = BufferedImage(1024, 256, BufferedImage.TYPE_INT_RGB)
        val thumbnailImage = BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB)
        thumbnailImage.setRGB(10, 10, java.awt.Color.RED.rgb)
        ImageIO.write(mapImage, "png", mapFile.toFile())
        Files.writeString(unitFile, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"4\" height=\"4\"/>")
        val unitTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            name = "Infantry",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            assetSource = AssetSource.fromFile(unitFile.toFile()),
            affiliation = UnitAffiliation.Friend,
        )
        val writer = writerWithUuids(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
        )

        val savePath = writer.write(
            request(
                operationId = operationId,
                mapImage = mapImage,
                thumbnailImage = thumbnailImage,
                mapSource = AssetSource.fromFile(mapFile.toFile()),
                ledgerEntries = listOf(CreateUnitTemplateAction(UnitTemplateSnapshot(unitTemplate.id, unitTemplate, 0))),
            ),
            tempDir.resolve("operation.oct.zip"),
        )
        val save = OperationSaveReader().read(savePath)

        assertThat(save.game.id, equalTo(operationId.toString()))
        assertThat(save.game.formatVersion, equalTo(6))
        assertThat(save.game.thumbnailAssetId, equalTo(operationId.toString()))
        assertThat(save.game.map.assetId, equalTo("00000000-0000-0000-0000-000000000001"))
        val expectedGrid = defaultGridConfiguration(mapImage, 20.0, 1234L)
        assertThat(save.game.map.referenceSystem, equalTo(ReferenceSystem.MGRS))
        assertThat(save.game.map.mapId, equalTo(expectedGrid.mapId))
        assertThat(save.game.map.gridStartX, equalTo(expectedGrid.startX.format()))
        assertThat(save.game.map.gridStartY, equalTo(expectedGrid.startY.format()))
        ZipFile(savePath.toFile()).use { zip ->
            val gameJson = OperationSaveJson.objectMapper.readTree(zip.getInputStream(zip.getEntry("game.json")))
            assertThat(gameJson["map"].has("name"), equalTo(false))
            assertThat(gameJson["map"].has("width"), equalTo(false))
            assertThat(gameJson["map"].has("height"), equalTo(false))
            assertThat(gameJson["map"]["referenceSystem"].asText(), equalTo("MGRS"))
            assertThat(gameJson["map"]["mapId"].asText(), equalTo(expectedGrid.mapId))
            assertThat(gameJson["map"]["gridStartX"].asText(), equalTo(expectedGrid.startX.format()))
            assertThat(gameJson["map"]["gridStartY"].asText(), equalTo(expectedGrid.startY.format()))
        }
        assertThat((save.ledger.entries.single() as CreateUnitTemplateEntrySaveDto).unitTemplate.template.assetId, equalTo("00000000-0000-0000-0000-000000000002"))
        assertThat((save.ledger.entries.single() as CreateUnitTemplateEntrySaveDto).unitTemplate.template.affiliation, equalTo(UnitAffiliation.Friend))

        ZipFile(savePath.toFile()).use { zip ->
            assertThat(zip.getEntry("game.json") != null, equalTo(true))
            assertThat(zip.getEntry("ledger.json") != null, equalTo(true))
            assertThat(zip.read("assets/00000000-0000-0000-0000-000000000001.png"), equalTo(Files.readAllBytes(mapFile)))
            assertThat(zip.read("assets/00000000-0000-0000-0000-000000000002.svg"), equalTo(Files.readAllBytes(unitFile)))
            val thumbnail = ImageIO.read(zip.getInputStream(zip.getEntry("assets/$operationId.jpg")))
            assertThat(thumbnail, notNullValue())
            assertThat(thumbnail.width, lessThanOrEqualTo(512))
        }
    }

    @Test
    fun `additional data json is written when provided`() {
        val payload = """{"custom":"value"}""".toByteArray()
        val savePath = OperationSaveWriter(uuidGenerator = UUID::randomUUID).write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-0000000000ee"),
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = emptyList(),
                additionalDataJson = payload,
            ),
            tempDir.resolve("additional-data.oct.zip"),
        )

        ZipFile(savePath.toFile()).use { zip ->
            assertThat(zip.getEntry("additionalData.json") != null, equalTo(true))
            assertThat(zip.read("additionalData.json").contentEquals(payload), equalTo(true))
        }
    }

    @Test
    fun `missing source assets fall back to jpg unless transparency requires png`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000bb")
        val missingSource = AssetSource(
            path = tempDir.resolve("missing.png").toString(),
            originalFilename = "missing.png",
            extension = "png",
            mediaType = "image/png",
        )
        val transparentTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000013")),
            name = "Generated",
            bitmap = Bitmap32(4, 4) { x, _ -> if (x == 0) RGBA(255, 0, 0, 0) else RGBA(255, 0, 0, 255) },
            assetSource = missingSource,
        )
        val writer = writerWithUuids(
            UUID.fromString("00000000-0000-0000-0000-000000000011"),
            UUID.fromString("00000000-0000-0000-0000-000000000012"),
        )

        val savePath = writer.write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = missingSource,
                ledgerEntries = listOf(CreateUnitTemplateAction(UnitTemplateSnapshot(transparentTemplate.id, transparentTemplate, 0))),
            ),
            tempDir.resolve("fallback.oct.zip"),
        )

        ZipFile(savePath.toFile()).use { zip ->
            assertThat(zip.getEntry("assets/00000000-0000-0000-0000-000000000011.jpg") != null, equalTo(true))
            assertThat(zip.getEntry("assets/00000000-0000-0000-0000-000000000012.png") != null, equalTo(true))
        }
    }

    @Test
    fun `game json stores normalized camera center`() {
        val writer = OperationSaveWriter()
        val mapImage = BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB)
        val geometry = geometryFor(mapImage)
        val viewport = CameraViewport(
            centerX = geometry.mapOffsetX + (mapImage.width * 0.25),
            centerY = geometry.mapOffsetY + (mapImage.height * 0.75),
            zoom = 1.5,
            viewportWidth = 800,
            viewportHeight = 600,
            worldWidth = geometry.backgroundWidth,
            worldHeight = geometry.backgroundHeight,
        )

        val savePath = writer.write(
            request(
                operationId = UUID.randomUUID(),
                mapImage = mapImage,
                mapSource = null,
                ledgerEntries = emptyList(),
                camera = viewport,
            ),
            tempDir.resolve("camera.oct.zip"),
        )

        val save = OperationSaveReader().read(savePath)

        assertThat(save.game.camera != null, equalTo(true))
        assertThat(save.game.camera!!.x, equalTo(0.25))
        assertThat(save.game.camera!!.y, equalTo(0.75))
        assertThat(save.game.camera!!.zoom, equalTo(1.5))
    }

    @Test
    fun `default save directory is under documents operational command tool`() {
        val writer = OperationSaveWriter()

        val directory = writer.defaultSaveDirectory()

        assertThat(directory.endsWith(Path.of("Documents", "My Games", "OperationalCommandTool", "saves")), equalTo(true))
    }

    @Test
    fun `default storage directory is under documents operational command tool`() {
        val writer = OperationSaveWriter()

        val directory = writer.defaultStorageDirectory()

        assertThat(directory.endsWith(Path.of("Documents", "My Games", "OperationalCommandTool")), equalTo(true))
    }

    @Test
    fun `default save name uses the full operation name and first eight id digits`() {
        val writer = OperationSaveWriter()

        val path = writer.defaultSavePath(
            operationName = "Test Operation",
            operationId = UUID.fromString("12345678-1234-5678-1234-567812345678"),
        )

        assertThat(path.fileName.toString(), equalTo("Test_Operation_12345678.oct.zip"))
        assertThat(path.parent.fileName.toString(), equalTo("saves"))
    }

    @Test
    fun `default storage creates saves folder and versioned settings file`() {
        val writer = OperationSaveWriter(storage = OperationToolStorage(tempDir))

        writer.ensureDefaultStorage()

        assertThat(Files.isDirectory(tempDir.resolve("saves")), equalTo(true))
        val settings = OperationSaveJson.objectMapper.readValue(
            tempDir.resolve("settings.json").toFile(),
            OperationToolSettingsDto::class.java,
        )
        assertThat(settings.formatVersion, equalTo(1))
    }

    @Test
    fun `tool settings persist destroyed units display mode`() {
        val storage = OperationToolStorage(tempDir)

        storage.writeSettings(OperationToolSettingsDto(formatVersion = 99, displayDestroyedUnits = "Partial", unitIconSizePx = 48.0))
        val settings = storage.readSettings()
        val settingsJson = OperationSaveJson.objectMapper.readTree(tempDir.resolve("settings.json").toFile())

        assertThat(settings.formatVersion, equalTo(1))
        assertThat(settings.displayDestroyedUnits, equalTo("Partial"))
        assertThat(settings.unitIconSizePx, equalTo(48.0))
        assertThat(settingsJson["displayDestroyedUnits"].asText(), equalTo("Partial"))
        assertThat(settingsJson["unitIconSizePx"].asDouble(), equalTo(48.0))
    }

    @Test
    fun `default save path writes into saves folder and creates settings file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000dd")
        val writer = writerWithUuids(
            UUID.fromString("00000000-0000-0000-0000-000000000041"),
            storage = OperationToolStorage(tempDir),
        )

        val savePath = writer.write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = emptyList(),
            ),
        )

        assertThat(savePath.parent, equalTo(tempDir.resolve("saves")))
        assertThat(Files.exists(savePath), equalTo(true))
        assertThat(Files.exists(tempDir.resolve("settings.json")), equalTo(true))
    }

    @Test
    fun `save writes epsilon reference system and designator starts`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000cc")
        val mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        val writer = writerWithUuids(UUID.fromString("00000000-0000-0000-0000-000000000031"))

        val savePath = writer.write(
            request(
                operationId = operationId,
                mapImage = mapImage,
                mapSource = null,
                ledgerEntries = emptyList(),
                settings = MapViewerSettings(
                    operationName = "Test Operation",
                    width = mapImage.width,
                    height = mapImage.height,
                    unitsPerPixel = 10.0,
                    referenceSystem = ReferenceSystem.EPSILON,
                    mapId = "EPSILON",
                    gridStartX = EpsilonCoord.fromUnits(60),
                    gridStartY = EpsilonCoord.fromUnits(1_120),
                    seed = 1234L,
                ),
            ),
            tempDir.resolve("epsilon.oct.zip"),
        )

        val save = OperationSaveReader().read(savePath)

        assertThat(save.game.formatVersion, equalTo(6))
        assertThat(save.game.map.referenceSystem, equalTo(ReferenceSystem.EPSILON))
        assertThat(save.game.map.unitsPerPixel, equalTo(10.0))
        assertThat(save.game.map.gridStartX, equalTo("4"))
        assertThat(save.game.map.gridStartY, equalTo("BE"))
    }

    private fun request(
        operationId: UUID,
        mapImage: BufferedImage,
        thumbnailImage: BufferedImage? = null,
        mapSource: AssetSource?,
        ledgerEntries: List<io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry>,
        settings: MapViewerSettings = MapViewerSettings(
            operationName = "Test Operation",
            width = mapImage.width,
            height = mapImage.height,
            unitsPerPixel = 20.0,
            seed = 1234L,
        ),
        camera: CameraViewport = CameraViewport.centeredOnMap(
            geometry = geometryFor(mapImage),
            mapWidth = mapImage.width,
            mapHeight = mapImage.height,
            viewportWidth = 800,
            viewportHeight = 600,
        ),
        additionalDataJson: ByteArray? = null,
    ) = OperationSaveRequest(
        operationId = operationId,
        operationName = "Test Operation",
        createdAt = Instant.parse("2026-06-18T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-18T10:05:00Z"),
        camera = camera,
        settings = settings,
        mapImage = mapImage,
        thumbnailImage = thumbnailImage,
        mapAssetSource = mapSource,
        ledgerEntries = ledgerEntries,
        additionalDataJson = additionalDataJson,
    )

    private fun writerWithUuids(
        vararg uuids: UUID,
        storage: OperationToolStorage = OperationToolStorage(),
    ): OperationSaveWriter {
        val iterator = uuids.iterator()
        return OperationSaveWriter(uuidGenerator = { iterator.next() }, storage = storage)
    }

    private fun ZipFile.read(name: String): ByteArray =
        getInputStream(getEntry(name)).use { it.readBytes() }
}
