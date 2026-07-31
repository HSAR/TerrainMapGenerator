package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerReplayObjectTest {

    @Test
    fun `graphic replay object exposes a trimmed display name for labels and headers`() {
        val graphic = MapViewerReplayObject(
            id = UUID.randomUUID(),
            kind = MapViewerReplayObjectKind.GRAPHIC,
            name = "Template name",
            customProperties = mapOf("displayName" to "  Objective Alpha  "),
        )

        assertThat(graphic.displayName, equalTo("Objective Alpha"))
    }
}
