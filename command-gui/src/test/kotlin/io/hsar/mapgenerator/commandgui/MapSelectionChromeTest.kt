package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapSelectionChromeTest {

    @Test
    fun `selection frame exposes four grabbable corner brackets and a rotate handle`() {
        val frame = MapSelectionScreenBounds(10.0, 20.0, 110.0, 120.0).frame()

        assertThat(frame.cornerBrackets().map(MapSelectionCornerBracket::corner), containsInAnyOrder(*MapSelectionCorner.entries.toTypedArray()))
        assertThat(frame.rotationHandle(), equalTo(ViewerPoint(126.0, 4.0)))
    }

    @Test
    fun `shift selection toggles objects while ordinary selection replaces it`() {
        assertThat(setOf("one").withSelection("two", additive = false), equalTo(setOf("two")))
        assertThat(setOf("one").withSelection("two", additive = true), equalTo(setOf("one", "two")))
        assertThat(setOf("one", "two").withSelection("two", additive = true), equalTo(setOf("one")))
    }

    @Test
    fun `shift marquee toggles every intersecting object`() {
        assertThat(setOf("one").withBoxSelection(setOf("one", "two"), additive = true), equalTo(setOf("two")))
    }
}
