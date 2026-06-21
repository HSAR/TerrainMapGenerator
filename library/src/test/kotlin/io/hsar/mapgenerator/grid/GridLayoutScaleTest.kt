package io.hsar.mapgenerator.grid

import io.hsar.mapgenerator.graph.Point
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class GridLayoutScaleTest {

    @Test
    fun `baseline dimensions keep the original scale`() {
        assertThat(GridLayoutScale.labelScale(2560, 1440), closeTo(1.0, 0.0001))
        assertThat(GridLayoutScale.fontScale(2560, 1440), closeTo(1.0, 0.0001))
        assertThat(GridLayoutScale.mapIdPadding(2560, 1440), equalTo(Point(60.0, 40.0)))
        assertThat(GridLayoutScale.gridLinePadding(2560, 1440), equalTo(Point(60.0, 40.0)))
    }

    @Test
    fun `smaller dimensions shrink more gently than linear scaling`() {
        val eased = GridLayoutScale.labelScale(1280, 720)

        assertThat(eased, closeTo(sqrt(0.5), 0.0001))
        assertThat(eased, closeTo(0.70710678, 0.0001))
    }

    @Test
    fun `larger dimensions grow more gently than linear scaling`() {
        val eased = GridLayoutScale.labelScale(3840, 2160)

        assertThat(eased, closeTo(sqrt(1.5), 0.0001))
        assertThat(eased, closeTo(1.22474487, 0.0001))
    }
}
