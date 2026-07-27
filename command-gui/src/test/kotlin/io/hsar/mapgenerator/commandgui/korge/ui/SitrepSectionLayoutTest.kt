package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class SitrepSectionLayoutTest {
    @Test
    fun `section height equals its header and rendered rows`() {
        val headerHeight = 34.0
        val headerToRowsGap = 8.0
        val rowHeight = 34.0
        val rowGap = 8.0
        val emptyRowTopInset = 2.0
        val rowCount = 5

        val sectionHeight = SitrepSectionLayout.sectionHeight(
            rowCount,
            headerHeight,
            headerToRowsGap,
            rowHeight,
            rowGap,
            emptyRowTopInset,
        )

        assertThat(
            sectionHeight,
            equalTo(
                headerHeight + headerToRowsGap + SitrepSectionLayout.rowsHeight(
                    rowCount,
                    rowHeight,
                    rowGap,
                    emptyRowTopInset,
                ),
            ),
        )
    }

    @Test
    fun `empty section includes its inset row height once`() {
        assertThat(
            SitrepSectionLayout.rowsHeight(0, rowHeight = 34.0, rowGap = 8.0, emptyRowTopInset = 2.0),
            equalTo(36.0),
        )
    }
}
