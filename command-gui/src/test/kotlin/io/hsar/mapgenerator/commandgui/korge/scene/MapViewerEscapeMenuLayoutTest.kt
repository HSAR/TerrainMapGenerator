package io.hsar.mapgenerator.commandgui.korge.scene

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerEscapeMenuLayoutTest {
    @Test
    fun `escape menu buttons share a width and stay separated`() {
        val layout = MapViewerEscapeMenuLayoutBuilder.build(1920, 1080)

        assertThat(layout.returnButtonBounds.width, equalTo(layout.closeButtonBounds.width))
        assertThat(layout.returnButtonBounds.width, equalTo(layout.exitButtonBounds.width))
        assertThat(
            layout.exitButtonBounds.top - (layout.returnButtonBounds.top + layout.returnButtonBounds.height),
            greaterThan(0.0),
        )
        assertThat(layout.panelBounds.left, closeTo(790.0, 0.1))
        assertThat(layout.panelBounds.top, closeTo(418.0, 0.1))
    }
}
