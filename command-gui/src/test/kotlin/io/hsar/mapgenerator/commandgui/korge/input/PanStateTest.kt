package io.hsar.mapgenerator.commandgui.korge.input

import korlibs.math.geom.Vector2D
import korlibs.render.GameWindow
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class PanStateTest {

    @Test
    fun `releasing the final trigger stops dragging`() {
        val cursors = mutableListOf<GameWindow.ICursor>()
        val panner = FakePanMotion()
        val state = PanState(
            panner = panner,
            updateCursor = { cursors += it },
        )

        state.update(Vector2D(10.0, 10.0), rightHeld = true, spaceHeld = false)
        state.update(Vector2D(20.0, 20.0), rightHeld = false, spaceHeld = false)

        assertThat(panner.starts, equalTo(1))
        assertThat(panner.drags, equalTo(listOf(Vector2D(10.0, 10.0))))
        assertThat(panner.stops, equalTo(1))
        assertThat(panner.isDragging, equalTo(false))
        assertThat(cursors, equalTo(listOf(GameWindow.Cursor.MOVE, GameWindow.Cursor.DEFAULT)))
    }

    private class FakePanMotion : PanMotion {
        var starts = 0
        var drags = mutableListOf<Vector2D>()
        var stops = 0

        override var isDragging: Boolean = false
            private set

        override fun start(position: Vector2D) {
            starts += 1
            isDragging = true
        }

        override fun drag(position: Vector2D): Boolean {
            drags += position
            return true
        }

        override fun stop() {
            stops += 1
            isDragging = false
        }
    }
}
