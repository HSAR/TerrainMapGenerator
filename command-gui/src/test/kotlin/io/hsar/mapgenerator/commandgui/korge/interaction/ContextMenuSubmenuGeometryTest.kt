package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ContextMenuSubmenuGeometryTest {
    @Test
    fun `submenu hold bounds bridge the travel gap between parent row and submenu`() {
        val holdBounds = contextMenuSubmenuHoldBounds(
            parentBounds = ScreenBounds(left = 20.0, top = 40.0, width = 160.0, height = 140.0),
            parentIndex = 1,
            submenuBounds = ScreenBounds(left = 180.0, top = 84.0, width = 160.0, height = 88.0),
        )

        assertThat(holdBounds.contains(Vector2D(179.0, 92.0)), equalTo(true))
        assertThat(holdBounds.contains(Vector2D(215.0, 92.0)), equalTo(true))
        assertThat(holdBounds.contains(Vector2D(12.0, 12.0)), equalTo(false))
    }
}
