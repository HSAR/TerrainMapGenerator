package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class MapDrawingOutlineBuilderTest {

    @Test
    fun `rotated square rectangle remains square on a non square map`() {
        val coordinateSpace = MapCoordinateSpace(
            geometry = ViewerGeometry(
                backgroundWidth = 4000,
                backgroundHeight = 2000,
                mapOffsetX = 0,
                mapOffsetY = 0,
            ),
            mapWidth = 2000,
            mapHeight = 1000,
        )
        val drawing = RectangleDrawing(
            start = MapDrawingPoint(0.45, 0.4),
            end = MapDrawingPoint(0.55, 0.6),
            rotationDegrees = 35.0,
            color = java.awt.Color.RED,
        )

        val sideLengths = MapDrawingOutlineBuilder.build(drawing, zoom = 1.0, coordinateSpace = coordinateSpace)
            .map { segment ->
                val start = coordinateSpace.toWorld(MapCoordinate(segment.start.x, segment.start.y))
                val end = coordinateSpace.toWorld(MapCoordinate(segment.end.x, segment.end.y))
                hypot(end.x - start.x, end.y - start.y)
            }

        sideLengths.forEach { assertThat(it, closeTo(200.0, 0.0001)) }
    }

    @Test
    fun `square oval outline remains circular on a non square map`() {
        val coordinateSpace = MapCoordinateSpace(
            geometry = ViewerGeometry(
                backgroundWidth = 4000,
                backgroundHeight = 2000,
                mapOffsetX = 0,
                mapOffsetY = 0,
            ),
            mapWidth = 2000,
            mapHeight = 1000,
        )
        val drawing = OvalDrawing(
            center = MapDrawingPoint(0.5, 0.5),
            xRadius = 0.05,
            yRadius = 0.1,
            rotationDegrees = 35.0,
            color = java.awt.Color.RED,
        )

        val segments = MapDrawingOutlineBuilder.build(drawing, zoom = 1.0, coordinateSpace = coordinateSpace)
        val centerWorld = coordinateSpace.toWorld(MapCoordinate(0.5, 0.5))
        val expectedRadius = 100.0

        segments.flatMap { listOf(it.start, it.end) }
            .forEach { point ->
                val worldPoint = coordinateSpace.toWorld(MapCoordinate(point.x, point.y))
                assertThat(
                    hypot(worldPoint.x - centerWorld.x, worldPoint.y - centerWorld.y),
                    closeTo(expectedRadius, 0.5),
                )
            }
    }

    @Test
    fun `rotated oval outline follows the parametric ellipse equation`() {
        val center = MapDrawingPoint(0.5, 0.4)
        val xRadius = 0.2
        val yRadius = 0.1
        val rotationDegrees = 35.0

        MapDrawingGeometry.ovalPoints(
            center = center,
            xRadius = xRadius,
            yRadius = yRadius,
            rotationDegrees = rotationDegrees,
            segmentCount = 48,
            aspectRatio = 2.0,
        ).forEach { point ->
            val unrotated = point.rotateAround(center, -rotationDegrees, aspectRatio = 2.0)
            val ellipseValue = (((unrotated.x - center.x) * 2.0) / (xRadius * 2.0)).let { it * it } +
                    ((unrotated.y - center.y) / yRadius).let { it * it }

            assertThat(ellipseValue, closeTo(1.0, 0.000001))
        }
    }

    private fun MapDrawingPoint.rotateAround(
        pivot: MapDrawingPoint,
        rotationDegrees: Double,
        aspectRatio: Double,
    ): MapDrawingPoint {
        val radians = Math.toRadians(rotationDegrees)
        val dx = (x - pivot.x) * aspectRatio
        val dy = y - pivot.y
        return MapDrawingPoint(
            x = pivot.x + (dx * cos(radians) - dy * sin(radians)) / aspectRatio,
            y = pivot.y + dx * sin(radians) + dy * cos(radians),
        )
    }
}
