package io.hsar.mapgenerator.commandgui

import com.fasterxml.jackson.databind.node.TextNode
import io.hsar.mapgenerator.commandgui.save.AddTurnEntrySaveDto
import io.hsar.mapgenerator.commandgui.save.CameraSaveDto
import io.hsar.mapgenerator.commandgui.save.CreateNoteEntrySaveDto
import io.hsar.mapgenerator.commandgui.save.EditNoteEntrySaveDto
import io.hsar.mapgenerator.commandgui.save.GameSaveDto
import io.hsar.mapgenerator.commandgui.save.LedgerEntrySaveDto
import io.hsar.mapgenerator.commandgui.save.LedgerFileDto
import io.hsar.mapgenerator.commandgui.save.MapSaveDto
import io.hsar.mapgenerator.commandgui.save.NoteSaveDto
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import korlibs.io.lang.use
import org.apache.batik.ext.awt.image.GraphicsUtil.createGraphics
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class MapViewerSaveRepositoryTest {

    @TempDir
    lateinit var savesDirectory: Path

    @Test
    fun `lists saves by update time and skips invalid archives`() {
        writeMapSave("older.zip", "Older", "2026-07-28T12:00:00Z")
        writeMapSave("newer.zip", "Newer", "2026-07-29T12:00:00Z")
        Files.writeString(savesDirectory.resolve("broken.zip"), "not a save")

        val saves = MapViewerSaveRepository(savesDirectory).list()

        assertThat(saves.map { it.operationName }, equalTo(listOf("Newer", "Older")))
        assertThat(saves.first().thumbnail?.getRGB(0, 0), equalTo(Color(22, 48, 80).rgb))
    }

    @Test
    fun `loads map grid and camera without requiring a ledger`() {
        val path = writeMapSave("map-only.zip", "Map Level", "2026-07-29T12:00:00Z")

        val loaded = MapViewerSaveRepository(savesDirectory).loadMap(path)

        assertThat(loaded.settings.operationName, equalTo("Map Level"))
        assertThat(loaded.renderData.baseImage.width, equalTo(120))
        assertThat(loaded.renderData.baseImage.height, equalTo(80))
        assertThat(loaded.renderData.gridLayer?.mapId, equalTo("31U DQ"))
        assertThat(loaded.renderData.gridLayer?.colour, equalTo(GridColourSelector.PRESETS.first()))
        assertThat(loaded.camera?.x ?: 0.0, closeTo(0.25, 0.0001))
        assertThat(loaded.camera?.y ?: 0.0, closeTo(0.75, 0.0001))
        assertThat(loaded.camera?.zoom ?: 0.0, closeTo(1.5, 0.0001))
        assertThat(loaded.ledger, equalTo(MapViewerLedgerReplay.EMPTY))
    }

    @Test
    fun `replays persisted ledger into a neutral timeline and current state`() {
        val noteId = UUID.fromString("00000000-0000-0000-0000-000000000020")
        val path = writeMapSave(
            fileName = "with-ledger.zip",
            name = "Ledger Operation",
            updatedAt = "2026-07-29T12:00:00Z",
            ledgerEntries = listOf(
                AddTurnEntrySaveDto("Operation Start"),
                CreateNoteEntrySaveDto(NoteSaveDto(noteId, "Initial", "First body")),
                EditNoteEntrySaveDto(
                    id = noteId,
                    forward = mapOf("title" to TextNode.valueOf("Updated"), "body" to TextNode.valueOf("Latest body")),
                    backward = mapOf("title" to TextNode.valueOf("Initial"), "body" to TextNode.valueOf("First body")),
                ),
            ),
        )

        val replay = MapViewerSaveRepository(savesDirectory).loadMap(path).ledger

        assertThat(
            replay.entries.map { it.kind }, equalTo(
                listOf(
                    MapViewerLedgerEntryKind.TURN,
                    MapViewerLedgerEntryKind.NOTE,
                    MapViewerLedgerEntryKind.NOTE,
                )
            )
        )
        assertThat(replay.entries.first().turnName, equalTo("Operation Start"))
        assertThat(replay.notes.single().title, equalTo("Updated"))
        assertThat(replay.notes.single().body, equalTo("Latest body"))
    }

    private fun writeMapSave(
        fileName: String,
        name: String,
        updatedAt: String,
        ledgerEntries: List<LedgerEntrySaveDto>? = null,
    ): Path {
        val mapAssetId = "00000000-0000-0000-0000-000000000010"
        val thumbnailAssetId = "00000000-0000-0000-0000-000000000011"
        val image = BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().let { graphics ->
                graphics.color = Color(22, 48, 80)
                graphics.fillRect(0, 0, width, height)
                graphics.dispose()
            }
        }
        val game = GameSaveDto(
            formatVersion = 6,
            id = "00000000-0000-0000-0000-000000000001",
            name = name,
            createdAt = Instant.parse("2026-07-27T12:00:00Z").toString(),
            updatedAt = updatedAt,
            thumbnailAssetId = thumbnailAssetId,
            camera = CameraSaveDto(x = 0.25, y = 0.75, zoom = 1.5),
            map = MapSaveDto(
                unitsPerPixel = 100.0,
                assetId = mapAssetId,
                mapId = "31U DQ",
                gridStartX = "10000",
                gridStartY = "20000",
                gridColour = GridColourSelector.toHex(GridColourSelector.PRESETS.first()),
            ),
        )
        return savesDirectory.resolve(fileName).also { path ->
            ZipOutputStream(Files.newOutputStream(path)).use { zip ->
                zip.write("game.json", OperationSaveJson.objectMapper.writeValueAsBytes(game))
                zip.write("assets/$mapAssetId.png", image.toPng())
                zip.write("assets/$thumbnailAssetId.png", image.toPng())
                ledgerEntries?.let {
                    zip.write(
                        "ledger.json",
                        OperationSaveJson.objectMapper.writeValueAsBytes(LedgerFileDto(it)),
                    )
                }
            }
        }
    }

    private fun BufferedImage.toPng() =
        ByteArrayOutputStream().use { output ->
            ImageIO.write(this, "png", output)
            output.toByteArray()
        }

    private fun ZipOutputStream.write(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }
}
