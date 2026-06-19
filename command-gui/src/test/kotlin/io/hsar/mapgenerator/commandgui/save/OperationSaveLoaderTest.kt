package io.hsar.mapgenerator.commandgui.save

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class OperationSaveLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `old save format is rejected`() {
        val savePath = tempDir.resolve("legacy.oct.zip")
        ZipOutputStream(Files.newOutputStream(savePath)).use { zip ->
            zip.putNextEntry(ZipEntry("game.json"))
            zip.write(
                """
                {
                  "formatVersion": 1,
                  "id": "00000000-0000-0000-0000-000000000001",
                  "name": "Legacy",
                  "createdAt": "2026-06-19T10:00:00Z",
                  "updatedAt": "2026-06-19T10:00:00Z",
                  "camera": { "x": 0.5, "y": 0.5, "zoom": 1.0 },
                  "map": {
                    "metresPerPixel": 20.0,
                    "assetId": "00000000-0000-0000-0000-000000000002"
                  }
                }
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ledger.json"))
            zip.write("""{"entries": []}""".toByteArray())
            zip.closeEntry()
        }

        val error = assertThrows<IllegalArgumentException> {
            OperationSaveLoader().load(savePath)
        }

        assertThat(error.message ?: "", containsString("Unsupported save format version 1"))
    }

    @Test
    fun `missing camera falls back to fitted startup viewport`() {
        val mapImage = BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB)
        val mapPath = tempDir.resolve("map.png")
        ImageIO.write(mapImage, "png", mapPath.toFile())
        val savePath = tempDir.resolve("missing-camera.oct.zip")
        val operationId = "00000000-0000-0000-0000-000000000010"
        val assetId = "00000000-0000-0000-0000-000000000011"

        ZipOutputStream(Files.newOutputStream(savePath)).use { zip ->
            zip.putNextEntry(ZipEntry("game.json"))
            zip.write(
                """
                {
                  "formatVersion": 2,
                  "id": "$operationId",
                  "name": "No Camera",
                  "createdAt": "2026-06-19T10:00:00Z",
                  "updatedAt": "2026-06-19T10:00:00Z",
                  "map": {
                    "metresPerPixel": 20.0,
                    "assetId": "$assetId"
                  }
                }
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ledger.json"))
            zip.write("""{"entries": []}""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/$assetId.png"))
            zip.write(Files.readAllBytes(mapPath))
            zip.closeEntry()
        }

        val loaded = OperationSaveLoader().load(savePath)
        assertThat(loaded.camera, equalTo(null))
        val fallback = io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport.centeredOnMap(
            geometry = io.hsar.mapgenerator.commandgui.geometryFor(loaded.mapImage),
            mapWidth = loaded.settings.width,
            mapHeight = loaded.settings.height,
            viewportWidth = loaded.settings.width,
            viewportHeight = loaded.settings.height,
            zoom = io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport.fitZoomToViewport(
                mapWidth = loaded.settings.width,
                mapHeight = loaded.settings.height,
                viewportWidth = loaded.settings.width,
                viewportHeight = loaded.settings.height,
            ),
        )
        assertThat(fallback.zoom, closeTo(0.95, 0.0001))
    }
}
