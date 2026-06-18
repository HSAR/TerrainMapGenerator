package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test

class MapCoordinateSpaceTest {

    @Test
    fun `resolves a screen point to normalized map coordinates inside the map`() {
        val mapCoordinate = MapCoordinate(0.37, 0.4125)
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)
        val screenPoint = Vector2D(
            viewport.screenX(worldPoint.x),
            viewport.screenY(worldPoint.y),
        )

        val resolved = coordinateSpace.resolveScreenPoint(screenPoint, viewport, coordinateSpace.mapBounds)
        val point = requireNotNull(resolved)

        assertThat(point.x, closeTo(mapCoordinate.x, 0.0001))
        assertThat(point.y, closeTo(mapCoordinate.y, 0.0001))
    }

    @Test
    fun `rejects points that land outside the map when map bounds are required`() {
        val resolved = coordinateSpace.resolveScreenPoint(
            screenPoint = Vector2D(2_000.0, 1_500.0),
            viewport = viewport,
            bounds = coordinateSpace.mapBounds,
        )

        assertThat(resolved, nullValue())
    }

    @Test
    fun `clamps points to the background bounds when requested`() {
        val resolved = coordinateSpace.resolveScreenPointClamped(
            screenPoint = Vector2D(-1_000.0, -1_000.0),
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        )

        assertThat(resolved.x, closeTo(-0.1, 0.0001))
        assertThat(resolved.y, closeTo(-0.125, 0.0001))
    }

    @Test
    fun `converts normalized coordinates back into local map pixels`() {
        val pixels = coordinateSpace.toMapPixels(MapCoordinate(0.25, 0.75))

        assertThat(pixels.x, equalTo(250.0))
        assertThat(pixels.y, equalTo(600.0))
    }

    private val geometry = ViewerGeometry(
        backgroundWidth = 1200,
        backgroundHeight = 1000,
        mapOffsetX = 100,
        mapOffsetY = 100,
    )

    private val coordinateSpace = MapCoordinateSpace(
        geometry = geometry,
        mapWidth = 1000,
        mapHeight = 800,
    )

    private val viewport = CameraViewport.centeredOnMap(
        geometry = geometry,
        mapWidth = 1000,
        mapHeight = 800,
        viewportWidth = 500,
        viewportHeight = 400,
    )
}
