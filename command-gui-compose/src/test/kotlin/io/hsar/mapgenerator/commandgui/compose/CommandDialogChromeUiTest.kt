package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CommandDialogChromeUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `floating dialog uses KorGE title bar geometry and icon controls`() = runComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            NordTheme {
                CommandFloatingPanel(
                    title = "Edit Unit",
                    onClose = { actions += "close" },
                    minimized = false,
                    pinned = true,
                    onMinimize = { actions += "minimize" },
                    onPin = { actions += "pin" },
                    modifier = Modifier.size(400.dp, 300.dp),
                ) { Text("Dialog body") }
            }
        }

        onNodeWithTag("command-dialog-title-bar")
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(42.dp)
        listOf("minimize", "pin", "close").forEach { control ->
            onNodeWithTag("command-dialog-$control")
                .assertWidthIsEqualTo(42.dp)
                .assertHeightIsEqualTo(42.dp)
                .performClick()
        }
        onNodeWithText("EDIT UNIT").fetchSemanticsNode()
        assertThat(actions, equalTo(listOf("minimize", "pin", "close")))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `minimized dialog advertises the legacy restore control`() = runComposeUiTest {
        setContent {
            NordTheme {
                CommandFloatingPanel(
                    title = "Notes",
                    onClose = {},
                    minimized = true,
                    pinned = false,
                    modifier = Modifier.size(400.dp, 42.dp),
                ) {}
            }
        }

        onNodeWithContentDescription("Restore dialog").fetchSemanticsNode()
        onNodeWithContentDescription("Pin dialog").fetchSemanticsNode()
        onNodeWithContentDescription("Close dialog").fetchSemanticsNode()
    }
}
