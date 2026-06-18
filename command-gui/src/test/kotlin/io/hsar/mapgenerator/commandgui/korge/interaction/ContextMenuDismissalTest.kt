package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ContextMenuDismissalTest {

    @Test
    fun `keeps the menu open when clicking inside the menu bounds`() {
        val shouldDismiss = ContextMenuDismissal.shouldDismiss(
            bounds = ScreenBounds(left = 100.0, top = 120.0, width = 180.0, height = 80.0),
            pointer = Vector2D(140.0, 150.0),
        )

        assertThat(shouldDismiss, equalTo(false))
    }

    @Test
    fun `dismisses the menu when clicking outside the menu bounds`() {
        val shouldDismiss = ContextMenuDismissal.shouldDismiss(
            bounds = ScreenBounds(left = 100.0, top = 120.0, width = 180.0, height = 80.0),
            pointer = Vector2D(40.0, 60.0),
        )

        assertThat(shouldDismiss, equalTo(true))
    }

    @Test
    fun `menu action execution always dismisses and notifies afterwards`() {
        val events = mutableListOf<String>()

        ContextMenuActionExecutor.execute(
            action = { events += "action" },
            dismiss = { events += "dismiss" },
            onActionSelected = { events += "notify" },
        )

        assertThat(events, equalTo(listOf("action", "dismiss", "notify")))
    }
}
