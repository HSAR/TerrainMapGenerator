package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test

class MapViewerUniversalListTest {

    @Test
    fun `sections collapse from the end by default until content fits`() {
        val collapsed = mutableSetOf<String>()

        MapViewerUniversalList.collapseSectionsToFit(
            sections = listOf("first", "second", "third"),
            collapsedSections = collapsed,
            availableHeight = 1.0,
            contentHeight = { 3.0 - collapsed.size },
        )

        assertThat(collapsed, contains("third", "second"))
    }

    @Test
    fun `custom section order takes precedence when fitting content`() {
        val collapsed = mutableSetOf<String>()

        MapViewerUniversalList.collapseSectionsToFit(
            sections = listOf("friend", "hostile", "neutral", "unaffiliated"),
            collapsedSections = collapsed,
            availableHeight = 2.0,
            contentHeight = { 4.0 - collapsed.size },
            collapseOrder = listOf("unaffiliated", "neutral", "friend", "hostile"),
        )

        assertThat(collapsed, contains("unaffiliated", "neutral"))
    }

    @Test
    fun `all sections collapse when their headers still do not fit`() {
        val collapsed = mutableSetOf<String>()

        MapViewerUniversalList.collapseSectionsToFit(
            sections = listOf("first", "second"),
            collapsedSections = collapsed,
            availableHeight = 0.0,
            contentHeight = { 10.0 },
        )

        assertThat(collapsed, contains("second", "first"))
    }
}
