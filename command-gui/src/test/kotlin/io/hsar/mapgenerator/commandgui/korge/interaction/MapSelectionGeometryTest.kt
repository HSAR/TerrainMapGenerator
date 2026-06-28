package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
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
    fun `pivot handle appears above right of a selected unit`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val unit = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000011",
            x = 0.5,
            y = 0.5,
        )

        val handle = pivotHandleForSelection(listOf(unit), coordinateSpace, viewport)

        assertThat(handle?.center?.x, equalTo(616.0))
        assertThat(handle?.center?.y, equalTo(384.0))
    }

    @Test
    fun `pivot handle uses combined bounds for group selection`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val leftUnit = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000012",
            x = 0.3,
            y = 0.3,
        )
        val rightUnit = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000013",
            x = 0.7,
            y = 0.7,
        )

        val handle = pivotHandleForSelection(listOf(leftUnit, rightUnit), coordinateSpace, viewport)
        val frame = listOf(leftUnit, rightUnit).groupSelectionFrame(coordinateSpace, viewport)

        assertThat(handle?.center, equalTo(frame?.topRightCorner))
    }

    @Test
    fun `pivot handle appears for a selected drawing`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000014")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.4, 0.4),
                end = MapDrawingPoint(0.6, 0.6),
                color = java.awt.Color.RED,
            ),
        )

        val handle = pivotHandleForSelection(listOf(drawing), coordinateSpace, viewport)

        assertThat(handle?.center?.x, equalTo(616.0))
        assertThat(handle?.center?.y, equalTo(384.0))
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

    @Test
    fun `selection centroid averages selected object centres for move previews`() {
        val leftUnit = unitSnapshot(id = "00000000-0000-0000-0000-000000000021", x = 0.25, y = 0.5)
        val rightUnit = unitSnapshot(id = "00000000-0000-0000-0000-000000000022", x = 0.75, y = 0.5)
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000023")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.4, 0.4),
                end = MapDrawingPoint(0.6, 0.6),
                color = Color.RED,
            ),
        )

        val centroid = listOf(leftUnit, rightUnit, drawing).selectionCentroid(coordinateSpace)

        assertThat(centroid?.x, equalTo(0.5))
        assertThat(centroid?.y, equalTo(0.5))
    }

    @Test
    fun `selection accent colour follows the selected unit affiliation`() {
        val unit = unitSnapshot(id = "00000000-0000-0000-0000-000000000031", x = 0.5, y = 0.5, affiliation = UnitAffiliation.Hostile)

        assertThat(unit.selectionAccentColor(), equalTo(UnitAffiliationUi.swatchColor(UnitAffiliation.Hostile)))
    }

    @Test
    fun `multi selection ignores drawings and treats null affiliations as wildcards`() {
        val friend = unitSnapshot(id = "00000000-0000-0000-0000-000000000041", x = 0.2, y = 0.2, affiliation = UnitAffiliation.Friend)
        val unknown = unitSnapshot(id = "00000000-0000-0000-0000-000000000042", x = 0.8, y = 0.2, affiliation = null)
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000043")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.4, 0.4),
                end = MapDrawingPoint(0.6, 0.6),
                color = Color.RED,
            ),
        )

        assertThat(listOf(friend, unknown, drawing).selectionAccentColor(), equalTo(UnitAffiliationUi.swatchColor(UnitAffiliation.Friend)))
    }

    @Test
    fun `multi selection falls back to default when affiliations are mixed or missing`() {
        val friend = unitSnapshot(id = "00000000-0000-0000-0000-000000000051", x = 0.2, y = 0.2, affiliation = UnitAffiliation.Friend)
        val hostile = unitSnapshot(id = "00000000-0000-0000-0000-000000000052", x = 0.8, y = 0.2, affiliation = UnitAffiliation.Hostile)
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000053")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.4, 0.4),
                end = MapDrawingPoint(0.6, 0.6),
                color = Color.RED,
            ),
        )

        assertThat(listOf(friend, hostile, drawing).selectionAccentColor(), equalTo(UnitAffiliationUi.swatchColor(null)))
        assertThat(listOf(drawing).selectionAccentColor(), equalTo(UnitAffiliationUi.swatchColor(null)))
        assertThat(
            listOf(unitSnapshot(id = "00000000-0000-0000-0000-000000000054", x = 0.5, y = 0.5, affiliation = null)).selectionAccentColor(),
            equalTo(UnitAffiliationUi.swatchColor(null))
        )
    }

    private fun unitSnapshot(id: String, x: Double, y: Double, affiliation: UnitAffiliation? = null) =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4), affiliation = affiliation),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(x, y),
            normalizedWidth = 0.2,
            normalizedHeight = 0.2,
        )
}
