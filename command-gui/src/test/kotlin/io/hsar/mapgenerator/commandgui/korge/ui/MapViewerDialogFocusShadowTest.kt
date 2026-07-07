package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerDialogFocusShadowTest {
    @Test
    fun `null anchor produces no shadow polygon`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = null,
            ),
            equalTo(null),
        )
    }

    @Test
    fun `anchor to the left selects the left edge corners`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(0.0, 50.0),
            ),
            equalTo(
                listOf(
                    Vector2D(0.0, 50.0),
                    Vector2D(10.0, 20.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `anchor to the right selects the right edge corners`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(120.0, 50.0),
            ),
            equalTo(
                listOf(
                    Vector2D(120.0, 50.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(90.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `anchor above selects the top edge corners`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(50.0, 0.0),
            ),
            equalTo(
                listOf(
                    Vector2D(50.0, 0.0),
                    Vector2D(10.0, 20.0),
                    Vector2D(90.0, 20.0),
                ),
            ),
        )
    }

    @Test
    fun `anchor below selects the bottom edge corners`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(50.0, 120.0),
            ),
            equalTo(
                listOf(
                    Vector2D(50.0, 120.0),
                    Vector2D(10.0, 80.0),
                    Vector2D(90.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `diagonal anchors with opposite-sign offsets select the top-left to bottom-right diagonal`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(120.0, -40.0),
            ),
            equalTo(
                listOf(
                    Vector2D(120.0, -40.0),
                    Vector2D(10.0, 20.0),
                    Vector2D(90.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `diagonal anchors with same-sign offsets select the top-right to bottom-left diagonal`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                anchor = Vector2D(-40.0, -40.0),
            ),
            equalTo(
                listOf(
                    Vector2D(-40.0, -40.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }
}
