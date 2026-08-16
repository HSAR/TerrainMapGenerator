package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    @Test
    fun `command buttons retain the legacy tool rail label size`() {
        assertThat(COMMAND_BUTTON_FONT_SIZE, equalTo(22.sp))
    }

    @Test
    fun `command buttons retain the legacy tool rail minimum dimensions`() {
        assertThat(COMMAND_BUTTON_MIN_WIDTH, equalTo(120.dp))
        assertThat(COMMAND_BUTTON_MIN_HEIGHT, equalTo(40.dp))
        assertThat(COMMAND_BUTTON_HORIZONTAL_PADDING, equalTo(12.dp))
    }

    @Test
    fun `focused buttons use a white rather than blue frame`() {
        assertThat(COMMAND_BUTTON_FOCUS_COLOUR, equalTo(CommandColours.White))
    }
}
