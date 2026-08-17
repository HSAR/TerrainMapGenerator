package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ComposeUniversalListUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `compact row supports one trailing action and secondary actions`() = runComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            NordTheme {
                ComposeUniversalListRow(
                    item = ComposeUniversalListItem("Scout", "Fast escort"),
                    modifier = Modifier.testTag("action-row"),
                    trailingAction = ComposeUniversalListAction("PLACE") { actions += "place" },
                    contextActions = listOf(
                        ComposeUniversalListAction("Edit Unit Type") { actions += "edit" },
                        ComposeUniversalListAction("Delete Unit Type", destructive = true) { actions += "delete" },
                    ),
                )
            }
        }

        onNodeWithText("Scout").fetchSemanticsNode()
        onNodeWithText("Fast escort").fetchSemanticsNode()
        onNodeWithText("PLACE").performClick()
        onNodeWithText("Scout").performMouseInput { rightClick() }
        onNodeWithText("Edit Unit Type").performClick()

        assertThat(actions, equalTo(listOf("place", "edit")))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `row height remains caller controlled for KorGE list variants`() = runComposeUiTest {
        setContent {
            NordTheme {
                ComposeUniversalListRow(
                    item = ComposeUniversalListItem("Note"),
                    modifier = Modifier.testTag("tall-row"),
                    height = 62.dp,
                )
            }
        }

        onNodeWithTag("tall-row").assertHeightIsEqualTo(62.dp)
    }
}
