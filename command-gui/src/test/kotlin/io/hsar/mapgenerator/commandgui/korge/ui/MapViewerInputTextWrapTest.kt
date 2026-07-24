package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerInputTextWrapTest {
    @Test
    fun `wrap inserts line breaks when words exceed available width`() {
        val maxWidth = MapViewerTextViews.measure("Alpha beta").width + 1.0

        val wrapped = MapViewerInputTextWrap.wrap("Alpha beta gamma", maxWidth)

        assertThat(wrapped.lines().size, greaterThan(1))
        assertThat(wrapped.lines().first(), equalTo("Alpha beta"))
    }

    @Test
    fun `wrap preserves a trailing space while the next word is being typed`() {
        val maxWidth = MapViewerTextViews.measure("Alpha").width

        assertThat(MapViewerInputTextWrap.wrap("Alpha ", maxWidth), equalTo("Alpha "))
    }

    @Test
    fun `wrap preserves explicit new lines`() {
        val maxWidth = MapViewerTextViews.measure("Alpha beta").width + 1.0

        val wrapped = MapViewerInputTextWrap.wrap("Alpha beta\ngamma delta", maxWidth)

        assertThat(wrapped.lines().take(2), equalTo(listOf("Alpha beta", "gamma")))
    }

    @Test
    fun `tracked wrap identifies soft line breaks`() {
        val maxWidth = MapViewerTextViews.measure("Alpha beta").width + 1.0

        val wrapped = MapViewerInputTextWrap.wrapTracked("Alpha beta gamma", maxWidth)

        assertThat(wrapped.text, equalTo("Alpha beta\ngamma"))
        assertThat(wrapped.softBreakIndexes, equalTo(setOf("Alpha beta".length)))
        assertThat(MapViewerInputTextWrap.unwrapSoft(wrapped.text, wrapped.softBreakIndexes), equalTo("Alpha beta gamma"))
    }

    @Test
    fun `tracked text can be unwrapped and rewrapped at a wider width`() {
        val narrowWidth = MapViewerTextViews.measure("Alpha beta").width + 1.0
        val wideWidth = MapViewerTextViews.measure("Alpha beta gamma").width + 1.0

        val narrow = MapViewerInputTextWrap.wrapTracked("Alpha beta gamma", narrowWidth)
        val widened = MapViewerInputTextWrap.wrapTracked(
            MapViewerInputTextWrap.unwrapSoft(narrow.text, narrow.softBreakIndexes),
            wideWidth,
        )

        assertThat(widened.text, equalTo("Alpha beta gamma"))
        assertThat(widened.softBreakIndexes, equalTo(emptySet()))
    }

    @Test
    fun `editing a wrapped visual value preserves soft breaks outside the edit`() {
        val previous = MapViewerInputTextWrap.wrapTracked(
            "Alpha beta gamma delta",
            MapViewerTextViews.measure("Alpha beta").width + 1.0,
        )
        val currentVisual = previous.text.replace("gamma", "pasted gamma")

        assertThat(
            MapViewerInputTextWrap.logicalTextAfterEdit(
                previousVisualText = previous.text,
                currentVisualText = currentVisual,
                previousLogicalText = "Alpha beta gamma delta",
                previousSoftBreakIndexes = previous.softBreakIndexes,
            ),
            equalTo("Alpha beta pasted gamma delta"),
        )
    }

    @Test
    fun `tracked wrap preserves hard line breaks as logical cursor positions`() {
        val maxWidth = MapViewerTextViews.measure("Alpha beta").width + 1.0

        val wrapped = MapViewerInputTextWrap.wrapTracked("Alpha beta\ngamma delta", maxWidth)

        assertThat(wrapped.text, equalTo("Alpha beta\ngamma\ndelta"))
        assertThat(wrapped.softBreakIndexes, equalTo(setOf("Alpha beta\ngamma".length)))
        assertThat(
            MapViewerInputTextWrap.logicalIndexForVisualIndex(
                "Alpha beta\ngamma".length,
                wrapped.text,
                wrapped.softBreakIndexes,
            ),
            equalTo("Alpha beta\ngamma".length),
        )
    }
}
