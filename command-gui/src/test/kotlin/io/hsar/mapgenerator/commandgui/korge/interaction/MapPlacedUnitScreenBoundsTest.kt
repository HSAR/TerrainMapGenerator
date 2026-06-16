package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapPlacedUnitScreenBoundsTest {

    @Test
    fun `unit screen bounds keep the same size when zoom changes`() {
        val unit = MapPlacedUnitSnapshot(
            mapPoint = Vector2D(500.0, 300.0),
            screenWidth = 64.0,
            screenHeight = 32.0,
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

        val outBounds = MapPlacedUnitScreenBounds.boundsFor(unit, zoomedOut)
        val inBounds = MapPlacedUnitScreenBounds.boundsFor(unit, zoomedIn)

        assertThat(outBounds.width, equalTo(64.0))
        assertThat(outBounds.height, equalTo(32.0))
        assertThat(inBounds.width, equalTo(64.0))
        assertThat(inBounds.height, equalTo(32.0))
    }

    @Test
    fun `unit hit test follows the unit on screen`() {
        val unit = MapPlacedUnitSnapshot(
            mapPoint = Vector2D(500.0, 300.0),
            screenWidth = 64.0,
            screenHeight = 32.0,
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

        assertThat(MapPlacedUnitScreenBounds.contains(Vector2D(400.0, 300.0), unit, viewport), equalTo(true))
        assertThat(MapPlacedUnitScreenBounds.contains(Vector2D(100.0, 100.0), unit, viewport), equalTo(false))
    }
}
