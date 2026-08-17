package io.hsar.mapgenerator.commandgui.compose

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import org.junit.jupiter.api.Test

class ComposeFloatingDialogLayoutTest {
    @Test
    fun `retains drag distance accumulated while constrained at the minimum`() {
        val initial = 520f
        val minimum = 420f

        val constrained = clampedDialogDimensionAtPointer(initial, -150f, minimum)
        val recovered = clampedDialogDimensionAtPointer(initial, -90f, minimum)

        assertThat(constrained, equalTo(420f))
        assertThat(recovered, equalTo(430f))
    }

    @Test
    fun `resize handle sits outside the dialog corner with the shared bottom right bracket`() {
        // Arrange
        val layout = composeFloatingDialogResizeHandleLayout(460f, 540f)

        // Act
        val bracket = composeFloatingDialogResizeBracket()

        // Assert
        assertThat(layout.left, equalTo(440f))
        assertThat(layout.top, equalTo(520f))
        assertThat(layout.size, equalTo(24f))
        assertThat(bracket.corner, equalTo(MapSelectionCorner.BOTTOM_RIGHT))
        assertThat(bracket.points.map { it.x to it.y }, equalTo(listOf(
            24.0 to 24.0, 24.0 to 6.0, 19.0 to 6.0, 19.0 to 19.0, 6.0 to 19.0, 6.0 to 24.0,
        )))
    }
}
