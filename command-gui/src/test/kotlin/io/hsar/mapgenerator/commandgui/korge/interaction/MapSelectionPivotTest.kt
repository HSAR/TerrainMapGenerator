package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import korlibs.image.bitmap.Bitmap32
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.PI

class MapSelectionPivotTest {
    private val coordinateSpace = MapCoordinateSpace(
        geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
        mapWidth = 1000,
        mapHeight = 1000,
    )

    @Test
    fun `bearing angle follows grid north clockwise`() {
        val origin = Vector2D(0.0, 0.0)

        assertThat(MapSelectionPivot.bearingAngle(origin, Vector2D(0.0, -10.0)), closeTo(0.0, 0.0001))
        assertThat(MapSelectionPivot.bearingAngle(origin, Vector2D(10.0, 0.0)), closeTo(PI / 2.0, 0.0001))
    }

    @Test
    fun `single unit pivot creates implicit zero facing without moving the unit`() {
        val unit = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000001",
            x = 0.5,
            y = 0.5,
            facing = null,
        )

        val updated = MapSelectionPivot.rotateSnapshots(
            originalSnapshots = listOf(unit),
            pivotCenterWorld = coordinateSpace.toWorld(unit.mapCoordinate),
            deltaRadians = PI / 2.0,
            coordinateSpace = coordinateSpace,
        ).single() as UnitSnapshot

        assertThat(updated.facingDegrees, equalTo(90))
        assertThat(updated.mapCoordinate, equalTo(unit.mapCoordinate))
    }

    @Test
    fun `multi unit pivot rotates positions around centroid and updates facings`() {
        val north = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000002",
            x = 0.5,
            y = 0.4,
            facing = 10,
        )
        val south = unitSnapshot(
            id = "00000000-0000-0000-0000-000000000003",
            x = 0.5,
            y = 0.6,
            facing = null,
        )
        val pivotCenter = MapSelectionPivot.pivotCenter(listOf(north, south), coordinateSpace)

        val updated = MapSelectionPivot.rotateSnapshots(
            originalSnapshots = listOf(north, south),
            pivotCenterWorld = coordinateSpace.toWorld(pivotCenter),
            deltaRadians = PI / 2.0,
            coordinateSpace = coordinateSpace,
        ).map { it as UnitSnapshot }

        assertThat(updated[0].facingDegrees, equalTo(100))
        assertThat(updated[0].mapCoordinate.x, closeTo(0.6, 0.0001))
        assertThat(updated[0].mapCoordinate.y, closeTo(0.5, 0.0001))
        assertThat(updated[1].facingDegrees, equalTo(90))
        assertThat(updated[1].mapCoordinate.x, closeTo(0.4, 0.0001))
        assertThat(updated[1].mapCoordinate.y, closeTo(0.5, 0.0001))
    }

    @Test
    fun `freeform drawings rotate around the shared pivot center`() {
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000004")),
            drawing = FreeformDrawing(
                points = listOf(
                    io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint(0.4, 0.5),
                    io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint(0.5, 0.6),
                    io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint(0.6, 0.5),
                ),
                color = java.awt.Color.RED,
            ),
        )

        val updated = MapSelectionPivot.rotateSnapshots(
            originalSnapshots = listOf(drawing),
            pivotCenterWorld = coordinateSpace.toWorld(MapCoordinate(0.5, 0.5)),
            deltaRadians = PI / 2.0,
            coordinateSpace = coordinateSpace,
        ).single() as DrawingSnapshot

        val points = (updated.drawing as FreeformDrawing).points
        assertThat(points[0].x, closeTo(0.5, 0.0001))
        assertThat(points[0].y, closeTo(0.4, 0.0001))
        assertThat(points[1].x, closeTo(0.4, 0.0001))
        assertThat(points[1].y, closeTo(0.5, 0.0001))
        assertThat(points[2].x, closeTo(0.5, 0.0001))
        assertThat(points[2].y, closeTo(0.6, 0.0001))
    }

    private fun unitSnapshot(id: String, x: Double, y: Double, facing: Int?) =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = facing,
            mapCoordinate = MapCoordinate(x, y),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )
}
