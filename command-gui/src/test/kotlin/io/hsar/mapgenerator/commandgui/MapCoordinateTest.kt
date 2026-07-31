package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapCoordinateTest {

    @Test
    fun `converts a normalized coordinate to shared world pixels`() {
        val worldPoint = MapCoordinate(0.25, 0.75).toWorldPoint(
            mapOffsetX = 30.0,
            mapOffsetY = 40.0,
            mapWidth = 400,
            mapHeight = 200,
        )

        assertThat(worldPoint, equalTo(ViewerPoint(130.0, 190.0)))
    }

    @Test
    fun `clamps a normalized coordinate to its bounds`() {
        val coordinate = MapCoordinateBounds(0.0, 0.0, 1.0, 1.0)
            .clamp(MapCoordinate(-0.5, 1.5))

        assertThat(coordinate, equalTo(MapCoordinate(0.0, 1.0)))
    }
}
