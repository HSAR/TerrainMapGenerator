package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ContextMenuLayoutTest {
    @Test
    fun `menu rows use the same padded origin regardless of submenu state`() {
        assertThat(ContextMenuLayout.optionTop(0), equalTo(ContextMenuLayout.PADDING))
        assertThat(ContextMenuLayout.optionTop(1), closeTo(ContextMenuLayout.PADDING + ContextMenuLayout.ACTION_HEIGHT, 0.0))
        assertThat(ContextMenuLayout.menuHeight(2), equalTo((ContextMenuLayout.ACTION_HEIGHT * 2) + ContextMenuLayout.PADDING * 2))
    }
}
