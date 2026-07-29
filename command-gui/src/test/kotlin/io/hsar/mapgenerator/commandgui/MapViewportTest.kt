package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewportTest {
    @Test
    fun `zoom keeps the world point under the cursor fixed`() {
        // Arrange
        val viewport = MapViewport.centered(1000.0, 800.0, 500, 400)
        val cursor = ViewerPoint(125.0, 100.0)
        val worldBefore = ViewerPoint(
            viewport.left + cursor.x / viewport.zoom,
            viewport.top + cursor.y / viewport.zoom,
        )

        // Act
        val zoomed = viewport.zoomAt(cursor, 2.0)
        val worldAfter = ViewerPoint(
            zoomed.left + cursor.x / zoomed.zoom,
            zoomed.top + cursor.y / zoomed.zoom,
        )

        // Assert
        assertThat(worldAfter.x, closeTo(worldBefore.x, 0.000001))
        assertThat(worldAfter.y, closeTo(worldBefore.y, 0.000001))
    }

    @Test
    fun `pan and resize remain clamped to the world`() {
        // Arrange
        val viewport = MapViewport.centered(100.0, 80.0, 50, 40)

        // Act
        val moved = viewport.pan(ViewerPoint(10_000.0, 10_000.0))
        val oversized = moved.resized(500, 400)

        // Assert
        assertThat(moved.left, equalTo(0.0))
        assertThat(moved.top, equalTo(0.0))
        assertThat(oversized.center, equalTo(ViewerPoint(50.0, 40.0)))
    }
}
