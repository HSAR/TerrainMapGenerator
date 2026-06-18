package io.hsar.mapgenerator.commandgui.korge.launch

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerLaunchLayoutTest {

    @Test
    fun `launchpad panel is centered`() {
        val layout = MapViewerLaunchpadLayoutBuilder.build(1600, 900)

        assertThat(layout.panelBounds.left, equalTo(590.0))
        assertThat(layout.panelBounds.top, equalTo(290.0))
    }

    @Test
    fun `new operation layout places layers left and units right`() {
        val layout = MapViewerNewOperationLayoutBuilder.build(1600, 900)

        assertThat(layout.mapPanelBounds.left < layout.unitsPanelBounds.left, equalTo(true))
        assertThat(layout.titleBounds.left, equalTo(layout.operationNameBounds.left))
        assertThat(layout.backButtonBounds.left < layout.launchButtonBounds.left, equalTo(true))
        assertThat(layout.launchButtonBounds.left > layout.operationNameBounds.right, equalTo(true))
    }
}
