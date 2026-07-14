package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogSpacing
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
    fun `launch dialogs share the persistent shell header`() {
        val shell = MapViewerLaunchShellLayoutBuilder.build(1600)
        val launchpad = MapViewerLaunchpadLayoutBuilder.build(1600, 900)
        val newOperation = MapViewerNewOperationLayoutBuilder.build(1600, 900)

        assertThat(launchpad.headerBounds, equalTo(shell.headerBounds))
        assertThat(newOperation.headerBounds, equalTo(shell.headerBounds))
    }

    @Test
    fun `save browser expands vertically`() {
        val layout = MapViewerLaunchpadLayoutBuilder.build(1600, 900)

        assertThat(layout.savePanelBounds.height > layout.homePanelBounds.height, equalTo(true))
        assertThat(layout.saveListBounds.height > 0.0, equalTo(true))
    }

    @Test
    fun `launchpad dialogs centre for the current viewport`() {
        val layout = MapViewerLaunchpadLayoutBuilder.build(1920, 1080)

        assertThat(layout.homePanelBounds.left + layout.homePanelBounds.width / 2, equalTo(960.0))
        assertThat(layout.savePanelBounds.left + layout.savePanelBounds.width / 2, equalTo(960.0))
    }

    @Test
    fun `new operation layout places layers left and units right`() {
        val layout = MapViewerNewOperationLayoutBuilder.build(1600, 900)

        assertThat(layout.mapPanelBounds.left < layout.unitsPanelBounds.left, equalTo(true))
        assertThat(layout.screenHeaderBounds.left, equalTo(100.0))
        assertThat(layout.titleBounds.left, equalTo(100.0))
        assertThat(layout.operationNameBounds.left < layout.mapPanelBounds.right, equalTo(true))
        assertThat(layout.backButtonBounds.left < layout.launchButtonBounds.left, equalTo(true))
        assertThat(layout.launchButtonBounds.left > layout.unitsPanelBounds.left, equalTo(true))
        assertThat(layout.launchButtonBounds.top, equalTo(132.0))
        assertThat(layout.unitsPanelBounds.left - layout.mapPanelBounds.right, equalTo(32.0))
        assertThat(layout.panelFocusButtonBounds.width < layout.panelFocusButtonBounds.height, equalTo(true))
        assertThat(layout.panelFocusButtonBounds.top, equalTo(layout.mapPanelBounds.top))
        assertThat(layout.panelFocusButtonBounds.height, equalTo(layout.mapPanelBounds.height))
    }

    @Test
    fun `unit types focus expands from the right to the details panel width`() {
        val detailsLayout = MapViewerNewOperationLayoutBuilder.build(1600, 900)
        val unitsLayout = MapViewerNewOperationLayoutBuilder.build(
            1600,
            900,
            MapViewerNewOperationPanelFocus.UNIT_TYPES,
        )

        assertThat(unitsLayout.unitsPanelBounds.width, equalTo(detailsLayout.mapPanelBounds.width))
        assertThat(unitsLayout.unitsPanelBounds.right, equalTo(detailsLayout.unitsPanelBounds.right))
        assertThat(unitsLayout.unitsPanelBounds.left < detailsLayout.unitsPanelBounds.left, equalTo(true))
        assertThat(unitsLayout.compactDetailsPanelBounds.width, equalTo(300.0))
        assertThat(unitsLayout.panelFocusButtonBounds.height, equalTo(unitsLayout.unitsPanelBounds.height))
    }

    @Test
    fun `new operation coordinate row aligns dropdown and input fields`() {
        val labelTop = 144.0
        val inputTop = MapViewerDialogSpacing.inputTop(labelTop)
        val layout = MapViewerNewOperationCoordinateRowLayoutBuilder.build(
            panelWidth = 760.0,
            labelTop = labelTop,
            inputHeight = 40.0,
            leftPadding = 24.0,
        )

        assertThat(layout.fields.map { it.labelBounds.top }.toSet(), equalTo(setOf(labelTop)))
        assertThat(layout.fields.map { it.inputBounds.top }.toSet(), equalTo(setOf(inputTop)))
        assertThat(layout.fields.map { it.inputBounds.height }.toSet(), equalTo(setOf(40.0)))
    }
}
