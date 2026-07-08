package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

class ContextMenuSurfaceGeometryTest {
    @Test
    fun `unioned context menu surface keeps the submenu seam out of the border`() {
        val geometry = contextMenuSurfaceGeometry(
            panels = listOf(
                ScreenBounds(left = 10.0, top = 10.0, width = 100.0, height = 80.0),
                ScreenBounds(left = 110.0, top = 30.0, width = 80.0, height = 50.0),
            ),
        )

        requireNotNull(geometry)

        assertThat(
            geometry.fillArea.contains(Point2D.Double(110.5, 40.0)),
            equalTo(true),
        )
        assertThat(
            geometry.borderArea.contains(Point2D.Double(110.5, 40.0)),
            equalTo(false),
        )
        assertThat(
            geometry.borderArea.contains(Point2D.Double(10.5, 10.5)),
            equalTo(true),
        )
    }
}
