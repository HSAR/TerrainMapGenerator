package io.hsar.mapgenerator.commandgui.korge.launch

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerAsyncRevisionTest {
    @Test
    fun `rejects a preview result superseded by a newer request`() {
        val revisions = MapViewerAsyncRevision()
        val stale = revisions.next()
        val current = revisions.next()

        assertThat(revisions.accepts(stale), equalTo(false))
        assertThat(revisions.accepts(current), equalTo(true))
    }
}
