package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class MapDrawingOutlineBuilderTest {

    @Test
    fun `circle outline remains circular on a non square map`() {
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
        val drawing = CircleDrawing(
            center = MapDrawingPoint(0.5, 0.5),
            radius = 0.1,
            color = java.awt.Color.RED,
        )

        val segments = MapDrawingOutlineBuilder.build(drawing, zoom = 1.0, coordinateSpace = coordinateSpace)
        val centerWorld = coordinateSpace.toWorld(MapCoordinate(0.5, 0.5))
        val expectedRadius = coordinateSpace.circleRadiusPixels(0.1)

        segments.flatMap { listOf(it.start, it.end) }
            .forEach { point ->
                val worldPoint = coordinateSpace.toWorld(MapCoordinate(point.x, point.y))
                assertThat(
                    hypot(worldPoint.x - centerWorld.x, worldPoint.y - centerWorld.y),
                    closeTo(expectedRadius, 0.5),
                )
            }
    }
}
