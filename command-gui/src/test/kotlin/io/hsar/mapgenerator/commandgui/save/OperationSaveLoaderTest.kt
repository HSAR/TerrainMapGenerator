package io.hsar.mapgenerator.commandgui.save

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
}
