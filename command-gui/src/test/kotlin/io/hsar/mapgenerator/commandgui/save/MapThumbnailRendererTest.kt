package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.geometryFor
import io.hsar.mapgenerator.commandgui.korge.adapter.toMapViewport
import io.hsar.mapgenerator.commandgui.korge.adapter.toOperationContent
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationContent
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

class MapThumbnailRendererTest {
    @Test
    fun `operation thumbnail uses camera pan to select the rendered crop`() {
        val mapImage = BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB).also { image ->
            image.createGraphics().apply {
                color = Color.RED
                fillRect(0, 0, 50, 50)
                color = Color.BLUE
                fillRect(50, 0, 50, 50)
                dispose()
            }
        }
        val geometry = geometryFor(mapImage)
        val leftViewport = viewportFor(geometry.mapOffsetX + 25.0, geometry.mapOffsetY + 25.0, geometry.backgroundWidth, geometry.backgroundHeight)
        val rightViewport = viewportFor(geometry.mapOffsetX + 75.0, geometry.mapOffsetY + 25.0, geometry.backgroundWidth, geometry.backgroundHeight)

        val leftThumbnail = MapThumbnailRenderer().renderOperationThumbnail(mapImage, geometry, leftViewport.toMapViewport(), null, OperationContent.EMPTY, outputWidth = 50)
        val rightThumbnail = MapThumbnailRenderer().renderOperationThumbnail(mapImage, geometry, rightViewport.toMapViewport(), null, OperationContent.EMPTY, outputWidth = 50)

        assertThat(leftThumbnail.getRGB(25, 25), not(equalTo(rightThumbnail.getRGB(25, 25))))
    }

    @Test
    fun `operation thumbnail includes persistent unit and drawing snapshots`() {
        val mapImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB).also { image ->
            image.createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, 100, 100)
                dispose()
            }
        }
        val geometry = geometryFor(mapImage)
        val viewport = viewportFor(geometry.mapOffsetX + 50.0, geometry.mapOffsetY + 50.0, geometry.backgroundWidth, geometry.backgroundHeight, 100, 100)
        val unitTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            name = "Red Unit",
            bitmap = Bitmap32(8, 8) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val snapshots = listOf(
            DrawingSnapshot(
                id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                drawing = RectangleDrawing(
                    start = MapDrawingPoint(0.1, 0.1),
                    end = MapDrawingPoint(0.9, 0.9),
                    color = Color.GREEN,
                ),
            ),
            UnitSnapshot(
                id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
                unitTemplateId = unitTemplate.id,
                template = unitTemplate,
                instanceName = "Red Unit",
                destroyed = false,
                facingDegrees = null,
                mapCoordinate = MapCoordinate(0.5, 0.5),
                normalizedWidth = 0.2,
                normalizedHeight = 0.2,
            ),
        )

        val thumbnail = MapThumbnailRenderer().renderOperationThumbnail(mapImage, geometry, viewport.toMapViewport(), null, snapshots.toOperationContent(), outputWidth = 100)

        assertThat(thumbnail.getRGB(50, 50), equalTo(Color.RED.rgb))
        assertThat(thumbnail.getRGB(10, 10), not(equalTo(Color.WHITE.rgb)))
    }

    private fun viewportFor(
        centerX: Double,
        centerY: Double,
        worldWidth: Int,
        worldHeight: Int,
        viewportWidth: Int = 50,
        viewportHeight: Int = 50,
    ) = CameraViewport(
        centerX = centerX,
        centerY = centerY,
        zoom = 1.0,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        worldWidth = worldWidth,
        worldHeight = worldHeight,
    )
}
