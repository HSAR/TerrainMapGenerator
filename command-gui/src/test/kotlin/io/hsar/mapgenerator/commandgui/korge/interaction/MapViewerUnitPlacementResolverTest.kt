package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test

class MapViewerUnitPlacementResolverTest {

    @Test
    fun `resolves a screen drop point to world coordinates inside the map`() {
        val worldPoint = Vector2D(470.0, 430.0)
        val screenPoint = Vector2D(
            viewport.screenX(worldPoint.x),
            viewport.screenY(worldPoint.y),
        )

        val resolved = MapViewerUnitPlacementResolver.resolve(
            screenPoint = screenPoint,
            viewport = viewport,
            geometry = geometry,
            mapWidth = 1000,
            mapHeight = 800,
        )
        val point = requireNotNull(resolved)

        assertThat(point.x, closeTo(worldPoint.x, 0.0001))
        assertThat(point.y, closeTo(worldPoint.y, 0.0001))
    }

    @Test
    fun `rejects drops that land outside the map image`() {
        val resolved = MapViewerUnitPlacementResolver.resolve(
            screenPoint = Vector2D(2_000.0, 1_500.0),
            viewport = viewport,
            geometry = geometry,
            mapWidth = 1000,
            mapHeight = 800,
        )

        assertThat(resolved, nullValue())
    }

    @Test
    fun `clamps dragged units to the map edge`() {
        val resolved = MapViewerUnitPlacementResolver.resolveClamped(
            screenPoint = Vector2D(2_000.0, 1_500.0),
            viewport = viewport,
            geometry = geometry,
            mapWidth = 1000,
            mapHeight = 800,
        )

        assertThat(resolved.x, closeTo(1100.0, 0.0001))
        assertThat(resolved.y, closeTo(900.0, 0.0001))
    }

    private val geometry = ViewerGeometry(
        backgroundWidth = 1200,
        backgroundHeight = 1000,
        mapOffsetX = 100,
        mapOffsetY = 100,
    )

    private val viewport = CameraViewport.centeredOnMap(
        geometry = geometry,
        mapWidth = 1000,
        mapHeight = 800,
        viewportWidth = 500,
        viewportHeight = 400,
    )
}
