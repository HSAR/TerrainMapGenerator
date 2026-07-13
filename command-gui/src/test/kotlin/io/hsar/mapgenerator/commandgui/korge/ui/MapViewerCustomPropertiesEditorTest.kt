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

    @Test
    fun `properties include inherited defaults and allow instance values to replace them`() {
        val editor = MapViewerCustomPropertiesEditor(
            initialProperties = mapOf("callsign" to "R1"),
            inheritedProperties = mapOf("callsign" to "Default", "morale" to "steady"),
        )

        assertThat(editor.properties(), equalTo(mapOf("callsign" to "R1", "morale" to "steady")))
    }

    @Test
    fun `editor is valid when initial reserved keys are filtered out`() {
        val editor = MapViewerCustomPropertiesEditor(
            initialProperties = mapOf("instanceName" to "reserved"),
            reservedKeys = setOf("instanceName"),
        )

        assertThat(editor.isValid, equalTo(true))
    }

    @Test
    fun `shared text input validation trims required text and reserved keys`() {
        assertThat(MapViewerTextInputValidation.isPresent(" Name "), equalTo(true))
        assertThat(MapViewerTextInputValidation.isPresent(" "), equalTo(false))
        assertThat(MapViewerTextInputValidation.isReservedKey(" instanceName ", setOf("instanceName")), equalTo(true))
    }

    @Test
    fun `trailing actions begin after the shared add property button`() {
        val editor = MapViewerCustomPropertiesEditor(emptyMap())
        var actionLeft = 0.0
        var actionTop = 0.0

        editor.renderBody(korlibs.korge.view.Container(), 400.0) { _, left, top ->
            actionLeft = left
            actionTop = top
        }

        assertThat(actionLeft, equalTo(MapViewerCustomPropertiesEditor.ACTION_BUTTON_WIDTH + MapViewerDialogSpacing.FIELD_GAP))
        assertThat(actionTop, equalTo(MapViewerDialogSpacing.LABEL_TO_FIELD_GAP))
        assertThat(MapViewerCustomPropertiesEditor.ACTION_BUTTON_HEIGHT, equalTo(32.0))
    }
}
