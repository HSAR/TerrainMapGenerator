package io.hsar.mapgenerator.commandgui.compose

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
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
}
