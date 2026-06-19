package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CameraViewportTest {

    @Test
    fun `initial viewport centres on the map image`() {
        val viewport = CameraViewport.centeredOnMap(
            geometry = geometry,
            mapWidth = 1000,
            mapHeight = 800,
            viewportWidth = 500,
            viewportHeight = 400,
        )

        assertThat(viewport.centerX, closeTo(600.0, 0.0001))
        assertThat(viewport.centerY, closeTo(500.0, 0.0001))
        assertThat(viewport.zoom, equalTo(1.0))
    }

    @Test
    fun `fit zoom scales the map to nearly fill the viewport`() {
        val zoom = CameraViewport.fitZoomToViewport(
            mapWidth = 1000,
            mapHeight = 800,
            viewportWidth = 500,
            viewportHeight = 400,
        )

        assertThat(zoom, closeTo(0.475, 0.0001))
    }

    @Test
    fun `dragging right moves the camera left through world space`() {
        val viewport = initialViewport.pan(Vector2D(100.0, 0.0))

        assertThat(viewport.centerX, closeTo(500.0, 0.0001))
    }

    @Test
    fun `panning clamps to the padded world bounds`() {
        val viewport = initialViewport.pan(Vector2D(10_000.0, 10_000.0))

        assertThat(viewport.left, closeTo(0.0, 0.0001))
        assertThat(viewport.top, closeTo(0.0, 0.0001))
    }

    @Test
    fun `zooming in preserves the world point under the mouse`() {
        val anchor = Vector2D(125.0, 100.0)
        val worldX = initialViewport.left + anchor.x / initialViewport.zoom
        val worldY = initialViewport.top + anchor.y / initialViewport.zoom
        val zoomed = initialViewport.zoomAt(anchor, 2.0)

        assertThat(zoomed.left + anchor.x / zoomed.zoom, closeTo(worldX, 0.0001))
        assertThat(zoomed.top + anchor.y / zoomed.zoom, closeTo(worldY, 0.0001))
    }

    @Test
    fun `zooming out recentres when the visible area is larger than the world`() {
        val viewport = initialViewport.zoomAt(Vector2D(250.0, 200.0), 0.1)

        assertThat(viewport.centerX, closeTo(600.0, 0.0001))
        assertThat(viewport.centerY, closeTo(500.0, 0.0001))
    }

    private val geometry = ViewerGeometry(
        backgroundWidth = 1200,
        backgroundHeight = 1000,
        mapOffsetX = 100,
        mapOffsetY = 100,
    )

    private val initialViewport = CameraViewport.centeredOnMap(
        geometry = geometry,
        mapWidth = 1000,
        mapHeight = 800,
        viewportWidth = 500,
        viewportHeight = 400,
    )
}
