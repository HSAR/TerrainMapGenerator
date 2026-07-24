package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.korge.view.Container
import korlibs.korge.view.Graphics
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test

class ContextMenuManagerTest {
    @Test
    fun `submenu indicator top matches row content inset`() {
        assertThat(contextMenuSubmenuIndicatorTop(), equalTo(ContextMenuLayout.OPTION_VERTICAL_INSET))
    }

    @Test
    fun `submenu indicator renders a chevron graphic`() {
        val parent = Container()

        renderContextMenuSubmenuIndicator(
            parent = parent,
            width = ContextMenuLayout.SUBMENU_ARROW_WIDTH,
            height = ContextMenuLayout.ACTION_HEIGHT - 4.0,
        )

        assertThat(parent.children.size, equalTo(1))
        assertThat(parent.children.single(), instanceOf(Container::class.java))
        assertThat((parent.children.single() as Container).children.any { it is Graphics }, equalTo(true))
    }
}
