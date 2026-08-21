package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapSelectionScreenBounds
import io.hsar.mapgenerator.commandgui.MapViewerScreenBounds
import io.hsar.mapgenerator.commandgui.ViewerPoint
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationRenderVisibilityTest {
    @Test
    fun `visibility is conservative and preserves paint order within each object layer`() {
        // Arrange
        val visibleFirst = graphic("visible-first", .25)
        val hidden = graphic("hidden", 2.0)
        val edgeDrawing = OperationDrawingObject(
            id = id("edge"),
            drawing = OperationRectangleDrawing(
                start = OperationDrawingPoint(.95, .45),
                end = OperationDrawingPoint(1.15, .55),
                colour = OperationArgb(0xFFFFFFFF.toInt()),
                rotationDegrees = 30.0,
            ),
        )
        val visibleLast = graphic("visible-last", .75)
        val unit = OperationUnit(id("unit"), OperationUnitTemplateId(UUID.randomUUID()), "Unit", false, null, MapCoordinate(1.06, .5), .01, .01)
        val content = OperationContent(objects = listOf(visibleFirst, hidden, edgeDrawing, visibleLast, unit))

        // Act
        val visibility = OperationRenderVisibility.resolve(content, projection, MapViewerScreenBounds(0.0, 0.0, 100.0, 100.0), screenPadding = 8.0)

        // Assert
        assertThat(visibility.drawings.map { it.id }, equalTo(listOf(edgeDrawing.id)))
        assertThat(visibility.graphics.map { it.id }, equalTo(listOf(visibleFirst.id, visibleLast.id)))
        assertThat(visibility.units.map { it.id }, equalTo(listOf(unit.id)))
        assertThat(content.objectAtScreen(ViewerPoint(200.0, 50.0), projection, drawingTolerance = .01)?.id, equalTo(hidden.id))
    }

    @Test
    fun `screen bounds retain crossing overlay primitives whose origins are off screen`() {
        // Arrange
        val viewport = MapViewerScreenBounds(0.0, 0.0, 100.0, 100.0)

        // Act / Assert
        assertThat(viewport.intersectsSegment(ViewerPoint(-50.0, 50.0), ViewerPoint(150.0, 50.0)), equalTo(true))
        assertThat(viewport.intersectsCircle(ViewerPoint(-20.0, 50.0), 25.0), equalTo(true))
        assertThat(viewport.intersectsCircle(ViewerPoint(-20.0, 50.0), 10.0), equalTo(false))
    }

    private fun graphic(name: String, x: Double) = OperationGraphic(
        id(name), OperationGraphicTemplateId(UUID.randomUUID()), name, MapCoordinate(x, .5), .1, .1,
    )

    private fun id(value: String) = OperationObjectId(UUID.nameUUIDFromBytes(value.toByteArray()))

    private val projection = object : OperationSelectionProjection {
        override val aspectRatio = 1.0
        override fun toScreen(point: MapCoordinate) = ViewerPoint(point.x * 100.0, point.y * 100.0)
        override fun toMap(point: ViewerPoint) = MapCoordinate(point.x / 100.0, point.y / 100.0)
        override fun unitScreenBounds(unit: OperationUnit) = MapSelectionScreenBounds(
            unit.coordinate.x * 100.0 - 4.0,
            unit.coordinate.y * 100.0 - 4.0,
            unit.coordinate.x * 100.0 + 4.0,
            unit.coordinate.y * 100.0 + 4.0,
        )
    }
}
