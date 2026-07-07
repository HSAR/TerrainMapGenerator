package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

class MapViewerDialogFocusShadowTest {
    @Test
    fun `null anchor produces no shadow shape`() {
        assertThat(
            focusShadowShape(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = null,
            ),
            equalTo(null),
        )
    }

    @Test
    fun `unit bounds to the left use the six convex hull corners`() {
        assertThat(
            focusShadowHullPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = -60.0, top = 35.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(-60.0, 35.0),
                    Vector2D(10.0, 20.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(90.0, 80.0),
                    Vector2D(10.0, 80.0),
                    Vector2D(-60.0, 55.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds to the right use the six convex hull corners`() {
        assertThat(
            focusShadowHullPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 120.0, top = 35.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(140.0, 35.0),
                    Vector2D(140.0, 55.0),
                    Vector2D(90.0, 80.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds above use the six convex hull corners`() {
        assertThat(
            focusShadowHullPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 30.0, top = -60.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(30.0, -60.0),
                    Vector2D(50.0, -60.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(90.0, 80.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `unit bounds below use the six convex hull corners`() {
        assertThat(
            focusShadowHullPolygon(
                bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
                targetBounds = ScreenBounds(left = 30.0, top = 120.0, width = 20.0, height = 20.0),
            ),
            equalTo(
                listOf(
                    Vector2D(10.0, 20.0),
                    Vector2D(90.0, 20.0),
                    Vector2D(90.0, 80.0),
                    Vector2D(50.0, 140.0),
                    Vector2D(30.0, 140.0),
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `diagonal unit bounds use the six convex hull corners`() {
        assertThat(
            focusShadowHullPolygon(
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
                    Vector2D(10.0, 80.0),
                ),
            ),
        )
    }

    @Test
    fun `subtracted shadow shape keeps area after removing dialog and target boxes`() {
        val shadow = focusShadowShape(
            bounds = MapViewerDialogBounds(left = 10.0, top = 20.0, width = 80.0, height = 60.0),
            targetBounds = ScreenBounds(left = 30.0, top = -60.0, width = 20.0, height = 20.0),
        )

        assertThat(shadow, notNullValue())
        assertThat(shadow!!.area, greaterThan(0.0))
        assertThat(shadow.containsPoint(Vector2D(40.0, 40.0)), equalTo(false))
        assertThat(shadow.containsPoint(Vector2D(40.0, -50.0)), equalTo(false))
    }
}
