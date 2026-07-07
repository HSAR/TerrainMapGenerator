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
                targetBounds = null,
            ),
            equalTo(null),
        )
    }

    @Test
    fun `unit bounds to the left select the cutout polygon`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = -60.0, top = 35.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(-60.0, 35.0),
                    Vector2D(-40.0, 35.0),
                    Vector2D(-40.0, 55.0),
                    Vector2D(-60.0, 55.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds to the left but not vertically contained still use the diagonal cut`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = -60.0, top = -30.0, width = 20.0, height = 120.0),
            ),
            equalTo(
                listOf(
                    Vector2D(90.0, 20.0),
                    Vector2D(-40.0, -30.0),
                    Vector2D(-40.0, 90.0),
                    Vector2D(-60.0, 90.0),
                    Vector2D(10.0, 80.0),
                    Vector2D(10.0, 20.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds to the right select the cutout polygon`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 120.0, top = 35.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(90.0, 20.0),
                    Vector2D(140.0, 35.0),
                    Vector2D(140.0, 55.0),
                    Vector2D(120.0, 55.0),
                    Vector2D(120.0, 35.0),
                    Vector2D(90.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds above select the cutout polygon`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 30.0, top = -60.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(30.0, -40.0),
                    Vector2D(30.0, -60.0),
                    Vector2D(50.0, -60.0),
                    Vector2D(50.0, -40.0),
                    Vector2D(90.0, 20.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds below select the cutout polygon`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 30.0, top = 120.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 80.0),
                    Vector2D(30.0, 140.0),
                    Vector2D(30.0, 120.0),
                    Vector2D(50.0, 120.0),
                    Vector2D(50.0, 140.0),
                    Vector2D(90.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `diagonal unit bounds keep the cut in at the nearest corner`() {
        assertThat(
            focusShadowPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 120.0, top = -40.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(120.0, -40.0),
                    Vector2D(140.0, -40.0),
                    Vector2D(140.0, -20.0),
                    Vector2D(90.0, 80.0),
                    Vector2D(90.0, 20.0),
                ),
            ),
        )
    }
}
