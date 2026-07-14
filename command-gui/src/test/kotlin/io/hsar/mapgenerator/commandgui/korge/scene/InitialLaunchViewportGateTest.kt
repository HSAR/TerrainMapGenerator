package io.hsar.mapgenerator.commandgui.korge.scene

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class InitialLaunchViewportGateTest {
    @Test
    fun `releases launch when maximising resizes the viewport`() {
        val gate = InitialLaunchViewportGate()

        gate.onViewportResized()

        assertThat(gate.releaseInitialLaunch(), equalTo(true))
        assertThat(gate.releaseInitialLaunch(), equalTo(false))
    }

    @Test
    fun `falls back after two frames when no resize event arrives`() {
        val gate = InitialLaunchViewportGate()

        assertThat(gate.releaseInitialLaunch(), equalTo(false))
        assertThat(gate.releaseInitialLaunch(), equalTo(true))
    }
}
