package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.ImageBitmap
import io.hsar.mapgenerator.commandgui.MapViewerLaunchTileCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ComposeLaunchBackgroundStateTest {
    @Test
    fun `initial generation uses the final observed viewport`() {
        val state = state()

        state.updateViewport(800, 600)
        state.updateViewport(1920, 1080)
        state.ensureStarted(1920, 1080)

        assertThat(state.spec?.textureWidth, equalTo(2880))
        assertThat(state.spec?.textureHeight, equalTo(1620))
    }

    @Test
    fun `later resize preserves generated extent and tiles while regeneration uses current viewport`() {
        val state = state()
        val coordinate = MapViewerLaunchTileCoordinate(0, 0)

        state.ensureStarted(1280, 720)
        state.tiles[coordinate] = ComposeLaunchTileVisual(coordinate)
        state.ensureStarted(1920, 1080)

        assertThat(state.spec?.textureWidth, equalTo(1920))
        assertThat(state.tiles.keys.toSet(), equalTo(setOf(coordinate)))

        state.regenerate(91L)

        assertThat(state.spec?.textureWidth, equalTo(2880))
        assertThat(state.spec?.textureHeight, equalTo(1620))
        assertThat(state.tiles.isEmpty(), equalTo(true))
        assertThat(state.outgoing?.tiles?.map(ComposeLaunchTileVisual::coordinate), equalTo(listOf(coordinate)))
    }

    @Test
    fun `completed refinement releases its preview bitmap`() {
        val state = state()
        val coordinate = MapViewerLaunchTileCoordinate(0, 0)
        state.tiles[coordinate] = ComposeLaunchTileVisual(
            coordinate = coordinate,
            preview = ImageBitmap(1, 1),
            refinement = ImageBitmap(1, 1),
            refinementArrivedAtNanos = 0L,
        )

        state.advanceAnimations(281_000_000L)

        assertThat(state.tiles.getValue(coordinate).preview, equalTo(null))
        assertThat(state.tiles.getValue(coordinate).refinement != null, equalTo(true))
    }

    private fun state() = ComposeLaunchBackgroundState(73L, CoroutineScope(Job().apply { cancel() }))
}
