package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerTextInputContentTest {
    @Test
    fun `rejects control codes emitted by ctrl shortcuts`() {
        assertThat(MapViewerTextInputContent.isAccepted("\u0001\u0003\u0016\u0018\u001A"), equalTo(false))
    }

    @Test
    fun `accepts printable text`() {
        assertThat(MapViewerTextInputContent.isAccepted("Alpha 123!"), equalTo(true))
    }

    @Test
    fun `accepts multiline and pasted layout whitespace`() {
        assertThat(MapViewerTextInputContent.isAccepted("Alpha\tbravo\r\ncharlie"), equalTo(true))
    }

    @Test
    fun `rejects a text change containing a control code`() {
        assertThat(MapViewerTextInputContent.isAccepted("Alpha\u0016bravo"), equalTo(false))
    }
}
