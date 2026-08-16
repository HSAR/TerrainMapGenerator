package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundSpec
import org.junit.jupiter.api.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo

class ComposeLaunchpadParityUiTest {
    @Test
    fun `launch background world extent does not double count display density`() {
        val worldViewportWidth = launchBackgroundWorldPixels(logicalDp = 800f)

        val sourceWidth = MapViewerLaunchBackgroundSpec.forViewport(worldViewportWidth, 600, seed = 73L).textureWidth

        assertThat(worldViewportWidth, equalTo(800))
        assertThat(sourceWidth, equalTo(1200))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `Nord theme defaults ordinary content to white`() = runComposeUiTest {
        var inherited: Color? = null
        setContent { NordTheme { inherited = LocalContentColor.current } }

        waitForIdle()

        assertThat(inherited, equalTo(CommandColours.White))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `background biome box expands in place without changing its title`() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { ComposeLaunchBackgroundState(73L, scope) }
            NordTheme {
                ComposeLaunchBackgroundPanel(
                    state = state,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    onRegenerate = {},
                    onClose = { expanded = false },
                )
            }
        }

        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()

        assertThat(expanded, equalTo(true))
        onNodeWithText("REGENERATE").performClick()
        assertThat(onAllNodesWithText("BACKGROUND:", substring = true).fetchSemanticsNodes().isEmpty(), equalTo(true))
        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()
        assertThat(expanded, equalTo(false))
        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()
        onNodeWithText("CLOSE").performClick()
        assertThat(expanded, equalTo(false))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `launchpad uses legacy header panel and button dimensions`() = runComposeUiTest {
        setContent {
            NordTheme {
                Box(Modifier.size(800.dp, 600.dp)) {
                    ComposeMainLaunchpad({}, {}, {}, backgroundEnabled = false)
                }
            }
        }

        onNodeWithTag("command-launch-header")
            .assertWidthIsEqualTo(540.dp)
            .assertHeightIsEqualTo(78.dp)
        onNodeWithTag("launchpad-panel")
            .assertWidthIsEqualTo(420.dp)
            .assertHeightIsEqualTo(320.dp)
        (0..2).forEach { index ->
            onNodeWithTag("launchpad-button-$index")
                .assertWidthIsEqualTo(300.dp)
                .assertHeightIsEqualTo(44.dp)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `temporary notice uses legacy font metrics and padding`() = runComposeUiTest {
        val state = ComposeTransientMessageHostState().apply { show("Operation saved") }
        setContent {
            NordTheme {
                Box(Modifier.size(800.dp, 600.dp)) {
                    ComposeTransientMessageHost(state)
                }
            }
        }

        onNodeWithTag("transient-notice-0").assertHeightIsEqualTo(43.dp)
    }
}
