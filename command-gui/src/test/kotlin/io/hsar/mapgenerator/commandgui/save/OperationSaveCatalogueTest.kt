package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

class OperationSaveCatalogueTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `catalogue sorts saves by most recently updated first`() {
        val writer = OperationSaveWriter()
        val mapImage = BufferedImage(128, 64, BufferedImage.TYPE_INT_RGB)

        writer.write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                operationName = "Older Operation",
                updatedAt = Instant.parse("2026-06-18T10:00:00Z"),
                mapImage = mapImage,
            ),
            tempDir.resolve("older.oct.zip"),
        )
        writer.write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                operationName = "Newer Operation",
                updatedAt = Instant.parse("2026-06-18T12:00:00Z"),
                mapImage = mapImage,
            ),
            tempDir.resolve("newer.oct.zip"),
        )

        val saves = OperationSaveCatalogue().list(tempDir)

        assertThat(saves.map { it.operationName }, equalTo(listOf("Newer Operation", "Older Operation")))
        assertThat(saves.first().thumbnailAssetId, equalTo(UUID.fromString("00000000-0000-0000-0000-000000000002")))
    }

    @Test
    fun `catalogue skips broken saves without failing`() {
        val writer = OperationSaveWriter()
        val mapImage = BufferedImage(128, 64, BufferedImage.TYPE_INT_RGB)

        writer.write(
            request(
                operationId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                operationName = "Valid Operation",
                updatedAt = Instant.parse("2026-06-18T12:00:00Z"),
                mapImage = mapImage,
            ),
            tempDir.resolve("valid.oct.zip"),
        )
        Files.writeString(tempDir.resolve("broken.oct.zip"), "not a zip")

        val saves = OperationSaveCatalogue().list(tempDir)

        assertThat(saves.map { it.operationName }, equalTo(listOf("Valid Operation")))
    }

    private fun request(
        operationId: UUID,
        operationName: String,
        updatedAt: Instant,
        mapImage: BufferedImage,
    ) = OperationSaveRequest(
        operationId = operationId,
        operationName = operationName,
        createdAt = Instant.parse("2026-06-18T09:00:00Z"),
        updatedAt = updatedAt,
        camera = CameraViewport.centeredOnMap(
            geometry = geometryFor(mapImage),
            mapWidth = mapImage.width,
            mapHeight = mapImage.height,
            viewportWidth = 800,
            viewportHeight = 600,
        ),
        settings = MapViewerSettings(
            operationName = operationName,
            width = mapImage.width,
            height = mapImage.height,
            metresPerPixel = 20.0,
        ),
        mapImage = mapImage,
        mapAssetSource = null,
        ledgerEntries = emptyList(),
    )
}
