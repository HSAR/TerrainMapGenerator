package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerTextViewsTest {
    @Test
    fun `title text is uppercased`() {
        assertThat(MapViewerTextViews.titleText("Operational Details"), equalTo("OPERATIONAL DETAILS"))
    }

    @Test
    fun `title font role defaults to regular and only bold uses strong`() {
        assertThat(MapViewerTextViews.titleFontRole(), equalTo(MapViewerFontRole.MAIN))
        assertThat(MapViewerTextViews.titleFontRole(bold = true), equalTo(MapViewerFontRole.STRONG))
    }
}
