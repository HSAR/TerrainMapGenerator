package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapPointerIntentResolverTest {

    @Test
    fun `blocked pointer suppresses map reference and fresh pan start`() {
        val intent = MapPointerIntentResolver.resolve(
            pointerBlocked = true,
            dragging = false,
            leftHeld = true,
            rightHeld = true,
            spaceHeld = true,
        )

        assertThat(intent.leftActive, equalTo(false))
        assertThat(intent.rightPanHeld, equalTo(false))
        assertThat(intent.spacePanHeld, equalTo(false))
        assertThat(intent.allowZoom, equalTo(false))
    }

    @Test
    fun `blocked pointer still allows an in-progress pan to continue`() {
        val intent = MapPointerIntentResolver.resolve(
            pointerBlocked = true,
            dragging = true,
            leftHeld = false,
            rightHeld = true,
            spaceHeld = true,
        )

        assertThat(intent.rightPanHeld, equalTo(true))
        assertThat(intent.spacePanHeld, equalTo(true))
        assertThat(intent.allowZoom, equalTo(false))
    }
}
