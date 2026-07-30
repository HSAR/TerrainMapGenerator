package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CommandDesignTest {

    @Test
    fun `design tokens retain the original command UI palette`() {
        assertThat(CommandColours.Dark, equalTo(Color(0xFF120909)))
        assertThat(CommandColours.Main, equalTo(Color(0xFF403A3A)))
        assertThat(CommandColours.Light, equalTo(Color(0xFF706B6B)))
        assertThat(CommandColours.Backing, equalTo(Color(0xFF9F9D9C)))
        assertThat(CommandColours.White, equalTo(Color(0xFFFFFFFF)))
    }

    @Test
    fun `Compose application starts maximized`() {
        assertThat(COMPOSE_INITIAL_WINDOW_PLACEMENT, equalTo(WindowPlacement.Maximized))
    }
}
