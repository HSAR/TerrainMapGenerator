package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectPatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class OperationSaveRoundTripTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save load and save again preserves the operation shape`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000cc")
        val mapFile = tempDir.resolve("map.png")
        val mapImage = BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(mapImage, "png", mapFile.toFile())
        val unitFile = tempDir.resolve("unit.svg")
        Files.writeString(
            unitFile,
            """<svg xmlns="http://www.w3.org/2000/svg" width="8" height="8"><rect x="0" y="0" width="8" height="8" fill="red"/></svg>""",
        )

        val unitBitmap = MapViewerIconLoader.load(unitFile.toFile()) ?: error("unit bitmap should load")
        val unitTemplate = MapViewerUnitTemplate(
            name = "Alpha",
            bitmap = unitBitmap,
            assetSource = AssetSource.fromFile(unitFile.toFile()),
        )
        val createdUnit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000007")),
            template = unitTemplate,
            instanceName = "Alpha 1",
            destroyed = false,
            mapCoordinate = MapCoordinate(0.25, 0.75),
            normalizedWidth = 0.08,
            normalizedHeight = 0.08,
        )
        val movedUnit = createdUnit.copy(
            instanceName = "Alpha 2",
            destroyed = true,
            mapCoordinate = MapCoordinate(0.5, 0.4),
        )
        val createdDrawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000009")),
            drawing = CircleDrawing(
                center = MapDrawingPoint(0.3, 0.4),
                radius = 0.2,
                color = java.awt.Color.RED,
            ),
        )
        val ledger = listOf(
            CreateUnitTemplateAction(UnitTemplateSnapshot(UnitTemplateId(5), unitTemplate, 0)),
            CreateObjectAction(createdUnit),
            EditObjectAction(
                objectId = createdUnit.id,
                forward = MapPlacedObjectPatch.diff(createdUnit, movedUnit),
                backward = MapPlacedObjectPatch.diff(movedUnit, createdUnit),
            ),
            CreateObjectAction(createdDrawing),
            DeleteObjectAction(createdDrawing),
        )

        val writer = OperationSaveWriter(uuidGenerator = UUID::randomUUID)
        val firstSave = writer.write(
            request(
                operationId = operationId,
                mapImage = mapImage,
                mapSource = AssetSource.fromFile(mapFile.toFile()),
                ledgerEntries = ledger,
            ),
            tempDir.resolve("first.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(firstSave)
        val secondSave = writer.write(
            request(
                operationId = loaded.operationId,
                operationName = loaded.operationName,
                createdAt = loaded.createdAt,
                updatedAt = loaded.updatedAt,
                camera = loaded.camera?.toViewport(
                    geometry = geometryFor(loaded.mapImage),
                    mapWidth = loaded.settings.width,
                    mapHeight = loaded.settings.height,
                    viewportWidth = loaded.settings.width,
                    viewportHeight = loaded.settings.height,
                ) ?: CameraViewport.centeredOnMap(
                    geometry = geometryFor(loaded.mapImage),
                    mapWidth = loaded.settings.width,
                    mapHeight = loaded.settings.height,
                    viewportWidth = loaded.settings.width,
                    viewportHeight = loaded.settings.height,
                    zoom = CameraViewport.fitZoomToViewport(
                        mapWidth = loaded.settings.width,
                        mapHeight = loaded.settings.height,
                        viewportWidth = loaded.settings.width,
                        viewportHeight = loaded.settings.height,
                    ),
                ),
                settings = loaded.settings,
                mapImage = loaded.mapImage,
                mapSource = null,
                ledgerEntries = loaded.ledgerEntries,
            ),
            tempDir.resolve("second.oct.zip"),
        )

        val reread = OperationSaveReader().read(secondSave)
        assertThat(reread.game.id, equalTo(operationId.toString()))
        assertThat(reread.game.name, equalTo("Round Trip"))
        assertThat(reread.game.map.metresPerPixel, equalTo(20.0))
        assertThat(reread.game.map.assetId.isNotBlank(), equalTo(true))
        assertThat(reread.ledger.entries.size, equalTo(5))
        assertThat(reread.ledger.entries[2] is EditObjectEntrySaveDto, equalTo(true))

        ZipFile(secondSave.toFile()).use { zip ->
            assertThat(zip.getEntry("assets/$operationId.jpg") != null, equalTo(true))
        }
    }

    @Test
    fun `png unit icons keep their colours across repeated save loads`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000cd")
        val iconSource = AssetSource(
            path = tempDir.resolve("missing-icon.png").toString(),
            originalFilename = "missing-icon.png",
            extension = "png",
            mediaType = "image/png",
        )
        val template = MapViewerUnitTemplate(
            name = "Alpha",
            bitmap = Bitmap32(2, 1) { x, _ ->
                if (x == 0) RGBA(255, 0, 0, 255) else RGBA(0, 0, 255, 255)
            },
            assetSource = iconSource,
        )

        val firstSave = OperationSaveWriter(uuidGenerator = UUID::randomUUID).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(CreateUnitTemplateAction(UnitTemplateSnapshot(UnitTemplateId(1), template, 0))),
            ),
            tempDir.resolve("first-icon-save.oct.zip"),
        )

        val firstLoaded = OperationSaveLoader().load(firstSave)
        val firstTemplate = firstLoaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().single().snapshot.template
        assertThat(firstTemplate.bitmap.getRgba(0, 0), equalTo(RGBA(255, 0, 0, 255)))
        assertThat(firstTemplate.bitmap.getRgba(1, 0), equalTo(RGBA(0, 0, 255, 255)))

        val secondSave = OperationSaveWriter(uuidGenerator = UUID::randomUUID).write(
            request(
                operationId = firstLoaded.operationId,
                operationName = firstLoaded.operationName,
                createdAt = firstLoaded.createdAt,
                updatedAt = firstLoaded.updatedAt,
                camera = firstLoaded.camera?.toViewport(
                    geometry = geometryFor(firstLoaded.mapImage),
                    mapWidth = firstLoaded.settings.width,
                    mapHeight = firstLoaded.settings.height,
                    viewportWidth = firstLoaded.settings.width,
                    viewportHeight = firstLoaded.settings.height,
                ) ?: CameraViewport.centeredOnMap(
                    geometry = geometryFor(firstLoaded.mapImage),
                    mapWidth = firstLoaded.settings.width,
                    mapHeight = firstLoaded.settings.height,
                    viewportWidth = firstLoaded.settings.width,
                    viewportHeight = firstLoaded.settings.height,
                    zoom = CameraViewport.fitZoomToViewport(
                        mapWidth = firstLoaded.settings.width,
                        mapHeight = firstLoaded.settings.height,
                        viewportWidth = firstLoaded.settings.width,
                        viewportHeight = firstLoaded.settings.height,
                    ),
                ),
                settings = firstLoaded.settings,
                mapImage = firstLoaded.mapImage,
                mapSource = null,
                ledgerEntries = firstLoaded.ledgerEntries,
            ),
            tempDir.resolve("second-icon-save.oct.zip"),
        )

        val secondLoaded = OperationSaveLoader().load(secondSave)
        val secondTemplate = secondLoaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().single().snapshot.template
        assertThat(secondTemplate.bitmap.getRgba(0, 0), equalTo(RGBA(255, 0, 0, 255)))
        assertThat(secondTemplate.bitmap.getRgba(1, 0), equalTo(RGBA(0, 0, 255, 255)))
    }

    private fun request(
        operationId: UUID,
        operationName: String = "Round Trip",
        createdAt: Instant = Instant.parse("2026-06-19T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-06-19T10:10:00Z"),
        camera: CameraViewport = CameraViewport(
            centerX = 480.0,
            centerY = 270.0,
            zoom = 1.5,
            viewportWidth = 1280,
            viewportHeight = 720,
            worldWidth = 960,
            worldHeight = 540,
        ),
        settings: MapViewerSettings = MapViewerSettings(
            operationName = operationName,
            availableLayers = emptyList(),
            enabledLayerIds = emptySet(),
            staticLayerImages = emptyMap(),
            width = 960,
            height = 540,
            metresPerPixel = 20.0,
        ),
        mapImage: BufferedImage,
        mapSource: AssetSource?,
        ledgerEntries: List<io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry>,
    ) = OperationSaveRequest(
        operationId = operationId,
        operationName = operationName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        camera = camera,
        settings = settings,
        mapImage = mapImage,
        mapAssetSource = mapSource,
        ledgerEntries = ledgerEntries,
    )
}
