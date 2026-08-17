package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerCommandItem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ContextMenuChromeUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `context menu keeps object heading over KorGE sized boxed rows`() = runComposeUiTest {
        val selected = mutableListOf<String>()
        val items = listOf(
            MapViewerCommandItem("Edit Unit"),
            MapViewerCommandItem(
                "Notes",
                children = listOf(MapViewerCommandItem("Contact report")),
            ),
        )
        setContent {
            NordTheme {
                ComposeCommandMenuPanel(
                    title = "HMS Example",
                    items = items,
                    focusedIndex = 0,
                    onHovered = {},
                    onSelected = { selected += it.label },
                    width = 220.0,
                    modifier = Modifier,
                    onPointerExited = {},
                )
            }
        }

        onNodeWithTag("command-menu-title", useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        onNodeWithTag("command-menu-row-0", useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        onNodeWithTag("command-menu-option-0", useUnmergedTree = true)
            .assertWidthIsEqualTo(212.dp)
            .assertHeightIsEqualTo(40.dp)
            .performClick()
        onNodeWithText("HMS Example").fetchSemanticsNode()
        onNodeWithText("Edit Unit").fetchSemanticsNode()
        assertThat(onAllNodesWithText("EDIT UNIT").fetchSemanticsNodes().isEmpty(), equalTo(true))
        onNodeWithTag("command-menu-submenu-arrow", useUnmergedTree = true).fetchSemanticsNode()
        assertThat(selected, equalTo(listOf("Edit Unit")))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `disabled context row uses warning icon and exposes its reason`() = runComposeUiTest {
        var selected = false
        setContent {
            NordTheme {
                ComposeCommandMenuPanel(
                    title = null,
                    items = listOf(
                        MapViewerCommandItem(
                            label = "Delete Unit",
                            enabled = false,
                            disabledReason = "History preview is read-only.",
                            destructive = true,
                        ),
                    ),
                    focusedIndex = 0,
                    onHovered = {},
                    onSelected = { selected = true },
                    width = 220.0,
                    modifier = Modifier,
                    onPointerExited = {},
                )
            }
        }

        onNodeWithTag("command-menu-warning", useUnmergedTree = true).fetchSemanticsNode()
        onNodeWithContentDescription("Delete Unit. Disabled: History preview is read-only.").performClick()
        assertThat(selected, equalTo(false))
    }
}
