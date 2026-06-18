package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapPlacedUnitScreenBoundsTest {

    @Test
    fun `unit screen bounds grow with zoom`() {
        val unit = MapPlacedUnitSnapshot(
            mapCoordinate = MapCoordinate(0.5, 0.375),
            normalizedWidth = 0.064,
            normalizedHeight = 0.04,
        )
        val zoomedOut = CameraViewport(
            centerX = 500.0,
            centerY = 300.0,
            zoom = 1.0,
            viewportWidth = 800,
            viewportHeight = 600,
            worldWidth = 2000,
            worldHeight = 2000,
        )
        val zoomedIn = zoomedOut.copy(zoom = 4.0)
        val coordinateSpace = MapCoordinateSpace(geometry, mapWidth = 1000, mapHeight = 800)

        val outBounds = MapPlacedUnitScreenBounds.boundsFor(unit, zoomedOut, coordinateSpace)
        val inBounds = MapPlacedUnitScreenBounds.boundsFor(unit, zoomedIn, coordinateSpace)

        assertThat(outBounds.width, equalTo(64.0))
        assertThat(outBounds.height, equalTo(32.0))
        assertThat(inBounds.width, equalTo(256.0))
        assertThat(inBounds.height, equalTo(128.0))
    }

    @Test
    fun `unit hit test follows the unit on screen`() {
        val unit = MapPlacedUnitSnapshot(
            mapCoordinate = MapCoordinate(0.5, 0.375),
            normalizedWidth = 0.064,
            normalizedHeight = 0.04,
        )
        val viewport = CameraViewport(
            centerX = 500.0,
            centerY = 300.0,
            zoom = 2.0,
            viewportWidth = 800,
            viewportHeight = 600,
            worldWidth = 2000,
            worldHeight = 2000,
        )
        val coordinateSpace = MapCoordinateSpace(geometry, mapWidth = 1000, mapHeight = 800)

        assertThat(
            MapPlacedUnitScreenBounds.contains(korlibs.math.geom.Vector2D(400.0, 300.0), unit, viewport, coordinateSpace),
            equalTo(true),
        )
        assertThat(
            MapPlacedUnitScreenBounds.contains(korlibs.math.geom.Vector2D(100.0, 100.0), unit, viewport, coordinateSpace),
            equalTo(false),
        )
    }

    private val geometry = io.hsar.mapgenerator.commandgui.ViewerGeometry(
        backgroundWidth = 2000,
        backgroundHeight = 2000,
        mapOffsetX = 0,
        mapOffsetY = 0,
    )
}
