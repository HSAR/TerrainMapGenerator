package io.hsar.mapgenerator.commandgui.korge.launch

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerLaunchLayoutTest {

    @Test
    fun `launchpad panel is centered`() {
        val layout = MapViewerLaunchpadLayoutBuilder.build(1600, 900)

        assertThat(layout.homePanelBounds.left, equalTo(590.0))
        assertThat(layout.homePanelBounds.top, equalTo(290.0))
        assertThat(layout.headerBounds.left, equalTo(530.0))
    }

    @Test
    fun `save browser expands vertically`() {
        val layout = MapViewerLaunchpadLayoutBuilder.build(1600, 900)

        assertThat(layout.savePanelBounds.height > layout.homePanelBounds.height, equalTo(true))
        assertThat(layout.saveListBounds.height > 0.0, equalTo(true))
    }

    @Test
    fun `new operation layout places layers left and units right`() {
        val layout = MapViewerNewOperationLayoutBuilder.build(1600, 900)

        assertThat(layout.mapPanelBounds.left < layout.unitsPanelBounds.left, equalTo(true))
        assertThat(layout.screenHeaderBounds.left, equalTo(280.0))
        assertThat(layout.titleBounds.left, equalTo(280.0))
        assertThat(layout.operationNameBounds.left < layout.mapPanelBounds.right, equalTo(true))
        assertThat(layout.backButtonBounds.left < layout.launchButtonBounds.left, equalTo(true))
        assertThat(layout.launchButtonBounds.left > layout.unitsPanelBounds.left, equalTo(true))
        assertThat(layout.launchButtonBounds.top, equalTo(132.0))
    }
}
