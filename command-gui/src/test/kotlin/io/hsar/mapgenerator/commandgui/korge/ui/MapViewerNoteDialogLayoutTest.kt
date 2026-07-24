package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerNoteDialogLayoutTest {

    @Test
    fun `body uses the remaining viewport height`() {
        assertThat(
            MapViewerNoteDialogLayout.bodyHeight(contentViewportHeight = 620.0, bodyTop = 180.0),
            equalTo(412.0),
        )
    }

    @Test
    fun `body keeps its minimum height when the viewport is small`() {
        assertThat(
            MapViewerNoteDialogLayout.bodyHeight(contentViewportHeight = 200.0, bodyTop = 180.0),
            equalTo(MapViewerNoteDialogLayout.MIN_BODY_HEIGHT),
        )
    }
}
