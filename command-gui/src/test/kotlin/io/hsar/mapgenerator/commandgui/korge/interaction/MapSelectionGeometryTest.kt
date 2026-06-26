package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class MapSelectionGeometryTest {
    private val coordinateSpace = MapCoordinateSpace(
        geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
        mapWidth = 1000,
        mapHeight = 1000,
    )

    @Test
    fun `unit selection bounds overlap partial selection boxes`() {
        val unit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.5, 0.5),
            normalizedWidth = 0.2,
            normalizedHeight = 0.2,
        )

        val bounds = unit.selectionBounds(coordinateSpace)

        assertThat(bounds.overlaps(MapSelectionBounds(0.59, 0.59, 0.7, 0.7)), equalTo(true))
        assertThat(bounds.overlaps(MapSelectionBounds(0.7, 0.7, 0.8, 0.8)), equalTo(false))
    }

    @Test
    fun `drawing selection bounds overlap partial selection boxes`() {
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.2, 0.3),
                end = MapDrawingPoint(0.6, 0.7),
                color = Color.RED,
            ),
        )

        val bounds = drawing.selectionBounds(coordinateSpace)

        assertThat(bounds.overlaps(MapSelectionBounds(0.55, 0.65, 0.8, 0.9)), equalTo(true))
        assertThat(bounds.overlaps(MapSelectionBounds(0.7, 0.1, 0.9, 0.2)), equalTo(false))
    }

    @Test
    fun `rectangle selection ignores a drag box that sits fully inside the rectangle`() {
        val rectangle = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.2, 0.2),
                end = MapDrawingPoint(0.8, 0.8),
                color = Color.RED,
            ),
        )

        assertThat(
            rectangle.overlapsSelectionBox(
                MapSelectionBounds(0.35, 0.35, 0.45, 0.45),
                coordinateSpace,
            ),
            equalTo(false),
        )
        assertThat(
            rectangle.overlapsSelectionBox(
                MapSelectionBounds(0.1, 0.1, 0.3, 0.3),
                coordinateSpace,
            ),
            equalTo(true),
        )
    }
}
