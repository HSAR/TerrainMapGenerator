package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUniversalList
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerUnitTypeOrderingTest {
    @Test
    fun `reordering keeps other categories in place`() {
        val items = listOf(
            TestUnit("friend-1", "friend"),
            TestUnit("hostile-1", "hostile"),
            TestUnit("friend-2", "friend"),
            TestUnit("friend-3", "friend"),
        )

        val reordered = MapViewerUniversalList.moveWithinCategory(
            items = items,
            source = items[0],
            categoryOf = TestUnit::category,
            targetPosition = 2,
        )

        assertThat(reordered.map(TestUnit::id), equalTo(listOf("friend-2", "hostile-1", "friend-3", "friend-1")))
    }

    @Test
    fun `reordering a row to its current position is a no-op`() {
        val items = listOf(TestUnit("friend-1", "friend"), TestUnit("friend-2", "friend"))

        assertThat(
            MapViewerUniversalList.moveWithinCategory(items, items[0], TestUnit::category, 0),
            equalTo(items),
        )
    }

    private data class TestUnit(val id: String, val category: String)
}
