package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapToolGestureGateTest {

    @Test
    fun `left tool gesture stays blocked for a press that begins on UI`() {
        val gate = MapToolGestureGate()

        assertThat(gate.update(leftHeld = true, pointerBlocked = true).leftToolActive, equalTo(false))
        assertThat(gate.update(leftHeld = true, pointerBlocked = false).leftToolActive, equalTo(false))
        assertThat(gate.update(leftHeld = false, pointerBlocked = false).leftToolActive, equalTo(false))
    }

    @Test
    fun `left tool gesture stays active for a press that begins on the map`() {
        val gate = MapToolGestureGate()

        assertThat(gate.update(leftHeld = true, pointerBlocked = false).leftToolActive, equalTo(true))
        assertThat(gate.update(leftHeld = true, pointerBlocked = true).leftToolActive, equalTo(true))
        assertThat(gate.update(leftHeld = false, pointerBlocked = true).leftToolActive, equalTo(false))
    }

    @Test
    fun `releasing resets the gesture ownership for the next press`() {
        val gate = MapToolGestureGate()

        gate.update(leftHeld = true, pointerBlocked = true)
        gate.update(leftHeld = false, pointerBlocked = false)

        assertThat(gate.update(leftHeld = true, pointerBlocked = false).leftToolActive, equalTo(true))
    }
}
