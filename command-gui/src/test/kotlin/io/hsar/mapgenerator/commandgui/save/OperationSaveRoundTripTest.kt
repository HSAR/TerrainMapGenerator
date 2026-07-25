package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.AddTurnLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.BatchLedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.EditNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditObjectAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditUnitTemplateAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNotePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectPatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerIconLoader
import io.hsar.mapgenerator.commandgui.nextTestUuid
import io.hsar.mapgenerator.grid.ReferenceSystem
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
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
        val additionalData = """{"note":"keep-me"}""".toByteArray()
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
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            name = "Alpha",
            bitmap = unitBitmap,
            assetSource = AssetSource.fromFile(unitFile.toFile()),
            affiliation = UnitAffiliation.Neutral,
        )
        val createdUnit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000007")),
            unitTemplateId = unitTemplate.id,
            template = unitTemplate,
            instanceName = "Alpha 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.25, 0.75),
            normalizedWidth = 0.08,
            normalizedHeight = 0.08,
        )
        val movedUnit = createdUnit.copy(
            instanceName = "Alpha 2",
            destroyed = true,
            facingDegrees = 135,
            mapCoordinate = MapCoordinate(0.5, 0.4),
        )
        val createdDrawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000009")),
            drawing = OvalDrawing(
                center = MapDrawingPoint(0.3, 0.4),
                xRadius = 0.2,
                yRadius = 0.2,
                rotationDegrees = 25.0,
                color = java.awt.Color.RED,
            ),
            customProperties = mapOf("displayName" to "Survey zone"),
        )
        val createdNote = MapNoteSnapshot(
            id = MapNoteId(UUID.fromString("00000000-0000-0000-0000-0000000000a1")),
            title = "Contact",
            body = "Initial note body",
            referencedObjectIds = listOf(createdUnit.id, createdDrawing.id),
        )
        val editedNote = createdNote.copy(
            title = "Contact moved",
            body = "Updated note body",
            referencedObjectIds = listOf(createdUnit.id),
        )
        val ledger = listOf(
            CreateUnitTemplateAction(UnitTemplateSnapshot(unitTemplate.id, unitTemplate, 0)),
            CreateObjectAction(createdUnit),
            EditObjectAction(
                objectId = createdUnit.id,
                forward = MapPlacedObjectPatch.diff(createdUnit, movedUnit),
                backward = MapPlacedObjectPatch.diff(movedUnit, createdUnit),
            ),
            CreateObjectAction(createdDrawing),
            DeleteObjectAction(createdDrawing),
            CreateNoteAction(createdNote),
            EditNoteAction(
                id = createdNote.id,
                forward = MapNotePatch.diff(createdNote, editedNote),
                backward = MapNotePatch.diff(editedNote, createdNote),
            ),
            DeleteNoteAction(editedNote),
        )

        val writer = OperationSaveWriter(uuidGenerator = ::nextTestUuid)
        val firstSave = writer.write(
            request(
                operationId = operationId,
                mapImage = mapImage,
                mapSource = AssetSource.fromFile(mapFile.toFile()),
                ledgerEntries = ledger,
                additionalDataJson = additionalData,
            ),
            tempDir.resolve("first.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(firstSave)
        assertThat(
            loaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().single().snapshot.template.affiliation,
            equalTo(UnitAffiliation.Neutral),
        )
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
                additionalDataJson = loaded.additionalDataJson,
            ),
            tempDir.resolve("second.oct.zip"),
        )

        val reread = OperationSaveReader().read(secondSave)
        assertThat(reread.game.id, equalTo(operationId.toString()))
        assertThat(reread.game.name, equalTo("Round Trip"))
        assertThat(reread.game.map.unitsPerPixel, equalTo(20.0))
        assertThat(reread.game.map.referenceSystem, equalTo(ReferenceSystem.MGRS))
        assertThat(reread.game.map.assetId.isNotBlank(), equalTo(true))
        assertThat(reread.game.map.mapId.isNullOrBlank(), equalTo(false))
        assertThat(reread.game.map.gridStartX != null, equalTo(true))
        assertThat(reread.game.map.gridStartY != null, equalTo(true))
        assertThat(reread.ledger.entries.size, equalTo(8))
        assertThat(reread.ledger.entries[2] is EditObjectEntrySaveDto, equalTo(true))
        assertThat((reread.ledger.entries[2] as EditObjectEntrySaveDto).forward["facingDegrees"]?.asInt(), equalTo(135))
        assertThat(reread.ledger.entries[5] is CreateNoteEntrySaveDto, equalTo(true))
        val savedNote = (reread.ledger.entries[5] as CreateNoteEntrySaveDto).note
        assertThat(savedNote.title, equalTo("Contact"))
        assertThat(savedNote.body, equalTo("Initial note body"))
        assertThat(savedNote.referencedObjectIds, equalTo(listOf(createdUnit.id.value, createdDrawing.id.value)))
        assertThat((reread.ledger.entries[6] as EditNoteEntrySaveDto).forward["title"]?.asText(), equalTo("Contact moved"))

        ZipFile(secondSave.toFile()).use { zip ->
            assertThat(zip.getEntry("assets/$operationId.jpg") != null, equalTo(true))
            assertThat(zip.read("additionalData.json").contentEquals(additionalData), equalTo(true))
            assertThat(
                zip.entries().asSequence().any { entry ->
                    !entry.isDirectory && entry.name.startsWith("assets/") && entry.name.endsWith(".svg")
                },
                equalTo(true),
            )
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
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000011")),
            name = "Alpha",
            bitmap = Bitmap32(2, 1) { x, _ ->
                if (x == 0) RGBA(255, 0, 0, 255) else RGBA(0, 0, 255, 255)
            },
            assetSource = iconSource,
        )

        val firstSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(CreateUnitTemplateAction(UnitTemplateSnapshot(template.id, template, 0))),
            ),
            tempDir.resolve("first-icon-save.oct.zip"),
        )

        val firstLoaded = OperationSaveLoader().load(firstSave)
        val firstTemplate = firstLoaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().single().snapshot.template
        assertThat(firstTemplate.bitmap.getRgba(0, 0), equalTo(RGBA(255, 0, 0, 255)))
        assertThat(firstTemplate.bitmap.getRgba(1, 0), equalTo(RGBA(0, 0, 255, 255)))

        val secondSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
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

    @Test
    fun `unit and unit template custom properties save load and save again as flat fields`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d4")
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000030")),
            name = "Recon",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(0, 255, 0, 255) },
            customProperties = mapOf("role" to "screen", "echelon" to "platoon"),
        )
        val unit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000031")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Recon 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.25, 0.75),
            normalizedWidth = 0.08,
            normalizedHeight = 0.08,
            customProperties = mapOf("callsign" to "R1", "morale" to "steady"),
        )

        val firstSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(UnitTemplateSnapshot(template.id, template, 0)),
                    CreateObjectAction(unit),
                ),
            ),
            tempDir.resolve("custom-properties-first.oct.zip"),
        )

        assertFlatCustomProperties(firstSave)
        val loaded = OperationSaveLoader().load(firstSave)
        assertThat(
            loaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().single().snapshot.template.customProperties,
            equalTo(template.customProperties),
        )
        assertThat(
            (loaded.ledgerEntries.filterIsInstance<CreateObjectAction>().single().snapshot as UnitSnapshot).customProperties,
            equalTo(unit.customProperties),
        )

        val secondSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = loaded.operationId,
                operationName = loaded.operationName,
                mapImage = loaded.mapImage,
                mapSource = loaded.mapAssetSource,
                ledgerEntries = loaded.ledgerEntries,
            ),
            tempDir.resolve("custom-properties-second.oct.zip"),
        )

        assertFlatCustomProperties(secondSave)
    }

    @Test
    fun `unit affiliation saves and loads through the save file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d5")
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000033")),
            name = "Recon",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(0, 255, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val unit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000034")),
            unitTemplateId = template.id,
            template = template.copy(affiliation = UnitAffiliation.Hostile),
            instanceName = "Recon 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.25, 0.75),
            normalizedWidth = 0.08,
            normalizedHeight = 0.08,
        )

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(UnitTemplateSnapshot(template.id, template, 0)),
                    CreateObjectAction(unit),
                ),
            ),
            tempDir.resolve("unit-affiliation-save.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(savePath)
        val loadedUnit = loaded.ledgerEntries.filterIsInstance<CreateObjectAction>().single().snapshot as UnitSnapshot

        assertThat(loadedUnit.template.affiliation, equalTo(UnitAffiliation.Hostile))
    }

    @Test
    fun `unit affiliation edits in ledger replay to the latest affiliation after load`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d6")
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000035")),
            name = "Recon",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(0, 255, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val created = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000036")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Recon 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.25, 0.75),
            normalizedWidth = 0.08,
            normalizedHeight = 0.08,
        )
        val edited = created.copy(template = created.template.copy(affiliation = UnitAffiliation.Hostile))

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(UnitTemplateSnapshot(template.id, template, 0)),
                    CreateObjectAction(created),
                    EditObjectAction(
                        objectId = created.id,
                        forward = MapPlacedObjectPatch.diff(created, edited),
                        backward = MapPlacedObjectPatch.diff(edited, created),
                    ),
                ),
            ),
            tempDir.resolve("unit-affiliation-edit-save.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(savePath)
        val replayed = io.hsar.mapgenerator.commandgui.korge.interaction.LedgerReplay.replay(loaded.ledgerEntries)
        val replayedUnit = replayed.objects.single() as UnitSnapshot

        assertThat(replayedUnit.template.affiliation, equalTo(UnitAffiliation.Hostile))
    }

    @Test
    fun `unknown non-string unit and unit template properties load as compact json strings`() {
        val unit = OperationSaveJson.objectMapper.readValue(
            """
            {
              "type": "unit",
              "unitTemplateId": "00000000-0000-0000-0000-000000000030",
              "instanceName": "Recon 1",
              "destroyed": false,
              "facingDegrees": null,
              "affiliation": "Friend",
              "mapCoordinate": { "x": 0.25, "y": 0.75 },
              "normalizedWidth": 0.08,
              "normalizedHeight": 0.08,
              "priority": 3,
              "flags": ["fast", true],
              "metadata": { "supply": 2 },
              "unknownNull": null
            }
            """.trimIndent(),
            SavedMapObjectDto::class.java,
        ) as SavedUnitObjectDto
        val template = OperationSaveJson.objectMapper.readValue(
            """
            {
              "name": "Recon",
              "assetId": "00000000-0000-0000-0000-000000000032",
              "affiliation": "Friend",
              "priority": 3,
              "metadata": { "supply": 2 },
              "unknownNull": null
            }
            """.trimIndent(),
            UnitTemplateSaveDto::class.java,
        )

        assertThat(
            unit.affiliation,
            equalTo(UnitAffiliation.Friend),
        )
        assertThat(
            unit.customProperties,
            equalTo(
                mapOf(
                    "priority" to "3",
                    "flags" to """["fast",true]""",
                    "metadata" to """{"supply":2}""",
                    "unknownNull" to "null",
                ),
            ),
        )
        assertThat(
            template.customProperties,
            equalTo(
                mapOf(
                    "priority" to "3",
                    "metadata" to """{"supply":2}""",
                    "unknownNull" to "null",
                ),
            ),
        )
    }

    @Test
    fun `unit templates that share an asset still round trip with distinct names`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000ce")
        val iconSource = AssetSource(
            path = tempDir.resolve("shared-icon.png").toString(),
            originalFilename = "shared-icon.png",
            extension = "png",
            mediaType = "image/png",
        )
        val bitmap = Bitmap32(2, 2) { _, _ -> RGBA(255, 0, 0, 255) }
        val alpha = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            name = "Alpha",
            bitmap = bitmap,
            assetSource = iconSource,
        )
        val bravo = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            name = "Bravo",
            bitmap = bitmap,
            assetSource = iconSource,
        )

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(UnitTemplateSnapshot(alpha.id, alpha, 0)),
                    CreateUnitTemplateAction(UnitTemplateSnapshot(bravo.id, bravo, 1)),
                ),
            ),
            tempDir.resolve("shared-icon-save.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(savePath)
        val loadedNames = loaded.ledgerEntries
            .filterIsInstance<CreateUnitTemplateAction>()
            .map { it.snapshot.template.name }

        assertThat(loadedNames, equalTo(listOf("Alpha", "Bravo")))
        assertThat(loaded.ledgerEntries.filterIsInstance<CreateUnitTemplateAction>().size, equalTo(2))
    }

    @Test
    fun `distinct unit template icons stay distinct after a save load save cycle`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000cf")
        val firstSource = AssetSource(
            path = tempDir.resolve("alpha-icon.png").toString(),
            originalFilename = "alpha-icon.png",
            extension = "png",
            mediaType = "image/png",
        )
        val secondSource = AssetSource(
            path = tempDir.resolve("bravo-icon.png").toString(),
            originalFilename = "bravo-icon.png",
            extension = "png",
            mediaType = "image/png",
        )
        val alpha = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            name = "Alpha",
            bitmap = Bitmap32(2, 2) { x, _ -> if (x == 0) RGBA(255, 0, 0, 255) else RGBA(255, 255, 0, 255) },
            assetSource = firstSource,
        )
        val bravo = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000004")),
            name = "Bravo",
            bitmap = Bitmap32(2, 2) { x, _ -> if (x == 0) RGBA(0, 0, 255, 255) else RGBA(0, 255, 0, 255) },
            assetSource = secondSource,
        )

        val firstSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(UnitTemplateSnapshot(alpha.id, alpha, 0)),
                    CreateUnitTemplateAction(UnitTemplateSnapshot(bravo.id, bravo, 1)),
                ),
            ),
            tempDir.resolve("distinct-icons-first.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(firstSave)
        val secondSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
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
            tempDir.resolve("distinct-icons-second.oct.zip"),
        )

        val reread = OperationSaveReader().read(secondSave)
        val assetIds = reread.ledger.entries
            .filterIsInstance<CreateUnitTemplateEntrySaveDto>()
            .map { it.unitTemplate.template.assetId }

        assertThat(assetIds.distinct().size, equalTo(2))
    }

    @Test
    fun `edit unit template entries round trip through the save file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d0")
        val bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) }
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000020")),
            name = "SP",
            bitmap = bitmap,
            assetSource = AssetSource(
                path = tempDir.resolve("template.png").toString(),
                originalFilename = "template.png",
                extension = "png",
                mediaType = "image/png",
            ),
            affiliation = UnitAffiliation.Hostile,
        )
        val before = UnitTemplateSnapshot(template.id, template, 0)
        val after = before.copy(template = before.template.copy(name = "S", affiliation = UnitAffiliation.Unknown))

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(before),
                    EditUnitTemplateAction(
                        id = before.id,
                        forward = mapOf("name" to com.fasterxml.jackson.databind.node.TextNode("S")),
                        backward = mapOf("name" to com.fasterxml.jackson.databind.node.TextNode("SP")),
                        beforeSnapshot = before,
                        afterSnapshot = after,
                    ),
                ),
            ),
            tempDir.resolve("edit-template-save.oct.zip"),
        )

        val reread = OperationSaveReader().read(savePath)
        assertThat(reread.ledger.entries[1] is EditUnitTemplateEntrySaveDto, equalTo(true))

        val loaded = OperationSaveLoader().load(savePath)
        val edit = loaded.ledgerEntries.filterIsInstance<EditUnitTemplateAction>().single()
        assertThat(edit.id, equalTo(before.id))
        assertThat(edit.afterSnapshot?.template?.name, equalTo("S"))
        assertThat(edit.afterSnapshot?.template?.affiliation, equalTo(UnitAffiliation.Unknown))
    }

    @Test
    fun `edited imported ship template unitClass override round trips through the save file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d6")
        val definition = io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"mass":10,"class":"Armed Cargo Ship Freighter","name":"Cargo"}""",
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000037")),
            name = "Cargo",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            customProperties = io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val before = UnitTemplateSnapshot(template.id, template, 0)
        val after = before.copy(
            template = before.template.copy(
                customProperties = io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip.importedTemplateProperties(
                    before.template.customProperties + ("unitClass" to "MV"),
                    definition,
                ),
            ),
        )

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(
                    CreateUnitTemplateAction(before),
                    EditUnitTemplateAction(
                        id = before.id,
                        forward = io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplatePatch.diff(before, after).forward,
                        backward = io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplatePatch.diff(before, after).backward,
                        beforeSnapshot = before,
                        afterSnapshot = after,
                    ),
                ),
            ),
            tempDir.resolve("edit-imported-template-unit-class-save.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(savePath)
        val edit = loaded.ledgerEntries.filterIsInstance<EditUnitTemplateAction>().single()
        assertThat(edit.afterSnapshot?.template?.customProperties?.get("unitClass"), equalTo("MV"))
    }

    @Test
    fun `drawing colour round trips through the save file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d1")
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000021")),
            drawing = OvalDrawing(
                center = MapDrawingPoint(0.4, 0.6),
                xRadius = 0.1,
                yRadius = 0.1,
                color = Color.BLUE,
            ),
        )

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(CreateObjectAction(drawing)),
            ),
            tempDir.resolve("drawing-colour-save.oct.zip"),
        )

        val loadedDrawing = OperationSaveLoader().load(savePath)
            .ledgerEntries
            .filterIsInstance<CreateObjectAction>()
            .single()
            .snapshot as DrawingSnapshot

        assertThat(loadedDrawing.drawing.color, equalTo(Color.BLUE))
    }

    @Test
    fun `batch ledger entries round trip through the save file`() {
        val operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d2")
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000022")),
            drawing = OvalDrawing(
                center = MapDrawingPoint(0.4, 0.6),
                xRadius = 0.1,
                yRadius = 0.1,
                color = Color.RED,
            ),
        )

        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = operationId,
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(BatchLedgerEntry(listOf(CreateObjectAction(drawing), DeleteObjectAction(drawing)))),
            ),
            tempDir.resolve("batch-ledger-save.oct.zip"),
        )

        assertThat(OperationSaveReader().read(savePath).ledger.entries.single() is BatchLedgerEntrySaveDto, equalTo(true))
        assertThat(OperationSaveLoader().load(savePath).ledgerEntries.single() is BatchLedgerEntry, equalTo(true))
    }

    @Test
    fun `turn marker ledger entries round trip through the save file`() {
        val thumbnailAssetId = UUID.fromString("00000000-0000-0000-0000-0000000000f3")
        val thumbnail = BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB).also {
            it.setRGB(10, 10, Color.RED.rgb)
        }
        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-0000000000d3"),
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                ledgerEntries = listOf(AddTurnLedgerEntry("Turn 3", thumbnailAssetId)),
                generatedImageAssets = listOf(GeneratedImageAsset(thumbnailAssetId, "turn-thumbnail", thumbnail)),
            ),
            tempDir.resolve("turn-ledger-save.oct.zip"),
        )

        val entry = OperationSaveReader().read(savePath).ledger.entries.single()
        val loaded = OperationSaveLoader().load(savePath)
        val loadedEntry = loaded.ledgerEntries.single()

        assertThat(entry is AddTurnEntrySaveDto, equalTo(true))
        assertThat((entry as AddTurnEntrySaveDto).name, equalTo("Turn 3"))
        assertThat(entry.thumbnailAssetId, equalTo(thumbnailAssetId))
        assertThat(loadedEntry, equalTo(AddTurnLedgerEntry("Turn 3", thumbnailAssetId)))
        assertThat(loaded.generatedImageAssets.single().id, equalTo(thumbnailAssetId))
        ZipFile(savePath.toFile()).use { zip ->
            assertThat(zip.getEntry("assets/$thumbnailAssetId.jpg") != null, equalTo(true))
        }
    }

    @Test
    fun `loaded jpg map assets are saved back out as jpg`() {
        val jpgFile = tempDir.resolve("map.jpg")
        val jpgImage = BufferedImage(48, 32, BufferedImage.TYPE_INT_RGB).also {
            it.setRGB(1, 1, Color.BLUE.rgb)
        }
        ImageIO.write(jpgImage, "jpg", jpgFile.toFile())

        val firstSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-0000000000f4"),
                mapImage = jpgImage,
                mapSource = AssetSource.fromFile(jpgFile.toFile()),
                ledgerEntries = emptyList(),
            ),
            tempDir.resolve("map-jpg-first.oct.zip"),
        )

        val loaded = OperationSaveLoader().load(firstSave)
        val secondSave = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = loaded.operationId,
                operationName = loaded.operationName,
                mapImage = loaded.mapImage,
                mapSource = loaded.mapAssetSource,
                ledgerEntries = loaded.ledgerEntries,
            ),
            tempDir.resolve("map-jpg-second.oct.zip"),
        )

        val reread = OperationSaveReader().read(secondSave)
        ZipFile(secondSave.toFile()).use { zip ->
            assertThat(zip.getEntry("assets/${reread.game.map.assetId}.jpg") != null, equalTo(true))
            assertThat(zip.getEntry("assets/${reread.game.map.assetId}.png") == null, equalTo(true))
        }
    }

    @Test
    fun `grid colour saves as map hex value and loads into settings`() {
        val savePath = OperationSaveWriter(uuidGenerator = ::nextTestUuid).write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-0000000000f5"),
                mapImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
                mapSource = null,
                settings = MapViewerSettings(
                    operationName = "Grid Colour",
                    availableLayers = emptyList(),
                    enabledLayerIds = emptySet(),
                    staticLayerImages = emptyMap(),
                    width = 64,
                    height = 64,
                    unitsPerPixel = 20.0,
                    gridColour = Palette.Colours.DARK,
                ),
                ledgerEntries = emptyList(),
            ),
            tempDir.resolve("grid-colour-save.oct.zip"),
        )

        val reread = OperationSaveReader().read(savePath)
        val loaded = OperationSaveLoader().load(savePath)

        assertThat(reread.game.map.gridColour, equalTo("#120909"))
        assertThat(loaded.settings.gridColour, equalTo(Palette.Colours.DARK))
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
            unitsPerPixel = 20.0,
        ),
        mapImage: BufferedImage,
        mapSource: AssetSource?,
        ledgerEntries: List<io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry>,
        generatedImageAssets: List<GeneratedImageAsset> = emptyList(),
        additionalDataJson: ByteArray? = null,
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
        generatedImageAssets = generatedImageAssets,
        additionalDataJson = additionalDataJson,
    )

    private fun assertFlatCustomProperties(savePath: Path) {
        ZipFile(savePath.toFile()).use { zip ->
            val ledgerJson = OperationSaveJson.objectMapper.readTree(zip.getInputStream(zip.getEntry("ledger.json")))
            val templateJson = ledgerJson["entries"][0]["unitTemplate"]["template"]
            val unitJson = ledgerJson["entries"][1]["objectData"]

            assertThat(templateJson["role"].asText(), equalTo("screen"))
            assertThat(templateJson["echelon"].asText(), equalTo("platoon"))
            assertThat(templateJson.has("customProperties"), equalTo(false))
            assertThat(unitJson["callsign"].asText(), equalTo("R1"))
            assertThat(unitJson["morale"].asText(), equalTo("steady"))
            assertThat(unitJson.has("customProperties"), equalTo(false))
        }
    }

    private fun ZipFile.read(name: String): ByteArray =
        getInputStream(getEntry(name)).use { it.readBytes() }
}
