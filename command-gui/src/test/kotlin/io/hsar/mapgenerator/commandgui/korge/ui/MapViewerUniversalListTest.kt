package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
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

    @Test
    fun `collapse state round trips through generic section keys`() {
        val draft = MapViewerUniversalList.collapseStateDraft(
            collapsedSections = setOf("friend", "unaffiliated"),
            sectionKey = { it },
        )

        val restored = MapViewerUniversalList.restoreCollapsedSections(draft) { key ->
            key.takeIf { it in setOf("friend", "unaffiliated") }
        }

        assertThat(restored, equalTo(setOf("friend", "unaffiliated")))
    }

    @Test
    fun `grouped sections leave extra space before following ungrouped items`() {
        val height = MapViewerUniversalList.unitAffiliationGroupsHeight(
            items = listOf(UnitAffiliation.Friend, UnitAffiliation.Friend, UnitAffiliation.Hostile),
            affiliationOf = { it },
            collapsedGroups = emptySet(),
            rowHeight = 10.0,
            rowGap = 1.0,
            headerHeight = 5.0,
            headerGap = 2.0,
        )

        assertThat(height, equalTo(48.0))
    }

    @Test
    fun `generic ordering moves an item to the final position`() {
        val items = listOf("first", "second", "third")

        val reordered = MapViewerUniversalList.moveWithinCategory(items, "first", { Unit }, 3)

        assertThat(reordered, equalTo(listOf("second", "third", "first")))
    }
}
