package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerCustomPropertiesEditorTest {
    @Test
    fun `properties trims keys and excludes blank or reserved keys`() {
        val editor = MapViewerCustomPropertiesEditor(
            initialProperties = mapOf(
                " callsign " to "R1",
                "instanceName" to "shadowed",
                " " to "blank",
            ),
            reservedKeys = setOf("instanceName"),
        )

        assertThat(editor.properties(), equalTo(mapOf("callsign" to "R1")))
    }

    @Test
    fun `height includes one row for each initial property`() {
        val empty = MapViewerCustomPropertiesEditor(emptyMap())
        val populated = MapViewerCustomPropertiesEditor(mapOf("a" to "1", "b" to "2"))

        assertThat(populated.height() > empty.height(), equalTo(true))
    }
}
