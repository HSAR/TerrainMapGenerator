package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.OvalDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import korlibs.image.bitmap.Bitmap32
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
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
    fun `unit selection frame follows dynamic icon size`() {
        val smallIconCoordinateSpace = MapCoordinateSpace(
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapWidth = 1000,
            mapHeight = 1000,
        )
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val unit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 8)),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.5, 0.5),
            normalizedWidth = 0.004,
            normalizedHeight = 0.008,
        )

        val frame = unit.selectionFrame(smallIconCoordinateSpace, viewport, screenPadding = 4.0)

        assertThat(frame.topLeft.x, closeTo(494.0, 0.0001))
        assertThat(frame.topLeft.y, closeTo(492.0, 0.0001))
        assertThat(frame.bottomRight.x, closeTo(506.0, 0.0001))
        assertThat(frame.bottomRight.y, closeTo(508.0, 0.0001))
    }

    @Test
    fun `unit selection frame remains axis aligned regardless of facing`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val unit = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000004")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = 135,
            mapCoordinate = MapCoordinate(0.5, 0.5),
            normalizedWidth = 0.2,
            normalizedHeight = 0.2,
        )

        val frame = unit.selectionFrame(coordinateSpace, viewport)

        assertThat(frame.topLeft.y, equalTo(frame.topRight.y))
        assertThat(frame.topLeft.x, equalTo(frame.bottomLeft.x))
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
    fun `pivot handle appears above right of group selection bounds`() {
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

        assertThat(handle?.center?.x, equalTo(816.0))
        assertThat(handle?.center?.y, equalTo(184.0))
    }

    @Test
    fun `pivot handle offset follows rotated frame orientation`() {
        val frame = ScreenSelectionFrame(
            topLeft = Vector2D(0.0, 0.0),
            topRight = Vector2D(10.0, 0.0),
            bottomRight = Vector2D(10.0, 20.0),
            bottomLeft = Vector2D(0.0, 20.0),
        ).rotatedAround(
            pivot = Vector2D(0.0, 0.0),
            deltaRadians = Math.PI * 0.5,
        )

        val handleCenter = frame.topRightOffsetCorner(5.0)

        assertThat(handleCenter.x, closeTo(5.0, 0.0001))
        assertThat(handleCenter.y, closeTo(15.0, 0.0001))
    }

    @Test
    fun `corner brackets render sharp six point shapes outside each selection frame corner`() {
        val frame = ScreenSelectionFrame(
            topLeft = Vector2D(0.0, 0.0),
            topRight = Vector2D(10.0, 0.0),
            bottomRight = Vector2D(10.0, 20.0),
            bottomLeft = Vector2D(0.0, 20.0),
        )

        val brackets = frame.cornerBrackets(length = 4.0, thickness = 2.0)

        assertThat(
            brackets.map { it.corner }, equalTo(
                listOf(
                    ScreenSelectionCorner.TOP_LEFT,
                    ScreenSelectionCorner.TOP_RIGHT,
                    ScreenSelectionCorner.BOTTOM_RIGHT,
                    ScreenSelectionCorner.BOTTOM_LEFT,
                )
            )
        )
        assertThat(
            brackets[0].points, equalTo(
                listOf(
                    Vector2D(0.0, 0.0),
                    Vector2D(4.0, 0.0),
                    Vector2D(4.0, -2.0),
                    Vector2D(-2.0, -2.0),
                    Vector2D(-2.0, 4.0),
                    Vector2D(0.0, 4.0),
                )
            )
        )
        assertThat(
            brackets[3].points, equalTo(
                listOf(
                    Vector2D(0.0, 20.0),
                    Vector2D(4.0, 20.0),
                    Vector2D(4.0, 22.0),
                    Vector2D(-2.0, 22.0),
                    Vector2D(-2.0, 16.0),
                    Vector2D(0.0, 16.0),
                )
            )
        )
        assertThat(brackets[0].contains(Vector2D(1.0, -1.0)), equalTo(true))
        assertThat(brackets[0].contains(Vector2D(3.0, 3.0)), equalTo(false))
    }

    @Test
    fun `corner brackets follow rotated frame orientation`() {
        val frame = ScreenSelectionFrame(
            topLeft = Vector2D(0.0, 0.0),
            topRight = Vector2D(10.0, 0.0),
            bottomRight = Vector2D(10.0, 20.0),
            bottomLeft = Vector2D(0.0, 20.0),
        ).rotatedAround(
            pivot = Vector2D(0.0, 0.0),
            deltaRadians = Math.PI * 0.5,
        )

        val topRightBracket = frame.cornerBrackets(length = 4.0, thickness = 2.0)[1]

        assertPoint(topRightBracket.points[0], 0.0, 10.0)
        assertPoint(topRightBracket.points[1], 0.0, 6.0)
        assertPoint(topRightBracket.points[2], 2.0, 6.0)
        assertPoint(topRightBracket.points[3], 2.0, 12.0)
        assertPoint(topRightBracket.points[4], -4.0, 12.0)
        assertPoint(topRightBracket.points[5], -4.0, 10.0)
    }

    @Test
    fun `line selection frame follows segment orientation`() {
        val frame = ScreenSelectionFrame.fromSegment(
            start = Vector2D(10.0, 10.0),
            end = Vector2D(30.0, 22.0),
            padding = 4.0,
        )

        assertThat(frame.topLeft.y == frame.topRight.y, equalTo(false))
        assertThat(frame.topLeft.x == frame.bottomLeft.x, equalTo(false))
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
    fun `rotated drawing selection frame follows drawing orientation`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000015")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.4, 0.45),
                end = MapDrawingPoint(0.6, 0.55),
                color = Color.RED,
                rotationDegrees = 45.0,
            ),
        )

        val frame = drawing.selectionFrame(coordinateSpace, viewport)

        assertThat(frame.topLeft.y == frame.topRight.y, equalTo(false))
        assertThat(frame.topLeft.x == frame.bottomLeft.x, equalTo(false))
    }

    @Test
    fun `pivot handle is hidden while moving a selection`() {
        assertThat(shouldPublishSelectionPivotHandle(boxSelectionActive = false, moveActive = false), equalTo(true))
        assertThat(shouldPublishSelectionPivotHandle(boxSelectionActive = false, moveActive = true), equalTo(false))
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
    fun `oval selection boxes only match the outline`() {
        val oval = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000024")),
            drawing = OvalDrawing(
                center = MapDrawingPoint(0.5, 0.5),
                xRadius = 0.1,
                yRadius = 0.1,
                color = Color.RED,
            ),
        )

        assertThat(
            oval.overlapsSelectionBox(
                MapSelectionBounds(0.48, 0.48, 0.52, 0.52),
                coordinateSpace,
            ),
            equalTo(false),
        )
        assertThat(
            oval.overlapsSelectionBox(
                MapSelectionBounds(0.595, 0.49, 0.603, 0.51),
                coordinateSpace,
            ),
            equalTo(true),
        )
        assertThat(
            oval.overlapsSelectionBox(
                MapSelectionBounds(0.49, 0.395, 0.51, 0.403),
                coordinateSpace,
            ),
            equalTo(true),
        )
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
    fun `drawing selection accent colour uses the supplied grid colour`() {
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000034")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.1, 0.1),
                end = MapDrawingPoint(0.9, 0.9),
                color = Color.RED,
            ),
        )

        assertThat(drawing.selectionAccentColor(Color.BLUE), equalTo(Color.BLUE))
    }

    @Test
    fun `measure text colour uses grid colour for units with no affiliation`() {
        val unit = unitSnapshot(id = "00000000-0000-0000-0000-000000000032", x = 0.5, y = 0.5, affiliation = null)

        assertThat(unit.measureTextColor(), equalTo(UnitAffiliationUi.unaffiliatedColor()))
    }

    @Test
    fun `measure text colour follows selected unit affiliation when present`() {
        val unit = unitSnapshot(id = "00000000-0000-0000-0000-000000000033", x = 0.5, y = 0.5, affiliation = UnitAffiliation.Hostile)

        assertThat(unit.measureTextColor(), equalTo(UnitAffiliationUi.swatchColor(UnitAffiliation.Hostile)))
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

        assertThat(listOf(friend, hostile, drawing).selectionAccentColor(), equalTo(UnitAffiliationUi.unaffiliatedColor()))
        assertThat(listOf(drawing).selectionAccentColor(), equalTo(UnitAffiliationUi.unaffiliatedColor()))
        assertThat(
            listOf(unitSnapshot(id = "00000000-0000-0000-0000-000000000054", x = 0.5, y = 0.5, affiliation = null)).selectionAccentColor(),
            equalTo(UnitAffiliationUi.unaffiliatedColor())
        )
    }

    @Test
    fun `multi selection measure text colour uses grid colour when affiliations are missing`() {
        val first = unitSnapshot(id = "00000000-0000-0000-0000-000000000055", x = 0.2, y = 0.2, affiliation = null)
        val second = unitSnapshot(id = "00000000-0000-0000-0000-000000000056", x = 0.8, y = 0.2, affiliation = null)

        assertThat(listOf(first, second).measureTextColor(), equalTo(UnitAffiliationUi.unaffiliatedColor()))
    }

    @Test
    fun `null affiliation overlay colour uses grid colour`() {
        assertThat(null.overlayColor(), equalTo(UnitAffiliationUi.unaffiliatedColor()))
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

    private fun assertPoint(point: Vector2D, x: Double, y: Double) {
        assertThat(point.x, closeTo(x, 0.0001))
        assertThat(point.y, closeTo(y, 0.0001))
    }
}
