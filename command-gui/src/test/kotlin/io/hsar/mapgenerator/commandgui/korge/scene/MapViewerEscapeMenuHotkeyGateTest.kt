package io.hsar.mapgenerator.commandgui.korge.scene

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerEscapeMenuHotkeyGateTest {
    @Test
    fun `escape menu hotkey ignores open dialogs`() {
        assertThat(
            MapViewerEscapeMenuHotkeyGate.canToggle(
                sidebarIsUnitDragging = false,
                unitIsDragging = false,
                drawingIsDragging = false,
                selectionIsDragging = false,
                anyDialogOpen = true,
            ),
            equalTo(true),
        )
    }

    @Test
    fun `escape menu hotkey still respects drag blockers`() {
        assertThat(
            MapViewerEscapeMenuHotkeyGate.canToggle(
                sidebarIsUnitDragging = false,
                unitIsDragging = false,
                drawingIsDragging = true,
                selectionIsDragging = false,
            ),
            equalTo(false),
        )
    }
}
