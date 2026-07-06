package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerNotesPanelLayoutBuilderTest {
    @Test
    fun `notes toggle sits to the right of game toggle`() {
        val game = MapViewerLedgerPanelLayoutBuilder.build(
            viewportWidth = 1280,
            viewportHeight = 720,
            collapsed = true,
        )
        val notes = MapViewerNotesPanelLayoutBuilder.build(
            viewportWidth = 1280,
            viewportHeight = 720,
            collapsed = true,
        )

        assertThat(notes.toggleBounds.left, equalTo(game.toggleBounds.right + MapViewerNotesPanelLayoutBuilder.TOGGLE_GAP))
        assertThat(notes.toggleBounds.top, equalTo(game.toggleBounds.top))
        assertThat(notes.toggleBounds.width, equalTo(game.toggleBounds.width))
        assertThat(notes.toggleBounds.height, equalTo(game.toggleBounds.height))
    }
}
